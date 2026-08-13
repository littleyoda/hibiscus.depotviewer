package de.open4me.depot.tools;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Hilfsfunktionen für die verlustfreie Umwandlung von Geldbeträgen, Kursen und
 * Stückzahlen nach {@link BigDecimal} sowie für die Berechnung der synthetischen
 * OrderIDs.
 *
 * Bewusst frei von Jameica-Abhängigkeiten, damit die Umrechnungsregeln ohne
 * laufende Anwendung testbar bleiben (siehe {@code de.open4me.depot.tools.ZahlenTest}).
 */
public final class Zahlen {

	private Zahlen() {
		// nur statische Methoden
	}

	/**
	 * Wandelt eine Zahl in deutscher Schreibweise ("1.234,56") in einen exakten
	 * BigDecimal um.
	 *
	 * Loest {@code Utils.getDoubleFromZahl} ab, das dieselbe Aufgabe ueber
	 * Double erledigt hat. Derzeit ohne Aufrufer im Produktivcode — gedacht
	 * fuer Importquellen, die Betraege als vorformatierten Text liefern.
	 */
	public static BigDecimal ausDeutscherSchreibweise(String s) {
		return new BigDecimal(s.replace(".", "").replace(",", "."));
	}

	/**
	 * Wandelt einen beliebigen Zahlwert verlustfrei in einen BigDecimal um.
	 *
	 * Wird für die per {@code QueryMessage} von fremden Plugins gelieferten,
	 * untypisierten Werte benötigt: die legen dort historisch Double ab, neuere
	 * Sender können BigDecimal oder String liefern. Ein harter Cast auf einen
	 * dieser Typen würde die jeweils anderen Sender brechen.
	 *
	 * Gilt genauso für Werte aus einem {@code GenericObjectSQL}: welchen
	 * konkreten Number-Typ der JDBC-Treiber für eine decimal-Spalte liefert,
	 * ist nicht garantiert und kann sich zwischen H2 und MySQL unterscheiden.
	 *
	 * @param o Zahlwert, darf null sein
	 * @return exakter BigDecimal oder null
	 */
	public static BigDecimal toBigDecimal(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof BigDecimal) {
			return (BigDecimal) o;
		}
		// Umweg über toString() statt doubleValue(): so bleiben Long/BigInteger
		// exakt, und bei Double entspricht es BigDecimal.valueOf(double), also
		// der dargestellten Dezimalzahl statt der binären Expansion.
		return new BigDecimal(o.toString());
	}

	/**
	 * Wie {@link #toBigDecimal(Object)}, liefert aber 0 statt null.
	 *
	 * Gedacht für die optionalen Beträge Gebühren und Steuern: die Datenbank
	 * führt sie als 0 und nicht als NULL — das Changeset auf dbversion 12 hat
	 * bestehende NULL-Werte auf 0 gesetzt (siehe {@code SQLChange}). Fehlt der
	 * Wert in einer QueryMessage, ist 0 daher die richtige Annahme.
	 *
	 * @param o Zahlwert, darf null sein
	 * @return exakter BigDecimal, nie null
	 */
	public static BigDecimal optionalerBetrag(Object o) {
		BigDecimal wert = toBigDecimal(o);
		return (wert == null) ? BigDecimal.ZERO : wert;
	}

	/**
	 * Berechnet die synthetische OrderID für importierte Umsätze, die vom
	 * Datenlieferanten keine eigene ID mitbekommen.
	 */
	public static String berechneOrderId(String kontoid, String wpid, String aktion, Date date,
			BigDecimal anzahl, BigDecimal kurs, String kursW) {
		return "" + ("" + kontoid + wpid + aktion + date
				+ fuerHash(anzahl) + fuerHash(kurs) + kursW).hashCode();
	}

	/**
	 * Berechnet die synthetische OrderID für im Umsatz-Editor manuell angelegte
	 * Umsätze.
	 *
	 * Feldreihenfolge und Position der Währung weichen bewusst von
	 * {@link #berechneOrderId} ab — es ist die Formel, die der Editor schon
	 * immer verwendet hat.
	 */
	public static String berechneManuelleOrderId(String wpid, String aktion,
			BigDecimal anzahl, BigDecimal kurs, String kursW, Date date) {
		return "" + (wpid + aktion + fuerHash(anzahl) + fuerHash(kurs) + kursW + date).hashCode();
	}

	/**
	 * Stellt einen Betrag genau so dar, wie ihn die frühere Double-Variante in
	 * den Hash gegeben hat.
	 *
	 * Der Umweg über {@link BigDecimal#doubleValue()} ist kein Versehen: anzahl
	 * und kurs waren früher vom Typ Double, und BigDecimal.toString() liefert
	 * eine andere Darstellung ("10" statt "10.0") und damit einen anderen Hash.
	 * Ohne diese Umrechnung würden bereits importierte Orders nicht mehr
	 * wiedergefunden und beim nächsten Abruf ein zweites Mal angelegt.
	 */
	private static String fuerHash(BigDecimal wert) {
		// "null" entspricht dem, was die Verkettung mit einem Double-Objekt
		// vor der Umstellung ergeben hat.
		return (wert == null) ? "null" : Double.toString(wert.doubleValue());
	}
}
