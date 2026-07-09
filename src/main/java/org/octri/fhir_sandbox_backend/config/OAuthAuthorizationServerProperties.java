package org.octri.fhir_sandbox_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth Authorization Server properties.
 */
@ConfigurationProperties(prefix = "octri.sandbox.oauth2")
public class OAuthAuthorizationServerProperties {

	private String issuerAddress;
	private String authorizeAddress;
	private String tokenAddress;
	private String registerAddress;
	private String jwkSetAddress;
	private Boolean enableTokenAuth;
	private String introspectionAddress;
	private String revocationAddress;

	public String getIssuerAddress() {
		return issuerAddress;
	}

	public void setIssuerAddress(String issuerAddress) {
		this.issuerAddress = issuerAddress;
	}

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

	public Boolean getEnableTokenAuth() {
		return enableTokenAuth;
	}

	public void setEnableTokenAuth(Boolean enableTokenAuth) {
		this.enableTokenAuth = enableTokenAuth;
	}

	public String getIntrospectionAddress() {
		return introspectionAddress;
	}

	public void setIntrospectionAddress(String introspectionAddress) {
		this.introspectionAddress = introspectionAddress;
	}

	public String getRevocationAddress() {
		return revocationAddress;
	}

	public void setRevocationAddress(String revocationAddress) {
		this.revocationAddress = revocationAddress;
	}

	@Override
	public String toString() {
		return "OAuthAuthorizationServerProperties [issuerAddress=" + issuerAddress + ", authorizeAddress="
				+ authorizeAddress + ", tokenAddress=" + tokenAddress + ", registerAddress=" + registerAddress
				+ ", jwkSetAddress=" + jwkSetAddress + ", enableTokenAuth=" + enableTokenAuth + ", introspectionAddress="
				+ introspectionAddress + ", revocationAddress=" + revocationAddress + "]";
	}

}
