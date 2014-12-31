package de.open4me.depot.sql;

import java.util.List;

public class SQLQueries {

	public static List<GenericObjectSQL> getWertpapiereMitKursdatum() {
		return SQLUtils.getResultSet("SELECT * , ( " + 
				"SELECT max( kursdatum ) " + 
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

}
