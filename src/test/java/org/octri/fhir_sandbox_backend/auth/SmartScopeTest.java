package org.octri.fhir_sandbox_backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.octri.fhir_sandbox_backend.auth.SmartScopeTestUtils.nonResourceScope;
import static org.octri.fhir_sandbox_backend.auth.SmartScopeTestUtils.resourceScope;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SmartScopeTest {

	@Nested
	@DisplayName("isResourceScope")
	class IsResourceScope {

		@Test
		void patientContextIsResourceScope() {
			assertTrue(resourceScope(SmartScopeContext.PATIENT, "Observation", "rs").isResourceScope());
		}

		@Test
		void userContextIsResourceScope() {
			assertTrue(resourceScope(SmartScopeContext.USER, "Patient", "cruds").isResourceScope());
		}

		@Test
		void systemContextIsResourceScope() {
			assertTrue(resourceScope(SmartScopeContext.SYSTEM, "Encounter", "r").isResourceScope());
		}

		@Test
		void nonResourceContextIsNotResourceScope() {
			assertFalse(nonResourceScope("openid").isResourceScope());
		}

		@Test
		void nullContextIsNotResourceScope() {
			var scope = new SmartScope("openid", null, null, null, null);
			assertFalse(scope.isResourceScope());
		}
	}

	@Nested
	@DisplayName("getCompartmentName")
	class GetCompartmentName {

		@Test
		void patientContextReturnsPatient() {
			assertEquals("patient", resourceScope(SmartScopeContext.PATIENT, "Observation", "rs").getContextName());
		}

		@Test
		void userContextReturnsUser() {
			assertEquals("user", resourceScope(SmartScopeContext.USER, "Patient", "r").getContextName());
		}

		@Test
		void systemContextReturnsSystem() {
			assertEquals("system", resourceScope(SmartScopeContext.SYSTEM, "Encounter", "cruds").getContextName());
		}

		@Test
		void nonResourceContextThrows() {
			assertThrows(IllegalStateException.class, () -> nonResourceScope("openid").getContextName());
		}

		@Test
		void nullContextThrows() {
			var scope = new SmartScope("openid", null, null, null, null);
			assertThrows(IllegalStateException.class, scope::getContextName);
		}
	}

	@Nested
	@DisplayName("permission checks")
	class PermissionChecks {

		@Test
		void allowsCreateWhenPermissionsContainC() {
			var scope = resourceScope(SmartScopeContext.PATIENT, "Observation", "cruds");
			assertTrue(scope.allowsCreate());
		}

		@Test
		void allowsReadWhenPermissionsContainR() {
			var scope = resourceScope(SmartScopeContext.PATIENT, "Observation", "rs");
			assertTrue(scope.allowsRead());
		}

		@Test
		void allowsUpdateWhenPermissionsContainU() {
			var scope = resourceScope(SmartScopeContext.PATIENT, "Observation", "cud");
			assertTrue(scope.allowsUpdate());
		}

		@Test
		void allowsDeleteWhenPermissionsContainD() {
			var scope = resourceScope(SmartScopeContext.PATIENT, "Observation", "cud");
			assertTrue(scope.allowsDelete());
		}

		@Test
		void allowsSearchWhenPermissionsContainS() {
			var scope = resourceScope(SmartScopeContext.PATIENT, "Observation", "rs");
			assertTrue(scope.allowsSearch());
		}

		@Test
		void doesNotAllowCreateWhenPermissionsLackC() {
			var scope = resourceScope(SmartScopeContext.PATIENT, "Observation", "rs");
			assertFalse(scope.allowsCreate());
		}

		@Test
		void doesNotAllowReadWhenPermissionsLackR() {
			var scope = resourceScope(SmartScopeContext.PATIENT, "Observation", "cud");
			assertFalse(scope.allowsRead());
		}

		@Test
		void doesNotAllowUpdateWhenPermissionsLackU() {
			var scope = resourceScope(SmartScopeContext.PATIENT, "Observation", "rs");
			assertFalse(scope.allowsUpdate());
		}

		@Test
		void doesNotAllowDeleteWhenPermissionsLackD() {
			var scope = resourceScope(SmartScopeContext.PATIENT, "Observation", "rs");
			assertFalse(scope.allowsDelete());
		}

		@Test
		void doesNotAllowSearchWhenPermissionsLackS() {
			var scope = resourceScope(SmartScopeContext.PATIENT, "Observation", "r");
			assertFalse(scope.allowsSearch());
		}

		@Test
		void nullPermissionsReturnsFalseForAllChecks() {
			var scope = new SmartScope("dummy", SmartScopeContext.PATIENT, "Observation", null, null);
			assertFalse(scope.allowsCreate());
			assertFalse(scope.allowsRead());
			assertFalse(scope.allowsUpdate());
			assertFalse(scope.allowsDelete());
			assertFalse(scope.allowsSearch());
			assertFalse(scope.allowsAllPermissions());
		}

		@ParameterizedTest(name = "cruds allows all permissions")
		@ValueSource(strings = { "cruds" })
		void crudsPermissionsAllowsEverything(String permissions) {
			var scope = resourceScope(SmartScopeContext.PATIENT, "Observation", permissions);
			assertTrue(scope.allowsCreate());
			assertTrue(scope.allowsRead());
			assertTrue(scope.allowsUpdate());
			assertTrue(scope.allowsDelete());
			assertTrue(scope.allowsSearch());
			assertTrue(scope.allowsAllPermissions());
		}
	}

	@Nested
	@DisplayName("allowsAllResources")
	class AllowsAllResourcesMethod {

		@Test
		void wildcardResourceTypeAllowsAllResources() {
			var scope = resourceScope(SmartScopeContext.PATIENT, "*", "cruds");
			assertTrue(scope.allowsAllResources(), "* should allow all resources");
		}

		@ParameterizedTest(name = "other resource types do not allow all resources")
		@ValueSource(strings = { "Patient", "Observation", "AllergyIntolerance" })
		void otherResourceTypesDoNotAllowAllResources(String resourceType) {
			var scope = resourceScope(SmartScopeContext.PATIENT, resourceType, "rs");
			assertFalse(scope.allowsAllPermissions(), resourceType + " should not allow all resources");
		}
	}

	@Nested
	@DisplayName("toString")
	class ToStringMethod {

		@Test
		void toStringReturnsRawScope() {
			var scope = resourceScope(SmartScopeContext.PATIENT, "Observation", "rs");
			assertEquals("patient/Observation.rs", scope.toString());
		}

		@Test
		void toStringReturnsNullWhenRawScopeIsNull() {
			assertEquals(null, new SmartScope(null, null, null, null, null).toString());
		}
	}

	@Nested
	@DisplayName("equals and hashCode")
	class EqualsAndHashCode {

		@Test
		void equalScopesAreEqual() {
			var a = resourceScope(SmartScopeContext.PATIENT, "Observation", "rs");
			var b = resourceScope(SmartScopeContext.PATIENT, "Observation", "rs");
			assertEquals(a, b);
		}

		@Test
		void sameInstanceIsEqual() {
			var scope = resourceScope(SmartScopeContext.PATIENT, "Observation", "rs");
			assertEquals(scope, scope);
		}

		@Test
		void nullIsNotEqual() {
			assertNotEquals(null, resourceScope(SmartScopeContext.PATIENT, "Observation", "rs"));
		}

		@Test
		void differentClassIsNotEqual() {
			assertNotEquals("patient/Observation.rs", resourceScope(SmartScopeContext.PATIENT, "Observation", "rs"));
		}

		@Test
		void differentRawScopeIsNotEqual() {
			var a = resourceScope(SmartScopeContext.PATIENT, "Observation", "rs");
			var b = new SmartScope("something/else.r", SmartScopeContext.PATIENT, "Observation", "rs", null);
			assertNotEquals(a, b);
		}

		@Test
		void differentContextIsNotEqual() {
			var a = resourceScope(SmartScopeContext.PATIENT, "Observation", "rs");
			var b = resourceScope(SmartScopeContext.USER, "Observation", "rs");
			assertNotEquals(a, b);
		}

		@Test
		void differentResourceTypeIsNotEqual() {
			var a = resourceScope(SmartScopeContext.PATIENT, "Observation", "rs");
			var b = resourceScope(SmartScopeContext.PATIENT, "Condition", "rs");
			assertNotEquals(a, b);
		}

		@Test
		void differentPermissionsIsNotEqual() {
			var a = resourceScope(SmartScopeContext.PATIENT, "Observation", "rs");
			var b = resourceScope(SmartScopeContext.PATIENT, "Observation", "cruds");
			assertNotEquals(a, b);
		}

		@Test
		void scopesWithDifferentQueryStringsAreNotEqual() {
			var a = resourceScope(SmartScopeContext.PATIENT, "Observation", "rs");
			var b = new SmartScope("patient/Observation.rs", SmartScopeContext.PATIENT, "Observation", "rs",
					"category=laboratory");
			assertNotEquals(a, b);
		}

		@Test
		void equalScopesHaveEqualHashCodes() {
			var a = resourceScope(SmartScopeContext.PATIENT, "Observation", "rs");
			var b = resourceScope(SmartScopeContext.PATIENT, "Observation", "rs");
			assertEquals(a.hashCode(), b.hashCode());
		}

		@Test
		void nonResourceScopesWithSameRawAreEqual() {
			assertEquals(nonResourceScope("openid"), nonResourceScope("openid"));
		}
	}
}
