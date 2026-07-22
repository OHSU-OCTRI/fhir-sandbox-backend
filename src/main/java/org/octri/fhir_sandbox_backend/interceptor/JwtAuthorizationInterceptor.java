package org.octri.fhir_sandbox_backend.interceptor;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.IdType;
import org.octri.fhir_sandbox_backend.auth.AuthUtils;
import org.octri.fhir_sandbox_backend.auth.AuthUtils.GroupedScopes;
import org.octri.fhir_sandbox_backend.auth.InvalidScopeException;
import org.octri.fhir_sandbox_backend.auth.LaunchContext;
import org.octri.fhir_sandbox_backend.auth.SmartScopeConverter;
import org.octri.fhir_sandbox_backend.auth.SmartScopeRuleBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.JWTProcessor;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;

/**
 * Custom interceptor that uses JSON web token (JWT) bearer tokens to authorize requests.
 */
@Interceptor
public class JwtAuthorizationInterceptor extends AuthorizationInterceptor {

	private static final int AUTHN_ERROR_CODE = 9991;
	private static final String AUTHN_ERROR_MSG = "Missing or invalid Authorization header";
	private static final int INVALID_TOKEN_CODE = 9992;
	private static final String INVALID_TOKEN_MSG = "Invalid or expired Bearer token";
	private static final int AUDIENCE_ERROR_CODE = 9993;
	private static final String AUDIENCE_ERROR_MSG = "Sandbox access not allowed by Bearer token";
	private static final int INVALID_SCOPE_CODE = 9994;
	private static final String INVALID_SCOPE_MSG = "Bearer token contains invalid scope";
	private static final int MISSING_CONTEXT_CODE = 9995;
	private static final String MISSING_CONTEXT_MSG = "Launch context missing or invalid";
	private static final int MISSING_PATIENT_ID_CODE = 9996;
	private static final String MISSING_PATIENT_ID_MSG = "Patient scope was provided, but no patient ID found in launch context";
	private static final int INVALID_AUDIENCE_CODE = 9997;
	private static final String INVALID_AUDIENCE_MSG = "Bearer token contains invalid audience: ";

	private static final String AUTH_HEADER_PREFIX = "Bearer ";
	private static final String SCOPE_CLAIM = "scope";
	private static final String LAUNCH_CONTEXT_CLAIM = "launchContext";
	private static final Pattern AUDIENCE_PATTERN = Pattern
			.compile("^https?://.+/fhir/(DEFAULT|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/$");

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final JWTProcessor<SecurityContext> jwtProcessor;
	private final ObjectMapper objectMapper;

	public JwtAuthorizationInterceptor(JWTProcessor<SecurityContext> jwtProcessor, ObjectMapper objectMapper) {
		this.jwtProcessor = jwtProcessor;
		this.objectMapper = objectMapper;
	}

	@Override
	public List<IAuthRule> buildRuleList(RequestDetails requestDetails) {
		log.debug("Authorization interceptor called");

		// Always allow capability statement requests
		if (isMetadataRequest(requestDetails)) {
			return new RuleBuilder().allow("allow metadata rule").metadata().build();
		}

		// Basic token validity checks: Presence, format, signature
		var authHeader = requestDetails.getHeader(HttpHeaders.AUTHORIZATION);
		var token = getTokenValueOrThrow(authHeader);
		var claims = getTokenClaimsOrThrow(token);

		// Verify that the token audience matches the FHIR server URL
		verifyTokenAudienceOrThrow(requestDetails.getFhirServerBase(), claims.getAudience());

		// Extract, parse, and validate the token's scopes
		var groupedScopes = extractScopes(claims);
		log.debug("Extracted scopes: {}", groupedScopes);

		// Allow all operations if the partition is DEFAULT and scope is system/*.cruds
		if (isPrivilegedRequest(requestDetails, groupedScopes)) {
			return new RuleBuilder().allowAll("privileged request rule").build();
		}

		// Convert token scopes to auth rules
		var scopeRules = buildScopeRules(groupedScopes, claims);
		return !scopeRules.isEmpty() ? scopeRules : new RuleBuilder().denyAll("deny all rule").build();
	}

	private boolean isMetadataRequest(RequestDetails requestDetails) {
		return "metadata".equals(requestDetails.getOperation());
	}

	private boolean isPrivilegedRequest(RequestDetails requestDetails, GroupedScopes groupedScopes) {
		var isDefaultPartition = "DEFAULT".equals(requestDetails.getTenantId());
		var otherScopesEmpty = groupedScopes.patientScopes().isEmpty() && groupedScopes.userScopes().isEmpty();
		var hasFullSystemScope = groupedScopes.systemScopes().stream()
				.anyMatch(scope -> scope.allowsAllResources() && scope.allowsAllPermissions());

		return isDefaultPartition && otherScopesEmpty && hasFullSystemScope;
	}

	private String getTokenValueOrThrow(String headerValue) {
		log.debug("Auth header value: {}", headerValue);
		if (StringUtils.isBlank(headerValue) || !headerValue.startsWith(AUTH_HEADER_PREFIX)) {
			throw new AuthenticationException(Msg.code(AUTHN_ERROR_CODE) + AUTHN_ERROR_MSG);
		}

		var token = headerValue.substring(AUTH_HEADER_PREFIX.length());
		log.debug("Extracted token: {}", token);
		if (StringUtils.isBlank(token)) {
			throw new AuthenticationException(Msg.code(AUTHN_ERROR_CODE) + AUTHN_ERROR_MSG);
		}

		return token;
	}

	private JWTClaimsSet getTokenClaimsOrThrow(String token) {
		log.debug("Token value: {}", token);

		JWTClaimsSet claimsSet;
		try {
			claimsSet = jwtProcessor.process(token, null);
		} catch (BadJOSEException | JOSEException | ParseException e) {
			log.warn("Error processing JWT token: {}", e.getMessage(), e);
			throw new AuthenticationException(Msg.code(INVALID_TOKEN_CODE) + INVALID_TOKEN_MSG, e);
		}

		log.debug("Token claims: {}", claimsSet);
		return claimsSet;
	}

	private void verifyTokenAudienceOrThrow(String fhirServerUrl, List<String> audienceList) {
		Assert.isTrue(StringUtils.isNotEmpty(fhirServerUrl), "Server URL is required");
		Assert.notNull(audienceList, "Token audience list may not be null");

		for (String audience : audienceList) {
			if (!AUDIENCE_PATTERN.matcher(audience).matches()) {
				throw new AuthenticationException(
						Msg.code(INVALID_AUDIENCE_CODE) + INVALID_AUDIENCE_MSG + audience);
			}
		}

		var hasMatch = audienceList.stream()
				.anyMatch(audience -> fhirServerUrl.equals(StringUtils.stripEnd(audience, "/")));
		if (!hasMatch) {
			throw new ForbiddenOperationException(Msg.code(AUDIENCE_ERROR_CODE) + AUDIENCE_ERROR_MSG);
		}
	}

	private GroupedScopes extractScopes(JWTClaimsSet claimSet) {
		try {
			var scopeClaim = claimSet.getStringListClaim(SCOPE_CLAIM);
			if (scopeClaim == null) {
				throw new InvalidScopeException("No scope claim in bearer token");
			}
			var scopes = SmartScopeConverter.convertScopes(scopeClaim);
			return AuthUtils.groupScopesByContext(scopes);
		} catch (InvalidScopeException | ParseException e) {
			log.warn("Could not extract and convert scopes: {}", e.getMessage());
			throw new AuthenticationException(Msg.code(INVALID_SCOPE_CODE) + INVALID_SCOPE_MSG, e);
		}
	}

	private LaunchContext getLaunchContext(JWTClaimsSet claimSet) {
		try {
			var launchContextClaim = claimSet.getJSONObjectClaim(LAUNCH_CONTEXT_CLAIM);
			return objectMapper.convertValue(launchContextClaim, LaunchContext.class);
		} catch (ParseException | IllegalArgumentException e) {
			log.warn("Could not extract launch context from claim set: {}", e.getMessage());
			throw new AuthenticationException(Msg.code(MISSING_CONTEXT_CODE) + MISSING_CONTEXT_MSG, e);
		}
	}

	private List<IAuthRule> buildScopeRules(GroupedScopes groupedScopes, JWTClaimsSet claims) {
		var ruleList = new ArrayList<IAuthRule>();

		if (!groupedScopes.patientScopes().isEmpty()) {
			var launchContext = getLaunchContext(claims);
			if (launchContext == null || launchContext.getPatient() == null) {
				throw new AuthenticationException(Msg.code(MISSING_PATIENT_ID_CODE) + MISSING_PATIENT_ID_MSG);
			}

			var patientId = new IdType("Patient", launchContext.getPatient());
			var patientRuleBuilder = new SmartScopeRuleBuilder(groupedScopes.patientScopes()).withPatientId(patientId);
			ruleList.addAll(patientRuleBuilder.build());
		}

		if (!groupedScopes.userScopes().isEmpty()) {
			ruleList.addAll(new SmartScopeRuleBuilder(groupedScopes.userScopes()).build());
		}

		if (!groupedScopes.systemScopes().isEmpty()) {
			ruleList.addAll(new SmartScopeRuleBuilder(groupedScopes.systemScopes()).build());
		}

		if (!ruleList.isEmpty()) {
			// Explicitly allow any bundle or batch transaction allowed by the other scopes
			var transactionRule = new RuleBuilder().allow("transaction rule").transaction().withAnyOperation()
					.andApplyNormalRules().build();
			ruleList.addAll(0, transactionRule);
		}

		var hasFhirUserScope = groupedScopes.nonResourceScopes().stream()
					.anyMatch(scope -> "fhirUser".equals(scope.getRawScope()));
		if (hasFhirUserScope) {
			// Explicitly allow reading the fhirUser resource
			var launchContext = getLaunchContext(claims);
			if (launchContext != null && launchContext.getFhirUser() != null) {
				var fhirUser = launchContext.getFhirUser();
				var fhirUserRule = new RuleBuilder().allow("fhirUser rule").read().instance(fhirUser).build();
				ruleList.addAll(fhirUserRule);
			}
		}

		return ruleList;
	}

}
