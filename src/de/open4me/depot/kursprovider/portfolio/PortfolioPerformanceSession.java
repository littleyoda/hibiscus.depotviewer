package de.open4me.depot.kursprovider.portfolio;

/** Im Jameica-Wallet gespeicherte OAuth-Sitzung. */
public class PortfolioPerformanceSession
{
	private String refreshToken;
	private String idToken;
	private String accessToken;
	private String scope;
	private long expiresAtEpochSecond;

	public String getRefreshToken() { return refreshToken; }
	public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
	public String getIdToken() { return idToken; }
	public void setIdToken(String idToken) { this.idToken = idToken; }
	public String getAccessToken() { return accessToken; }
	public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
	public String getScope() { return scope; }
	public void setScope(String scope) { this.scope = scope; }
	public long getExpiresAtEpochSecond() { return expiresAtEpochSecond; }
	public void setExpiresAtEpochSecond(long expiresAtEpochSecond) { this.expiresAtEpochSecond = expiresAtEpochSecond; }
}
