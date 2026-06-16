package org.octri.fhir_sandbox_backend.auth;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility for converting SMART on FHIR scope strings to {@link SmartScope} objects.
 *
 * <p>
 * Scope strings are normalized to V2 format during conversion.
 *
 * <p>
 * V1 resource scope format: {@code {context}/{resourceType}.{permission}}
 * <br>
 * V2 resource scope format: {@code {context}/{resourceType}.{cruds}[?params]}
 *
 * <p>
 * V1-to-V2 permission mapping:
 * <ul>
 * <li>{@code .read} → {@code .rs}</li>
 * <li>{@code .write} → {@code .cud}</li>
 * <li>{@code .*} → {@code .cruds}</li>
 * </ul>
 *
 * @see <a href="https://build.fhir.org/ig/HL7/smart-app-launch/scopes-and-launch-context.html">
 *      SMART App Launch: Scopes and Launch Context</a>
 */
public class SmartScopeConverter {

	/** Valid V2 permission characters, in the required order. */
	private static final String V2_PERMISSION_ORDER = "cruds";

	private record ScopeSegments(String context, String resourceType, String permissions, String queryString) {
	}

	/**
	 * Converts a list of scopes extracted from a JSON web token's {@code scope} claim to a set of
	 * equivalent {@link SmartScope} objects.
	 *
	 * @param scopeString
	 * @return
	 */
	public static Set<SmartScope> convertScopes(List<String> scopes) {
		if (scopes == null || scopes.isEmpty()) {
			throw new InvalidScopeException("Scope string must not be null or blank.");
		}

		return scopes.stream()
				.map(String::trim)
				.map(SmartScopeConverter::convertScopeString)
				.collect(Collectors.toSet());
	}

	/**
	 * Converts a single scope string to a {@link SmartScope}.
	 *
	 * @param scope
	 *           a scope string
	 * @return a {@link SmartScope} representing the scope described by the string
	 */
	public static SmartScope convertScopeString(String scopeStr) {
		if (scopeStr == null || scopeStr.isBlank()) {
			throw new InvalidScopeException("Individual scope token must not be null or blank.");
		}

		// Non-resource scopes pass through unchanged.
		if (isNonResourceScope(scopeStr)) {
			return new SmartScope(scopeStr, SmartScopeContext.NON_RESOURCE, null, null, null);
		}

		var segments = parseScope(scopeStr);
		validateContext(segments.context(), scopeStr);
		validateResourceType(segments.resourceType(), scopeStr);

		// Detect V1 vs V2 by the permission token.
		if (isV1Permission(segments.permissions())) {
			// V1 scopes do not carry query strings.
			if (segments.queryString() != null) {
				throw new InvalidScopeException(
						"Invalid scope (V1 permission may not include a query string): \"" + scopeStr + "\"");
			}
			var v2Permission = mapV1ToV2Permission(segments.permissions(), scopeStr);
			return new SmartScope(scopeStr, SmartScopeContext.valueOf(segments.context().toUpperCase()),
					segments.resourceType(), v2Permission, null);
		} else {
			// Must be a valid V2 permission (optionally followed by '?...' query string).
			validateV2PermissionAndQuery(segments.permissions(), segments.queryString(), scopeStr);
			return new SmartScope(scopeStr, SmartScopeContext.valueOf(segments.context().toUpperCase()),
					segments.resourceType(), segments.permissions(), segments.queryString());
		}
	}

	/**
	 * Reports whether the given scope is one of the non-resource scopes recognized by the SMART specification. These are
	 * returned without modification and without further parsing.
	 *
	 * @param scope
	 *           a scope string
	 * @return true if the scope is one of the non-resource scopes, false otherwise
	 */
	private static boolean isNonResourceScope(String scope) {
		return scope.equals("openid")
				|| scope.equals("fhirUser")
				|| scope.equals("profile")
				|| scope.equals("launch")
				|| scope.startsWith("launch/")
				|| scope.equals("offline_access")
				|| scope.equals("online_access");
	}

	/**
	 * Parses a scope string into its constituent segments.
	 *
	 * @param scope
	 *           a scope string
	 * @return the parsed segments
	 */
	private static ScopeSegments parseScope(String scope) {
		// Find the slash that separates the context from the resource permissions
		var slashIndex = scope.indexOf('/');
		if (slashIndex < 0) {
			throw new InvalidScopeException("Invalid scope (missing '/'): \"" + scope + "\"");
		}

		// Ensure that there are no other slashes before the query string
		var scopeBeforeQuery = scope.contains("?") ? scope.substring(0, scope.indexOf('?')) : scope;
		if (scopeBeforeQuery.indexOf('/', slashIndex + 1) >= 0) {
			throw new InvalidScopeException("Invalid scope (multiple '/' characters): \"" + scope + "\"");
		}

		var context = scope.substring(0, slashIndex);
		var afterSlash = scope.substring(slashIndex + 1);

		// Split on the first '.' to separate resource type from permission suffix.
		var dotIndex = afterSlash.indexOf('.');
		if (dotIndex < 0) {
			throw new InvalidScopeException("Invalid scope (missing '.' permission suffix): \"" + scope + "\"");
		}

		var resourceType = afterSlash.substring(0, dotIndex);
		var permissionAndQuery = afterSlash.substring(dotIndex + 1);

		var questionIndex = permissionAndQuery.indexOf("?");
		if (questionIndex >= 0) {
			var permissions = permissionAndQuery.substring(0, questionIndex);
			var queryString = permissionAndQuery.substring(questionIndex + 1);
			return new ScopeSegments(context, resourceType, permissions, queryString);
		} else {
			return new ScopeSegments(context, resourceType, permissionAndQuery, null);
		}
	}

	/**
	 * Validates that the context token is one of the three permitted values.
	 *
	 * @param context
	 *           a scope's context token
	 * @param fullScope
	 *           the full scope string, for context in error messages
	 */
	private static void validateContext(String context, String fullScope) {
		if (!context.equals("patient") && !context.equals("user") && !context.equals("system")) {
			throw new InvalidScopeException("Invalid scope context \"" + context
					+ "\" (must be 'patient', 'user', or 'system'): \"" + fullScope + "\"");
		}
	}

	/**
	 * Validates that the resource type is either the wildcard {@code *} or a non-empty string that starts with an
	 * uppercase letter, which is the convention for all FHIR resource types.
	 *
	 * @param resourceType
	 *           FHIR resource type
	 * @param fullScope
	 *           full scope string (for context in error messages)
	 */
	private static void validateResourceType(String resourceType, String fullScope) {
		if (resourceType.isEmpty()) {
			throw new InvalidScopeException(
					"Invalid scope (empty resource type): \"" + fullScope + "\"");
		}
		if (!resourceType.equals("*") && !Character.isUpperCase(resourceType.charAt(0))) {
			throw new InvalidScopeException(
					"Invalid scope (resource type must be '*' or start with an uppercase letter): \"" + fullScope + "\"");
		}
	}

	/**
	 * Reports whether the given token is a V1 permission keyword.
	 *
	 * @param token
	 *           the token to check
	 * @return true if the token is one of the allowed V1 permissions, otherwise false
	 */
	private static boolean isV1Permission(String token) {
		// V1 keywords are exactly "read", "write", or "*" — no query string.
		return token.equals("read")
				|| token.equals("write")
				|| token.equals("*");
	}

	/**
	 * Maps a validated V1 permission string to its V2 equivalent.
	 *
	 * @param v1Permission
	 *           the permission to convert
	 * @param fullScope
	 *           the full scope string (for context in error messages)
	 * @return the equivalent V2 permission
	 */
	private static String mapV1ToV2Permission(String v1Permission, String fullScope) {
		return switch (v1Permission) {
			case "read" -> "rs";
			case "write" -> "cud";
			case "*" -> "cruds";
			default -> throw new InvalidScopeException(
					"Unrecognized V1 permission \"" + v1Permission + "\" in scope: \"" + fullScope + "\"");
		};
	}

	/**
	 * Validates a V2 permission string, which may optionally be followed by a {@code ?param=value} query string.
	 *
	 * <p>
	 * The permission characters must be a non-empty, ordered subset of {@code "cruds"} (i.e. they must appear in that
	 * relative order with no duplicates).
	 *
	 * @param permissionAndQuery
	 *           the tail of a scope string, with permission and optional query string
	 * @param fullScope
	 *           the full scope string (for context in error messages)
	 */
	private static void validateV2PermissionAndQuery(String permissions, String queryString, String fullScope) {
		if (permissions.isEmpty()) {
			throw new InvalidScopeException("Invalid scope (empty permission string): \"" + fullScope + "\"");
		}

		// Each character must be in V2_PERMISSION_ORDER, in order, no duplicates.
		var lastIndex = -1;
		for (char c : permissions.toCharArray()) {
			var idx = V2_PERMISSION_ORDER.indexOf(c);
			if (idx < 0) {
				throw new InvalidScopeException("Invalid scope (unknown permission character '" + c
						+ "', must be a subset of \"" + V2_PERMISSION_ORDER + "\"): \"" + fullScope + "\"");
			}
			if (idx <= lastIndex) {
				throw new InvalidScopeException("Invalid scope (permission characters must be in order \""
						+ V2_PERMISSION_ORDER + "\" with no duplicates): \"" + fullScope + "\"");
			}
			lastIndex = idx;
		}

		// Validate query string: must not be empty if the '?' separator was present.
		if (queryString != null && queryString.isBlank()) {
			throw new InvalidScopeException("Invalid scope (trailing '?' with no query string): \"" + fullScope + "\"");
		}
	}

}
