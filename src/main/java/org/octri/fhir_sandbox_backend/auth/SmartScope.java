package org.octri.fhir_sandbox_backend.auth;

/**
 * Represents a parsed SMART on FHIR scope, which encodes access context, resource type,
 * permissions (cruds), and an optional query string filter.
 *
 * <p>
 * SMART scopes follow the format: {@code <context>/<resourceType>.<permissions>?<queryString>}
 * For example: {@code patient/Observation.rs} or {@code user/*.cruds}.
 *
 * @see SmartScopeContext
 */
public class SmartScope {

	private final String rawScope;
	private final SmartScopeContext context;
	private final String resourceType;
	private final String permissions;
	private final String queryString;

	public SmartScope(String rawScope, SmartScopeContext context, String resourceType, String permissions,
			String queryString) {
		this.rawScope = rawScope;
		this.context = context;
		this.resourceType = resourceType;
		this.permissions = permissions;
		this.queryString = queryString;
	}

	/**
	 * Returns the original, unparsed scope string as received in the token.
	 *
	 * @return the raw scope string
	 */
	public String getRawScope() {
		return rawScope;
	}

	/**
	 * Returns the access context (e.g., patient, user, system) for this scope.
	 *
	 * @return the {@link SmartScopeContext}, or {@code null} if not set
	 */
	public SmartScopeContext getContext() {
		return context;
	}

	/**
	 * Returns the FHIR resource type this scope applies to (e.g., {@code Observation}, {@code *}).
	 *
	 * @return the resource type string, or {@code null} if not set
	 */
	public String getResourceType() {
		return resourceType;
	}

	/**
	 * Returns the permission characters granted by this scope (e.g., {@code cruds}).
	 *
	 * @return the permissions string, or {@code null} if not set
	 */
	public String getPermissions() {
		return permissions;
	}

	/**
	 * Returns the optional query string filter appended to the scope.
	 *
	 * @return the query string, or {@code null} if none
	 */
	public String getQueryString() {
		return queryString;
	}

	/**
	 * Returns true if this scope targets a FHIR resource context (patient, user, or system),
	 * as opposed to a non-resource scope such as {@code openid} or {@code launch}.
	 *
	 * @return true if this is a resource scope, false otherwise
	 */
	public boolean isResourceScope() {
		return context != null && !SmartScopeContext.NON_RESOURCE.equals(context);
	}

	/**
	 * Returns the compartment name derived from the scope context (e.g., {@code "patient"}).
	 *
	 * @return the lowercase compartment name
	 * @throws IllegalStateException
	 *            if this is not a resource scope
	 */
	public String getCompartmentName() {
		if (!isResourceScope()) {
			throw new IllegalStateException("Cannot get compartment for non-resource scope");
		}

		return context.name().toLowerCase();
	}

	/**
	 * Reports whether this scope grants create permission.
	 *
	 * @return true if create is permitted, false otherwise
	 */
	public boolean allowsCreate() {
		return hasPermission("c");
	}

	/**
	 * Reports whether this scope grants read permission.
	 *
	 * @return true if read is permitted, false otherwise
	 */
	public boolean allowsRead() {
		return hasPermission("r");
	}

	/**
	 * Reports whether this scope grants update permission.
	 *
	 * @return true if update is permitted, false otherwise
	 */
	public boolean allowsUpdate() {
		return hasPermission("u");
	}

	/**
	 * Reports whether this scope grants delete permission.
	 *
	 * @return true if delete is permitted, false otherwise
	 */
	public boolean allowsDelete() {
		return hasPermission("d");
	}

	/**
	 * Reports whether this scope grants search permission.
	 *
	 * @return true if search is permitted, false otherwise
	 */
	public boolean allowsSearch() {
		return hasPermission("s");
	}

	@Override
	public String toString() {
		return rawScope;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((rawScope == null) ? 0 : rawScope.hashCode());
		result = prime * result + ((context == null) ? 0 : context.hashCode());
		result = prime * result + ((resourceType == null) ? 0 : resourceType.hashCode());
		result = prime * result + ((permissions == null) ? 0 : permissions.hashCode());
		result = prime * result + ((queryString == null) ? 0 : queryString.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SmartScope other = (SmartScope) obj;
		if (rawScope == null) {
			if (other.rawScope != null)
				return false;
		} else if (!rawScope.equals(other.rawScope))
			return false;
		if (context != other.context)
			return false;
		if (resourceType == null) {
			if (other.resourceType != null)
				return false;
		} else if (!resourceType.equals(other.resourceType))
			return false;
		if (permissions == null) {
			if (other.permissions != null)
				return false;
		} else if (!permissions.equals(other.permissions))
			return false;
		if (queryString == null) {
			if (other.queryString != null)
				return false;
		} else if (!queryString.equals(other.queryString))
			return false;
		return true;
	}

	private boolean hasPermission(String permission) {
		return permissions != null && permissions.contains(permission);
	}

}
