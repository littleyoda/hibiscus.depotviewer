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
import jsq.datastructes.Const;
import jsq.datastructes.Datacontainer;
import jsq.fetcher.history.BaseFetcher;
import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.gui.dialogs.KursAktualisierenAnbieterAuswahlDialog;
import de.open4me.depot.gui.dialogs.KursAktualisierenDialog;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.util.ApplicationException;


public class UpdateStock {

	public static void update(GenericObjectSQL wertpapier) throws ApplicationException {
		try {
			String searchterm = null;
			if (!wertpapier.isEmpty("isin")) {
				searchterm = wertpapier.getAttribute("isin").toString();
			} else if (!wertpapier.isEmpty("wkn")) {
				searchterm = wertpapier.getAttribute("wkn").toString();
			}


			KursAktualisierenAnbieterAuswahlDialog dialog1 = new KursAktualisierenAnbieterAuswahlDialog(KursAktualisierenDialog.POSITION_CENTER);
			BaseFetcher base = (BaseFetcher) dialog1.open();
			Date d = new Date();
			base.prepare(searchterm, 2000, 1, 1, d.getYear() + 1900, d.getMonth() + 1, d.getDate());
			while (base.hasMoreConfig()) {
				List<Config> cfg = base.getConfigs();
				KursAktualisierenDialog dialog= new KursAktualisierenDialog(KursAktualisierenDialog.POSITION_CENTER, cfg);
				dialog.open();
				base.process(cfg);
			}
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
				insert.executeUpdate();
			}
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
					insert.executeUpdate();
				}
			}
			calcPerformanceKurse(wertpapier, conn);
			Utils.markRecalc(null);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ApplicationException("Fehler beim Abruf der Kurse." , e);
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
			update.executeUpdate();
		}
	}
}
