package de.open4me.depot.tools;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.jameica.hbci.accounts.balance.AccountBalanceProvider;
import de.willuhn.jameica.hbci.gui.chart.AbstractChartDataSaldo;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.hbci.rmi.KontoType;
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
		return new ChartDataPortfolioBalanceHistory(konto, start, end);
	}
	
	@Override
	public String getName() {
		return "Bestandsabfragen für Depots";
	}
}
