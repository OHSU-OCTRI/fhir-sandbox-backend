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

class AuthUtilsTest {

	@Nested
	@DisplayName("groupScopesByContext")
	class GroupScopesByContext {

		@Test
		void throwsForNullScopeSet() {
			assertThrows(IllegalArgumentException.class, () -> AuthUtils.groupScopesByContext(null));
		}

		@Test
		void emptySetReturnsEmptyGroups() {
			var result = AuthUtils.groupScopesByContext(Set.of());
			assertEquals(List.of(), result.patientScopes());
			assertEquals(List.of(), result.userScopes());
			assertEquals(List.of(), result.systemScopes());
		}

		@Test
		void nonResourceScopesAreExcludedFromAllGroups() {
			var scopes = Set.of(nonResourceScope("openid"), nonResourceScope("launch/patient"));
			var result = AuthUtils.groupScopesByContext(scopes);
			assertEquals(List.of(), result.patientScopes());
			assertEquals(List.of(), result.userScopes());
			assertEquals(List.of(), result.systemScopes());
		}

		@Test
		void patientScopesAreGroupedCorrectly() {
			var patientScope = resourceScope("patient/Observation.rs", SmartScopeContext.PATIENT, "Observation", "rs");
			var result = AuthUtils.groupScopesByContext(Set.of(patientScope));
			assertEquals(List.of(patientScope), result.patientScopes());
			assertEquals(List.of(), result.userScopes());
			assertEquals(List.of(), result.systemScopes());
		}

		@Test
		void userScopesAreGroupedCorrectly() {
			var userScope = resourceScope("user/Observation.rs", SmartScopeContext.USER, "Observation", "rs");
			var result = AuthUtils.groupScopesByContext(Set.of(userScope));
			assertEquals(List.of(), result.patientScopes());
			assertEquals(List.of(userScope), result.userScopes());
			assertEquals(List.of(), result.systemScopes());
		}

		@Test
		void systemScopesAreGroupedCorrectly() {
			var systemScope = resourceScope("system/Observation.rs", SmartScopeContext.SYSTEM, "Observation", "rs");
			var result = AuthUtils.groupScopesByContext(Set.of(systemScope));
			assertEquals(List.of(), result.patientScopes());
			assertEquals(List.of(), result.userScopes());
			assertEquals(List.of(systemScope), result.systemScopes());
		}

		@Test
		void mixedScopesAreRoutedToCorrectGroups() {
			var patientScope = resourceScope("patient/Observation.rs", SmartScopeContext.PATIENT, "Observation", "rs");
			var userScope = resourceScope("user/Observation.rs", SmartScopeContext.USER, "Observation", "rs");
			var systemScope = resourceScope("system/Observation.rs", SmartScopeContext.SYSTEM, "Observation", "rs");
			var nonResource = nonResourceScope("openid");

			var result = AuthUtils.groupScopesByContext(Set.of(patientScope, userScope, systemScope, nonResource));

			assertEquals(List.of(patientScope), result.patientScopes());
			assertEquals(List.of(userScope), result.userScopes());
			assertEquals(List.of(systemScope), result.systemScopes());
		}

		@Test
		void multiplePatientScopesAreAllIncluded() {
			var scope1 = resourceScope("patient/Observation.rs", SmartScopeContext.PATIENT, "Observation", "rs");
			var scope2 = new SmartScope("patient/Condition.cud", SmartScopeContext.PATIENT, "Condition", "cud", null);

			var result = AuthUtils.groupScopesByContext(Set.of(scope1, scope2));

			assertEquals(2, result.patientScopes().size());
			assertTrue(result.patientScopes().contains(scope1));
			assertTrue(result.patientScopes().contains(scope2));
		}
	}

	@Nested
	@DisplayName("GroupedScopes record")
	class GroupedScopesRecord {

		@Test
		void nullPatientScopesDefaultsToEmptyList() {
			var grouped = new AuthUtils.GroupedScopes(null, List.of(), List.of());
			assertEquals(List.of(), grouped.patientScopes());
		}

		@Test
		void nullUserScopesDefaultsToEmptyList() {
			var grouped = new AuthUtils.GroupedScopes(List.of(), null, List.of());
			assertEquals(List.of(), grouped.userScopes());
		}

		@Test
		void nullSystemScopesDefaultsToEmptyList() {
			var grouped = new AuthUtils.GroupedScopes(List.of(), List.of(), null);
			assertEquals(List.of(), grouped.systemScopes());
		}
	}
}
