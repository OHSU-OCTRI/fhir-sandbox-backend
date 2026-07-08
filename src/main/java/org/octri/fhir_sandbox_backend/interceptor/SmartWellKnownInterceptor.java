package org.octri.fhir_sandbox_backend.interceptor;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.octri.fhir_sandbox_backend.config.OAuthAuthorizationServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Custom interceptor that handles requests to the SMART configuration endpoint
 * (/.well-known/smart-configuration).
 *
 * Based on the following example implementations:
 * <ul>
 * <li><a href="https://github.com/mcode/smart-backend-auth">mCODE SMART Backend
 * Auth</a></li>
 * <li><a href="https://groups.google.com/g/hapi-fhir/c/9b1djB7AW_E">HAPI FHIR Google Group discussion on SMART
 * configuration</a></li>
 * </ul>
 *
 * @see <a
 *      href="https://hl7.org/fhir/smart-app-launch/app-launch.html">SMART App
 *      Launch and Authorization</a>
 * @see <a
 *      href=
 *      "https://build.fhir.org/ig/HL7/smart-app-launch/conformance.html">SMART
 *      App Launch Conformance</a>
 */
@Interceptor
public class SmartWellKnownInterceptor {

	private static final Logger log = LoggerFactory.getLogger(SmartWellKnownInterceptor.class);

	private static final String AUTHORIZATION_ENDPOINT_KEY = "authorization_endpoint";
	private static final String CAPABILITIES_KEY = "capabilities";
	private static final String CODE_CHALLENGE_METHODS_SUPPORTED_KEY = "code_challenge_methods_supported";
	private static final String GRANT_TYPES_SUPPORTED_KEY = "grant_types_supported";
	private static final String INTROSPECTION_ENDPOINT_KEY = "introspection_endpoint";
	private static final String ISSUER_KEY = "issuer";
	private static final String JWK_SET_URI_KEY = "jwks_uri";
	private static final String REGISTRATION_ENDPOINT_KEY = "registration_endpoint";
	private static final String RESPONSE_TYPES_SUPPORTED_KEY = "response_types_supported";
	private static final String REVOCATION_ENDPOINT_KEY = "revocation_endpoint";
	private static final String SCOPES_SUPPORTED_KEY = "scopes_supported";
	private static final String TOKEN_ENDPOINT_KEY = "token_endpoint";

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final OAuthAuthorizationServerProperties properties;

	public SmartWellKnownInterceptor(OAuthAuthorizationServerProperties properties) {
		this.properties = properties;
	}

	@Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)
	public boolean processIncomingRequest(HttpServletRequest request, HttpServletResponse response) {
		String requestURI = request.getRequestURI();
		log.info("Received request for URI: {}", requestURI);

		if (requestURI.endsWith("/.well-known/smart-configuration")) {
			log.debug("Handling SMART configuration request");
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");

			ObjectNode smartConfig = objectMapper.createObjectNode();
			smartConfig.put(ISSUER_KEY, this.properties.getIssuerAddress());
			smartConfig.put(AUTHORIZATION_ENDPOINT_KEY, this.properties.getAuthorizeAddress());
			smartConfig.put(TOKEN_ENDPOINT_KEY, this.properties.getTokenAddress());

			if (!StringUtils.isEmpty(this.properties.getRegisterAddress())) {
				smartConfig.put(REGISTRATION_ENDPOINT_KEY, this.properties.getRegisterAddress());
			}

			if (!StringUtils.isEmpty(this.properties.getJwkSetAddress())) {
				smartConfig.put(JWK_SET_URI_KEY, this.properties.getJwkSetAddress());
			}

			if (!StringUtils.isEmpty(this.properties.getIntrospectionAddress())) {
				smartConfig.put(INTROSPECTION_ENDPOINT_KEY, this.properties.getIntrospectionAddress());
			}

			if (!StringUtils.isEmpty(this.properties.getRevocationAddress())) {
				smartConfig.put(REVOCATION_ENDPOINT_KEY, this.properties.getRevocationAddress());
			}

			smartConfig.putArray(GRANT_TYPES_SUPPORTED_KEY)
					.add("authorization_code")
					.add("refresh_token")
					.add("urn:ietf:params:oauth:grant-type:jwt-bearer");

			smartConfig.putArray(CODE_CHALLENGE_METHODS_SUPPORTED_KEY)
					.add("S256");

			smartConfig.putArray(RESPONSE_TYPES_SUPPORTED_KEY)
					.add("code");

			smartConfig.putArray(SCOPES_SUPPORTED_KEY)
					.add("fhirUser")
					.add("launch")
					.add("launch/patient")
					.add("offline_access")
					.add("openid")
					.add("patient/*.cruds")
					.add("system/*.cruds")
					.add("user/*.cruds");

			// TODO: RFS-257 support confidential clients
			// See https://hl7.org/fhir/smart-app-launch/conformance.html#capabilities
			smartConfig.putArray(CAPABILITIES_KEY)
					.add("client-public")
					.add("context-ehr-patient")
					.add("context-standalone-patient")
					.add("launch-ehr")
					.add("launch-standalone")
					.add("permission-offline")
					.add("permission-patient")
					.add("permission-user")
					.add("permission-v1")
					.add("permission-v2")
					.add("sso-openid-connect");

			try {
				response.getWriter().write(objectMapper.writeValueAsString(smartConfig));
				response.setStatus(HttpServletResponse.SC_OK);
			} catch (IOException e) {
				log.error("Error writing SMART configuration response", e);
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
			return false; // We've handled the request, no need to continue processing
		}

		return true; // Not our endpoint, continue processing
	}

}
