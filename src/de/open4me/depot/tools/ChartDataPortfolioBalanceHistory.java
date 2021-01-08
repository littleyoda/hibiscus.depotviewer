/**********************************************************************
 *
 * Copyright (c) 2004 Olaf Willuhn
 * All rights reserved.
 * 
 * This software is copyrighted work licensed under the terms of the
 * Jameica License.  Please consult the file "LICENSE" for details. 
 *
 **********************************************************************/

package de.open4me.depot.tools;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DateUtils;

import de.open4me.depot.sql.GenericObjectHashMap;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.datasource.rmi.DBIterator;
import de.willuhn.jameica.hbci.gui.chart.AbstractChartDataSaldo;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.hbci.server.KontoUtil;
import de.willuhn.jameica.hbci.server.UmsatzUtil;
import de.willuhn.jameica.hbci.server.Value;
import de.willuhn.jameica.hbci.util.SaldoFinder;
import de.willuhn.jameica.util.DateUtil;
import de.willuhn.logging.Logger;

/**
 * Implementierung eines Datensatzes fuer die Darstellung des Saldenverlaufs.
 */
public class ChartDataPortfolioBalanceHistory extends AbstractChartDataSaldo
{
  private Konto konto      = null;
  private Date start       = null;
  private Date end         = null;
  private List<Value> data = null;
  
  /**
   * ct.
   * @param k das Konto, fuer das das Diagramm gemalt werden soll.
   * @param start Start-Datum.
   * @param end Ende-Datum.
   */
  public ChartDataPortfolioBalanceHistory(Konto konto, Date start, Date end)
  {
    this.konto = konto;
    this.start = DateUtil.startOfDay(start == null ? new Date() : start);
    this.end = DateUtil.endOfDay(end == null ? new Date() : end);
  }

  /**
   * @see de.willuhn.jameica.hbci.gui.chart.ChartData#getData()
   */
  @Override
  public List<Value> getData() throws RemoteException
  {
    if (this.data != null)
      return this.data;
    
	try {
		// Alle relevanten Buchungen und Kurse werden auf einmal geladen.
		// Wenn man versucht, jeden Tag oder jedes Wertpapier einzeln mit SQL-Abfragen zu berechnen, sieht der Code zwar schöner aus,
		// läuft aber zu lange.
		Map<Integer, List<GenericObjectSQL>> buchungenProWertpapier = getBuchungenProWertpapier();
		Map<Integer, List<GenericObjectSQL>> kurseProWertpapier = getKurseProWertpapier();		
		
		long diffInMillies = Math.abs(this.end.getTime() - this.start.getTime());
	    int numdays = (int)TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS) + 1;
	    BigDecimal[] wpTagWerte = new BigDecimal[numdays]; // für das aktuelle Wertpapier werden hier Anzahl * Kurs für jeden Tag verwaltet
	    
	    // Ausgabe vorbereiten
	    this.data = new ArrayList<Value>(numdays);
 		for (Date currentDate = this.start; !currentDate.after(this.end); currentDate = DateUtils.addDays(currentDate, 1)) {
 	    	Value v = new Value(currentDate, 0);
 			this.data.add(v);
 	    }	    	    
		
 		// jedes Wertpapier durchgehen, Bestand für jeden Tag ermitteln, Kurs für jeden Tag ermitteln
		for(Integer wp : buchungenProWertpapier.keySet()) {
			List<GenericObjectSQL> wpBuchungen = buchungenProWertpapier.get(wp);
			List<GenericObjectSQL> wpKurse = kurseProWertpapier.get(wp);
			
			BigDecimal wpAnzahl = BigDecimal.ZERO;
			int tagIdx = 0;
			int buchungIdx = 0;
			
			for (Date currentDate = this.start; !currentDate.after(this.end); currentDate = DateUtils.addDays(currentDate, 1))
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
		    Date tag = this.end;
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
		    	Value v = this.data.get(tagIdx);
		    	v.setValue(v.getValue() + wpTagWerte[tagIdx].doubleValue());
		    }	    
		}    
	} catch (Exception e) {
		Logger.error("Fehler beim der Ermittlung der Depotsalden des DeportViewers", e);
	}
    return this.data;
       
    
  }

	private Map<Integer, List<GenericObjectSQL>> getBuchungenProWertpapier() throws Exception {
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
		stmtBuchungen.setDate(2, new java.sql.Date(this.end.getTime()));
		
		List<GenericObjectSQL> buchungen = SQLUtils.getResultSet(stmtBuchungen, "depotviewer_umsaetze", null, null);
		conn.close();
		Map<Integer, List<GenericObjectSQL>> buchungenProWertpapier = SQLUtils.<Integer>groupBy(buchungen, "wpid"); // gruppiert Buchungen pro Wertpapier
		return buchungenProWertpapier;
	}
	
	private Map<Integer, List<GenericObjectSQL>> getKurseProWertpapier() throws Exception {
		Connection conn = SQLUtils.getConnection();
		String sqlKurse = "select k.wpid, k.kursdatum, k.kurs "
				+ " from depotviewer_kurse k, depotviewer_umsaetze u"
				+ " where kontoid=? and k.wpid=u.wpid and kursdatum >= ? and  kursdatum <= ?"
				+ " order by k.wpid, k.kursdatum";
		
		PreparedStatement stmtKurse = conn.prepareStatement(sqlKurse);
		stmtKurse.setString(1, konto.getID());
		final Calendar cal = Calendar.getInstance();
	    cal.setTime(this.start);
	    cal.add(Calendar.DATE,-5); // wir ziehen vom Startdatum ein paar Tage ab, für den Fall dass kein Kurs am Starttag vorhanden ist, beispielsweise am Wochenende oder Feiertage
		stmtKurse.setDate(2, new java.sql.Date(cal.getTime().getTime()));
		stmtKurse.setDate(3, new java.sql.Date(this.end.getTime()));
		
		List<GenericObjectSQL> kurse = SQLUtils.getResultSet(stmtKurse, "depotviewer_kurse", null, null);
		
		conn.close();
		
		Map<Integer, List<GenericObjectSQL>> kurseProWertpapier = SQLUtils.<Integer>groupBy(kurse, "wpid"); // gruppiert Kurse pro Wertpapier
		return kurseProWertpapier;
	}


	
	
  /**
   * @see de.willuhn.jameica.hbci.gui.chart.ChartData#getLabel()
   */
  public String getLabel() throws RemoteException
  {
    if (this.konto != null)
      return this.konto.getBezeichnung();
    return i18n.tr("Alle Konten");
  }
}
