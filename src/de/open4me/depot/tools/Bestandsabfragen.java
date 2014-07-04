package de.open4me.depot.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.List;

import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;

public class Bestandsabfragen {

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
				+ "   (select top 1 kurs from depotviewer_kurse where wpid=depotviewer_umsaetze.wpid and kursdatum >= ? order by kursdatum) as kurs ,"
				+ "   (select top 1 kursw from depotviewer_kurse where wpid=depotviewer_umsaetze.wpid and kursdatum >= ? order by kursdatum) as kursw ,"
				+ "   (select top 1 kursdatum from depotviewer_kurse where wpid=depotviewer_umsaetze.wpid and kursdatum >= ? order by kursdatum) as bewertungszeitpunkt "
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
}
