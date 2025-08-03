package de.open4me.depot.sql;

import java.util.List;

public class SQLQueries {

	public static List<GenericObjectSQL> getWertpapiereMitKursdatum() {
		return SQLUtils.getResultSet("SELECT * , " +
				"(SELECT kurs " +
				"	FROM depotviewer_kurse k " +
				"	INNER JOIN " +
				"		(SELECT wpid, max( kursdatum ) as MaxDatum " +
				"		FROM depotviewer_kurse " +
				"		GROUP BY wpid" +
				"		) AS groupeddatum " +
				"	ON k.wpid = groupeddatum.wpid AND k.wpid = depotviewer_wertpapier.id " +
				"	AND k.kursdatum = groupeddatum.MaxDatum) AS Kurs," +
				"(SELECT max( kursdatum ) " + 
				"FROM depotviewer_kurse " + 
				"WHERE wpid = depotviewer_wertpapier.id " + 
				") AS Kursdatum " + 
				"FROM depotviewer_wertpapier", 
				"depotviewer_wertpapier", "id", "wertpapiername");		
	}

	public static List<GenericObjectSQL> getWertpapiere() {
		return SQLUtils.getResultSet("select *, concat(wertpapiername , ' (' , wkn , ' / ' , isin , ')') as nicename from depotviewer_wertpapier order by wertpapiername", 
			"depotviewer_wertpapier", "id", "nicename");
	}

	public static List<GenericObjectSQL> getOwnedWertpapiere() {
		return SQLUtils.getResultSet("select distinct w.*, concat(w.wertpapiername , ' (' , w.wkn , ' / ' , w.isin , ')') as nicename " +
			"from depotviewer_wertpapier w " +
			"inner join depotviewer_umsaetze u on w.id = u.wpid " +
			"order by w.wertpapiername", 
			"depotviewer_wertpapier", "id", "nicename");
	}

}
