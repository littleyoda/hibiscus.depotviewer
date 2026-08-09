package de.open4me.depot.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import java.util.Date;

import org.junit.Test;

/**
 * Sichert die Umstellung der Import-Schnittstellen von Double auf BigDecimal ab.
 *
 * Zwei Dinge werden geprüft:
 * <ol>
 * <li>Der Wertebereich, den {@link Zahlen#toBigDecimal(Object)} von fremden
 *     Plugins entgegennimmt (die liefern historisch Double).</li>
 * <li>Dass die synthetische OrderID exakt so berechnet wird wie vor der
 *     Umstellung — sonst würden bereits importierte Orders doppelt angelegt.</li>
 * </ol>
 *
 * Der Weg über {@code Utils.addUmsatz(...)} selbst lässt sich nicht als Unit-Test
 * abbilden: die Methode holt die Verbindung über {@code Settings.getDBService()},
 * und sowohl {@code Settings} als auch {@code Utils} greifen bereits beim Laden
 * der Klasse auf die Jameica-Anwendung zu. Genau deshalb liegen die reinen
 * Rechenregeln in {@link Zahlen}. Die Speicherpräzision der Spalten selbst deckt
 * {@code database.PrecisionTest} auf JDBC-Ebene ab.
 */
public class ZahlenTest {

	/**
	 * Der Wert, an dem die alte Implementierung scheiterte: {@code new BigDecimal(double)}
	 * expandiert die Binärdarstellung exakt, statt die dargestellte Dezimalzahl zu nehmen.
	 */
	private static final String KURS = "54321.12345678";

	@Test
	public void toBigDecimalNimmtBigDecimalUnveraendert() {
		BigDecimal erwartet = new BigDecimal(KURS);
		assertEquals(0, erwartet.compareTo(Zahlen.toBigDecimal(erwartet)));
	}

	@Test
	public void toBigDecimalLiestStringExakt() {
		assertEquals(0, new BigDecimal(KURS).compareTo(Zahlen.toBigDecimal(KURS)));
	}

	@Test
	public void toBigDecimalLiefertNullFuerNull() {
		assertNull(Zahlen.toBigDecimal(null));
	}

	/**
	 * Fremde Plugins liefern Double. Der Wert muss der dargestellten Dezimalzahl
	 * entsprechen und darf nicht die binaere Expansion enthalten.
	 */
	@Test
	public void toBigDecimalNimmtDoubleOhneBinaerartefakte() {
		assertEquals("0.1", Zahlen.toBigDecimal(Double.valueOf(0.1d)).toPlainString());
		// Gegenprobe: so haette es die alte Implementierung (new BigDecimal(double)) gemacht
		assertNotEquals("0.1", new BigDecimal(0.1d).toPlainString());
	}

	@Test
	public void toBigDecimalNimmtGanzzahlenExakt() {
		assertEquals(0, new BigDecimal("42").compareTo(Zahlen.toBigDecimal(Integer.valueOf(42))));
		// Wert jenseits der exakten double-Aufloesung: ueber doubleValue() ginge er verloren
		assertEquals(0, new BigDecimal("9007199254740993").compareTo(
				Zahlen.toBigDecimal(Long.valueOf(9007199254740993L))));
	}

	@Test
	public void deutscheSchreibweiseWirdExaktGelesen() {
		assertEquals(0, new BigDecimal("1234.56").compareTo(
				Zahlen.ausDeutscherSchreibweise("1.234,56")));
	}

	/**
	 * Gebuehren und Steuern sind optional. Fehlen sie in der QueryMessage, muss
	 * 0 in die Datenbank wandern und nicht NULL - so haelt es auch das Changeset
	 * auf dbversion 12, das bestehende NULL-Werte auf 0 gesetzt hat.
	 */
	@Test
	public void optionalerBetragLiefertNullStattNichts() {
		assertEquals(0, BigDecimal.ZERO.compareTo(Zahlen.optionalerBetrag(null)));
	}

	@Test
	public void optionalerBetragReichtVorhandeneWerteDurch() {
		assertEquals(0, new BigDecimal("12.34").compareTo(
				Zahlen.optionalerBetrag(Double.valueOf(12.34d))));
	}

	/**
	 * Die OrderID muss bit-identisch zur Berechnung vor der BigDecimal-Umstellung
	 * bleiben. Referenz ist die alte Formel mit Double-Parametern:
	 * {@code ("" + kontoid + wpid + aktion + date + anzahl + kurs + kursW).hashCode()}
	 */
	@Test
	public void orderIdBleibtKompatibelZurDoubleVariante() {
		String kontoid = "1";
		String wpid = "4711";
		String aktion = "KAUF";
		Date date = new Date(0L);
		String kursW = "EUR";

		Double altAnzahl = Double.valueOf(10.0d);
		Double altKurs = Double.valueOf(123.45d);
		String erwartet = "" + ("" + kontoid + wpid + aktion + date + altAnzahl + altKurs + kursW).hashCode();

		String actual = Zahlen.berechneOrderId(kontoid, wpid, aktion, date,
				new BigDecimal("10"), new BigDecimal("123.45"), kursW);

		assertEquals(erwartet, actual);
	}

	/**
	 * Gegenprobe: Ohne die doubleValue()-Umrechnung wuerde sich die OrderID aendern.
	 * Schlaegt dieser Test fehl, ist der Kompatibilitaets-Schutz wirkungslos geworden.
	 */
	@Test
	public void orderIdWuerdeSichOhneUmrechnungAendern() {
		Date date = new Date(0L);
		String naiv = "" + ("" + "1" + "4711" + "KAUF" + date
				+ new BigDecimal("10") + new BigDecimal("123.45") + "EUR").hashCode();
		String actual = Zahlen.berechneOrderId("1", "4711", "KAUF", date,
				new BigDecimal("10"), new BigDecimal("123.45"), "EUR");
		assertNotEquals(naiv, actual);
	}

	/**
	 * Fehlende Werte durften frueher nicht knallen: die Verkettung mit einem
	 * Double-Objekt ergab schlicht "null". Ein Sender, der weder orderid noch
	 * anzahl mitschickt, muss weiterhin eine ID bekommen.
	 */
	@Test
	public void orderIdVertraegtFehlendeWerte() {
		Date date = new Date(0L);
		Double fehlt = null;
		String erwartet = "" + ("" + "1" + "4711" + "KAUF" + date + fehlt + fehlt + "EUR").hashCode();

		assertEquals(erwartet, Zahlen.berechneOrderId("1", "4711", "KAUF", date, null, null, "EUR"));
	}

	/**
	 * Die OrderID des Umsatz-Editors hat eine eigene Feldreihenfolge. Referenz
	 * ist die Formel, die dort vor der BigDecimal-Umstellung stand — die Felder
	 * lieferten damals ueber {@code DecimalInput.getValue()} ein Double.
	 */
	@Test
	public void manuelleOrderIdBleibtKompatibelZurDoubleVariante() {
		String wpid = "4711";
		String aktion = "KAUF";
		Date date = new Date(0L);

		Double altAnzahl = Double.valueOf(10.0d);
		Double altKurs = Double.valueOf(123.45d);
		String erwartet = "" + (wpid + aktion + altAnzahl + altKurs + "EUR" + date).hashCode();

		String actual = Zahlen.berechneManuelleOrderId(wpid, aktion,
				new BigDecimal("10"), new BigDecimal("123.45"), "EUR", date);

		assertEquals(erwartet, actual);
	}

	/**
	 * Die beiden Formeln duerfen nicht zusammenfallen — sie unterscheiden sich
	 * in Feldreihenfolge und Kontobezug.
	 */
	@Test
	public void beideOrderIdFormelnSindVerschieden() {
		Date date = new Date(0L);
		BigDecimal anzahl = new BigDecimal("10");
		BigDecimal kurs = new BigDecimal("123.45");

		assertNotEquals(
				Zahlen.berechneOrderId("1", "4711", "KAUF", date, anzahl, kurs, "EUR"),
				Zahlen.berechneManuelleOrderId("4711", "KAUF", anzahl, kurs, "EUR", date));
	}
}
