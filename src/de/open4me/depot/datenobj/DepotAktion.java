package de.open4me.depot.datenobj;

import java.util.HashMap;

import org.jfree.util.Log;

public class DepotAktion {

	String aktionsName;

	private String name;
	
	public DepotAktion(String name, String internal, String... alternativeSchreibweisen) {
		aktionsName = internal;
		this.name = name;
		all.put(internal, this);
		for (String s : alternativeSchreibweisen) {
			all.put(s.toUpperCase(), this);
		}
	}
	
	public String toString() {
		return name;
	}
	
	public String internal() {
		return aktionsName;
	}

	static private HashMap<String, DepotAktion> all = new HashMap<String, DepotAktion>();
	
	static public final DepotAktion EINBUCHUNG = new DepotAktion("Einlieferung", "EINLIEFERUNG","EINLIEFERUNGEN", "EINBUCHUNG",  "EINLAGE");
	static public final DepotAktion AUSBUCHUNG = new DepotAktion("Auslieferungen", "AUSLIEFERUNG", "AUSLIEFERUNGEN", "AUSBUCHUNG");
	static public final DepotAktion VERKAUF = new DepotAktion("Verkauf", "VERKAUF", "GESAMTVERKAUF BESTAND", "Verkauf Betrag", "S");
	static public final DepotAktion KAUF = new DepotAktion("Kauf", "KAUF", "Kauf Betrag", "B", "Kauf aus Sparplan");
	
	public static DepotAktion getByString(String s) {
		DepotAktion x = all.get(s.toUpperCase());
		if (x == null) {
			Log.warn("Aktion '" + s + "' nicht gefunden!");
		}
		return x;
		
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass()) {
			throw new IllegalStateException("Equals für falsches Objekt: " + obj);
		}
		DepotAktion other = (DepotAktion) obj;
		if (aktionsName == null) {
			if (other.aktionsName != null)
				return false;
		} else if (!aktionsName.equals(other.aktionsName))
			return false;
		return true;
	}
	
	
}
