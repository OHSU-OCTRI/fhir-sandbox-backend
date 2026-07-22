package org.octri.fhir_sandbox_backend.auth;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

/**
 * Utility methods for working with SMART app scopes.
 */
public class AuthUtils {

	/**
	 * Scopes grouped by resource context / compartment.
	 */
	public record GroupedScopes(List<SmartScope> patientScopes, List<SmartScope> userScopes,
			List<SmartScope> systemScopes, List<SmartScope> nonResourceScopes) {

		public GroupedScopes {
			if (patientScopes == null) {
				patientScopes = List.of();
			}

			if (userScopes == null) {
				userScopes = List.of();
			}

			if (systemScopes == null) {
				systemScopes = List.of();
			}

			if (nonResourceScopes == null) {
				nonResourceScopes = List.of();
			}
		}

	}

	/**
	 * Returns the given list of scopes, grouped by context / compartment.
	 *
	 * @param scopes
	 *           list of scopes
	 * @return grouped scopes
	 */
	public static GroupedScopes groupScopesByContext(Set<SmartScope> scopes) {
		Assert.notNull(scopes, "Scope list is required");
		var groups = scopes.stream()
				.collect(Collectors.groupingBy(SmartScope::getContext));
		return new GroupedScopes(groups.get(SmartScopeContext.PATIENT), groups.get(SmartScopeContext.USER),
				groups.get(SmartScopeContext.SYSTEM), groups.get(SmartScopeContext.NON_RESOURCE));
	}

}
