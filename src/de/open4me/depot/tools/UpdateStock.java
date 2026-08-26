package de.open4me.depot.tools;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.gui.dialogs.KursAktualisierenAnbieterAuswahlDialog;
import de.open4me.depot.kursprovider.KursAbrufResult;
import de.open4me.depot.kursprovider.KursProvider;
import de.open4me.depot.kursprovider.KursProviderEinstellung;
import de.open4me.depot.kursprovider.KursProviderErgebnis;
import de.open4me.depot.kursprovider.KursProviderKontext;
import de.open4me.depot.kursprovider.KursProviderRegistry;
import de.open4me.depot.kursprovider.KursAbrufResult.Ereignis;
import de.open4me.depot.kursprovider.KursAbrufResult.Kurs;
import de.open4me.depot.messaging.KursUpdatesMsg;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.open4me.depot.sql.SQLUtils.PreparedSQL;
import de.willuhn.jameica.system.Application;
import de.willuhn.jameica.system.BackgroundTask;
import de.willuhn.jameica.system.OperationCanceledException;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.ProgressMonitor;
import jsq.fetch.factory.Factory;



public class UpdateStock implements BackgroundTask {

	private  GenericObjectSQL[] wertpapiere;
	private  boolean forceNewSettings;
	private boolean abort = false;
	private final KursProviderRegistry providerRegistry = KursProviderRegistry.createDefault();

	public UpdateStock(GenericObjectSQL[] context, boolean forceNewSettings)   {
		this.wertpapiere = context;
		this.forceNewSettings = forceNewSettings;
		setProxy();
	}

	private void setProxy() {
		boolean useSystem = Application.getConfig().getUseSystemProxy();
		try {
			if (useSystem) {
				List<Proxy> proxies;
				proxies = ProxySelector.getDefault().select(new URI("https://www.willuhn.de/"));
				Logger.info("Using system proxy settings: " + proxies);
				for (Proxy p : proxies) {
					if (p.type() == Proxy.Type.HTTP && p.address() instanceof InetSocketAddress) {
						InetSocketAddress addr = (InetSocketAddress) p.address();
						Factory.setProxy(addr.getHostString(), addr.getPort());
						return;
					}
				}
				Logger.error("No default Proxy found");
			} else {
				String host = Application.getConfig().getHttpsProxyHost();
				int port = Application.getConfig().getHttpsProxyPort();
				if (host != null && host.length() > 0 && port > 0) {
					Factory.setProxy(host, port);
					return;
				}
			}
			Logger.info("Keine gültige Proxy-Einstellunge gefunden. (" + useSystem + ")");
		} catch (URISyntaxException e) {
			Logger.error("Keine gültige Proxy-Einstellunge gefunden", e);
		}
	}

	@Override
	public void run(ProgressMonitor monitor) throws ApplicationException {
		List<String> errors = new ArrayList<String>();
		float proWert = wertpapiere.length == 0 ? 100.0f : 100.0f / wertpapiere.length;
		float babysteps = proWert / 5;
		try {
			for (int i = 0; i < wertpapiere.length; i++) {
				if (abort) return;
				GenericObjectSQL wertpapier = wertpapiere[i];
				String name = String.valueOf(wertpapier.getAttribute("wertpapiername"));
				String searchterm = getSearchTerm(wertpapier);
				monitor.setPercentComplete((int) (proWert * i));
				monitor.setStatusText("Starte mit " + (searchterm == null ? name : searchterm));
				try {
					processStock(wertpapier, searchterm, monitor, babysteps);
				}
				catch (OperationCanceledException e)
				{
					return;
				}
				catch (Exception e)
				{
					Logger.error("Fehler beim Kursabruf für " + name, e);
					errors.add(name + ": " + readableMessage(e));
				}
			}
		}
		catch (Exception e)
		{
			throw new ApplicationException("Fehler beim Abruf der Kurse.", e);
		}
		finally
		{
			providerRegistry.close();
		}

		monitor.setPercentComplete(100);
		if (!errors.isEmpty())
		{
			String message = errors.size() + " Kursabruf(e) fehlgeschlagen:\n" + String.join("\n", errors);
			monitor.setStatus(ProgressMonitor.STATUS_ERROR);
			monitor.setStatusText(message);
			throw new ApplicationException(message);
		}
		monitor.setStatusText("Fertig");
		monitor.setStatus(ProgressMonitor.STATUS_DONE);
	}

	private void processStock(GenericObjectSQL wertpapier, String searchterm, ProgressMonitor monitor,
			float babysteps) throws Exception
	{
		String wpid = wertpapier.getID();
		String savedProvider = getAnbieterName(wpid);
		boolean manual = savedProvider == null || forceNewSettings;
		KursProvider provider;
		boolean vorbereiten = false;
		boolean saveSettings = false;
		if (manual)
		{
			KursAktualisierenAnbieterAuswahlDialog dialog = new KursAktualisierenAnbieterAuswahlDialog(
					KursAktualisierenAnbieterAuswahlDialog.POSITION_CENTER,
					wertpapier.getAttribute("wertpapiername").toString(), providerRegistry);
			provider = (KursProvider) dialog.open();
			if (provider == null) throw new OperationCanceledException("Abgebrochen");
			vorbereiten = dialog.isForceConnect();
			saveSettings = dialog.getSpeichernSetting();
		}
		else
		{
			provider = providerRegistry.findById(savedProvider);
			if (provider == null)
				throw new ApplicationException("Gespeicherter Kursprovider ist nicht mehr verfügbar. Bitte Einstellungen neu vornehmen.");
		}

		KursProviderKontext context = new KursProviderKontext(wertpapier, searchterm, manual,
				vorbereiten, monitor, babysteps, () -> abort);
		KursProviderErgebnis result = provider.abrufen(context);
		if (abort) return;
		Abruf abruf = new Abruf(provider, result.getKurse(), saveSettings, result.getKonfiguration());
		boolean replaceAll = provider.ersetztBestandBeimWechsel() && !provider.getId().equals(savedProvider);
		monitor.setStatusText("Speichern");
		saveStockData(wertpapier, abruf, replaceAll);
		monitor.setStatusText("Fertig mit " + searchterm);
		Application.getMessagingFactory().sendMessage(new KursUpdatesMsg(wpid));
	}

	private static String readableMessage(Throwable error)
	{
		Throwable current = error;
		while ((current.getMessage() == null || current.getMessage().isBlank()) && current.getCause() != null)
			current = current.getCause();
		return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
	}

	private String getSearchTerm(GenericObjectSQL wertpapier) throws RemoteException {
		String searchterm = null;
		if (!wertpapier.isEmpty("isin")) {
			searchterm = wertpapier.getAttribute("isin").toString();
		} else if (!wertpapier.isEmpty("wkn")) {
			searchterm = wertpapier.getAttribute("wkn").toString();
		}
		return searchterm;
	}

	// Speichert Kursdaten und eine optional geänderte Anbieter-Konfiguration atomar.
	private static void saveStockData(GenericObjectSQL wertpapier, Abruf abruf, boolean replaceAll) throws Exception {
		try(Connection conn = SQLUtils.getConnection()) {
			conn.setAutoCommit(false);
			try {
				if (replaceAll)
				{
					try (PreparedStatement del = conn.prepareStatement("delete from depotviewer_kurse where wpid = ?"))
					{
						del.setString(1, wertpapier.getID());
						del.executeUpdate();
					}
					try (PreparedStatement del = conn.prepareStatement("delete from depotviewer_kursevent where wpid = ?"))
					{
						del.setString(1, wertpapier.getID());
						del.executeUpdate();
					}
				}

				try(PreparedStatement del = conn.prepareStatement("delete from depotviewer_kurse where wpid = ? and kursdatum = ?")) {
					for (Kurs kurs : abruf.result.getKurse()) {
						del.setString(1, wertpapier.getID());
						del.setDate(2, java.sql.Date.valueOf(kurs.getDatum()));
						del.addBatch();
					}
					del.executeBatch();
				}
				try(PreparedStatement insert = conn.prepareStatement("insert into depotviewer_kurse (wpid, kurs, kursw, kursdatum) values (?,?,?,?)")) {
					for (Kurs kurs : abruf.result.getKurse()) {
						insert.setString(1, wertpapier.getID());
						insert.setBigDecimal(2, kurs.getWert());
						insert.setString(3, kurs.getWaehrung());
						insert.setDate(4, java.sql.Date.valueOf(kurs.getDatum()));
						insert.addBatch();
					}
					insert.executeBatch();
				}

				if (abruf.result.getEreignisse() != null) {
					try(PreparedStatement del = conn.prepareStatement("delete from depotviewer_kursevent where wpid = ? and datum = ?")) {
						for (Ereignis event : abruf.result.getEreignisse()) {
							del.setString(1, wertpapier.getID());
							del.setDate(2, java.sql.Date.valueOf(event.getDatum()));
							del.addBatch();
						}
						del.executeBatch();
					}
					try(PreparedStatement insert = conn.prepareStatement("insert into depotviewer_kursevent (wpid, ratio, value, aktion, datum, waehrung) values (?,?,?,?,?,?)")) {
						for (Ereignis event : abruf.result.getEreignisse()) {
							insert.setString(1, wertpapier.getID());
							insert.setString(2, event.getRatio());
							insert.setBigDecimal(3, event.getWert());
							insert.setString(4, event.getAktion());
							insert.setDate(5, java.sql.Date.valueOf(event.getDatum()));
							insert.setString(6, event.getWaehrung());
							insert.addBatch();
						}
						insert.executeBatch();
					}
				}

				if (abruf.saveSettings)
					saveSettings(conn, wertpapier.getID(), abruf.provider.getId(), abruf.configs);
				calcPerformanceKurse(wertpapier, conn);
				conn.commit();
			}
			catch (Exception e)
			{
				conn.rollback();
				throw e;
			}
		}
		Utils.markRecalc(null);
	}

	private static String getAnbieterName(String wpid) throws Exception {

		try(PreparedSQL preparedSQL = SQLUtils.getPreparedSQL("select value from depotviewer_cfgupdatestock where `wpid`= ? and `key` is null")) {
			preparedSQL.prest.setString(1, wpid);
			return (String) SQLUtils.getObject(preparedSQL.prest);
		}
	}

	private static void saveSettings(Connection conn, String wpid, String provider,
			List<KursProviderEinstellung> einstellungen) throws Exception {
		try (PreparedStatement delete = conn.prepareStatement("delete from depotviewer_cfgupdatestock where `wpid`= ?"))
		{
			delete.setString(1, wpid);
			delete.executeUpdate();
		}
		try(PreparedStatement insert = conn.prepareStatement("insert into depotviewer_cfgupdatestock (`wpid`, `key`, `value`) values (?,?,?)")) {
			insert.setString(1, wpid);
			insert.setString(2, null);
			insert.setString(3, provider);
			insert.executeUpdate();
			for (KursProviderEinstellung einstellung : einstellungen) {
				insert.setString(1, wpid);
				insert.setString(2, einstellung.getSchluessel());
				insert.setString(3, einstellung.getWert());
				insert.executeUpdate();
			}
		}
	}

	private static void calcPerformanceKurse(GenericObjectSQL wertpapier,
			Connection conn) throws Exception {
		// Performance Kurs berechnen
		try(PreparedStatement update = conn.prepareStatement("update depotviewer_kurse set kursperf = ? where id = ?")) {

			List<GenericObjectSQL> kurse = SQLUtils.getResultSet(conn, "select *   from depotviewer_kurse where wpid = " + wertpapier.getID() + " order by kursdatum desc", "", "id");
			Date lastKurs = null;
			if (kurse.size() > 0) {
				lastKurs = (Date) kurse.get(0).getAttribute("kursdatum");
			}
			try(PreparedStatement queryKursEvet = SQLUtils.getPreparedSQL(conn, "select *  from depotviewer_kursevent where wpid = ? and datum <= ? order by datum desc");) {
				queryKursEvet.setString(1, wertpapier.getID());
				queryKursEvet.setDate(2, SQLUtils.getSQLDate(lastKurs));
				List<GenericObjectSQL> kursevt = SQLUtils.getResultSet(queryKursEvet, "depotviewer_kursevent", "", "id");
				int kurseEvtIdx = 0;
				GenericObjectSQL currentEvt = null;
				if (kursevt.size() > 0 ) {
					currentEvt = kursevt.get(kurseEvtIdx);
				}
				BigDecimal korrektur = new BigDecimal("0.0000");
				BigDecimal faktor = new BigDecimal("1.0000");
				for (GenericObjectSQL kurs : kurse) {
					if (kurseEvtIdx < kursevt.size()) {
						Date kursdatum = (Date) kurs.getAttribute("kursdatum");
						Date evtdatum = (Date) currentEvt.getAttribute("datum");
						if (evtdatum.getTime() > kursdatum.getTime()) {
							String action = currentEvt.getAttribute("aktion").toString();
							if (action.equals("D")) {
								korrektur = korrektur.subtract(faktor.multiply((BigDecimal) currentEvt.getAttribute("value")));
							}  else if (action.equals("S") || action.equals("R")) { // split or reverse split
								String[] s = ((String) currentEvt.getAttribute("ratio")).split(":");
								BigDecimal splitfaktor = (new BigDecimal(s[0])).divide(new BigDecimal(s[1]), 10, RoundingMode.HALF_UP);
								faktor = faktor.multiply(splitfaktor);
							}  else if (action.equals("G")) {
								String[] s = ((String) currentEvt.getAttribute("ratio")).split(":");
								BigDecimal splitfaktor = (new BigDecimal(s[0])).divide((new BigDecimal(s[1])).add(new BigDecimal(s[0])), 10, RoundingMode.HALF_UP);
								faktor = faktor.multiply(splitfaktor);
							}
							kurseEvtIdx++;
							if (kurseEvtIdx < kursevt.size()) {
								currentEvt = kursevt.get(kurseEvtIdx);
							}
						}
					}
					BigDecimal k = faktor.multiply((BigDecimal) kurs.getAttribute("kurs"));
					k = k.add(korrektur);
					update.setBigDecimal(1, k);
					update.setString(2, kurs.getID());
					update.addBatch();;
				}
			}
			update.executeBatch();
		}
	}

	private static final class Abruf
	{
		final KursProvider provider;
		final KursAbrufResult result;
		final boolean saveSettings;
		final List<KursProviderEinstellung> configs;

		Abruf(KursProvider provider, KursAbrufResult result, boolean saveSettings,
				List<KursProviderEinstellung> configs)
		{
			this.provider = provider;
			this.result = result;
			this.saveSettings = saveSettings;
			this.configs = configs;
		}
	}



	@Override
	public void interrupt() {
		abort = true;

	}

	@Override
	public boolean isInterrupted() {
		return abort;
	}
}
