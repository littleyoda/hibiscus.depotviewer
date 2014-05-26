package de.open4me.depot.sql;

import java.util.ArrayList;
import java.util.List;

public class SQLChange {

	private int version;

	private String[] query;


	public int getVersion() {
		return version;
	}

	public String[] getQuery() {
		return query;
	}

	public SQLChange(int newVersion, String...strings ) {
		this.version = newVersion;
		this.query = strings;
	}

	public static List<SQLChange> getChangesSinceVersion(int currentversion) {
		ArrayList<SQLChange> liste = new ArrayList<SQLChange>();
		if (currentversion < 1) {
			liste.add(new SQLChange(1, 		"CREATE TABLE depotviewer_umsaetze (\n" + 
					"  id NUMERIC NOT NULL auto_increment,\n" + 
					"  kontoid NUMERIC,\n" + 
					"  wertpapiername varchar(255) NOT NULL,\n" + 
					"  wkn varchar(6) NOT NULL,\n" + 
					"  anzahl decimal(20,10),\n" + 
					"  \n" + 
					"  kurs decimal(20,2),\n" + 
					"  kursw varchar(3) NOT NULL,\n" + 
					"  \n" + 
					"  kosten decimal(20,2),\n" + 
					"  kostenw varchar(3) NOT NULL,\n" + 
					"  aktion varchar(10) NOT NULL,\n" + 
					"  buchungsdatum date,\n" + 
					"  buchungsinformationen text,\n" + 
					"  orderid varchar(50),\n" + 
					"  UNIQUE (id),\n" + 
					"  PRIMARY KEY (id)\n" + 
					");",


					"create table depotviewer_bestand (\n" + 
							"  id NUMERIC NOT NULL auto_increment,\n" + 
							"  kontoid NUMERIC,\n" + 
							"  wkn varchar(6) NOT NULL,\n" + 
							"  anzahl decimal(20,10),\n" + 
							"  kurs decimal(20,2),\n" + 
							"  kursw varchar(3) NOT NULL,\n" + 
							"  \n" + 
							"  wert decimal(20,2),\n" + 
							"  wertw varchar(3) NOT NULL,\n" + 
							"  \n" + 
							"  datum date NOT NULL,\n" + 
							"  \n" + 
							"  UNIQUE (id),\n" + 
							"  PRIMARY KEY (id)\n" + 
							");",


							"create table depotviewer_cfg (\n" + 
									"  id NUMERIC NOT NULL auto_increment,\n" + 
									"  key varchar(200),\n" + 
									"  value text,\n" + 
									"  \n" + 
									"  UNIQUE (id),\n" + 
									"  PRIMARY KEY (id)\n" + 
									");",

							"insert into depotviewer_cfg (key,value) values ('dbversion','1');"
					));
		}
		return liste;
	}





}
