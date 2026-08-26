package de.open4me.depot.kursprovider.scalable;

import javax.xml.bind.annotation.XmlRootElement;

/** Verschlüsselt gespeicherte OAuth-Sitzung des Scalable-MCP-Clients. */
@XmlRootElement
public class ScalableOAuthData
{
	private String clientId;
	private String accessToken;
	private String refreshToken;
	private String tokenType;
	private String scope;
	private long expiresAtEpochSecond;
	private String redirectUri;

	public String getClientId() { return clientId; }
	public void setClientId(String clientId) { this.clientId = clientId; }
	public String getAccessToken() { return accessToken; }
	public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
	public String getRefreshToken() { return refreshToken; }
	public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
	public String getTokenType() { return tokenType; }
	public void setTokenType(String tokenType) { this.tokenType = tokenType; }
	public String getScope() { return scope; }
	public void setScope(String scope) { this.scope = scope; }
	public long getExpiresAtEpochSecond() { return expiresAtEpochSecond; }
	public void setExpiresAtEpochSecond(long expiresAtEpochSecond) { this.expiresAtEpochSecond = expiresAtEpochSecond; }
	public String getRedirectUri() { return redirectUri; }
	public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
}
