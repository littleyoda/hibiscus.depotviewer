package database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Test;

import de.open4me.depot.sql.SQLChange;

/**
 * Prüft, dass Beträge, Kurse, Gebühren und Steuern mit 8 Nachkommastellen
 * verlustfrei gespeichert und wieder ausgelesen werden (dbversion 19)
 * und dass die Migration von dbversion 18 auf 19 bestehende Werte
 * unverändert lässt.
 *
 * Hintergrund zu den H2-Verbindungsoptionen: Die produktiven Depotviewer-Tabellen
 * wurden ursprünglich unter H2 1.x angelegt. H2 2.x akzeptiert die alten
 * CREATE-TABLE-Statements (int(10), Spaltenname "value") nur noch mit
 * MODE=MySQL und NON_KEYWORDS=VALUE. Der Aufbau des Alt-Schemas läuft daher mit
 * diesen Optionen; die eigentliche v19-Migration wird anschließend über eine
 * Verbindung im H2-Standardmodus ausgeführt — genau wie in einer produktiven
 * Hibiscus-Installation mit H2 2.x.
 */
public class PrecisionTest {

	private static final int OLD_VERSION = 18;
	private static final String LEGACY_OPTS = ";MODE=MySQL;NON_KEYWORDS=VALUE";

	private File dbFile() throws Exception {
		Class.forName("org.h2.Driver");
		File file = File.createTempFile("depotviewer_precision", "test");
		file.delete(); // Not safe, but in this case ok (siehe DataBaseCreation)
		file.deleteOnExit();
		return file;
	}

	private Connection connect(File file, String opts) throws SQLException {
		return DriverManager.getConnection("jdbc:h2:" + file.getAbsolutePath() + opts, "", "");
	}

	/**
	 * Erzeugt eine frische Datenbank und spielt alle Changesets bis
	 * einschließlich targetVersion ein (Integer.MAX_VALUE = alle).
	 */
	private Connection createDb(File file, int targetVersion, boolean mysql) throws Exception {
		Connection conn = connect(file, LEGACY_OPTS);
		try (Statement s = conn.createStatement()) {
			s.execute("create table konto (id int NOT NULL auto_increment, PRIMARY KEY (id));");
		}
		migrate(conn, 0, targetVersion, mysql);
		return conn;
	}

	private void migrate(Connection conn, int fromVersion, int toVersion, boolean mysql) throws SQLException {
		for (SQLChange change : SQLChange.getChangesSinceVersion(fromVersion, mysql)) {
			if (change.getVersion() > toVersion) {
				continue;
			}
			try (Statement s = conn.createStatement()) {
				for (String query : change.getQuery()) {
					s.execute(query);
				}
			}
		}
	}

	private BigDecimal readBack(Connection conn, String query) throws SQLException {
		try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(query)) {
			assertTrue("Datensatz nicht gefunden: " + query, rs.next());
			return rs.getBigDecimal(1);
		}
	}

	private void assertSameValue(String column, BigDecimal expected, BigDecimal actual) {
		assertEquals("Präzisionsverlust in " + column + ": erwartet " + expected.toPlainString()
				+ ", gelesen " + (actual == null ? "null" : actual.toPlainString()),
				0, expected.compareTo(actual));
	}

	private int insertUmsatz(Connection conn, BigDecimal anzahl, BigDecimal kurs, BigDecimal kosten,
			BigDecimal gebuehren, BigDecimal steuern) throws SQLException {
		try (PreparedStatement p = conn.prepareStatement(
				"insert into depotviewer_umsaetze (anzahl, kurs, kursw, kosten, kostenw, "
				+ "transaktionskosten, transaktionskostenw, steuern, steuernw, aktion, orderid) "
				+ "values (?,?,'EUR',?,'EUR',?,'EUR',?,'EUR','KAUF','test')",
				Statement.RETURN_GENERATED_KEYS)) {
			p.setBigDecimal(1, anzahl);
			p.setBigDecimal(2, kurs);
			p.setBigDecimal(3, kosten);
			p.setBigDecimal(4, gebuehren);
			p.setBigDecimal(5, steuern);
			p.executeUpdate();
			try (ResultSet keys = p.getGeneratedKeys()) {
				assertTrue(keys.next());
				return keys.getInt(1);
			}
		}
	}

	private void checkUmsatzRoundtrip(Connection conn, BigDecimal anzahl, BigDecimal kurs,
			BigDecimal kosten, BigDecimal gebuehren, BigDecimal steuern) throws SQLException {
		int id = insertUmsatz(conn, anzahl, kurs, kosten, gebuehren, steuern);
		String from = " from depotviewer_umsaetze where id = " + id;
		assertSameValue("umsaetze.anzahl", anzahl, readBack(conn, "select anzahl" + from));
		assertSameValue("umsaetze.kurs", kurs, readBack(conn, "select kurs" + from));
		assertSameValue("umsaetze.kosten", kosten, readBack(conn, "select kosten" + from));
		assertSameValue("umsaetze.transaktionskosten", gebuehren, readBack(conn, "select transaktionskosten" + from));
		assertSameValue("umsaetze.steuern", steuern, readBack(conn, "select steuern" + from));
	}

	@Test
	public void umsatzSpeichert8Nachkommastellen() throws Exception {
		try (Connection conn = createDb(dbFile(), Integer.MAX_VALUE, true)) {
			checkUmsatzRoundtrip(conn,
					new BigDecimal("123.1234567891"),      // anzahl: decimal(20,10)
					new BigDecimal("54321.12345678"),      // kurs
					new BigDecimal("-98765432.12345678"),  // kosten (Kauf: negativ)
					new BigDecimal("1.00000001"),          // transaktionskosten
					new BigDecimal("0.12345678"));         // steuern
		}
	}

	@Test
	public void bestandSpeichert8Nachkommastellen() throws Exception {
		try (Connection conn = createDb(dbFile(), Integer.MAX_VALUE, true)) {
			BigDecimal anzahl = new BigDecimal("0.0000000001");
			BigDecimal kurs = new BigDecimal("12345.12345678");
			BigDecimal wert = new BigDecimal("999999999999.12345678");
			try (PreparedStatement p = conn.prepareStatement(
					"insert into depotviewer_bestand (anzahl, kurs, kursw, wert, wertw, datum) "
					+ "values (?,?,'EUR',?,'EUR','2026-07-06')")) {
				p.setBigDecimal(1, anzahl);
				p.setBigDecimal(2, kurs);
				p.setBigDecimal(3, wert);
				p.executeUpdate();
			}
			assertSameValue("bestand.anzahl", anzahl, readBack(conn, "select anzahl from depotviewer_bestand"));
			assertSameValue("bestand.kurs", kurs, readBack(conn, "select kurs from depotviewer_bestand"));
			assertSameValue("bestand.wert", wert, readBack(conn, "select wert from depotviewer_bestand"));
		}
	}

	@Test
	public void kurseSpeichert8Nachkommastellen() throws Exception {
		try (Connection conn = createDb(dbFile(), Integer.MAX_VALUE, true)) {
			BigDecimal kurs = new BigDecimal("1.23456789").setScale(8, java.math.RoundingMode.HALF_UP);
			BigDecimal kursperf = new BigDecimal("0.00000001");
			try (PreparedStatement p = conn.prepareStatement(
					"insert into depotviewer_kurse (wpid, kurs, kursw, kursdatum, kursperf) "
					+ "values (1,?,'EUR','2026-07-06',?)")) {
				p.setBigDecimal(1, kurs);
				p.setBigDecimal(2, kursperf);
				p.executeUpdate();
			}
			assertSameValue("kurse.kurs", kurs, readBack(conn, "select kurs from depotviewer_kurse"));
			assertSameValue("kurse.kursperf", kursperf, readBack(conn, "select kursperf from depotviewer_kurse"));
		}
	}

	@Test
	public void kurseventSpeichert8Nachkommastellen() throws Exception {
		try (Connection conn = createDb(dbFile(), Integer.MAX_VALUE, true)) {
			// vor v19 nur decimal(10,5): 5 Nachkommastellen und max. 5 Vorkommastellen
			BigDecimal dividende = new BigDecimal("123456.12345678");
			try (PreparedStatement p = conn.prepareStatement(
					"insert into depotviewer_kursevent (wpid, value, aktion, datum, waehrung) "
					+ "values (1,?,'D','2026-07-06','EUR')")) {
				p.setBigDecimal(1, dividende);
				p.executeUpdate();
			}
			assertSameValue("kursevent.value", dividende, readBack(conn, "select value from depotviewer_kursevent"));
		}
	}

	/**
	 * Abwärtskompatibilität H2: Werte, die mit dem alten Schema (v18, decimal(20,6)
	 * bzw. decimal(10,5)) gespeichert wurden, müssen die Migration auf v19
	 * unverändert überstehen. Die v19-Statements laufen dabei — wie in einer
	 * echten Hibiscus-Installation — über eine H2-Verbindung im Standardmodus.
	 */
	@Test
	public void migrationV19ErhaeltBestehendeWerteH2() throws Exception {
		File file = dbFile();
		int umsatzId;
		try (Connection conn = createDb(file, OLD_VERSION, false)) {
			// Alt-Daten mit der maximalen Präzision des alten Schemas
			umsatzId = insertUmsatz(conn,
					new BigDecimal("10.1234567891"),
					new BigDecimal("100.123456"),
					new BigDecimal("-1013.123456"),
					new BigDecimal("9.990000"),
					new BigDecimal("3.141592"));
			try (PreparedStatement p = conn.prepareStatement(
					"insert into depotviewer_kursevent (wpid, value, aktion, datum, waehrung) "
					+ "values (1,?,'D','2026-01-01','EUR')")) {
				p.setBigDecimal(1, new BigDecimal("12345.12345"));
				p.executeUpdate();
			}
		}

		// v19 im H2-Standardmodus ausführen (Produktionsszenario)
		try (Connection conn = connect(file, "")) {
			migrate(conn, OLD_VERSION, Integer.MAX_VALUE, false);

			String from = " from depotviewer_umsaetze where id = " + umsatzId;
			assertSameValue("umsaetze.anzahl", new BigDecimal("10.1234567891"), readBack(conn, "select anzahl" + from));
			assertSameValue("umsaetze.kurs", new BigDecimal("100.123456"), readBack(conn, "select kurs" + from));
			assertSameValue("umsaetze.kosten", new BigDecimal("-1013.123456"), readBack(conn, "select kosten" + from));
			assertSameValue("umsaetze.transaktionskosten", new BigDecimal("9.99"), readBack(conn, "select transaktionskosten" + from));
			assertSameValue("umsaetze.steuern", new BigDecimal("3.141592"), readBack(conn, "select steuern" + from));
			assertSameValue("kursevent.value", new BigDecimal("12345.12345"), readBack(conn, "select \"VALUE\" from depotviewer_kursevent"));

			// Nach der Migration: volle 8 Nachkommastellen speicherbar
			checkUmsatzRoundtrip(conn,
					new BigDecimal("1"),
					new BigDecimal("0.12345678"),
					new BigDecimal("-0.12345678"),
					new BigDecimal("0.00000001"),
					new BigDecimal("0.99999999"));
			try (PreparedStatement p = conn.prepareStatement(
					"insert into depotviewer_kursevent (wpid, \"VALUE\", aktion, datum, waehrung) "
					+ "values (2,?,'D','2026-07-06','EUR')")) {
				p.setBigDecimal(1, new BigDecimal("0.12345678"));
				p.executeUpdate();
			}
			assertSameValue("kursevent.value (8 Stellen)", new BigDecimal("0.12345678"),
					readBack(conn, "select \"VALUE\" from depotviewer_kursevent where wpid = 2"));
		}
	}

	/**
	 * Gegenprobe / Dokumentation der alten Grenze: vor v19 wurden Kurse auf
	 * 6 Nachkommastellen gerundet. Damit ist sichergestellt, dass die
	 * Testmethodik Rundung überhaupt erkennen würde.
	 */
	@Test
	public void altesSchemaRundetAuf6Nachkommastellen() throws Exception {
		try (Connection conn = createDb(dbFile(), OLD_VERSION, true)) {
			int id = insertUmsatz(conn,
					new BigDecimal("1"),
					new BigDecimal("0.12345678"), // 8 Stellen in ein decimal(20,6)-Feld
					new BigDecimal("-1"),
					BigDecimal.ZERO,
					BigDecimal.ZERO);
			BigDecimal gelesen = readBack(conn, "select kurs from depotviewer_umsaetze where id = " + id);
			assertEquals("v18-Schema muss auf 6 Stellen runden",
					0, new BigDecimal("0.123457").compareTo(gelesen));
		}
	}
}
