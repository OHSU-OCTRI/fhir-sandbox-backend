package org.octri.fhir_sandbox_backend.fhir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CompartmentUtilsTest {

	@Nested
	@DisplayName("resourceIsInCompartment")
	class ResourceIsInCompartment {

		@Test
		void throwsForNullCompartment() {
			assertThrows(IllegalArgumentException.class,
					() -> CompartmentUtils.resourceIsInCompartment(null, "Patient"));
		}

		@Test
		void throwsForNullResourceName() {
			assertThrows(IllegalArgumentException.class,
					() -> CompartmentUtils.resourceIsInCompartment(Compartment.PATIENT, null));
		}

		@Test
		void trueForResourceInPatientCompartment() {
			assertTrue(CompartmentUtils.resourceIsInCompartment(Compartment.PATIENT, "Observation"));
		}

		@Test
		void falseForResourceNotInPatientCompartment() {
			assertFalse(CompartmentUtils.resourceIsInCompartment(Compartment.PATIENT, "NotARealResource"));
		}

		@Test
		void trueForResourceInEncounterCompartment() {
			assertTrue(CompartmentUtils.resourceIsInCompartment(Compartment.ENCOUNTER, "Condition"));
		}

		@Test
		void falseForResourceNotInEncounterCompartment() {
			assertFalse(CompartmentUtils.resourceIsInCompartment(Compartment.ENCOUNTER, "Patient"));
		}

		@Test
		void trueForResourceInRelatedPersonCompartment() {
			assertTrue(CompartmentUtils.resourceIsInCompartment(Compartment.RELATED_PERSON, "Encounter"));
		}

		@Test
		void falseForResourceNotInRelatedPersonCompartment() {
			assertFalse(CompartmentUtils.resourceIsInCompartment(Compartment.RELATED_PERSON, "Immunization"));
		}

		@Test
		void trueForResourceInPractitionerCompartment() {
			assertTrue(CompartmentUtils.resourceIsInCompartment(Compartment.PRACTITIONER, "PractitionerRole"));
		}

		@Test
		void falseForResourceNotInPractitionerCompartment() {
			assertFalse(CompartmentUtils.resourceIsInCompartment(Compartment.PRACTITIONER, "RelatedPerson"));
		}

		@Test
		void trueForResourceInDeviceCompartment() {
			assertTrue(CompartmentUtils.resourceIsInCompartment(Compartment.DEVICE, "DeviceRequest"));
		}

		@Test
		void falseForResourceNotInDeviceCompartment() {
			assertFalse(CompartmentUtils.resourceIsInCompartment(Compartment.DEVICE, "Condition"));
		}
	}

	@Nested
	@DisplayName("resourceIsInPatientCompartment")
	class ResourceIsInPatientCompartment {

		@Test
		void throwsForNullResourceName() {
			assertThrows(IllegalArgumentException.class,
					() -> CompartmentUtils.resourceIsInPatientCompartment(null));
		}

		@Test
		void trueForPatient() {
			assertTrue(CompartmentUtils.resourceIsInPatientCompartment("Patient"));
		}

		@Test
		void falseForUnrelatedResource() {
			assertFalse(CompartmentUtils.resourceIsInPatientCompartment("Linkage"));
		}

		@Test
		void resourceNamesAreCaseInsensitive() {
			assertTrue(CompartmentUtils.resourceIsInPatientCompartment("medicationrequest"));
		}
	}

	@Nested
	@DisplayName("resourceIsInEncounterCompartment")
	class ResourceIsInEncounterCompartment {

		@Test
		void throwsForNullResourceName() {
			assertThrows(IllegalArgumentException.class,
					() -> CompartmentUtils.resourceIsInEncounterCompartment(null));
		}

		@Test
		void trueForEncounterResource() {
			assertTrue(CompartmentUtils.resourceIsInEncounterCompartment("DiagnosticReport"));
		}

		@Test
		void falseForUnrelatedResource() {
			assertFalse(CompartmentUtils.resourceIsInEncounterCompartment("Patient"));
		}

		@Test
		void resourceNamesAreCaseInsensitive() {
			assertTrue(CompartmentUtils.resourceIsInEncounterCompartment("careteam"));
		}
	}

	@Nested
	@DisplayName("resourceIsInRelatedPersonCompartment")
	class ResourceIsInRelatedPersonCompartment {

		@Test
		void throwsForNullResourceName() {
			assertThrows(IllegalArgumentException.class,
					() -> CompartmentUtils.resourceIsInRelatedPersonCompartment(null));
		}

		@Test
		void trueForRelatedPersonResource() {
			assertTrue(CompartmentUtils.resourceIsInRelatedPersonCompartment("Coverage"));
		}

		@Test
		void falseForUnrelatedResource() {
			assertFalse(CompartmentUtils.resourceIsInRelatedPersonCompartment("Immunization"));
		}

		@Test
		void resourceNamesAreCaseInsensitive() {
			assertTrue(CompartmentUtils.resourceIsInRelatedPersonCompartment("invoice"));
		}
	}

	@Nested
	@DisplayName("resourceIsInPractitionerCompartment")
	class ResourceIsInPractitionerCompartment {

		@Test
		void throwsForNullResourceName() {
			assertThrows(IllegalArgumentException.class,
					() -> CompartmentUtils.resourceIsInPractitionerCompartment(null));
		}

		@Test
		void trueForPractitionerResource() {
			assertTrue(CompartmentUtils.resourceIsInPractitionerCompartment("Linkage"));
		}

		@Test
		void falseForUnrelatedResource() {
			assertFalse(CompartmentUtils.resourceIsInPractitionerCompartment("RelatedPerson"));
		}

		@Test
		void resourceNamesAreCaseInsensitive() {
			assertTrue(CompartmentUtils.resourceIsInPractitionerCompartment("condition"));
		}
	}

	@Nested
	@DisplayName("resourceIsInDeviceCompartment")
	class ResourceIsInDeviceCompartment {

		@Test
		void throwsForNullResourceName() {
			assertThrows(IllegalArgumentException.class,
					() -> CompartmentUtils.resourceIsInDeviceCompartment(null));
		}

		@Test
		void trueForDeviceResource() {
			assertTrue(CompartmentUtils.resourceIsInDeviceCompartment("MessageHeader"));
		}

		@Test
		void falseForUnrelatedResource() {
			assertFalse(CompartmentUtils.resourceIsInDeviceCompartment("Patient"));
		}

		@Test
		void resourceNamesAreCaseInsensitive() {
			assertTrue(CompartmentUtils.resourceIsInDeviceCompartment("devicerequest"));
		}
	}
}
