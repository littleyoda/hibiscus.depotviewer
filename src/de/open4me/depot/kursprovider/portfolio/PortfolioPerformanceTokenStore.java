package de.open4me.depot.kursprovider.portfolio;

import de.willuhn.jameica.security.Wallet;
import de.willuhn.jameica.security.crypto.AESEngine;

/** Speichert die Portfolio-Performance-Sitzung verschlüsselt im Jameica-Wallet. */
public class PortfolioPerformanceTokenStore
{
	private static final String SESSION = "oauth.session";
	private Wallet wallet;

	private synchronized Wallet wallet() throws Exception
	{
		if (wallet == null)
			wallet = new Wallet(PortfolioPerformanceTokenStore.class, new AESEngine());
		return wallet;
	}

	public synchronized PortfolioPerformanceSession load() throws Exception
	{
		String json = (String) wallet().get(SESSION);
		return json == null ? null : PortfolioPerformanceHttp.JSON.readValue(json, PortfolioPerformanceSession.class);
	}

	public synchronized void save(PortfolioPerformanceSession session) throws Exception
	{
		wallet().set(SESSION, PortfolioPerformanceHttp.JSON.writeValueAsString(session));
	}

	public synchronized void clear() throws Exception
	{
		wallet().delete(SESSION);
	}

	public synchronized boolean hasSession()
	{
		try
		{
			PortfolioPerformanceSession session = load();
			return session != null && session.getRefreshToken() != null && !session.getRefreshToken().isBlank();
		}
		catch (Exception e)
		{
			return false;
		}
	}
}
