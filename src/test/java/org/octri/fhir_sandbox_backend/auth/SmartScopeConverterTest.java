package org.octri.fhir_sandbox_backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.octri.fhir_sandbox_backend.auth.SmartScopeTestUtils.nonResourceScope;
import static org.octri.fhir_sandbox_backend.auth.SmartScopeTestUtils.resourceScope;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SmartScopeConverterTest {

	@Nested
	@DisplayName("convertScopes returns a Set of SmartScope objects")
	class ConvertScopes {

		@Test
		void nullScopeStringThrows() {
			assertThrows(InvalidScopeException.class, () -> SmartScopeConverter.convertScopes(null));
		}

		@Test
		void emptyListThrows() {
			assertThrows(InvalidScopeException.class, () -> SmartScopeConverter.convertScopes(List.of()));
		}

		@Test
		void singleV1ScopeIsConvertedToV2SmartScope() {
			Set<SmartScope> result = SmartScopeConverter.convertScopes(List.of("patient/Observation.read"));
			assertEquals(Set.of(resourceScope("patient/Observation.read", SmartScopeContext.PATIENT,
					"Observation", "rs", null)), result);
		}

		@Test
		void singleV2ScopeIsReturnedUnchanged() {
			Set<SmartScope> result = SmartScopeConverter.convertScopes(List.of("user/Patient.cruds"));
			assertEquals(Set.of(resourceScope("user/Patient.cruds", SmartScopeContext.USER,
					"Patient", "cruds", null)), result);
		}

		@Test
		void v2ScopeWithQueryStringPreservesQueryString() {
			Set<SmartScope> result = SmartScopeConverter
					.convertScopes(List.of("patient/Observation.rs?category=laboratory"));
			assertEquals(Set.of(resourceScope("patient/Observation.rs?category=laboratory",
					SmartScopeContext.PATIENT, "Observation", "rs", "category=laboratory")), result);
		}

		@Test
		void nonResourceScopeHasNonResourceContext() {
			Set<SmartScope> result = SmartScopeConverter.convertScopes(List.of("openid"));
			assertEquals(Set.of(nonResourceScope("openid")), result);
		}

		@Test
		void multipleScopesReturnSetOfSmartScopes() {
			Set<SmartScope> result = SmartScopeConverter.convertScopes(
					List.of("openid", "launch/patient", "patient/Observation.read", "system/Encounter.write"));
			assertEquals(Set.of(
					nonResourceScope("openid"),
					nonResourceScope("launch/patient"),
					resourceScope("patient/Observation.read", SmartScopeContext.PATIENT, "Observation", "rs", null),
					resourceScope("system/Encounter.write", SmartScopeContext.SYSTEM, "Encounter", "cud", null)),
					result);
		}

		@Test
		void extraWhitespaceIsNormalized() {
			Set<SmartScope> result = SmartScopeConverter
					.convertScopes(List.of("  patient/Observation.read ", " user/Patient.rs  "));
			assertEquals(Set.of(
					resourceScope("patient/Observation.read", SmartScopeContext.PATIENT, "Observation", "rs", null),
					resourceScope("user/Patient.rs", SmartScopeContext.USER, "Patient", "rs", null)), result);
		}

		@Test
		void duplicateScopesAreCollapsedBySet() {
			Set<SmartScope> result = SmartScopeConverter.convertScopes(List.of("openid", "openid"));
			assertEquals(1, result.size());
			assertTrue(result.contains(nonResourceScope("openid")));
		}

		@Test
		void invalidScopeTokenThrows() {
			assertThrows(InvalidScopeException.class,
					() -> SmartScopeConverter.convertScopes(List.of("openid", "patient/observation.read")));
		}

		@Test
		void wildcardResourceTypeIsHandled() {
			Set<SmartScope> result = SmartScopeConverter.convertScopes(List.of("patient/*.read"));
			assertEquals(Set.of(resourceScope("patient/*.read", SmartScopeContext.PATIENT, "*", "rs", null)),
					result);
		}
	}

	@Nested
	@DisplayName("convertScopeString returns a SmartScope object")
	class ConvertScopeString {

		@Test
		void nullThrows() {
			assertThrows(InvalidScopeException.class, () -> SmartScopeConverter.convertScopeString(null));
		}

		@Test
		void blankThrows() {
			assertThrows(InvalidScopeException.class, () -> SmartScopeConverter.convertScopeString("   "));
		}

		@ParameterizedTest(name = "{0}")
		@ValueSource(strings = { "openid", "fhirUser", "profile", "launch", "launch/patient", "offline_access",
				"online_access" })
		void nonResourceScopeHasNonResourceContextAndRawScopeSet(String input) {
			var result = SmartScopeConverter.convertScopeString(input);
			assertEquals(SmartScopeContext.NON_RESOURCE, result.getContext());
			assertEquals(input, result.getRawScope());
		}

		@Test
		void nonResourceScopeHasNullResourceTypeAndPermissions() {
			var result = SmartScopeConverter.convertScopeString("openid");
			assertEquals(null, result.getResourceType());
			assertEquals(null, result.getPermissions());
			assertEquals(null, result.getQueryString());
		}

		@ParameterizedTest(name = "{0} → permissions={1}, context={2}")
		@CsvSource({
				"patient/Observation.read,  rs,   PATIENT",
				"patient/Observation.write, cud,  PATIENT",
				"patient/Observation.*,     cruds,PATIENT",
				"user/Patient.read,         rs,   USER",
				"system/Encounter.write,    cud,  SYSTEM",
		})
		void v1PermissionIsConvertedToV2(String input, String expectedPermissions, String expectedContext) {
			var result = SmartScopeConverter.convertScopeString(input.trim());
			assertEquals(SmartScopeContext.valueOf(expectedContext.trim()), result.getContext());
			assertEquals(expectedPermissions.trim(), result.getPermissions());
			assertEquals(input.trim(), result.getRawScope());
			assertEquals(null, result.getQueryString());
		}

		@ParameterizedTest(name = "{0}")
		@ValueSource(strings = {
				"patient/Observation.rs",
				"patient/Observation.cud",
				"patient/Observation.cruds",
				"patient/Observation.r",
				"patient/Observation.cu",
				"user/Patient.ruds",
				"system/Encounter.s",
		})
		void v2PermissionIsPreservedUnchanged(String input) {
			var result = SmartScopeConverter.convertScopeString(input);
			var expectedPermissions = input.substring(input.indexOf('.') + 1);
			assertEquals(expectedPermissions, result.getPermissions());
			assertEquals(input, result.getRawScope());
			assertEquals(null, result.getQueryString());
		}

		@Test
		void v2ScopeWithQueryStringPreservesQueryString() {
			var result = SmartScopeConverter.convertScopeString("patient/Observation.rs?category=laboratory");
			assertEquals(SmartScopeContext.PATIENT, result.getContext());
			assertEquals("Observation", result.getResourceType());
			assertEquals("rs", result.getPermissions());
			assertEquals("category=laboratory", result.getQueryString());
		}

		@Test
		void wildcardResourceTypeIsAccepted() {
			var result = SmartScopeConverter.convertScopeString("patient/*.read");
			assertEquals("*", result.getResourceType());
			assertEquals("rs", result.getPermissions());
		}

		@Test
		void contextIsSetCorrectlyForPatientUserSystem() {
			assertEquals(SmartScopeContext.PATIENT,
					SmartScopeConverter.convertScopeString("patient/Observation.rs").getContext());
			assertEquals(SmartScopeContext.USER,
					SmartScopeConverter.convertScopeString("user/Observation.rs").getContext());
			assertEquals(SmartScopeContext.SYSTEM,
					SmartScopeConverter.convertScopeString("system/Observation.rs").getContext());
		}

		@ParameterizedTest(name = "{0}")
		@ValueSource(strings = {
				// Missing slash
				"patientObservation.read",
				// Invalid context
				"group/Observation.read",
				// Missing resource type
				"patient/.read",
				// Missing permission
				"patient/Observation",
				"patient/Observation.?foo=bar",
				// Resource type starts with lowercase (not wildcard)
				"patient/observation.read",
				"patient/observation.rs",
				// Invalid V2 permission character
				"patient/Observation.x",
				// V2 permissions in incorrect order
				"patient/Observation.sr",
				// Duplicate V2 permission characters
				"patient/Observation.rr",
				"patient/Observation.ss",
				// Empty query string
				"patient/Observation.rs?",
				// V1 permission with query string
				"patient/Observation.read?foo=bar",
				// Multiple slashes
				"patient/Observation/extra.rs",
		})
		void invalidScopeThrows(String input) {
			assertThrows(InvalidScopeException.class, () -> SmartScopeConverter.convertScopeString(input));
		}
	}
}
