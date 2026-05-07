package org.octri.fhir_sandbox_backend.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.JWTProcessor;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;

@ExtendWith(MockitoExtension.class)
class JwtAuthorizationInterceptorTest {

	private static final String VALID_TOKEN = "valid.jwt.token";
	private static final String VALID_HEADER = "Bearer " + VALID_TOKEN;
	private static final String EXAMPLE_AUDIENCE = "http://localhost:8001/fhir/3d2055de-ffd0-4a95-a587-1b54e3d19946/";

	@Mock
	private JWTProcessor<SecurityContext> jwtProcessor;

	@Mock
	private RequestDetails requestDetails;

	private JwtAuthorizationInterceptor interceptor;

	@BeforeEach
	void setUp() {
		interceptor = new JwtAuthorizationInterceptor(jwtProcessor);
	}

	@Test
	void validTokenReturnsAllowAllRules() throws BadJOSEException, JOSEException, ParseException {
		var claims = new JWTClaimsSet.Builder()
				.subject("test-user")
				.issueTime(new Date())
				.expirationTime(new Date(System.currentTimeMillis() + 3600000))
				.jwtID("test-jwt-id")
				.audience(EXAMPLE_AUDIENCE)
				.build();
		when(requestDetails.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(VALID_HEADER);
		when(requestDetails.getCompleteUrl())
				.thenReturn(EXAMPLE_AUDIENCE + "Patient/eFTHaVbQzCEwOEE97maN2MC2jJi-r8nnkhRh.umMUlz03");
		when(jwtProcessor.process(VALID_TOKEN, null)).thenReturn(claims);

		List<IAuthRule> rules = interceptor.buildRuleList(requestDetails);

		assertThat(rules).isNotEmpty();
	}

	@Test
	void missingAuthHeaderThrowsAuthenticationException() {
		when(requestDetails.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

		assertThrows(AuthenticationException.class, () -> interceptor.buildRuleList(requestDetails),
				"Throws AuthenticationException when Authorization header is missing");
	}

	@Test
	void missingBearerPrefixThrowsAuthenticationException() {
		when(requestDetails.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic abc123");

		assertThrows(AuthenticationException.class, () -> interceptor.buildRuleList(requestDetails),
				"Throws AuthenticationException if the Authorization scheme is not Bearer");
	}

	@Test
	void emptyBearerTokenThrowsAuthenticationException() {
		when(requestDetails.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer ");

		assertThrows(AuthenticationException.class, () -> interceptor.buildRuleList(requestDetails),
				"Throws AuthenticationException if the bearer token is empty");
	}

	@Test
	void blankBearerTokenThrowsAuthenticationException() {
		when(requestDetails.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer   ");

		assertThrows(AuthenticationException.class, () -> interceptor.buildRuleList(requestDetails),
				"Throws AuthenticationException if the bearer token is blank");
	}

	@Test
	void invalidTokenBadJoseExceptionThrowsAuthenticationException()
			throws BadJOSEException, JOSEException, ParseException {
		when(requestDetails.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(VALID_HEADER);
		when(jwtProcessor.process(VALID_TOKEN, null)).thenThrow(new BadJOSEException("bad jose"));

		assertThrows(AuthenticationException.class, () -> interceptor.buildRuleList(requestDetails),
				"Throws AuthenticationException for invalid tokens (BADJOSEException");
	}

	@Test
	void invalidTokenJoseExceptionThrowsAuthenticationException()
			throws BadJOSEException, JOSEException, ParseException {
		when(requestDetails.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(VALID_HEADER);
		when(jwtProcessor.process(VALID_TOKEN, null)).thenThrow(new JOSEException("jose error"));

		assertThrows(AuthenticationException.class, () -> interceptor.buildRuleList(requestDetails),
				"Throws AuthenticationException for invalid tokens (JOSEException)");
	}

	@Test
	void invalidTokenParseExceptionThrowsAuthenticationException()
			throws BadJOSEException, JOSEException, ParseException {
		when(requestDetails.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(VALID_HEADER);
		when(jwtProcessor.process(VALID_TOKEN, null)).thenThrow(new ParseException("parse error", 0));

		assertThrows(AuthenticationException.class, () -> interceptor.buildRuleList(requestDetails),
				"Throws AuthenticationException for invalid tokens (ParseException)");
	}

	@Test
	void audienceMismatchThrowsForbiddenOperationException() throws ParseException, BadJOSEException, JOSEException {
		var wrongAudienceUrl = "http://localhost:8001/fhir/89ce5978-acc8-46c8-8e12-52694a25b0d3/Patient/abcd1234";
		var claims = new JWTClaimsSet.Builder()
				.subject("test-user")
				.issueTime(new Date())
				.expirationTime(new Date(System.currentTimeMillis() + 3600000))
				.jwtID("test-jwt-id")
				.audience(EXAMPLE_AUDIENCE)
				.build();
		when(requestDetails.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(VALID_HEADER);
		when(requestDetails.getCompleteUrl()).thenReturn(wrongAudienceUrl);
		when(jwtProcessor.process(VALID_TOKEN, null)).thenReturn(claims);

		assertThrows(ForbiddenOperationException.class, () -> interceptor.buildRuleList(requestDetails),
				"Throws ForbiddenOperationException when token audience does not match the sandbox");
	}

}
