package org.octri.fhir_sandbox_backend.interceptor;

import java.text.ParseException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.JWTProcessor;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;

/**
 * Custom interceptor that uses JSON web token (JWT) bearer tokens to authorize requests.
 */
@Interceptor
public class JwtAuthorizationInterceptor extends AuthorizationInterceptor {

	// Magic number from the documentation
	private static final int AUTH_ERROR_CODE = 644;
	private static final String AUTH_ERROR_MSG = "Missing or invalid authorization header";

	private static final String AUTH_HEADER_PREFIX = "Bearer ";

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final JWTProcessor<SecurityContext> jwtProcessor;

	public JwtAuthorizationInterceptor(JWTProcessor<SecurityContext> jwtProcessor) {
		this.jwtProcessor = jwtProcessor;
	}

	@Override
	public List<IAuthRule> buildRuleList(RequestDetails requestDetails) {
		log.info("Authorization interceptor called");
		var authHeader = requestDetails.getHeader(HttpHeaders.AUTHORIZATION);
		var token = getTokenValue(authHeader);
		if (token == null) {
			throw new AuthenticationException(Msg.code(AUTH_ERROR_CODE) + AUTH_ERROR_MSG);
		}

		var claims = getTokenClaims(token);
		log.debug("Token claims: {}", claims);
		if (claims == null) {
			throw new AuthenticationException(Msg.code(AUTH_ERROR_CODE) + AUTH_ERROR_MSG);
		}

		return new RuleBuilder().allowAll().build();
	}

	private String getTokenValue(String headerValue) {
		log.debug("Auth header value: {}", headerValue);
		if (StringUtils.isBlank(headerValue) || !headerValue.startsWith(AUTH_HEADER_PREFIX)) {
			return null;
		}

		var token = headerValue.substring(AUTH_HEADER_PREFIX.length());
		log.debug("Extracted token: {}", token);
		return StringUtils.isBlank(token) ? null : token;
	}

	private JWTClaimsSet getTokenClaims(String token) {
		log.debug("Token value: {}", token);

		JWTClaimsSet claimsSet;
		try {
			claimsSet = jwtProcessor.process(token, null);
		} catch (BadJOSEException | JOSEException | ParseException e) {
			log.error("Error processing JWT token: {}", e.getMessage(), e);
			return null;
		}

		return claimsSet;
	}
}
