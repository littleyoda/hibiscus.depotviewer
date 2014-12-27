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
}
