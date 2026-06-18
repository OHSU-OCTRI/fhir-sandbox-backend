package org.octri.fhir_sandbox_backend.auth;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.util.Assert;

import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRuleBuilderRuleOp;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRuleBuilderRuleOpClassifier;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;

/**
 * Builds HAPI FHIR {@link IAuthRule} objects from a list of {@link SmartScope} objects.
 *
 * <p>
 * The scopes are expected to be grouped by compartment before being passed to this builder.
 * Non-resource scopes (e.g. {@code openid}, {@code launch}) are ignored.
 * Scope query-string filters (e.g. {@code ?category=vital-signs}) are ignored; HAPI FHIR
 * compartment rules do not natively enforce them.
 *
 * <p>
 * Context compartment mapping:
 * <ul>
 * <li>{@code patient/} scopes → {@code inCompartment("Patient", patientId)} — requires a patient ID</li>
 * <li>{@code user/} and {@code system/} scopes → {@code withAnyId()}</li>
 * </ul>
 *
 * @see <a href="https://build.fhir.org/ig/HL7/smart-app-launch/scopes-and-launch-context.html">
 *      SMART App Launch: Scopes and Launch Context</a>
 */
public class SmartScopeRuleBuilder {

	private final List<SmartScope> scopes;
	private IIdType patientId;

	public SmartScopeRuleBuilder(List<SmartScope> scopes) {
		Assert.notNull(scopes, "List of scopes is required");
		this.scopes = scopes;
	}

	/**
	 * Sets the patient ID used for {@code patient/}-context scope compartment restrictions.
	 * Required when the scope list contains any {@code patient/} scopes.
	 *
	 * @param patientId
	 *           the patient compartment ID
	 * @return this builder
	 */
	public SmartScopeRuleBuilder withPatientId(IIdType patientId) {
		this.patientId = patientId;
		return this;
	}

	/**
	 * Builds the list of {@link IAuthRule} objects from the configured scopes.
	 *
	 * @return list of auth rules
	 * @throws IllegalArgumentException
	 *            if any {@code patient/} scope is present and no patient ID has been set
	 */
	public List<IAuthRule> build() {
		List<IAuthRule> rules = new ArrayList<>();
		for (SmartScope scope : scopes) {
			if (scope.isResourceScope()) {
				rules.addAll(buildRulesForScope(scope));
			}
		}
		return rules;
	}

	/**
	 * Builds a list of {@link IAuthRule} objects from the given scope.
	 *
	 * @param scope
	 *           SMART scope to convert
	 * @return list of auth rules
	 */
	private List<IAuthRule> buildRulesForScope(SmartScope scope) {
		List<IAuthRule> rules = new ArrayList<>();

		// r and s both map to HAPI FHIR read, which also covers search.
		if (scope.allowsRead() || scope.allowsSearch()) {
			rules.addAll(applyCompartment(scope.getCompartmentName(),
					resourcesOfType(new RuleBuilder().allow(scope.getRawScope()).read(), scope.getResourceType())));
		}

		if (scope.allowsCreate()) {
			rules.addAll(applyCompartment(scope.getCompartmentName(),
					resourcesOfType(new RuleBuilder().allow(scope.getRawScope()).create(), scope.getResourceType())));
		}

		if (scope.allowsUpdate()) {
			rules.addAll(applyCompartment(scope.getCompartmentName(),
					resourcesOfType(new RuleBuilder().allow(scope.getRawScope()).write(), scope.getResourceType())));
		}

		if (scope.allowsDelete()) {
			rules.addAll(applyCompartment(scope.getCompartmentName(),
					resourcesOfType(new RuleBuilder().allow(scope.getRawScope()).delete(), scope.getResourceType())));
		}

		return rules;
	}

	private IAuthRuleBuilderRuleOpClassifier resourcesOfType(IAuthRuleBuilderRuleOp op, String resourceType) {
		return "*".equals(resourceType) ? op.allResources() : op.resourcesOfType(resourceType);
	}

	private List<IAuthRule> applyCompartment(String context, IAuthRuleBuilderRuleOpClassifier classifier) {
		if ("patient".equals(context)) {
			Assert.notNull(patientId, "Patient ID is required for patient-context scopes");
			return classifier.inCompartment("Patient", patientId).build();
		}
		return classifier.withAnyId().build();
	}

}
