-- Consolidated schema for hibiscus.depotviewer at dbversion 19.
-- Generated from src/de/open4me/depot/sql/SQLChange.java (versions 3-19 folded together).
-- Contains only CREATE TABLE / index statements, no ALTER TABLE.

DROP TABLE IF EXISTS depotviewer_kursevent;
DROP TABLE IF EXISTS depotviewer_cfgupdatestock;
DROP TABLE IF EXISTS depotviewer_umsaetze;
DROP TABLE IF EXISTS depotviewer_bestand;
DROP TABLE IF EXISTS depotviewer_kurse;
DROP TABLE IF EXISTS depotviewer_wertpapier;
DROP TABLE IF EXISTS depotviewer_cfg;

CREATE TABLE depotviewer_cfg (
  id int NOT NULL auto_increment,
  `key` varchar(200) COMMENT 'Setting name, e.g. dbversion, status_bestand_order, or csv.<savename>.<source>.* import-profile keys',
  value varchar(2000) COMMENT 'Setting value as text (int string for dbversion, boolean-like for status_bestand_order, free text for CSV import options)',
  PRIMARY KEY (id)
) COMMENT='Generic key/value settings store for the plugin, not tied to a specific security';

CREATE TABLE depotviewer_wertpapier (
  id int NOT NULL auto_increment,
  wertpapiername varchar(255) NOT NULL COMMENT 'Display name of the security',
  wkn varchar(6) NOT NULL COMMENT 'German Wertpapierkennnummer, used for lookup and as a fallback quote-provider search term',
  isin varchar(12) NOT NULL COMMENT 'ISIN, primary lookup key and preferred quote-provider search term',
  PRIMARY KEY (id)
) COMMENT='Master data for one security (stock/fund) tracked by the depot viewer';

CREATE TABLE depotviewer_umsaetze (
  id int NOT NULL auto_increment,
  wpid int COMMENT 'References depotviewer_wertpapier.id: the security this transaction is for',
  kontoid int(10) COMMENT 'References konto.id (Hibiscus account): the account this transaction belongs to',
  anzahl decimal(20,10) COMMENT 'Number of units/shares transacted (always positive)',
  kurs decimal(20,8) COMMENT 'Price per unit for the transaction',
  kursw varchar(3) NOT NULL COMMENT 'ISO currency code of kurs',
  kosten decimal(20,8) COMMENT 'Total transaction amount (Anzahl x Kurs, adjusted); negative for purchases, positive for sales/outbound bookings',
  kostenw varchar(3) NOT NULL COMMENT 'ISO currency code of kosten',
  aktion varchar(30) NOT NULL COMMENT 'Transaction type: KAUF (purchase), VERKAUF (sale), EINLIEFERUNG (inbound transfer), AUSLIEFERUNG (outbound transfer), see DepotAktion',
  buchungsdatum date COMMENT 'Booking/settlement date of the transaction',
  buchungsinformationen varchar(2000) COMMENT 'Free-text booking description, typically taken verbatim from the bank/broker statement or import source',
  orderid varchar(50) COMMENT 'Dedup/identity key for the transaction: broker order id if known, otherwise a synthetic hash of key fields, used to skip duplicate imports',
  kommentar varchar(2000) COMMENT 'Free-text user comment/note on the transaction, editable in the UI',
  transaktionskosten decimal(20,8) COMMENT 'Broker/transaction fees charged for this order',
  transaktionskostenw varchar(3) COMMENT 'ISO currency code of transaktionskosten',
  steuern decimal(20,8) COMMENT 'Taxes withheld/charged on this transaction',
  steuernw varchar(3) COMMENT 'ISO currency code of steuern',
  PRIMARY KEY (id),
  CONSTRAINT fkdvumsaetze FOREIGN KEY (kontoid) REFERENCES konto (id) ON DELETE CASCADE
) COMMENT='Booked depot transactions/orders (buys, sells, inbound/outbound transfers of securities)';

CREATE TABLE depotviewer_bestand (
  id int NOT NULL auto_increment,
  wpid int COMMENT 'References depotviewer_wertpapier.id: the security held',
  kontoid int(10) COMMENT 'References konto.id (Hibiscus account): the account holding the position',
  anzahl decimal(20,10) COMMENT 'Quantity of units held at snapshot time',
  kurs decimal(20,8) COMMENT 'Price per unit at valuation time',
  kursw varchar(3) NOT NULL COMMENT 'ISO currency code of kurs',
  wert decimal(20,8) COMMENT 'Total value of the position (Anzahl x Kurs / Depotwert) as reported by the bank',
  wertw varchar(3) NOT NULL COMMENT 'ISO currency code of wert',
  datum date NOT NULL COMMENT 'Date the depot snapshot/retrieval was taken',
  bewertungszeitpunkt date COMMENT 'Bank-supplied timestamp at which the price used for this position was determined; can differ per position within a snapshot',
  PRIMARY KEY (id),
  CONSTRAINT fkdvbestand FOREIGN KEY (kontoid) REFERENCES konto (id) ON DELETE CASCADE
) COMMENT='Point-in-time snapshot of held positions (portfolio holdings)';

CREATE TABLE depotviewer_kurse (
  id int NOT NULL auto_increment,
  wpid int COMMENT 'References depotviewer_wertpapier.id: the security this quote belongs to',
  kurs decimal(20,8) COMMENT 'Raw quoted price on kursdatum',
  kursw varchar(3) NOT NULL COMMENT 'ISO currency code of kurs',
  kursdatum date COMMENT 'Date of the quote',
  kursperf decimal(20,8) COMMENT 'Corporate-action-adjusted ("performance"/adjusted-close) price, derived from kurs by applying depotviewer_kursevent splits and dividends so historical values are comparable to today''s price',
  PRIMARY KEY (id)
) COMMENT='Historical daily price series per security';

CREATE INDEX idxKurseWpid ON depotviewer_kurse (wpid);
CREATE INDEX idxKurseDatum ON depotviewer_kurse (kursdatum);
CREATE INDEX idxKurseId ON depotviewer_kurse (id);

CREATE TABLE depotviewer_kursevent (
  id int NOT NULL auto_increment,
  wpid int COMMENT 'References depotviewer_wertpapier.id: the security affected by this corporate action',
  ratio varchar(30) COMMENT 'Colon-separated ratio (e.g. "2:1"), used for split-type events (S/R/G) to compute the price-adjustment factor',
  value decimal(20,8) COMMENT 'Per-share monetary amount, used for cash-dividend events (D) as the amount subtracted from the adjusted price; meaning for other event types is unused/ambiguous in code',
  aktion varchar(100) NOT NULL COMMENT 'Event-type code (see KursEventAktion): D=Dividende, G=Aktien-Dividende, S=Split, R=Reverse Split, B=Bezugsrecht (stored but not fully consumed by the performance calc)',
  datum date COMMENT 'Date the corporate action took effect (ex-date), used to order events for the performance-price walk',
  waehrung varchar(3) COMMENT 'ISO currency code associated with the event (e.g. the dividend currency); not consumed in the adjustment calculation',
  PRIMARY KEY (id)
) COMMENT='Corporate actions (splits, dividends, subscription rights) affecting a security''s price history';

CREATE TABLE depotviewer_cfgupdatestock (
  id int NOT NULL auto_increment,
  wpid int COMMENT 'References depotviewer_wertpapier.id: the security these settings apply to',
  `key` varchar(200) COMMENT 'NULL for the selected quote-provider name in value, otherwise the description of a provider-specific config option answered in value',
  value varchar(200) COMMENT 'Selected quote-provider name (when key is NULL) or the chosen answer for the named config option',
  PRIMARY KEY (id)
) COMMENT='Per-security settings remembered for the automatic price-update job, so it does not re-prompt the user';
