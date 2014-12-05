package de.open4me.depot.abruf.utils;

import java.util.ArrayList;
import java.util.List;

public class PropHelper {
	public final static String PROP_OPTIONS = "Optionen (kann leer bleiben)";
	public final static String NURBESTAND = "Nur Bestand via HBCI abholen?";
	public final static String NURBESTANDINKLFORMAT = NURBESTAND  + "(true/false)";
	//public final static String UMSAETZEERGAENZEN = "Umsätze aus Bestandsänderungen ermitteln?";
	//public final static String UMSAETZEERGAENZENINCLFORMAT = UMSAETZEERGAENZEN + "(true/false)";

	public static List<String> getPROP() {
		List<String> result = new ArrayList<String>();
		result.add(PROP_OPTIONS);
	//	result.add(UMSAETZEERGAENZENINCLFORMAT);
		return result;

	}


}
