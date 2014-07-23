package de.open4me.depot.tools;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import de.open4me.depot.sql.GenericObjectHashMap;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;

public class WertBerechnung {

	public static List<GenericObjectHashMap> getWertBerechnung() throws Exception {
		GenericObjectHashMap erg;
		List<GenericObjectHashMap> list = new ArrayList<GenericObjectHashMap>();
		List<GenericObjectSQL> bestaende = Bestandsabfragen.getBestand(null);
		
		// Über alle wpid / depot Kommbinationen iterieren
		for (GenericObjectSQL wpid : SQLUtils.getResultSet("select distinct wpid, wertpapiername, wkn, isin, bezeichnung,kontoid from depotviewer_umsaetze \n" + 
				"left join depotviewer_wertpapier on depotviewer_umsaetze.wpid = depotviewer_wertpapier.id \n" + 
				"left join konto on konto.id = depotviewer_umsaetze.kontoid"
				, "", "wpid", "")) {
			
			// Und jetzt die Umsätze für diese Kombination bestimmen
			List<GenericObjectSQL> orders = SQLUtils.getResultSet("select * from depotviewer_umsaetze where wpid = " + wpid.getAttribute("wpid") + " and kontoid = "+ wpid.getAttribute("kontoid") + " order by buchungsdatum", "", "", "");
			
			// Erstmal die Verkäuf berechnen
			int idx;
			while ((idx = getFirstVerkauf(orders)) != -1) {
					erg = getKaufVerkaufInformationen(wpid, orders, idx);
					list.add(erg);
			}

			// Die restlichen Order, die auch noch im Bestand sind
			if (orders.size() > 0) {
				erg = getKaufundBestandInformationen(list, bestaende, wpid, orders);
				list.add(erg);
			}
		}
		
		// Und jetzt die Wertentwicklung berechnen
		for (GenericObjectHashMap l : list) {
			BigDecimal start = ((BigDecimal) l.getAttribute("einstand")).negate();
			BigDecimal jetzt = (BigDecimal) l.getAttribute("erloese");
			if (jetzt == null) {
				jetzt = (BigDecimal) l.getAttribute("wert");
			}
			if (jetzt == null) {
				jetzt = BigDecimal.ZERO;
			}
			if (start.compareTo(BigDecimal.ZERO) == 0) {
				l.setAttribute("abs", BigDecimal.ZERO);
				l.setAttribute("absproz", BigDecimal.ZERO);
			} else {
				BigDecimal abs = jetzt.subtract(start.abs());
				l.setAttribute("abs", abs);

				BigDecimal absproz = jetzt.subtract(start).multiply(new BigDecimal("100.0")).divide(start, 2, RoundingMode.HALF_UP);
				l.setAttribute("absproz", absproz);
				l.setAttribute("währung", (String) l.getAttribute("währung")); 
			}
			
		}
		return list;
	}

	private static GenericObjectHashMap getKaufundBestandInformationen(
			List<GenericObjectHashMap> list, List<GenericObjectSQL> bestaende,
			GenericObjectSQL wpid, List<GenericObjectSQL> orders)
			throws RemoteException {
		// Es gilt as FIFO Prinzip
		// Und jetzt alle Positionen addieren, die noch im Bestand sind und somit unverkauft sind
		BigDecimal anzahl = BigDecimal.ZERO; 
		BigDecimal kosten = BigDecimal.ZERO;
		String waehrung = "";
		for (GenericObjectSQL  order: orders) {
			kosten = kosten.add((BigDecimal) order.getAttribute("kosten"));
			anzahl = anzahl.add((BigDecimal) order.getAttribute("anzahl"));
			// TODO Sicherstellen, dass immer mit der gleichen Währung gerechnet wird
			waehrung = (String) order.getAttribute("kursw"); 
		}
		GenericObjectHashMap erg = new GenericObjectHashMap();
		erg.setAttribute("anzahl", anzahl);
		erg.setAttribute("einstand", kosten);
		erg.setAttribute("währung", waehrung);
		for (String s : new String[] {"wpid", "kontoid", "bezeichnung" ,"wertpapiername", "wkn", "isin"}) {
			erg.setAttribute(s, wpid.getAttribute(s));
		}
		// Bewertung 
		for (GenericObjectSQL bestand : bestaende) {
			if (wpid.getAttribute("wpid").equals(bestand.getAttribute("wpid")) &&
					wpid.getAttribute("kontoid").equals(bestand.getAttribute("kontoid"))) {
				erg.setAttribute("wert", bestand.getAttribute("wert"));
				erg.setAttribute("datum", bestand.getAttribute("datum"));
			}
		}
		return erg;
	}

	// Es gilt as FIFO Prinzip
	private static GenericObjectHashMap getKaufVerkaufInformationen(
			GenericObjectSQL wpid, List<GenericObjectSQL> orders, int idx)
			throws RemoteException {
		GenericObjectSQL verkauf = orders.get(idx);
		orders.remove(idx);
		BigDecimal anzahl = (BigDecimal) verkauf.getAttribute("anzahl"); 
		GenericObjectHashMap erg = new GenericObjectHashMap();
		erg.setAttribute("währung", verkauf.getAttribute("kostenw"));
		erg.setAttribute("erloese", verkauf.getAttribute("kosten"));
		erg.setAttribute("anzahl", verkauf.getAttribute("anzahl"));
		erg.setAttribute("datum", verkauf.getAttribute("buchungsdatum"));
		for (String s : new String[] {"wpid", "kontoid", "bezeichnung" ,"wertpapiername", "wkn", "isin"}) {
			erg.setAttribute(s, wpid.getAttribute(s));
		}

		// Jetzt die Kauforder zusammensuchen
		BigDecimal kosten = BigDecimal.ZERO; 
		BigDecimal rest = anzahl;
		List<Integer> removeListe = new ArrayList<Integer>(); 
		for (int i = 0; i < idx; i++) {
			GenericObjectSQL  kauf = orders.get(i);
			BigDecimal kaufanzahl = (BigDecimal) kauf.getAttribute("anzahl");
			BigDecimal kaufkosten = (BigDecimal) kauf.getAttribute("kosten");
			switch (kaufanzahl.compareTo(rest)) {
			case -1: 
			case 0: rest = rest.subtract(kaufanzahl); 
			kosten = kosten.add(kaufkosten);
			removeListe.add(i, 0); // umgekehrte Reihenfolge wegen Löschen
			break;
			case 1:
				// Wir haben nur einen Teil der Aktien verkauft
				// Vom Kauf den Verkauf abziehen
				BigDecimal kaufteilkosten = kaufkosten.divide(kaufanzahl, 8, RoundingMode.HALF_UP).multiply(rest);
				kosten = kosten.add(kaufteilkosten);
				kauf.setAttribute("anzahl", kaufanzahl.subtract(rest));
				kauf.setAttribute("kosten", kaufkosten.subtract(kaufteilkosten));
				rest = BigDecimal.ZERO;
				break;

			}
			if (rest.compareTo(BigDecimal.ZERO) == 0) {
				break;
			}
		}
		erg.setAttribute("einstand", kosten);

		// "Verbrauchte" Order entfernen
		for (Integer x : removeListe) {
			orders.remove((int) x);
		}
		return erg;
	}
	
	
	private static int getFirstVerkauf(List<GenericObjectSQL> orders)
			throws RemoteException {
		int idx;
		idx = -1;
		for (int i = 0; i < orders.size(); i++) {
			if ("VERKAUF".equals(orders.get(i).getAttribute("aktion"))) {
				idx = i;
				break;
			}
		}
		return idx;
	}

}
