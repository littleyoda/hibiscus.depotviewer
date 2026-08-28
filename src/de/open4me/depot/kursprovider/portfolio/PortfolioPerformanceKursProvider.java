package de.open4me.depot.kursprovider.portfolio;

import java.sql.PreparedStatement;
import java.util.List;

import de.open4me.depot.gui.dialogs.KursAktualisierenDialog;
import de.open4me.depot.gui.dialogs.KursanbieterDialogErweiterung;
import de.open4me.depot.gui.dialogs.PortfolioPerformanceKursanbieterDialogErweiterung;
import de.open4me.depot.kursprovider.KursProvider;
import de.open4me.depot.kursprovider.KursProviderEinstellung;
import de.open4me.depot.kursprovider.KursProviderErgebnis;
import de.open4me.depot.kursprovider.KursProviderKontext;
import de.open4me.depot.kursprovider.portfolio.PortfolioPerformanceApi.Market;
import de.open4me.depot.sql.SQLUtils;
import de.open4me.depot.sql.SQLUtils.PreparedSQL;
import de.willuhn.jameica.gui.Action;
import de.willuhn.util.ApplicationException;
import jsq.config.Config;

/** Einbindung der Portfolio-Performance-Marktdaten-API in den Providervertrag. */
public final class PortfolioPerformanceKursProvider implements KursProvider
{
	public static final String ID = "portfolio-performance";
	public static final String WEBSITE = "https://www.portfolio-performance.info/";
	private static final String SYMBOL = "symbol";
	private static final String EXCHANGE = "exchange";
	private static final String CURRENCY = "currency";

	private final PortfolioPerformanceTokenStore tokenStore;
	private final PortfolioPerformanceOAuthClient oauth;
	private final PortfolioPerformanceApi api;

	public PortfolioPerformanceKursProvider()
	{
		this.tokenStore = new PortfolioPerformanceTokenStore();
		this.oauth = new PortfolioPerformanceOAuthClient(tokenStore);
		this.api = new PortfolioPerformanceApi(oauth);
	}

	@Override public String getId() { return ID; }
	@Override public String getName() { return "Portfolio Performance"; }
	@Override public String getWebsite() { return WEBSITE; }
	@Override public String toString() { return getName(); }

	public boolean isConnected() { return oauth.hasSession(); }
	public void disconnect() throws Exception { oauth.disconnect(); }

	@Override
	public KursProviderErgebnis abrufen(KursProviderKontext context) throws Exception
	{
		if (context.getWertpapier().isEmpty("isin"))
			throw new ApplicationException("Portfolio Performance benötigt eine ISIN; für dieses Wertpapier ist keine hinterlegt.");
		if (context.isVorbereiten()) oauth.reconnect(context::isAbgebrochen);

		Market market = context.isManuell() ? selectMarket(context) : loadMarket(context);
		context.getMonitor().setStatusText("Portfolio Performance: Historische Kurse für " + market.getSymbol() + " abrufen");
		return new KursProviderErgebnis(api.fetch(market, context::isAbgebrochen), List.of(
				new KursProviderEinstellung(SYMBOL, market.getSymbol()),
				new KursProviderEinstellung(EXCHANGE, market.getExchange()),
				new KursProviderEinstellung(CURRENCY, market.getCurrency())));
	}

	private Market selectMarket(KursProviderKontext context) throws Exception
	{
		String isin = context.getWertpapier().getAttribute("isin").toString();
		context.getMonitor().setStatusText("Portfolio Performance: Börsenplätze suchen");
		List<Market> markets = api.searchByIsin(isin);
		Config config = new Config("Börsenplatz");
		for (Market market : markets) config.addAuswahl(market.toString(), market);
		new KursAktualisierenDialog(KursAktualisierenDialog.POSITION_CENTER, List.of(config)).open();
		if (config.getSelected().isEmpty())
			throw new ApplicationException("Es wurde kein Portfolio-Performance-Börsenplatz ausgewählt.");
		return (Market) config.getSelected().get(0).getObj();
	}

	private Market loadMarket(KursProviderKontext context) throws Exception
	{
		try (PreparedSQL sql = SQLUtils.getPreparedSQL(
				"select value from depotviewer_cfgupdatestock where `wpid`= ? and `key` = ?"))
		{
			PreparedStatement query = sql.prest;
			query.setString(1, context.getWertpapier().getID());
			String symbol = setting(query, SYMBOL);
			String exchange = setting(query, EXCHANGE);
			String currency = setting(query, CURRENCY);
			Market market = new Market(symbol, exchange, currency);
			if (!market.isValid())
				throw new ApplicationException("Gespeicherte Portfolio-Performance-Einstellungen sind nicht mehr gültig. Bitte neu einrichten.");
			return market;
		}
	}

	private static String setting(PreparedStatement query, String key) throws Exception
	{
		query.setString(2, key);
		return (String) SQLUtils.getObject(query);
	}

	@Override
	public KursanbieterDialogErweiterung createDialogErweiterung(Action vorbereiten)
	{
		return createDialogErweiterung(vorbereiten, () -> {});
	}

	@Override
	public KursanbieterDialogErweiterung createDialogErweiterung(Action vorbereiten, Runnable statusGeaendert)
	{
		return new PortfolioPerformanceKursanbieterDialogErweiterung(this, vorbereiten, statusGeaendert);
	}

	@Override public boolean istAbrufbereit() { return isConnected(); }
	@Override public boolean ersetztBestandBeimWechsel() { return true; }
}
