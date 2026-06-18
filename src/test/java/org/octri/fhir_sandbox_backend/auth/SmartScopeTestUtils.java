package org.octri.fhir_sandbox_backend.auth;

class SmartScopeTestUtils {

	static SmartScope resourceScope(SmartScopeContext context, String resourceType, String permissions) {
		var raw = context.name().toLowerCase() + "/" + resourceType + "." + permissions;
		return new SmartScope(raw, context, resourceType, permissions, null);
	}

	static SmartScope resourceScope(String raw, SmartScopeContext context, String resourceType, String permissions) {
		return new SmartScope(raw, context, resourceType, permissions, null);
	}

	static SmartScope resourceScope(String raw, SmartScopeContext context, String resourceType, String permissions,
			String queryString) {
		return new SmartScope(raw, context, resourceType, permissions, queryString);
	}

	static SmartScope nonResourceScope(String raw) {
		return new SmartScope(raw, SmartScopeContext.NON_RESOURCE, null, null, null);
	}
}
