package org.octri.fhir_sandbox_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth Authorization Server properties.
 */
@ConfigurationProperties(prefix = "octri.sandbox.oauth2")
public class OAuthAuthorizationServerProperties {

	private String authorizeAddress;
	private String tokenAddress;
	private String registerAddress;
	private String jwkSetAddress;

	public String getAuthorizeAddress() {
			return authorizeAddress;
	}

	public void setAuthorizeAddress(String authorizeAddress) {
			this.authorizeAddress = authorizeAddress;
	}

	public String getTokenAddress() {
			return tokenAddress;
	}

	public void setTokenAddress(String tokenAddress) {
			this.tokenAddress = tokenAddress;
	}

	public String getRegisterAddress() {
		return registerAddress;
	}

	public void setRegisterAddress(String registerAddress) {
		this.registerAddress = registerAddress;
	}

	public String getJwkSetAddress() {
		return jwkSetAddress;
	}

	public void setJwkSetAddress(String jwkSetAddress) {
		this.jwkSetAddress = jwkSetAddress;
	}

	@Override
	public String toString() {
		return "OAuthAuthorizationServerProperties [authorizeAddress=" + authorizeAddress + ", tokenAddress="
				+ tokenAddress + ", registerAddress=" + registerAddress + ", jwkSetAddress=" + jwkSetAddress + "]";
	}

}
