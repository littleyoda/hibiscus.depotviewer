package de.open4me.depot.tools;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import jsq.config.Config;
import jsq.config.ConfigTuple;
import jsq.datastructes.Const;
import jsq.datastructes.Datacontainer;
import jsq.fetch.factory.Factory;
import jsq.fetcher.history.BaseFetcher;
import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.gui.dialogs.KursAktualisierenAnbieterAuswahlDialog;
import de.open4me.depot.gui.dialogs.KursAktualisierenDialog;
import de.open4me.depot.messaging.KursUpdatesMsg;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.jameica.system.Application;
import de.willuhn.jameica.system.BackgroundTask;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.ProgressMonitor;


public class UpdateStock implements BackgroundTask {

	private  GenericObjectSQL[] wertpapiere;
	private  boolean forceNewSettings;
	private boolean abort = false;

	public UpdateStock(GenericObjectSQL[] context, boolean forceNewSettings)   {
		this.wertpapiere = context;
		this.forceNewSettings = forceNewSettings;
	}

	@Override
	public void run(ProgressMonitor monitor) throws ApplicationException {
		try {
			float proWert = 100.0f / wertpapiere.length; 
			float babysteps = proWert / 5;
			BaseFetcher base;
			for (int i = 0; i < wertpapiere.length; i++) {
				if (abort) {
					return;
				}
				GenericObjectSQL wertpapier = wertpapiere[i];
				String wpid = wertpapier.getAttribute("id").toString();
				String searchterm = null;
				if (!wertpapier.isEmpty("isin")) {
					searchterm = wertpapier.getAttribute("isin").toString();
				} else if (!wertpapier.isEmpty("wkn")) {
					searchterm = wertpapier.getAttribute("wkn").toString();
				}
				String anbietername = getAnbieterName(wpid);
				monitor.setPercentComplete((int) (proWert * i));
				monitor.setStatusText("Starte mit " + searchterm);
				if (anbietername == null || forceNewSettings) {
					KursAktualisierenAnbieterAuswahlDialog dialog1 = new KursAktualisierenAnbieterAuswahlDialog(KursAktualisierenDialog.POSITION_CENTER,
							wertpapier.getAttribute("wertpapiername").toString());
					base = (BaseFetcher) dialog1.open();
					Boolean saveSettings = dialog1.getSpeichernSetting();
					if (saveSettings) {
						doSaveAnbieter(wertpapier.getAttribute("id").toString(), base.getName());
					}
					Date d = new Date();
					base.prepare(searchterm, 2000, 1, 1, d.getYear() + 1900, d.getMonth() + 1, d.getDate());
					while (base.hasMoreConfig()) {
						if (abort) {
							return;
						}
						List<Config> cfg = base.getConfigs();
						monitor.setPercentComplete((int) (monitor.getPercentComplete() + babysteps));
						monitor.setStatusText(cfg.toString());
						KursAktualisierenDialog dialog= new KursAktualisierenDialog(KursAktualisierenDialog.POSITION_CENTER, cfg);
						dialog.open();
						base.process(cfg);
						if (saveSettings) {
							doSaveSettings(wertpapier.getAttribute("id").toString(), cfg);
						}
					}
				} else {
					base = null;
					for (BaseFetcher x :Factory.getHistoryFetcher()) {
						if (anbietername.equals(x.getName())) {
							base = x;
							break;
						}
					}
					if (base == null) {
						doCleanSaveSettings(wpid);
						throw new ApplicationException("Fehler beim Abruf der Kurse. Bitte nochmal aktualisieren und Einstellungen neu vornehmen!");
					}
					Date d = new Date();
					base.prepare(searchterm, 2000, 1, 1, d.getYear() + 1900, d.getMonth() + 1, d.getDate());
					PreparedStatement getCfg = SQLUtils.getPreparedSQL("select value from depotviewer_cfgupdatestock where `wpid`= ? and `key` = ?");
					getCfg.setString(1, wpid);
					while (base.hasMoreConfig()) {
						if (abort) {
							return;
						}
						List<Config> cfgs = base.getConfigs();
						monitor.setPercentComplete((int) (monitor.getPercentComplete() + babysteps));
						monitor.setStatusText(cfgs.toString());
						for (Config cfg : cfgs) {
							getCfg.setString(2, cfg.getBeschreibung());
							String ret = (String) SQLUtils.getObject(getCfg);
							if (ret == null) {
								doCleanSaveSettings(wpid);
								throw new ApplicationException("Fehler beim Abruf der Kurse. Bitte nochmal aktualisieren und Einstellungen neu vornehmen!");
							}
							ConfigTuple selected = null;
							for (ConfigTuple opts : cfg.getOptions()) {
								if (ret.equals(opts.getDescription().toString())) {
									selected = opts;
									break;
								}
							}
							if (selected == null) {
								doCleanSaveSettings(wpid);
								throw new ApplicationException("Fehler beim Abruf der Kurse. Bitte nochmal aktualisieren und Einstellungen neu vornehmen!");
							}
							cfg.addSelectedOptions(selected);
						}
						base.process(cfgs);
					}
				}
				monitor.setPercentComplete((int) (monitor.getPercentComplete() + babysteps));
				monitor.setStatusText("Speichern");
				saveStockData(wertpapier, base);
				monitor.setStatusText("Ferting mit " + searchterm);
				Application.getMessagingFactory().sendMessage(new KursUpdatesMsg(wpid));

			}
		} catch (ApplicationException e) {
			monitor.setStatus(ProgressMonitor.STATUS_ERROR);
			monitor.setStatusText(e.getMessage());
			e.printStackTrace();
			throw e;
		} catch (Exception e) {
			monitor.setStatus(ProgressMonitor.STATUS_ERROR);
			monitor.setStatusText(e.getMessage());
			e.printStackTrace();
			throw new ApplicationException("Fehler beim Abruf der Kurse." , e);
		}
		monitor.setStatusText("Fertig");
		monitor.setStatus(ProgressMonitor.STATUS_DONE);
		monitor.setPercentComplete(101);
		


	}

	// Speichert die Kursinformationen
	private static void saveStockData(GenericObjectSQL wertpapier,
			BaseFetcher base) throws Exception, SQLException, RemoteException,
			ApplicationException {
		// Kurse
		Connection conn = SQLUtils.getConnection();	
		PreparedStatement del = conn.prepareStatement("delete from depotviewer_kurse where wpid = ? ");
		del.setString(1, wertpapier.getID());
		del.executeUpdate();

		PreparedStatement insert = conn.prepareStatement("insert into depotviewer_kurse (wpid, kurs, kursw, kursdatum) values (?,?,?,?)");
		for (Datacontainer dc : base.getHistQuotes()) {
			insert.setString(1, wertpapier.getID());
			insert.setBigDecimal(2, (BigDecimal) dc.data.get("last")); 
			insert.setString(3, (String) dc.data.get("currency")); 
			insert.setDate(4,  new java.sql.Date(((Date) dc.data.get("date")).getTime()));
			insert.addBatch();
		}
		insert.executeBatch();

		// Events
		del = conn.prepareStatement("delete from depotviewer_kursevent where wpid = ? ");
		del.setString(1, wertpapier.getID());
		del.executeUpdate();
		if (base.getHistEvents() != null) {
			insert = conn.prepareStatement("insert into depotviewer_kursevent (wpid, ratio, value, aktion, datum, waehrung) values (?,?,?,?,?,?)");
			for (Datacontainer dc : base.getHistEvents()) {
				String action = (String) dc.data.get("action");
				if (action.equals(Const.CASHDIVIDEND)) {
					action = "D";
				} else if (action.equals(Const.STOCKDIVIDEND)) {
					action = "G";
				} else if (action.equals(Const.STOCKSPLIT)) {
					action = "S";
				} else if (action.equals(Const.SUBSCRIPTIONRIGHTS)) {
					action = "B";
				} else {
					System.out.println("Warning: " + action + " is unknown!");
					continue;
				}


				insert.setString(1, wertpapier.getID());
				insert.setString(2, (String) dc.data.get("ratio")); 
				insert.setBigDecimal(3, (BigDecimal) dc.data.get("value")); 
				insert.setString(4, action); 
				insert.setDate(5,  new java.sql.Date(((Date) dc.data.get("date")).getTime()));
				insert.setString(6, (String) dc.data.get("currency")); 
				insert.addBatch();
			}
			insert.executeBatch();
		}

		// PerformanceKurs
		calcPerformanceKurse(wertpapier, conn);
		Utils.markRecalc(null);
	}

	private static String getAnbieterName(String wpid) throws Exception {
		PreparedStatement pre = SQLUtils.getPreparedSQL("select value from depotviewer_cfgupdatestock where `wpid`= ? and `key` is null");
		pre.setString(1, wpid);
		return (String) SQLUtils.getObject(pre);
	}

	private static void doSaveAnbieter(String string, String name) throws Exception {
		doCleanSaveSettings(string);

		PreparedStatement pre = SQLUtils.getPreparedSQL("insert into depotviewer_cfgupdatestock set `wpid`= ?, `key` = ?, `value` = ?");
		pre.setString(1, string);
		pre.setString(2, null);
		pre.setString(3, name);
		pre.execute();
	}

	private static void doCleanSaveSettings(String string) throws Exception,
	SQLException {
		PreparedStatement pre = SQLUtils.getPreparedSQL("delete from depotviewer_cfgupdatestock where `wpid`= ?");
		pre.setString(1, string);
		pre.execute();
	}

	private static void doSaveSettings(String string, List<Config> cfg) throws Exception {
		PreparedStatement pre = SQLUtils.getPreparedSQL("insert into depotviewer_cfgupdatestock set `wpid`= ?, `key` = ?, `value` = ?");
		for (Config c : cfg) {
			for (ConfigTuple sel : c.getSelected()) {
				pre.setString(1, string);
				pre.setString(2, sel.getObj().toString());
				pre.setString(3, sel.getDescription());
				pre.execute();
			}
		}
	}

	private static void calcPerformanceKurse(GenericObjectSQL wertpapier,
			Connection conn) throws SQLException, RemoteException {
		// Performance Kurs berechnen
		PreparedStatement update = conn.prepareStatement("update depotviewer_kurse set kursperf = ? where id = ?");
		List<GenericObjectSQL> kurse = SQLUtils.getResultSet("select *   from depotviewer_kurse where wpid = " + wertpapier.getID() + " order by kursdatum desc", "", "id");
		List<GenericObjectSQL> kursevt = SQLUtils.getResultSet("select *  from depotviewer_kursevent where wpid = " + wertpapier.getID() + " order by datum desc", "", "id");
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
					}  else if (action.equals("S")) {
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
		update.executeBatch();
	}



	@Override
	public void interrupt() {
		abort = true;

	}

	@Override
	public boolean isInterrupted() {
		return false;
	}
}
