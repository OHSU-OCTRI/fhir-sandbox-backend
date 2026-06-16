package org.octri.fhir_sandbox_backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.octri.fhir_sandbox_backend.auth.SmartScopeTestUtils.nonResourceScope;
import static org.octri.fhir_sandbox_backend.auth.SmartScopeTestUtils.resourceScope;

import java.util.List;

import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;

class SmartScopeRuleBuilderTest {

	private static final IdType PATIENT_ID = new IdType("Patient", "123");

	@Test
	void nullScopeListThrows() {
		assertThrows(IllegalArgumentException.class, () -> new SmartScopeRuleBuilder(null));
	}

	@Test
	void emptyScopeListReturnsEmptyRules() {
		var rules = new SmartScopeRuleBuilder(List.of()).build();
		assertTrue(rules.isEmpty());
	}

	@Test
	void nonResourceScopesAreSkipped() {
		var scopes = List.of(nonResourceScope("openid"), nonResourceScope("launch/patient"));
		var rules = new SmartScopeRuleBuilder(scopes).build();
		assertTrue(rules.isEmpty());
	}

	@Nested
	@DisplayName("Permission character → rule count")
	class PermissionMapping {

		@Test
		void readPermissionProducesOneRule() {
			var scope = resourceScope("user/Observation.r", SmartScopeContext.USER, "Observation", "r");
			var rules = new SmartScopeRuleBuilder(List.of(scope)).build();
			assertEquals(1, rules.size());
		}

		@Test
		void searchPermissionProducesOneRule() {
			var scope = resourceScope("user/Observation.s", SmartScopeContext.USER, "Observation", "s");
			var rules = new SmartScopeRuleBuilder(List.of(scope)).build();
			assertEquals(1, rules.size());
		}

		@Test
		void readAndSearchTogetherProduceOneRule() {
			var scope = resourceScope("user/Observation.rs", SmartScopeContext.USER, "Observation", "rs");
			var rules = new SmartScopeRuleBuilder(List.of(scope)).build();
			assertEquals(1, rules.size());
		}

		@Test
		void createPermissionProducesOneRule() {
			var scope = resourceScope("user/Observation.c", SmartScopeContext.USER, "Observation", "c");
			var rules = new SmartScopeRuleBuilder(List.of(scope)).build();
			assertEquals(1, rules.size());
		}

		@Test
		void updatePermissionProducesOneRule() {
			var scope = resourceScope("user/Observation.u", SmartScopeContext.USER, "Observation", "u");
			var rules = new SmartScopeRuleBuilder(List.of(scope)).build();
			assertEquals(1, rules.size());
		}

		@Test
		void deletePermissionProducesOneRule() {
			var scope = resourceScope("user/Observation.d", SmartScopeContext.USER, "Observation", "d");
			var rules = new SmartScopeRuleBuilder(List.of(scope)).build();
			assertEquals(1, rules.size());
		}

		@Test
		void crudsPermissionsProduceFourRules() {
			// r and s collapse to one read rule; c, u, d each produce one rule → 4 total
			var scope = resourceScope("user/Observation.cruds", SmartScopeContext.USER, "Observation", "cruds");
			var rules = new SmartScopeRuleBuilder(List.of(scope)).build();
			assertEquals(4, rules.size());
		}

		@Test
		void cudPermissionsProduceThreeRules() {
			var scope = resourceScope("user/Observation.cud", SmartScopeContext.USER, "Observation", "cud");
			var rules = new SmartScopeRuleBuilder(List.of(scope)).build();
			assertEquals(3, rules.size());
		}

		@Test
		void noPermissionsProduceNoRules() {
			var scope = resourceScope("user/Observation.", SmartScopeContext.USER, "Observation", "");
			var rules = new SmartScopeRuleBuilder(List.of(scope)).build();
			assertTrue(rules.isEmpty());
		}
	}

	@Nested
	@DisplayName("Rule names carry the raw scope string")
	class RuleNames {

		@Test
		void readRuleNameMatchesRawScope() {
			var scope = resourceScope("user/Observation.rs", SmartScopeContext.USER, "Observation", "rs");
			var rules = new SmartScopeRuleBuilder(List.of(scope)).build();
			assertEquals(1, rules.size());
			assertEquals("user/Observation.rs", rules.get(0).getName());
		}

		@Test
		void allRulesForScopeShareTheSameName() {
			var rawScope = "user/Observation.cruds";
			var scope = resourceScope(rawScope, SmartScopeContext.USER, "Observation", "cruds");
			var rules = new SmartScopeRuleBuilder(List.of(scope)).build();
			assertTrue(rules.stream().allMatch(r -> rawScope.equals(r.getName())));
		}

		@Test
		void multipleScopesEachNameTheirRules() {
			var obs = resourceScope("user/Observation.r", SmartScopeContext.USER, "Observation", "r");
			var pat = resourceScope("user/Patient.r", SmartScopeContext.USER, "Patient", "r");
			var rules = new SmartScopeRuleBuilder(List.of(obs, pat)).build();
			assertEquals(2, rules.size());
			var names = rules.stream().map(IAuthRule::getName).toList();
			assertTrue(names.contains("user/Observation.r"));
			assertTrue(names.contains("user/Patient.r"));
		}
	}

	@Nested
	@DisplayName("Compartment assignment")
	class CompartmentAssignment {

		@Test
		void patientScopeWithoutPatientIdThrows() {
			var scope = resourceScope("patient/Observation.r", SmartScopeContext.PATIENT, "Observation", "r");
			var builder = new SmartScopeRuleBuilder(List.of(scope));
			assertThrows(IllegalArgumentException.class, builder::build);
		}

		@Test
		void patientScopeWithPatientIdProducesRules() {
			var scope = resourceScope("patient/Observation.r", SmartScopeContext.PATIENT, "Observation", "r");
			var rules = new SmartScopeRuleBuilder(List.of(scope))
					.withPatientId(PATIENT_ID)
					.build();
			assertEquals(1, rules.size());
		}

		@ParameterizedTest(name = "{0} context produces rules without requiring a patient ID")
		@ValueSource(strings = { "USER", "SYSTEM" })
		void userAndSystemScopesDoNotRequirePatientId(String contextName) {
			var context = SmartScopeContext.valueOf(contextName);
			var scope = resourceScope(contextName.toLowerCase() + "/Observation.r", context, "Observation", "r");
			var rules = new SmartScopeRuleBuilder(List.of(scope)).build();
			assertEquals(1, rules.size());
		}
	}

	@Nested
	@DisplayName("Resource type targeting")
	class ResourceTypeTargeting {

		@Test
		void namedResourceTypeProducesRules() {
			var scope = resourceScope("user/Observation.r", SmartScopeContext.USER, "Observation", "r");
			var rules = new SmartScopeRuleBuilder(List.of(scope)).build();
			assertEquals(1, rules.size());
		}

		@Test
		void wildcardResourceTypeProducesRules() {
			var scope = resourceScope("user/*.r", SmartScopeContext.USER, "*", "r");
			var rules = new SmartScopeRuleBuilder(List.of(scope)).build();
			assertEquals(1, rules.size());
		}
	}

	@Nested
	@DisplayName("Mixed scope lists")
	class MixedScopeLists {

		@Test
		void nonResourceScopesAreSkippedWhileResourceScopesAreProcessed() {
			var nonResource = nonResourceScope("openid");
			var resource = resourceScope("user/Patient.r", SmartScopeContext.USER, "Patient", "r");
			var rules = new SmartScopeRuleBuilder(List.of(nonResource, resource)).build();
			assertEquals(1, rules.size());
			assertEquals("user/Patient.r", rules.get(0).getName());
		}

		@Test
		void multipleResourceScopesProduceRulesForEach() {
			var obs = resourceScope("user/Observation.rs", SmartScopeContext.USER, "Observation", "rs");
			var cond = resourceScope("user/Condition.cud", SmartScopeContext.USER, "Condition", "cud");
			var rules = new SmartScopeRuleBuilder(List.of(obs, cond)).build();
			// rs → 1 rule, cud → 3 rules
			assertEquals(4, rules.size());
		}

		@Test
		void patientAndUserScopesCanCoexist() {
			var patientScope = resourceScope("patient/Observation.r", SmartScopeContext.PATIENT, "Observation", "r");
			var userScope = resourceScope("user/Patient.r", SmartScopeContext.USER, "Patient", "r");
			var rules = new SmartScopeRuleBuilder(List.of(patientScope, userScope))
					.withPatientId(PATIENT_ID)
					.build();
			assertEquals(2, rules.size());
		}
	}
}
