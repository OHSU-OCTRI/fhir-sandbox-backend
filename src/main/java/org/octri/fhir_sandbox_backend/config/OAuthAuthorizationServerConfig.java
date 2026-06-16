package org.octri.fhir_sandbox_backend.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;

import org.octri.fhir_sandbox_backend.interceptor.CapabilityStatementCustomizer;
import org.octri.fhir_sandbox_backend.interceptor.JwtAuthorizationInterceptor;
import org.octri.fhir_sandbox_backend.interceptor.SmartWellKnownInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jwt.proc.JWTProcessor;

import ca.uhn.fhir.rest.server.RestfulServer;

/**
 * OAuth authorization server configuration. Reads the configured OAuth endpoint addresses from the application
 * properties and registers a CapabilityStatementCustomizer interceptor to include the OAuth endpoints in the server's
 * CapabilityStatement.
 */
@Configuration
@EnableConfigurationProperties(OAuthAuthorizationServerProperties.class)
public class OAuthAuthorizationServerConfig {

	private static final Logger log = LoggerFactory.getLogger(OAuthAuthorizationServerConfig.class);

	private final OAuthAuthorizationServerProperties properties;

	public OAuthAuthorizationServerConfig(OAuthAuthorizationServerProperties properties, RestfulServer restfulServer,
			ObjectMapper objectMapper) {
		log.info("Initializing OAuthAuthorizationServerConfig with properties: {}", properties);
		this.properties = properties;
		restfulServer.registerInterceptor(this.capabilityStatementCustomizer());
		restfulServer.registerInterceptor(this.smartWellKnownInterceptor());

		if (Boolean.TRUE.equals(properties.getEnableTokenAuth())) {
			log.info("Enabling token authentication");
			try {
				restfulServer.registerInterceptor(this.jwtAuthorizationInterceptor(objectMapper));
			} catch (MalformedURLException e) {
				log.error("JWK source URL is malformed: {}", e.getMessage(), e);
				throw new IllegalArgumentException(
						"octri.sandbox.oauth2.jwkset URL (" + properties.getJwkSetAddress() + ") is malformed", e);
			}
		}
	}

	private CapabilityStatementCustomizer capabilityStatementCustomizer() {
		log.debug("Creating CapabilityStatementCustomizer with authorizeAddress={} and tokenAddress={}",
				this.properties.getAuthorizeAddress(), this.properties.getTokenAddress());
		return new CapabilityStatementCustomizer(
				this.properties.getAuthorizeAddress(),
				this.properties.getTokenAddress());
	}

	private SmartWellKnownInterceptor smartWellKnownInterceptor() {
		log.debug("Creating SmartWellKnownInterceptor with authorizeAddress={} and tokenAddress={}",
				this.properties.getAuthorizeAddress(), this.properties.getTokenAddress());
		return new SmartWellKnownInterceptor(this.properties);
	}

	private JwtAuthorizationInterceptor jwtAuthorizationInterceptor(ObjectMapper objectMapper)
			throws MalformedURLException {
		log.debug("Creating JwtAuthorizationInterceptor");
		return new JwtAuthorizationInterceptor(jwtProcessor(), objectMapper);
	}

	private JWKSource<SecurityContext> jwkSource() throws MalformedURLException {
		return JWKSourceBuilder.create(new URL(properties.getJwkSetAddress()))
				.outageTolerant(true)
				.retrying(true)
				.rateLimited(true)
				.build();
	}

	private JWTProcessor<SecurityContext> jwtProcessor() throws MalformedURLException {
		var jwtProcessor = new DefaultJWTProcessor<>();
		jwtProcessor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource()));

		var claimSet = new JWTClaimsSet.Builder().issuer(properties.getIssuerAddress()).build();
		var requiredClaims = new HashSet<>(Arrays.asList(JWTClaimNames.SUBJECT, JWTClaimNames.ISSUED_AT,
				JWTClaimNames.EXPIRATION_TIME, JWTClaimNames.JWT_ID));
		jwtProcessor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(claimSet, requiredClaims));

		return jwtProcessor;
	}

}
