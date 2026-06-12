package org.octri.fhir_sandbox_backend.interceptor;

import java.text.ParseException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

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

	private static final String AUTH_HEADER_PREFIX = "Bearer ";

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final JWTProcessor<SecurityContext> jwtProcessor;

	public JwtAuthorizationInterceptor(JWTProcessor<SecurityContext> jwtProcessor) {
		this.jwtProcessor = jwtProcessor;
	}

	@Override
	public List<IAuthRule> buildRuleList(RequestDetails requestDetails) {
		log.info("Authorization interceptor called");

		if ("metadata".equals(requestDetails.getOperation())) {
			return new RuleBuilder().allow("allow metadata rule").metadata().build();
		}

		var authHeader = requestDetails.getHeader(HttpHeaders.AUTHORIZATION);
		var token = getTokenValueOrThrow(authHeader);
		var claims = getTokenClaimsOrThrow(token);
		verifyTokenAudienceOrThrow(requestDetails.getCompleteUrl(), claims.getAudience());

		return new RuleBuilder().allowAll().build();
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

	private void verifyTokenAudienceOrThrow(String requestUrl, List<String> tokenAudience) {
		Assert.isTrue(StringUtils.isNotEmpty(requestUrl), "Request URL is required");
		Assert.notNull(tokenAudience, "Token audience may not be null");
		log.debug("Request URL: {}", requestUrl);
		log.debug("Token audience: {}", tokenAudience);

		var hasMatch = tokenAudience.stream().anyMatch(audience -> requestUrl.indexOf(audience) == 0);
		if (!hasMatch) {
			throw new ForbiddenOperationException(Msg.code(AUDIENCE_ERROR_CODE) + AUDIENCE_ERROR_MSG);
		}
	}

}
