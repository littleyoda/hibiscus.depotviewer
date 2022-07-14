package de.open4me.depot.tools;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.datenobj.DepotAktion;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

public class Bestandspruefung {

	/**
	 * Liefert das gespeicherte Ergebnisse der Bestandsprüfung zurück.
	 * Falls noch keine Bestandsprüfung durchgeführt wurde bzw. neu durchgeführt werden muss, 
	 * wird vorher durchgeführt 
	 * 
	 * @return true= wenn die Bestandprüfung ok war
	 * 			false: wenn die Bestandprüfung fehlerhaft war
	 * 			null: im Fehlerfall
	 * 
	 */
	public static Boolean isOK() {
		Boolean ret = Utils.getUmsatzBestandTest();
		if (ret == null) {
			return ret;
		}
		try {
			checkAndRecalcOfflineDepots();
		} catch (RemoteException | ApplicationException e) {
			e.printStackTrace();
		}
		return Utils.getUmsatzBestandTest();
	}

	public static String exec() throws RemoteException, ApplicationException {
		String output = checkAndRecalcOfflineDepots();
		if (output.isEmpty()) {
			output = "Keine Abweichungen gefunden.<br/>Der Bestand passt zu den Umsätzen im Orderbuch.";
		} else {
			output = "Folgende Abweichungen wurden gefunden: <p/>" + output;
		}
		return output;
	}
	
	/**
	 * 
	 * @return
	 * @throws RemoteException
	 * @throws ApplicationException
	 */
	private static String checkAndRecalcOfflineDepots() throws RemoteException, ApplicationException {
		String output = "";

		// Überprüfe alle Konten für die es Umsätze bzw. Bestände gibt
		List<GenericObjectSQL> konten = SQLUtils.getResultSet("select * from konto where id in (select distinct kontoid from depotviewer_bestand union select distinct kontoid from depotviewer_umsaetze)", null, "id");
		for (GenericObjectSQL konto : konten) {
			String outputKonto = pruefe(konto);
			if (konto.getAttribute("flags") != null
					&& (((Integer) konto.getAttribute("flags")) & Konto.FLAG_OFFLINE) == Konto.FLAG_OFFLINE) {
				createOfflineBestand(konto);
				outputKonto = pruefe(konto);
			}
			output +=  outputKonto;
		}
		try {
			Utils.setUmsatzBetsandTest(output.isEmpty());
		} catch (ApplicationException e) {
			e.printStackTrace();
			Logger.error("Fehler beim Setzen des Umsatz/Bestand-Flags", e);
		}
		return output;
	}

	/**
	 * Bestimmt den Bestand von Offline Konten aus den Umsätzen und 
	 * bewertet ihn falls Kursdaten vorhanden sind
	 * @param konto Konto
	 * @throws RemoteException
	 * @throws ApplicationException
	 */
	private static void createOfflineBestand(GenericObjectSQL konto) throws RemoteException, ApplicationException {
		Konto k = Utils.getKontoByID(konto.getAttribute("id").toString());
		HashMap<Integer, BigDecimal> bestand = getBestandLautOrder(konto);
		Utils.clearBestand(k);
		BigDecimal saldo = BigDecimal.ZERO;
		for (Entry<Integer, BigDecimal> position : bestand.entrySet()) {
			BigDecimal wert = BigDecimal.ZERO;
			String wertW =  "EUR";
			BigDecimal kurs = BigDecimal.ZERO;
			String kursW =  "EUR";
			Date bewertung = null;
			String id = position.getKey().toString();
			// Falls Kursdaten vorhanden sind, bitte diese nutzen
			List<GenericObjectSQL> res = SQLUtils.getResultSet(SQLUtils.addTop(1, "select * from depotviewer_kurse where wpid = " + id +" order by kursdatum desc"), "depotviewer_kurse", id, id);
			if (res.size() == 1) {
				GenericObjectSQL data = res.get(0);
				kurs = new BigDecimal(data.getAttribute("kurs").toString());
				kursW = data.getAttribute("kursw").toString();
				wertW = kursW;
				wert = kurs.multiply(position.getValue());
				bewertung = (Date) data.getAttribute("kursdatum");
				saldo = saldo.add(wert); // Währung beachten!
			}
			
			// Bestand hinzufügen
			Utils.addBestand(position.getKey().toString(), k, position.getValue().doubleValue(), kurs.doubleValue(), kursW, wert.doubleValue(), wertW, new Date(), bewertung);
		}
		k.setSaldo(saldo.doubleValue());
		k.store();
		
	}

	private static String pruefe(GenericObjectSQL konto) throws RemoteException {
		String output = "";
		HashMap<Integer, BigDecimal> bestandLautOrder = getBestandLautOrder(konto);
		
		// Abgleich tatsächlicher Bestand vs. errechneter Bestand
		List<GenericObjectSQL> bestaende = SQLUtils.getResultSet("select * from depotviewer_bestand  where kontoid = " + konto.getID() , null, null);
		for (GenericObjectSQL bestand : bestaende) {
			BigDecimal depotAnzahl = (BigDecimal) bestand.getAttribute("anzahl");
			BigDecimal x = bestandLautOrder.get(bestand.getAttribute("wpid"));
			if (x == null) {
				x = BigDecimal.ZERO;
			}
			if (x.compareTo(depotAnzahl) != 0) {
				output = addDifferenz(konto, (Integer) bestand.getAttribute("wpid"), x, depotAnzahl, output);
			}
			bestandLautOrder.remove(bestand.getAttribute("wpid"));
		}

		// Und jetzt noch alles prüfen, was wir jetzt noch in den Umsätzen haben
		for (Entry<Integer, BigDecimal> set : bestandLautOrder.entrySet()) {
			if (set.getValue().compareTo(BigDecimal.ZERO) != 0) {
				output = addDifferenz(konto, set.getKey(), set.getValue(), BigDecimal.ZERO, output);
			}
		}
		return output;

	}

	private static HashMap<Integer, BigDecimal> getBestandLautOrder(GenericObjectSQL konto) throws RemoteException {
		HashMap<Integer, BigDecimal> bestandLautOrder = new HashMap<Integer, BigDecimal>(); 

		// Alle Umsätze nehmen und daraus einen Bestand berechnen.
		List<GenericObjectSQL> buchungen = SQLUtils.getResultSet("select * from depotviewer_umsaetze where kontoid = " + konto.getID() + " order by buchungsdatum asc", null, null);
		for (GenericObjectSQL x : buchungen) {
			Integer wpid = (Integer) x.getAttribute("wpid");
			BigDecimal transactionanzahl = (BigDecimal) x.getAttribute("anzahl");

			BigDecimal aktuellerBestand = (bestandLautOrder.containsKey(wpid)) ? bestandLautOrder.get(wpid) : new BigDecimal(0);

			DepotAktion  aktion = DepotAktion.getByString((String) x.getAttribute("aktion"));
			if (aktion.equals(DepotAktion.KAUF) || aktion.equals(DepotAktion.EINBUCHUNG)) {
				aktuellerBestand = aktuellerBestand.add(transactionanzahl);
			} else if (aktion.equals(DepotAktion.VERKAUF) || aktion.equals(DepotAktion.AUSBUCHUNG)) {
				aktuellerBestand = aktuellerBestand.subtract(transactionanzahl);
			}
			bestandLautOrder.put(wpid, aktuellerBestand);
		}
		return bestandLautOrder;
	}

	private static String addDifferenz(GenericObjectSQL konto, Integer wpid, BigDecimal lautUmsaetze,
			BigDecimal lautBestand, String output) throws RemoteException {
		GenericObjectSQL wp = SQLUtils.getResultSet("select * from depotviewer_wertpapier where id = " + wpid, 
				null, null).get(0);


		output +=  "Konto: " 
				+ konto.getAttribute("bezeichnung").toString()  
				+ "<br/>"
				+ "Wertpapier: " + ((wp.getAttribute("wertpapiername") == null) ? "" :wp.getAttribute("wertpapiername")) + "<br/>"  
				+ "Wertpapier: " + ((wp.getAttribute("wkn") == null) ? "" :wp.getAttribute("wkn"))  + "<br/>"
				+ "Wertpapier: " + ((wp.getAttribute("isin") == null) ? "" :wp.getAttribute("isin"))  + "<br/>"
				+ "Anzahl laut Bestand  : " +  lautBestand.toPlainString() + "<br/>"
				+ "Anzahl laut Orderbuch: " +  lautUmsaetze.toPlainString() + "<br/>"
				+ "Differenz:             " +  lautBestand.subtract(lautUmsaetze) + "<br/>"
				+ "<p/>";
		return output;
	}

}
