package de.open4me.depot.kursprovider;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.open4me.depot.gui.dialogs.KursAktualisierenDialog;
import de.open4me.depot.sql.SQLUtils;
import de.open4me.depot.sql.SQLUtils.PreparedSQL;
import de.open4me.depot.kursprovider.KursAbrufResult.Ereignis;
import de.open4me.depot.kursprovider.KursAbrufResult.Kurs;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;
import jsq.config.Config;
import jsq.config.ConfigTuple;
import jsq.datastructes.Const;
import jsq.datastructes.Datacontainer;
import jsq.fetcher.history.BaseFetcher;

/** Adapter eines JavaStockQuotes-History-Fetchers auf den allgemeinen Providervertrag. */
public final class JavaStockQuotesKursProvider implements KursProvider
{
	private final BaseFetcher fetcher;

	public JavaStockQuotesKursProvider(BaseFetcher fetcher)
	{
		this.fetcher = fetcher;
	}

	@Override public String getId() { return fetcher.getName(); }
	@Override public String getName() { return fetcher.getName(); }
	@Override public String getWebsite() { return fetcher.getURL(); }
	@Override public String toString() { return getName(); }

	@Override
	public KursProviderErgebnis abrufen(KursProviderKontext context) throws Exception
	{
		LocalDate heute = LocalDate.now();
		fetcher.prepare(context.getSuchbegriff(), 2000, 1, 1,
				heute.getYear(), heute.getMonthValue(), heute.getDayOfMonth());
		List<Config> gespeichert = new ArrayList<Config>();
		if (context.isManuell())
			manuellKonfigurieren(context, gespeichert);
		else
			gespeichertKonfigurieren(context);
		List<KursProviderEinstellung> einstellungen = new ArrayList<KursProviderEinstellung>();
		for (Config config : gespeichert)
			for (ConfigTuple selected : config.getSelected())
				einstellungen.add(new KursProviderEinstellung(config.getBeschreibung(), selected.getDescription()));
		return new KursProviderErgebnis(toResult(), einstellungen);
	}

	private void manuellKonfigurieren(KursProviderKontext context, List<Config> gespeichert) throws Exception
	{
		while (fetcher.hasMoreConfig())
		{
			if (context.isAbgebrochen()) return;
			List<Config> configs = fetcher.getConfigs();
			context.getMonitor().setPercentComplete((int) (context.getMonitor().getPercentComplete()
					+ context.getFortschrittsschritt()));
			context.getMonitor().setStatusText(configs.toString());
			new KursAktualisierenDialog(KursAktualisierenDialog.POSITION_CENTER, configs).open();
			fetcher.process(configs);
			gespeichert.addAll(configs);
		}
	}

	private void gespeichertKonfigurieren(KursProviderKontext context) throws Exception
	{
		try (PreparedSQL sql = SQLUtils.getPreparedSQL(
				"select value from depotviewer_cfgupdatestock where `wpid`= ? and `key` = ?"))
		{
			PreparedStatement query = sql.prest;
			query.setString(1, context.getWertpapier().getID());
			while (fetcher.hasMoreConfig())
			{
				if (context.isAbgebrochen()) return;
				List<Config> configs = fetcher.getConfigs();
				for (Config config : configs)
				{
					query.setString(2, config.getBeschreibung());
					String value = (String) SQLUtils.getObject(query);
					ConfigTuple selected = null;
					for (ConfigTuple option : config.getOptions())
						if (option.getDescription().toString().equals(value)) { selected = option; break; }
					if (selected == null)
						throw new ApplicationException("Gespeicherte Einstellungen des Kursproviders sind nicht mehr gültig.");
					config.addSelectedOptions(selected);
				}
				context.getMonitor().setPercentComplete((int) (context.getMonitor().getPercentComplete()
						+ context.getFortschrittsschritt()));
				context.getMonitor().setStatusText(configs.toString());
				fetcher.process(configs);
			}
		}
	}

	private KursAbrufResult toResult()
	{
		List<Kurs> quotes = new ArrayList<Kurs>();
		for (Datacontainer data : fetcher.getHistQuotes())
		{
			Date date = (Date) data.data.get("date");
			quotes.add(new Kurs(Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate(),
					(BigDecimal) data.data.get("last"), (String) data.data.get("currency")));
		}
		List<Ereignis> events = null;
		if (fetcher.getHistEvents() != null)
		{
			events = new ArrayList<Ereignis>();
			for (Datacontainer data : fetcher.getHistEvents())
			{
				String action = mapAction((String) data.data.get("action"));
				if (action == null) continue;
				Date date = (Date) data.data.get("date");
				events.add(new Ereignis(Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate(),
						(String) data.data.get("ratio"), (BigDecimal) data.data.get("value"), action,
						(String) data.data.get("currency")));
			}
		}
		return new KursAbrufResult(quotes, events);
	}

	private static String mapAction(String action)
	{
		if (Const.CASHDIVIDEND.equals(action)) return "D";
		if (Const.STOCKDIVIDEND.equals(action)) return "G";
		if (Const.STOCKSPLIT.equals(action)) return "S";
		if (Const.STOCKREVERSESPLIT.equals(action)) return "R";
		if (Const.SUBSCRIPTIONRIGHTS.equals(action)) return "B";
		Logger.warn("Unbekanntes Kursereignis: " + action);
		return null;
	}
}
