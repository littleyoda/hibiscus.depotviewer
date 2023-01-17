package de.open4me.depot.datenobj;

import java.util.HashMap;

import de.open4me.depot.Settings;

public class KursEventAktion {
	
	static private HashMap<String, String> eventNames;
	
	static {
		eventNames = new HashMap<>();
		eventNames.put("D", Settings.i18n().tr("Dividende"));
		eventNames.put("G", Settings.i18n().tr("Aktien-Dividende"));
		eventNames.put("S", Settings.i18n().tr("Split"));
		eventNames.put("R", Settings.i18n().tr("Reverse Split"));
		eventNames.put("B", Settings.i18n().tr("Bezugsrecht"));
	}
	
	public static String getName(String id) {
		return eventNames.get(id);
	}
}
