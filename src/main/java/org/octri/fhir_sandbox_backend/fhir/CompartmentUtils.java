package org.octri.fhir_sandbox_backend.fhir;

import static org.apache.commons.lang3.StringUtils.lowerCase;

import java.util.Set;

import org.springframework.util.Assert;

/**
 * Utility methods for working with FHIR compartments.
 */
public class CompartmentUtils {

	// Extracted from https://hl7.org/fhir/R4B/compartmentdefinition-patient.html
	private static final Set<String> patientCompartmentResources = Set.of(
			"account",
			"adverseevent",
			"allergyintolerance",
			"appointment",
			"appointmentresponse",
			"auditevent",
			"basic",
			"bodystructure",
			"careplan",
			"careteam",
			"chargeitem",
			"claim",
			"claimresponse",
			"clinicalimpression",
			"communication",
			"communicationrequest",
			"composition",
			"condition",
			"consent",
			"coverage",
			"coverageeligibilityrequest",
			"coverageeligibilityresponse",
			"detectedissue",
			"devicerequest",
			"deviceusestatement",
			"diagnosticreport",
			"documentmanifest",
			"documentreference",
			"encounter",
			"enrollmentrequest",
			"episodeofcare",
			"explanationofbenefit",
			"familymemberhistory",
			"flag",
			"goal",
			"group",
			"imagingstudy",
			"immunization",
			"immunizationevaluation",
			"immunizationrecommendation",
			"invoice",
			"list",
			"measurereport",
			"media",
			"medicationadministration",
			"medicationdispense",
			"medicationrequest",
			"medicationstatement",
			"molecularsequence",
			"nutritionorder",
			"observation",
			"patient",
			"person",
			"procedure",
			"provenance",
			"questionnaireresponse",
			"relatedperson",
			"requestgroup",
			"researchsubject",
			"riskassessment",
			"schedule",
			"servicerequest",
			"specimen",
			"supplydelivery",
			"supplyrequest",
			"visionprescription"
	);

	// Extracted from https://hl7.org/fhir/R4B/compartmentdefinition-encounter.html
	private static final Set<String> encounterCompartmentResources = Set.of(
			"careplan",
			"careteam",
			"chargeitem",
			"claim",
			"clinicalimpression",
			"communication",
			"communicationrequest",
			"composition",
			"condition",
			"devicerequest",
			"diagnosticreport",
			"documentmanifest",
			"documentreference",
			"explanationofbenefit",
			"media",
			"medicationadministration",
			"medicationrequest",
			"nutritionorder",
			"observation",
			"procedure",
			"questionnaireresponse",
			"requestgroup",
			"servicerequest",
			"visionprescription"
	);

	// Extracted from https://hl7.org/fhir/R4B/compartmentdefinition-relatedperson.html
	private static final Set<String> relatedPersonCompartmentResources = Set.of(
			"adverseevent",
			"allergyintolerance",
			"appointment",
			"appointmentresponse",
			"basic",
			"careplan",
			"careteam",
			"chargeitem",
			"claim",
			"communication",
			"communicationrequest",
			"composition",
			"condition",
			"coverage",
			"documentmanifest",
			"documentreference",
			"encounter",
			"explanationofbenefit",
			"invoice",
			"medicationadministration",
			"medicationstatement",
			"observation",
			"patient",
			"person",
			"procedure",
			"provenance",
			"questionnaireresponse",
			"requestgroup",
			"schedule",
			"servicerequest",
			"supplyrequest"
	);

	// Extracted from https://hl7.org/fhir/R4B/compartmentdefinition-practitioner.html
	private static final Set<String> practitionerCompartmentResources = Set.of(
			"account",
			"adverseevent",
			"allergyintolerance",
			"appointment",
			"appointmentresponse",
			"auditevent",
			"basic",
			"careplan",
			"careteam",
			"chargeitem",
			"claim",
			"claimresponse",
			"clinicalimpression",
			"communication",
			"communicationrequest",
			"composition",
			"condition",
			"coverageeligibilityrequest",
			"coverageeligibilityresponse",
			"detectedissue",
			"devicerequest",
			"diagnosticreport",
			"documentmanifest",
			"documentreference",
			"encounter",
			"episodeofcare",
			"explanationofbenefit",
			"flag",
			"group",
			"immunization",
			"invoice",
			"linkage",
			"list",
			"media",
			"medicationadministration",
			"medicationdispense",
			"medicationrequest",
			"medicationstatement",
			"messageheader",
			"nutritionorder",
			"observation",
			"patient",
			"paymentnotice",
			"paymentreconciliation",
			"person",
			"practitionerrole",
			"procedure",
			"provenance",
			"questionnaireresponse",
			"requestgroup",
			"researchstudy",
			"riskassessment",
			"schedule",
			"servicerequest",
			"specimen",
			"supplydelivery",
			"supplyrequest",
			"visionprescription"
	);

	// Extracted from https://hl7.org/fhir/R4B/compartmentdefinition-device.html
	private static final Set<String> deviceCompartmentResources = Set.of(
			"account",
			"appointment",
			"appointmentresponse",
			"auditevent",
			"chargeitem",
			"claim",
			"communication",
			"communicationrequest",
			"composition",
			"detectedissue",
			"devicerequest",
			"deviceusestatement",
			"diagnosticreport",
			"documentmanifest",
			"documentreference",
			"explanationofbenefit",
			"flag",
			"group",
			"invoice",
			"list",
			"media",
			"medicationadministration",
			"messageheader",
			"observation",
			"provenance",
			"questionnaireresponse",
			"requestgroup",
			"riskassessment",
			"schedule",
			"servicerequest",
			"specimen",
			"supplyrequest"
	);

	/**
	 * Reports whether the given resource is included in the specified compartment.
	 *
	 * @param compartment the compartment to check (may not be null)
	 * @param resourceName FHIR resource name (may not be null)
	 * @return true if the resource is included in the compartment, false otherwise
	 */
	public static boolean resourceIsInCompartment(Compartment compartment, String resourceName) {
		Assert.notNull(compartment, "Compartment is required");
		Assert.notNull(resourceName, "Resource name is required");
		var canonicalName = resourceName.toLowerCase();
		switch (compartment) {
			case PATIENT:
				return patientCompartmentResources.contains(canonicalName);
			case ENCOUNTER:
				return encounterCompartmentResources.contains(canonicalName);
			case RELATED_PERSON:
				return relatedPersonCompartmentResources.contains(canonicalName);
			case PRACTITIONER:
				return practitionerCompartmentResources.contains(canonicalName);
			case DEVICE:
				return deviceCompartmentResources.contains(canonicalName);
			default:
				throw new IllegalArgumentException("Unexpected compartment type " + compartment);
		}
	}

	/**
	 * Reports whether the given resource is included in the patient compartment.
	 *
	 * @param resourceName FHIR resource name (may not be null)
	 * @return true if the resource is included in the patient compartment, false otherwise
	 */
	public static boolean resourceIsInPatientCompartment(String resourceName) {
		Assert.notNull(resourceName, "Resource name is required");
		return resourceIsInCompartment(Compartment.PATIENT, resourceName);
	}

	/**
	 * Reports whether the given resource is included in the encounter compartment.
	 *
	 * @param resourceName FHIR resource name (may not be null)
	 * @return true if the resource is included in the encounter compartment, false otherwise
	 */
	public static boolean resourceIsInEncounterCompartment(String resourceName) {
		Assert.notNull(resourceName, "Resource name is required");
		return resourceIsInCompartment(Compartment.ENCOUNTER, resourceName);
	}

	/**
	 * Reports whether the given resource is included in the related person compartment.
	 *
	 * @param resourceName FHIR resource name (may not be null)
	 * @return true if the resource is included in the related person compartment, false otherwise
	 */
	public static boolean resourceIsInRelatedPersonCompartment(String resourceName) {
		Assert.notNull(resourceName, "Resource name is required");
		return resourceIsInCompartment(Compartment.RELATED_PERSON, resourceName);
	}

	/**
	 * Reports whether the given resource is included in the practitioner compartment.
	 *
	 * @param resourceName FHIR resource name (may not be null)
	 * @return true if the resource is included in the practitioner compartment, false otherwise
	 */
	public static boolean resourceIsInPractitionerCompartment(String resourceName) {
		Assert.notNull(resourceName, "Resource name is required");
		return resourceIsInCompartment(Compartment.PRACTITIONER, resourceName);
	}

	/**
	 * Reports whether the given resource is included in the device compartment.
	 *
	 * @param resourceName FHIR resource name (may not be null)
	 * @return true if the resource is included in the device compartment, false otherwise
	 */
	public static boolean resourceIsInDeviceCompartment(String resourceName) {
		Assert.notNull(resourceName, "Resource name is required");
		return resourceIsInCompartment(Compartment.DEVICE, resourceName);
	}

}
