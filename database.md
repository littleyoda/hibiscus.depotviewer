# DepotViewer — Datenbank und Zahlen-Präzision

Stand: dbversion 19 (Branch `feat/precision-8`). Dieses Dokument beschreibt, wo
Beträge, Kurse, Gebühren und Steuern gespeichert werden, wo die Präzision bisher
begrenzt war (5 bzw. 6 Nachkommastellen) und was mit der Erweiterung auf
8 Nachkommastellen geändert wurde.

## Architektur-Überblick

Der DepotViewer legt seine Tabellen in der **Hibiscus-Datenbank** an
(`SQLUtils.getConnection()` holt die Verbindung über den Hibiscus-`HBCIDBService`).
Unterstützte Datenbanken sind damit **H2** (Hibiscus-Standard) und **MySQL/MariaDB**
(`DBSupportH2Impl` / `DBSupportMySqlImpl`, siehe `SQLUtils.getDateDiff()`).

Schema-Änderungen laufen ausschließlich über
[`SQLChange.getChangesSinceVersion()`](src/de/open4me/depot/sql/SQLChange.java):
Beim Start führt `SQLUtils.checkforupdates()` alle Changesets aus, deren Version
größer als der Wert `dbversion` in `depotviewer_cfg` ist. Es gibt kein separates
"frisches" Schema — auch eine Neuinstallation durchläuft alle Versionen 3 → 19.
Die Dateien unter [sql/](sql/) sind generierte, konsolidierte Abbilder des Schemas
zu Dokumentationszwecken.

## Wo werden Beträge gespeichert?

| Tabelle | Spalte | Bedeutung | bis v18 | ab v19 |
|---|---|---|---|---|
| `depotviewer_umsaetze` | `anzahl` | Stückzahl der Order | `decimal(20,10)` | unverändert `decimal(20,10)` |
| `depotviewer_umsaetze` | `kurs` | Kurs pro Stück | `decimal(20,6)` | `decimal(20,8)` |
| `depotviewer_umsaetze` | `kosten` | Gesamtbetrag der Order | `decimal(20,6)` | `decimal(20,8)` |
| `depotviewer_umsaetze` | `transaktionskosten` | Gebühren | `decimal(20,6)` | `decimal(20,8)` |
| `depotviewer_umsaetze` | `steuern` | Steuern | `decimal(20,6)` | `decimal(20,8)` |
| `depotviewer_bestand` | `anzahl` | Stückzahl im Bestand | `decimal(20,10)` | unverändert |
| `depotviewer_bestand` | `kurs` | Bewertungskurs | `decimal(20,6)` | `decimal(20,8)` |
| `depotviewer_bestand` | `wert` | Positionswert | `decimal(20,6)` | `decimal(20,8)` |
| `depotviewer_kurse` | `kurs` | historischer Kurs | `decimal(20,6)` | `decimal(20,8)` |
| `depotviewer_kurse` | `kursperf` | bereinigter Kurs | `decimal(20,6)` | `decimal(20,8)` |
| `depotviewer_kursevent` | `value` | Dividende pro Stück | **`decimal(10,5)`** | `decimal(20,8)` |

Historie der Präzision: v3–v15 speicherten Geldbeträge mit `decimal(20,2)`;
v16 erweiterte auf `decimal(20,6)`. `depotviewer_kursevent.value` stammt aus v7
und blieb bis v18 bei `decimal(10,5)` — das war die einzige Spalte, die noch auf
5 Nachkommastellen begrenzt war. **v19** hebt alle Geld-/Kurs-Spalten auf
`decimal(20,8)` an.

## Gefundene 5-Nachkommastellen-Begrenzungen (vor diesem Branch)

### Datenbank (verlustbehaftet — Werte wurden gerundet gespeichert)

1. **`depotviewer_kursevent.value decimal(10,5)`**
   ([SQLChange.java](src/de/open4me/depot/sql/SQLChange.java), Changeset v7).
   Dividenden pro Stück wurden auf 5 Nachkommastellen gerundet. → v19: `decimal(20,8)`.
2. Alle übrigen Geldspalten waren seit v16 auf **6** Nachkommastellen begrenzt
   (`decimal(20,6)`), siehe Tabelle oben. → v19: 8 Nachkommastellen.

### Backend (verlustbehaftet — Rundung vor dem Speichern)

3. **[BestandImportAction.java:101](src/de/open4me/depot/gui/action/BestandImportAction.java#L101)** —
   beim CSV-Bestandsimport wurde ein fehlender Kurs als `wert / anzahl` mit
   **Scale 5** (`divide(..., 5, HALF_UP)`) berechnet. → jetzt Scale 8.
4. **[UmsatzImportAction.java:124](src/de/open4me/depot/gui/action/UmsatzImportAction.java#L124)** —
   beim CSV-Orderimport wurde ein fehlender Kurs als `kosten / anzahl` mit
   **Scale 5** berechnet. → jetzt Scale 8.

### Frontend / GUI (verlustbehaftet beim Editier-Roundtrip)

5. **[UmsatzEditorControl.java](src/de/open4me/depot/gui/control/UmsatzEditorControl.java)** —
   die Eingabefelder Anzahl, Einzelkurs, Kurswert, Transaktionskosten und Steuern
   verwendeten `DecimalInput` mit `VarDecimalFormat(2, 3)` = 2 feste + max. 3
   zusätzliche = **maximal 5 angezeigte Nachkommastellen**. Beim Öffnen einer
   bestehenden Order wurde der Wert auf 5 Stellen gerundet angezeigt und beim
   Speichern der gerundete Anzeigetext zurückgeschrieben — d. h. jede Bearbeitung
   rundete gespeicherte Werte auf 5 Nachkommastellen. → jetzt `VarDecimalFormat(2, 6)`
   (bis zu 8 Nachkommastellen).
   Das Feld „Gesamtsumme" bleibt bewusst bei 2 Nachkommastellen (reine Anzeige eines
   berechneten EUR-Betrags; gespeichert wird `kosten` aus dem Kurswert-Feld).

### Nur Anzeige (nicht verlustbehaftet für die DB, aber auf 5 Stellen gekappt)

6. **[UmsatzHelper.java:41](src/de/open4me/depot/tools/UmsatzHelper.java#L41)** —
   `VarDecimalFormat(5)` beim Übertrag eines Depot-Umsatzes in den Hibiscus-Umsatz
   (Verwendungszweck-Text „Kurs: …", „… STK"). → jetzt `VarDecimalFormat(5, 3)`:
   weiterhin 5 feste Nachkommastellen (Textformat bleibt kompatibel), aber bis zu
   8 wenn tatsächlich mehr Präzision vorhanden ist.
7. **[OrderSearchProvider.java:73](src/de/open4me/depot/search/OrderSearchProvider.java#L73)** —
   `VarDecimalFormat(5)` für Suchergebnisse. → jetzt `VarDecimalFormat(5, 3)`.
8. Listen-Spalten (reine Anzeige, unverändert gelassen):
   `OrderListControl`/`BestandTableControl` zeigen Anzahl mit `%,.5f`, Kurs mit
   `%,.6f`, Beträge mit `%,.2f`; `WertpapiereDatenControl` zeigt Kurse mit `%.6f`.
   Diese Formatierungen betreffen nur die Darstellung in Tabellen, nicht die
   gespeicherten Werte.

### Bekannte, nicht geänderte Präzisionsgrenzen

- `anzahl` ist mit `decimal(20,10)` bereits genauer als 8 Stellen — unverändert.
- Mehrere Abrufwege reichen Werte als `double` durch (z. B.
  `Utils.addUmsatz(..., Double kurs, ...)`, `HBCIDepotBestandJob` mit
  `getValue().doubleValue()`). `double` trägt ~15–16 signifikante Stellen und ist
  damit für 8 Nachkommastellen bei üblichen Kursen ausreichend; eine Umstellung
  auf durchgängig `BigDecimal` wäre eine separate Refaktorierung.
- `WertBerechnung.java:136` rechnet bereits mit Scale 8, `UpdateStock` (Splitfaktor)
  mit Scale 10.

## Migration v19

Neues Changeset in [`SQLChange.java`](src/de/open4me/depot/sql/SQLChange.java).
**Wichtig:** Ab v19 sind die Statements datenbankspezifisch, denn H2 2.x (das
aktuelle Jameica/Hibiscus bündelt H2 2.3) versteht das MySQL-`MODIFY COLUMN`
der älteren Changesets nicht mehr, und MySQL versteht das H2-`ALTER COLUMN <typ>`
nicht. `SQLChange.getChangesSinceVersion(version, mysql)` liefert deshalb je
Datenbank die passende Variante (gleiches Muster wie die Hibiscus-eigenen
Updates, z. B. `update0065.java`); `SQLUtils.checkforupdates()` wählt anhand des
Hibiscus-Treibers (`DBSupportMySqlImpl`).

H2:

```sql
ALTER TABLE depotviewer_umsaetze  ALTER COLUMN kurs               decimal(20,8);
ALTER TABLE depotviewer_umsaetze  ALTER COLUMN kosten             decimal(20,8);
ALTER TABLE depotviewer_umsaetze  ALTER COLUMN transaktionskosten decimal(20,8);
ALTER TABLE depotviewer_umsaetze  ALTER COLUMN steuern            decimal(20,8);
ALTER TABLE depotviewer_bestand   ALTER COLUMN kurs               decimal(20,8);
ALTER TABLE depotviewer_bestand   ALTER COLUMN wert               decimal(20,8);
ALTER TABLE depotviewer_kurse     ALTER COLUMN kurs               decimal(20,8);
ALTER TABLE depotviewer_kurse     ALTER COLUMN kursperf           decimal(20,8);
ALTER TABLE depotviewer_kursevent ALTER COLUMN "VALUE"            decimal(20,8);
```

(`value` ist in H2 2.x ein Schlüsselwort und muss gequotet werden; unquotierte
Bezeichner wurden beim Anlegen unter H2 1.x in Großbuchstaben gespeichert,
daher `"VALUE"`.)

MySQL/MariaDB (Stil wie v16):

```sql
ALTER TABLE depotviewer_umsaetze  MODIFY COLUMN `kurs`               decimal(20,8);
-- … analog für kosten, transaktionskosten, steuern, bestand.kurs/wert,
--    kurse.kurs/kursperf, kursevent.`value`
```

### Abwärtskompatibilität

- Die H2-Variante (`ALTER TABLE … ALTER COLUMN <spalte> <typ>`) funktioniert in
  H2 1.x **und** im Standardmodus von H2 2.x (empirisch mit H2 2.3.232 geprüft);
  die MySQL-Variante entspricht der bereits in v10/v16 verwendeten Syntax.
- Hinweis (vorbestehend, nicht Teil dieses Branches): Eine **Neuinstallation**
  auf H2 2.x schlägt bereits im Changeset v3 fehl (`int(10)`, Spaltenname
  `value`), unabhängig von v19. Bestehende Datenbanken sind nicht betroffen,
  da ihre Tabellen unter H2 1.x angelegt und per Jameica-Migration in das
  2.x-Format überführt wurden.
- Die Scale wird nur **erweitert** (2/5/6 → 8). Bestehende Werte werden von beiden
  Datenbanken verlustfrei übernommen (Auffüllen mit Nullen, keine Rundung). Der
  Unit-Test `PrecisionTest.migrationV19ErhaeltBestehendeWerteH2` weist das nach.
- Die Precision 20 bleibt erhalten; der ganzzahlige Anteil sinkt von 14 auf
  12 Stellen (`decimal(20,8)`), was für Beträge weiterhin bis zu 999 Milliarden
  zulässt. Für `kursevent.value` wächst die Kapazität (10,5 → 20,8).
- Ältere Datenbestände (Version < 19) werden beim ersten Start automatisch
  migriert; ein Downgrade des Plugins nach der Migration würde — wie bei allen
  bisherigen Schema-Erweiterungen — von den alten Formaten toleriert, da die
  alten Versionen nur mit größerer Rundung schreiben.
- Konsolidiertes Schema: [sql/depotviewer_schema_v19.sql](sql/depotviewer_schema_v19.sql)
  (v18-Datei bleibt als historisches Abbild erhalten).

## Tests

[test/database/PrecisionTest.java](test/database/PrecisionTest.java) (H2, JUnit 4):

- **Roundtrip-Tests** pro Tabelle: Werte mit 8 Nachkommastellen werden eingefügt
  und wieder ausgelesen; der zurückgelesene `BigDecimal` muss numerisch exakt dem
  eingefügten entsprechen (`compareTo == 0`).
- **Migrationstest**: Schema wird bis v18 aufgebaut, Alt-Werte mit 6 (bzw. 5 für
  `kursevent.value`) Nachkommastellen eingefügt, dann v19 angewendet. Die
  Alt-Werte müssen unverändert erhalten bleiben, und anschließend müssen
  8-stellige Werte speicherbar sein.
- Der bestehende Test `DataBaseCreation` prüft weiterhin, dass die komplette
  Changeset-Kette 0 → aktuell auf einer leeren Datenbank durchläuft
  (mit H2 1.x bzw. MySQL; siehe Hinweis oben zu Neuinstallationen auf H2 2.x).

Ausführen (es gibt kein Ant-Test-Target; JUnit 4 + H2-Treiber genügen):

```sh
javac -d /tmp/testbin -cp junit-4.13.2.jar \
    src/de/open4me/depot/sql/SQLChange.java test/database/PrecisionTest.java
java -cp /tmp/testbin:junit-4.13.2.jar:hamcrest-core-1.3.jar:h2-2.3.232.jar \
    org.junit.runner.JUnitCore database.PrecisionTest
```
