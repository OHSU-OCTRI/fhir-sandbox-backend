package org.octri.fhir_sandbox_backend.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents the SMART application launch context attributes, for example the patient or encounter context to use when
 * opening the application.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LaunchContext {

	/**
	 * Context ID
	 */
	private String id;

	/**
	 * Registered client ID associated with the launch context.
	 */
	private String clientId;

	/**
	 * Patient ID selected for the launch context.
	 */
	private String patient;

	/**
	 * Encounter ID selected for the launch context.
	 */
	private String encounter;

	/**
	 * FHIR ID of the user associated with the launch.
	 */
	private String fhirUser;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getPatient() {
		return patient;
	}

	public void setPatient(String patient) {
		this.patient = patient;
	}

	public String getEncounter() {
		return encounter;
	}

	public void setEncounter(String encounter) {
		this.encounter = encounter;
	}

	public String getFhirUser() {
		return fhirUser;
	}

	public void setFhirUser(String fhirUser) {
		this.fhirUser = fhirUser;
	}

	@Override
	public String toString() {
		return "LaunchContext [id=" + id + ", clientId=" + clientId + ", patient=" + patient + ", encounter=" + encounter
				+ ", fhirUser=" + fhirUser + "]";
	}

}
