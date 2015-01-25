package de.open4me.depot.datenobj;

import java.util.ArrayList;
import java.util.List;

import org.jfree.util.Log;

public class DepotAktion {

	String aktionsName;

	private String name;
	
	public DepotAktion(String name, String internal) {
		aktionsName = internal;
		this.name = name;
		all.add(this);
	}
	
	public String toString() {
		return name;
	}
	
	public String internal() {
		return aktionsName;
	}

	static private List<DepotAktion> all = new ArrayList<DepotAktion>();
	
	static public final DepotAktion EINLAGE = new DepotAktion("Einlage", "EINLAGE");
	static public final DepotAktion VERKAUF = new DepotAktion("Verkauf", "VERKAUF");
	static public final DepotAktion KAUF = new DepotAktion("Kauf", "KAUF");
	
	public static DepotAktion getByString(String s) {
		for (DepotAktion a : all) {
			if (a.internal().equals(s)) {
				return a;
			}
		}
		Log.warn("Aktion '" + s + "' nicht gefunden!");
		return null;
		
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
