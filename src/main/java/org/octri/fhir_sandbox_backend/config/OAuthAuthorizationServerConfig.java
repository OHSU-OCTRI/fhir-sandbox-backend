package org.octri.fhir_sandbox_backend.config;

import org.octri.fhir_sandbox_backend.interceptor.CapabilityStatementCustomizer;
import org.octri.fhir_sandbox_backend.interceptor.SmartWellKnownInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.rest.server.RestfulServer;

/**
 * OAuth authorization server configuration. Reads the configured OAuth endpoint addresses from the application properties
 * and registers a CapabilityStatementCustomizer interceptor to include the OAuth endpoints in the server's CapabilityStatement.
 */
@Configuration
@EnableConfigurationProperties(OAuthAuthorizationServerProperties.class)
public class OAuthAuthorizationServerConfig {

	private static final Logger log = LoggerFactory.getLogger(OAuthAuthorizationServerConfig.class);

	private final OAuthAuthorizationServerProperties properties;

	public OAuthAuthorizationServerConfig(OAuthAuthorizationServerProperties properties, RestfulServer restfulServer) {
		log.info("Initializing OAuthAuthorizationServerConfig with properties: {}", properties);
		this.properties = properties;
		restfulServer.registerInterceptor(this.capabilityStatementCustomizer());
		restfulServer.registerInterceptor(this.smartWellKnownInterceptor());
	}

	private CapabilityStatementCustomizer capabilityStatementCustomizer() {
		log.debug("Creating CapabilityStatementCustomizer with authorizeAddress={} and tokenAddress={}",
			this.properties.getAuthorizeAddress(), this.properties.getTokenAddress());
		return new CapabilityStatementCustomizer(
			this.properties.getAuthorizeAddress(),
			this.properties.getTokenAddress()
		);
	}

	private SmartWellKnownInterceptor smartWellKnownInterceptor() {
		log.debug("Creating SmartWellKnownInterceptor with authorizeAddress={} and tokenAddress={}",
			this.properties.getAuthorizeAddress(), this.properties.getTokenAddress());
		return new SmartWellKnownInterceptor(this.properties);
	}

}
