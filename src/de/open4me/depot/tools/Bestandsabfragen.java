package de.open4me.depot.tools;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DateUtils;

import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.jameica.hbci.gui.chart.AbstractChartDataSaldo;
import de.willuhn.jameica.hbci.report.balance.AccountBalanceProvider;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.hbci.rmi.KontoType;
import de.willuhn.jameica.hbci.server.Value;
import de.willuhn.jameica.util.DateUtil;
import de.willuhn.logging.Logger;

public class Bestandsabfragen implements AccountBalanceProvider {

	static public List<GenericObjectSQL> getBestand(Date d) throws Exception {
		if (d == null) {
			List<GenericObjectSQL> list = SQLUtils.getResultSet("select *, concat(kurs,' ', kursw) as joinkurs, concat(wert,' ', wertw) as joinwert from depotviewer_bestand left join depotviewer_wertpapier on  depotviewer_bestand.wpid = depotviewer_wertpapier.id"
					+ "	left join konto on  konto.id = depotviewer_bestand.kontoid order by wpid", 
					"depotviewer_bestand", "id");
			return list;
		}
		Connection conn = SQLUtils.getConnection();
		String innersql = 
				  "select "
				+ "		kontoid, "
				+ "		wpid, " 
				+ "		round(sum( case when aktion='VERKAUF' then -anzahl else anzahl end),6) as anzahl, "
				+ "   (" + SQLUtils.addTop(1, "select kurs from depotviewer_kurse where wpid=depotviewer_umsaetze.wpid and kursdatum <= ? order by kursdatum desc") + ") as kurs ,"
				+ "   (" + SQLUtils.addTop(1, "select kursw from depotviewer_kurse where wpid=depotviewer_umsaetze.wpid and kursdatum <= ? order by kursdatum desc") + ") as kursw ,"
				+ "   (" + SQLUtils.addTop(1, "select kursdatum from depotviewer_kurse where wpid=depotviewer_umsaetze.wpid and kursdatum <= ? order by kursdatum desc") + ") as bewertungszeitpunkt "
				+ "	from depotviewer_umsaetze where buchungsdatum <= ? \n" 
				+ "	group by kontoid, wpid";
		PreparedStatement prest = conn.prepareStatement(
				
				"select *, ? as datum, round(anzahl * kurs,2) as wert , kursw as wertw, concat(kurs,' ', kursw) as joinkurs, concat(round(anzahl * kurs,2),' ', kursw) as joinwert  from(	" + innersql
				+ ") as zzzz\n" + 
				"	left join depotviewer_wertpapier on zzzz.wpid = depotviewer_wertpapier.id\n" + 
				"	left join konto on  konto.id = zzzz.kontoid\n"
				+ " where anzahl > 0 order by wpid"); 
		prest.setDate(1, new java.sql.Date(d.getTime()));
		prest.setDate(2, new java.sql.Date(d.getTime()));
		prest.setDate(3, new java.sql.Date(d.getTime()));
		prest.setDate(4, new java.sql.Date(d.getTime()));
		prest.setDate(5, new java.sql.Date(d.getTime()));

		return SQLUtils.getResultSet(prest, "", "", "");
	}
	
	
	// Die vom AccountSaldo-Provider unterstuetzten Konto-Arten
	private final static Set<KontoType> SUPPORTED = new HashSet<KontoType>(Arrays.asList(KontoType.FONDSDEPOT, KontoType.WERTPAPIERDEPOT));


	@Override
	public boolean supports(Konto k) {
		if (k == null)
			return false;

		KontoType kt = null;
		try
		{
			// Kontotyp ermitteln
			kt = KontoType.find(k.getAccountType());

			// Wenn kein konkreter Typ angegeben ist, dann unterstuetzen wir es nicht
			if (kt == null)
				return false;

			// Ansonsten dann, wenn er in supported ist
			return SUPPORTED.contains(kt);
		}
		catch (RemoteException re)
		{
			Logger.error("unable to determine support for account-type " + kt, re);
		}
		return false;
	}

	@Override
	public AbstractChartDataSaldo getBalanceChartData(Konto konto, Date start, Date end) {
		List<Value> data = getBalanceData(konto, start, end);
		return new ChartDataPortfolioBalanceHistory(konto, data);
	}
	
	@Override
	public String getName() {
		return "Bestandsabfragen für Depots";
	}
	
	@Override
	public List<Value> getBalanceData(Konto konto, Date start, Date end)
	{
		start = DateUtil.startOfDay(start == null ? new Date() : start);
	    end = DateUtil.endOfDay(end == null ? new Date() : end);
	    ArrayList<Value> data = null;
	    
		try {
			// Alle relevanten Buchungen und Kurse werden auf einmal geladen.
			// Wenn man versucht, jeden Tag oder jedes Wertpapier einzeln mit SQL-Abfragen zu berechnen, sieht der Code zwar schöner aus,
			// läuft aber zu lange.
			Map<Integer, List<GenericObjectSQL>> buchungenProWertpapier = getBuchungenProWertpapier(konto, start, end);
			Map<Integer, List<GenericObjectSQL>> kurseProWertpapier = getKurseProWertpapier(konto, start, end);		
			
		    int numdays = (int)Utils.getDifferenceDays(start, end) + 1; 
		    BigDecimal[] wpTagWerte = new BigDecimal[numdays]; // für das aktuelle Wertpapier werden hier Anzahl * Kurs für jeden Tag verwaltet
		    
		    // Ausgabe vorbereiten
		    data = new ArrayList<Value>(numdays);
	 		for (Date currentDate = start; !currentDate.after(end); currentDate = DateUtils.addDays(currentDate, 1)) {
	 	    	Value v = new Value(currentDate, 0);
	 			data.add(v);
	 	    }	    	    
			
	 		// jedes Wertpapier durchgehen, Bestand für jeden Tag ermitteln, Kurs für jeden Tag ermitteln
			for(Integer wp : buchungenProWertpapier.keySet()) {
				List<GenericObjectSQL> wpBuchungen = buchungenProWertpapier.get(wp);
				List<GenericObjectSQL> wpKurse = kurseProWertpapier.get(wp);
				
				BigDecimal wpAnzahl = BigDecimal.ZERO;
				int tagIdx = 0;
				int buchungIdx = 0;
				
				for (Date currentDate = start; !currentDate.after(end); currentDate = DateUtils.addDays(currentDate, 1))
			    {
			      	// Buchungen bis zum aktuellen Tag verarbeiten und Anzahl ermitteln
					while (buchungIdx < wpBuchungen.size()
							&& !((Date) wpBuchungen.get(buchungIdx).getAttribute("buchungsdatum")).after(currentDate)) {
						wpAnzahl = wpAnzahl.add(new BigDecimal(wpBuchungen.get(buchungIdx).getAttribute("anzahl").toString()));
						++buchungIdx;
					}
			    	
			    	wpTagWerte[tagIdx] = wpAnzahl;
			        ++tagIdx;
			    }			
			    
			    // Kurse ermitteln
			    BigDecimal kurs = BigDecimal.ZERO;
			    Date tag = end;
			    tagIdx = numdays - 1;
			    int kursIdx = (wpKurse == null ? 0 : wpKurse.size()) - 1;
			    // Kurse rückwärts durchgehen und für die Tage anwenden
			    while(kursIdx >= 0) {
			    	Date kursDatum = (Date)wpKurse.get(kursIdx).getAttribute("kursdatum");
			    	kurs = new BigDecimal(wpKurse.get(kursIdx).getAttribute("kurs").toString());
			    	while (tagIdx >= 0 && !tag.before(kursDatum))   	
			    	{
				    	wpTagWerte[tagIdx] = wpTagWerte[tagIdx].multiply(kurs);
				    	tag = DateUtils.addDays(tag, -1);
				        --tagIdx;
			    	}
			        --kursIdx;
			    }
			    
			    if(tagIdx >= 0) { 	// es gibt noch Tage ohne Kurs
			    	if(kurs.compareTo(BigDecimal.ZERO) == 0) {
			    		// TODO : was sollen wir tun, wenn gar kein Kurs gefunden wurde? 
			    	}
			    	
			    	while (tagIdx >= 0)   	
			    	{
				    	wpTagWerte[tagIdx] = wpTagWerte[tagIdx].multiply(kurs); // kurs ist der früheste Kurs aus wpKurse 
				        --tagIdx;
			    	}
			    }		    	   
			
				// Werte des Wertpapiers auf Ergebnis summieren		
				for (tagIdx = 0; tagIdx < numdays; ++tagIdx) {
			    	Value v = data.get(tagIdx);
			    	v.setValue(v.getValue() + wpTagWerte[tagIdx].doubleValue());
			    }	    
			}    
		} catch (Exception e) {
			Logger.error("Fehler beim der Ermittlung der Depotsalden des DeportViewers", e);
		}
	    return data;	    
	}
	
	/**
	 * Liefert für alle Wertpapiere eines Kontos die Buchungen gruppiert pro Wertpapier.
	 * Schlüssel der Rückgabe-Map ist die Wertpapier-ID.
	 * @param konto
	 * @param start
	 * @param end
	 * @return
	 * @throws Exception
	 */
	public Map<Integer, List<GenericObjectSQL>> getBuchungenProWertpapier(Konto konto, Date start, Date end) throws Exception {
		Connection conn = SQLUtils.getConnection();
		String sqlBuchungen = "select "
				+ "	wpid"
				+ "	, (case when aktion='VERKAUF' then -anzahl else anzahl end) as anzahl"
				+ "	, buchungsdatum"
				+ " from depotviewer_umsaetze"
				+ " where kontoid=? and buchungsdatum <= ?"
				+ " order by wpid, buchungsdatum";
		PreparedStatement stmtBuchungen = conn.prepareStatement(sqlBuchungen);
		stmtBuchungen.setString(1, konto.getID());
		stmtBuchungen.setDate(2, new java.sql.Date(end.getTime()));
		
		List<GenericObjectSQL> buchungen = SQLUtils.getResultSet(stmtBuchungen, "depotviewer_umsaetze", null, null);
		conn.close();
		Map<Integer, List<GenericObjectSQL>> buchungenProWertpapier = SQLUtils.<Integer>groupBy(buchungen, "wpid"); // gruppiert Buchungen pro Wertpapier
		return buchungenProWertpapier;
	}
	
	/**
	 * Liefert für alle Wertpapiere eines Kontos die Kurse gruppiert pro Wertpapier.
	 * Schlüssel der Rückgabe-Map ist die Wertpapier-ID.
	 * @param konto
	 * @param start
	 * @param end
	 * @return
	 * @throws Exception
	 */
	public Map<Integer, List<GenericObjectSQL>> getKurseProWertpapier(Konto konto, Date start, Date end) throws Exception {
		Connection conn = SQLUtils.getConnection();
		String sqlKurse = "select k.wpid, k.kursdatum, k.kurs "
				+ " from depotviewer_kurse k, depotviewer_umsaetze u"
				+ " where kontoid=? and k.wpid=u.wpid and kursdatum >= ? and  kursdatum <= ?"
				+ " order by k.wpid, k.kursdatum";
		
		PreparedStatement stmtKurse = conn.prepareStatement(sqlKurse);
		stmtKurse.setString(1, konto.getID());
		final Calendar cal = Calendar.getInstance();
	    cal.setTime(start);
	    cal.add(Calendar.DATE,-5); // wir ziehen vom Startdatum ein paar Tage ab, für den Fall dass kein Kurs am Starttag vorhanden ist, beispielsweise am Wochenende oder Feiertage
		stmtKurse.setDate(2, new java.sql.Date(cal.getTime().getTime()));
		stmtKurse.setDate(3, new java.sql.Date(end.getTime()));
		
		List<GenericObjectSQL> kurse = SQLUtils.getResultSet(stmtKurse, "depotviewer_kurse", null, null);
		
		conn.close();
		
		Map<Integer, List<GenericObjectSQL>> kurseProWertpapier = SQLUtils.<Integer>groupBy(kurse, "wpid"); // gruppiert Kurse pro Wertpapier
		return kurseProWertpapier;
	}
}
