package org.octri.fhir_sandbox_backend.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.JWTProcessor;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;

@ExtendWith(MockitoExtension.class)
class JwtAuthorizationInterceptorTest {

	private static final String MOCK_TOKEN = "valid.jwt.token";
	private static final String VALID_HEADER = "Bearer " + MOCK_TOKEN;
	private static final String EXAMPLE_AUDIENCE = "http://localhost:8001/fhir/3d2055de-ffd0-4a95-a587-1b54e3d19946/";
	private static final String PATIENT_PATH = "Patient/eFTHaVbQzCEwOEE97maN2MC2jJi-r8nnkhRh.umMUlz03";
	private static final String DEFAULT_URL = EXAMPLE_AUDIENCE + PATIENT_PATH;
	private static final Pattern BASE_URL_PATTERN = Pattern.compile(
			"^(https?://.+/fhir/(DEFAULT|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}))");

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Mock
	private JWTProcessor<SecurityContext> jwtProcessor;

	@Mock
	private RequestDetails requestDetails;

	private JwtAuthorizationInterceptor interceptor;

	@BeforeEach
	void setUp() {
		interceptor = new JwtAuthorizationInterceptor(jwtProcessor, objectMapper);
	}

	@Test
	void metadataOperationAllowedWithoutAuthentication() {
		when(requestDetails.getOperation()).thenReturn("metadata");

		var rules = interceptor.buildRuleList(requestDetails);
		assertEquals(1, rules.size(), "There should be one rule");
		assertEquals("allow metadata rule", rules.get(0).getName(), "The rule should have the expected name");
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
		when(jwtProcessor.process(MOCK_TOKEN, null)).thenThrow(new BadJOSEException("bad jose"));

		assertThrows(AuthenticationException.class, () -> interceptor.buildRuleList(requestDetails),
				"Throws AuthenticationException for invalid tokens (BADJOSEException");
	}

	@Test
	void invalidTokenJoseExceptionThrowsAuthenticationException()
			throws BadJOSEException, JOSEException, ParseException {
		when(requestDetails.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(VALID_HEADER);
		when(jwtProcessor.process(MOCK_TOKEN, null)).thenThrow(new JOSEException("jose error"));

		assertThrows(AuthenticationException.class, () -> interceptor.buildRuleList(requestDetails),
				"Throws AuthenticationException for invalid tokens (JOSEException)");
	}

	@Test
	void invalidTokenParseExceptionThrowsAuthenticationException()
			throws BadJOSEException, JOSEException, ParseException {
		when(requestDetails.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(VALID_HEADER);
		when(jwtProcessor.process(MOCK_TOKEN, null)).thenThrow(new ParseException("parse error", 0));

		assertThrows(AuthenticationException.class, () -> interceptor.buildRuleList(requestDetails),
				"Throws AuthenticationException for invalid tokens (ParseException)");
	}

	@Test
	void urlsMatchingAudienceWithoutSlashAreAllowed() throws ParseException, BadJOSEException, JOSEException {
		var noSlashSandboxUrl = "http://localhost:8001/fhir/3d2055de-ffd0-4a95-a587-1b54e3d19946";
		var claims = getDefaultClaims()
				.claim("scope", makeScopeClaim("system/*.*"))
				.build();
		mockRequestWithClaims(noSlashSandboxUrl, claims);

		assertDoesNotThrow(() -> interceptor.buildRuleList(requestDetails),
				"Audience check should allow access to the sandbox URL without the trailing slash, as in paginated responses");
	}

	@Test
	void urlsWithQueryStringMatchingAudienceAreAllowed() throws ParseException, BadJOSEException, JOSEException {
		var noSlashSandboxUrl = "http://localhost:8001/fhir/3d2055de-ffd0-4a95-a587-1b54e3d19946";
		var queryString = "?_getpages=ac766649-d62c-4e5b-a9ec-24037b358823&_getpagesoffset=20&_count=20&_pretty=true&_bundletype=searchset";
		var claims = getDefaultClaims()
				.claim("scope", makeScopeClaim("system/*.*"))
				.build();
		mockRequestWithClaims(noSlashSandboxUrl + queryString, claims);

		assertDoesNotThrow(() -> interceptor.buildRuleList(requestDetails),
				"Audience check should allow access to the sandbox URL if one of the audiences matches");
	}

	@Test
	void urlsMatchingOneOfMultipleAudiencesAreAllowed() throws ParseException, BadJOSEException, JOSEException {
		var secondAudience = "https://octridev.ohsu.edu/fhir/d676ccc9-eeb9-40c7-a573-a249fb6ee15d/";
		var audiences = List.of(EXAMPLE_AUDIENCE, secondAudience);
		var claims = getDefaultClaims()
				.audience(audiences)
				.claim("scope", makeScopeClaim("system/*.*"))
				.build();
		mockRequestWithClaims(secondAudience + PATIENT_PATH, claims);

		assertDoesNotThrow(() -> interceptor.buildRuleList(requestDetails),
				"Audience check should allow access to the sandbox URL if one of the audiences matches");
	}

	@Test
	void audienceMismatchThrowsForbiddenOperationException() throws ParseException, BadJOSEException, JOSEException {
		var wrongAudienceUrl = "http://localhost:8001/fhir/89ce5978-acc8-46c8-8e12-52694a25b0d3/Patient/abcd1234";
		var claims = getDefaultClaims().build();
		mockRequestWithClaims(wrongAudienceUrl, claims);

		assertThrows(ForbiddenOperationException.class, () -> interceptor.buildRuleList(requestDetails),
				"Throws ForbiddenOperationException when token audience does not match the sandbox");
	}

	@Test
	void blankAudienceThrowsAuthenticationException() throws ParseException, BadJOSEException, JOSEException {
		var wrongAudienceUrl = "http://localhost:8001/fhir/89ce5978-acc8-46c8-8e12-52694a25b0d3/Patient/abcd1234";
		var audiences = List.of("", EXAMPLE_AUDIENCE);
		var claims = getDefaultClaims()
				.audience(audiences)
				.build();
		mockRequestWithClaims(wrongAudienceUrl, claims);

		assertThrows(AuthenticationException.class, () -> interceptor.buildRuleList(requestDetails),
				"Throws AuthenticationException when blank audience is present");
	}

	@Test
	void malformedAudienceThrowsAuthenticationException() throws ParseException, BadJOSEException, JOSEException {
		// audience must have a trailing slash
		var malformedAudience = "http://localhost:8001/fhir/DEFAULT";
		var audiences = List.of(malformedAudience, EXAMPLE_AUDIENCE);
		var claims = getDefaultClaims()
				.audience(audiences)
				.build();
		mockRequestWithClaims(DEFAULT_URL, claims);

		assertThrows(AuthenticationException.class, () -> interceptor.buildRuleList(requestDetails),
				"Throws AuthenticationException when audience without trailing slash is present");
	}

	@Test
	void emptyAudienceListThrowsAuthenticationException() throws ParseException, BadJOSEException, JOSEException {
		List<String> audiences = List.of();
		var claims = getDefaultClaims()
				.audience(audiences)
				.build();
		mockRequestWithClaims(DEFAULT_URL, claims);

		assertThrows(ForbiddenOperationException.class, () -> interceptor.buildRuleList(requestDetails),
				"Throws ForbiddenOperationException when token audience list is empty");
	}

	@Test
	void invalidScopeArrayThrowsAuthenticationException() throws ParseException, BadJOSEException, JOSEException {
		var claims = getDefaultClaims()
				.claim("scope", "not-an-array")
				.build();
		mockRequestWithClaims(DEFAULT_URL, claims);

		assertThrows(AuthenticationException.class, () -> interceptor.buildRuleList(requestDetails),
				"Throws AuthenticationException when launch context is missing");
	}

	@Test
	void emptyScopeArrayThrowsAuthenticationException() throws ParseException, BadJOSEException, JOSEException {
		var claims = getDefaultClaims()
				.claim("scope", List.of())
				.build();
		mockRequestWithClaims(DEFAULT_URL, claims);

		assertThrows(AuthenticationException.class, () -> interceptor.buildRuleList(requestDetails),
				"Throws AuthenticationException when scope list is empty");
	}

	@Test
	void missingScopeClaimThrowsAuthenticationException() throws ParseException, BadJOSEException, JOSEException {
		var claims = getDefaultClaims().build();
		mockRequestWithClaims(DEFAULT_URL, claims);

		assertThrows(AuthenticationException.class, () -> interceptor.buildRuleList(requestDetails),
				"Throws AuthenticationException when scope list is missing");
	}

	@Test
	void scopeClaimContainingInvalidScopeThrowsAuthenticationException()
			throws ParseException, BadJOSEException, JOSEException {
		var claims = getDefaultClaims()
				.claim("scope", makeScopeClaim("not-a-valid-scope"))
				.build();
		mockRequestWithClaims(DEFAULT_URL, claims);

		assertThrows(AuthenticationException.class, () -> interceptor.buildRuleList(requestDetails),
				"Throws AuthenticationException when scope list is missing");
	}

	@Test
	void missingLaunchContextWithPatientScopeThrowsAuthenticationException()
			throws ParseException, BadJOSEException, JOSEException {
		var claims = getDefaultClaims()
				.claim("scope", makeScopeClaim("patient/*.*"))
				.build();
		mockRequestWithClaims(DEFAULT_URL, claims);

		assertThrows(AuthenticationException.class, () -> interceptor.buildRuleList(requestDetails),
				"Throws AuthenticationException when launch context is missing");
	}

	@Test
	void missingPatientIdWithPatientScopeThrowsAuthenticationException()
			throws ParseException, BadJOSEException, JOSEException {
		var mockContext = Map.of("id", "mockLaunchId", "clientId", "mockClientId", "encounter", "mockEncounterId");
		var claims = getDefaultClaims()
				.claim("scope", makeScopeClaim("patient/*.*"))
				.claim("launchContext", mockContext)
				.build();
		mockRequestWithClaims(DEFAULT_URL, claims);

		assertThrows(AuthenticationException.class, () -> interceptor.buildRuleList(requestDetails),
				"Throws AuthenticationException when no patient ID in launch context");
	}

	@Test
	void malformedLaunchContextWithPatientScopeThrowsAuthenticationException()
			throws ParseException, BadJOSEException, JOSEException {
		var claims = getDefaultClaims()
				.claim("scope", makeScopeClaim("patient/*.*"))
				.claim("launchContext", "not an object")
				.build();
		mockRequestWithClaims(DEFAULT_URL, claims);

		assertThrows(AuthenticationException.class, () -> interceptor.buildRuleList(requestDetails),
				"Throws AuthenticationException when launch context cannot be deserialized");
	}

	@Test
	void validPatientScopeReturnsRules() throws ParseException, BadJOSEException, JOSEException {
		var mockContext = Map.of("id", "mockLaunchId", "clientId", "mockClientId", "patient",
				"eFTHaVbQzCEwOEE97maN2MC2jJi-r8nnkhRh.umMUlz03");
		var claims = getDefaultClaims()
				.claim("scope", makeScopeClaim("patient/*.*"))
				.claim("launchContext", mockContext)
				.build();
		mockRequestWithClaims(DEFAULT_URL, claims);

		var rules = interceptor.buildRuleList(requestDetails);
		assertThat(rules).isNotEmpty();

		var transactionRule = rules.stream().filter(rule -> "transaction rule".equals(rule.getName())).findFirst()
				.orElse(null);
		assertNotNull(transactionRule, "a rule to allow transactions should be present");
	}

	@Test
	void requestToDefaultPartitionWithSystemWildcardScopeAllowsAll()
			throws ParseException, BadJOSEException, JOSEException {
		var defaultPartitionUrl = "http://localhost:8001/fhir/DEFAULT/";
		var partitionCreateRequest = defaultPartitionUrl + "$partition-management-create-partition";
		var claims = getDefaultClaims()
				.audience(defaultPartitionUrl)
				.claim("scope", makeScopeClaim("system/*.*"))
				.build();
		mockRequestWithClaims(partitionCreateRequest, claims);
		when(requestDetails.getTenantId()).thenReturn("DEFAULT");

		var rules = interceptor.buildRuleList(requestDetails);
		assertEquals(1, rules.size(), "privileged request rule");
		assertEquals("privileged request rule", rules.get(0).getName(), "The rule should have the expected name");
	}

	@Test
	void validSystemScopeReturnsRules() throws BadJOSEException, JOSEException, ParseException {
		var claims = getDefaultClaims()
				.claim("scope", makeScopeClaim("system/*.*"))
				.build();
		mockRequestWithClaims(DEFAULT_URL, claims);

		var rules = interceptor.buildRuleList(requestDetails);
		assertThat(rules).isNotEmpty();

		var transactionRule = rules.stream().filter(rule -> "transaction rule".equals(rule.getName())).findFirst()
				.orElse(null);
		assertNotNull(transactionRule, "a rule to allow transactions should be present");
	}

	@Test
	void validUserScopeReturnsRules() throws BadJOSEException, JOSEException, ParseException {
		var claims = getDefaultClaims()
				.claim("scope", makeScopeClaim("user/*.*"))
				.build();
		mockRequestWithClaims(DEFAULT_URL, claims);

		var rules = interceptor.buildRuleList(requestDetails);
		assertThat(rules).isNotEmpty();

		var transactionRule = rules.stream().filter(rule -> "transaction rule".equals(rule.getName())).findFirst()
				.orElse(null);
		assertNotNull(transactionRule, "a rule to allow transactions should be present");
	}

	@Test
	void defaultRuleIsDenyAll() throws ParseException, BadJOSEException, JOSEException {
		var claims = getDefaultClaims()
				.claim("scope", makeScopeClaim("openid"))
				.build();
		mockRequestWithClaims(DEFAULT_URL, claims);

		var rules = interceptor.buildRuleList(requestDetails);
		assertEquals(1, rules.size(), "There should be one rule");
		assertEquals("deny all rule", rules.get(0).getName(), "The rule should have the expected name");
	}

	private void mockRequestWithClaims(String requestUrl, JWTClaimsSet claims)
			throws ParseException, BadJOSEException, JOSEException {
		// Use a regex to extract the URL up to the tenant ID
		var urlMatcher = BASE_URL_PATTERN.matcher(requestUrl);
		urlMatcher.find();
		var fhirBaseUrl = urlMatcher.group(0);
		when(requestDetails.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(VALID_HEADER);
		when(requestDetails.getFhirServerBase()).thenReturn(fhirBaseUrl);
		when(jwtProcessor.process(MOCK_TOKEN, null)).thenReturn(claims);
	}

	private JWTClaimsSet.Builder getDefaultClaims() {
		return new JWTClaimsSet.Builder()
				.subject("test-user")
				.issueTime(new Date())
				.expirationTime(new Date(System.currentTimeMillis() + 3600000))
				.jwtID("test-jwt-id")
				.audience(EXAMPLE_AUDIENCE);
	}

	private List<String> makeScopeClaim(String... scopes) {
		return List.of(scopes);
	}

}
