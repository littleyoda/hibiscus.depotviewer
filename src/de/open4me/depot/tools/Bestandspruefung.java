package de.open4me.depot.tools;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import de.open4me.depot.gui.view.BestandsAbgleichView;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.jameica.gui.GUI;

public class Bestandspruefung {

	private static String output;
	
	public static void exec() throws RemoteException {
		output = "";
		
		List<GenericObjectSQL> konten = SQLUtils.getResultSet("select * from konto where id in (select distinct kontoid from depotviewer_bestand union select distinct kontoid from depotviewer_umsaetze)", null, "id");
		for (GenericObjectSQL konto : konten) {
			pruefe(konto);
		}
		if (output.isEmpty()) {
			output = "Keine Abweichungen gefunden.<br/>Der Bestand passt zu den Umsätzen im Orderbuch.";
		} else {
			output = "Folgende Abweichungen wurden gefunden: <p/>" + output; 
		}
		GUI.startView(BestandsAbgleichView.class,output);
	}

	private static void pruefe(GenericObjectSQL konto) throws RemoteException {
		HashMap<Integer, BigDecimal> bestandLautOrder = new HashMap<Integer, BigDecimal>(); 
		
		// Alle Umsätze nehmen und daraus einen Bestand berechnen.
		List<GenericObjectSQL> buchungen = SQLUtils.getResultSet("select * from depotviewer_umsaetze where kontoid = " + konto.getID() + " order by buchungsdatum asc", null, null);
		for (GenericObjectSQL x : buchungen) {
			Integer wpid = (Integer) x.getAttribute("wpid");
			String aktion = (String) x.getAttribute("aktion");
			BigDecimal transactionanzahl = (BigDecimal) x.getAttribute("anzahl");
			
			BigDecimal aktuellerBestand = (bestandLautOrder.containsKey(wpid)) ? bestandLautOrder.get(wpid) : new BigDecimal(0);

			if (aktion.equals("KAUF") || aktion.equals("EINLAGE")) {
				aktuellerBestand = aktuellerBestand.add(transactionanzahl);
			} else if (aktion.equals("VERKAUF")) {
				aktuellerBestand = aktuellerBestand.subtract(transactionanzahl);
			}
			bestandLautOrder.put(wpid, aktuellerBestand);
		}
		// Abgleich tatsächlicher Bestand vs. errechneter Bestand
		List<GenericObjectSQL> bestaende = SQLUtils.getResultSet("select * from depotviewer_bestand  where kontoid = " + konto.getID() , null, null);
		for (GenericObjectSQL bestand : bestaende) {
			BigDecimal depotAnzahl = (BigDecimal) bestand.getAttribute("anzahl");
			BigDecimal x = bestandLautOrder.get(bestand.getAttribute("wpid"));
			if (x == null) {
				x = BigDecimal.ZERO;
			}
			if (x.compareTo(depotAnzahl) != 0) {
				addDifferenz(konto, (Integer) bestand.getAttribute("wpid"), x, depotAnzahl);
			}
			bestandLautOrder.remove(bestand.getAttribute("wpid"));
		}
		
		// Und jetzt noch alles prüfen, was wir jetzt noch in den Umsätzen haben
		for (Entry<Integer, BigDecimal> set : bestandLautOrder.entrySet()) {
			if (set.getValue().compareTo(BigDecimal.ZERO) != 0) {
				addDifferenz(konto, set.getKey(), set.getValue(), BigDecimal.ZERO);
			}
		}
		
	}

	private static void addDifferenz(GenericObjectSQL konto, Integer wpid, BigDecimal lautUmsaetze,
			BigDecimal lautBestand) throws RemoteException {
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
	}

}
