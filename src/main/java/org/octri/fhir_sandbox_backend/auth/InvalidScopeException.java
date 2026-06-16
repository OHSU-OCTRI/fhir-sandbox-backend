package org.octri.fhir_sandbox_backend.auth;

/**
 * Exception thrown for malformed or otherwise invalid SMART app scopes.
 */
public class InvalidScopeException extends RuntimeException {

	public InvalidScopeException(String message) {
		super(message);
	}

	public InvalidScopeException(String message, Throwable cause) {
		super(message, cause);
	}

}
