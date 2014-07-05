package de.open4me.depot.depotabruf;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.kapott.hbci.GV.HBCIJob;
import org.kapott.hbci.GV_Result.GVRWPDepotList;
import org.kapott.hbci.GV_Result.GVRWPDepotUms;
import org.kapott.hbci.GV_Result.GVRWPDepotList.Entry;
import org.kapott.hbci.GV_Result.GVRWPDepotList.Entry.Gattung;
import org.kapott.hbci.GV_Result.GVRWPDepotUms.Entry.FinancialInstrument;
import org.kapott.hbci.GV_Result.GVRWPDepotUms.Entry.FinancialInstrument.Transaction;
import org.kapott.hbci.manager.HBCIHandler;
import org.kapott.hbci.status.HBCIExecStatus;
import org.kapott.hbci.structures.TypedValue;

import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.jameica.hbci.PassportRegistry;
import de.willuhn.jameica.hbci.passport.Passport;
import de.willuhn.jameica.hbci.passport.PassportHandle;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.hbci.server.Converter;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

public class HBCIDepot extends BasisDepotAbruf {


	@Override
	public String getName() {
		return "HBCI";
	}

	@Override
	public void run(Konto konto) throws ApplicationException {

		try {
			Passport passport = PassportRegistry.findByClass(konto.getPassportClass());
			if (passport == null) {
				throw new ApplicationException("Kein HBCI-Sicherheitsmedium für das Konto gefunden");
			}
			PassportHandle handle = passport.getHandle();
			if (handle == null)
				throw new ApplicationException("Fehler beim Erzeugen der HBCI-Verbindung");

			HBCIHandler handler = handle.open();

			if (handler == null)
				throw new ApplicationException("Fehler beim Öffnen der HBCI-Verbindung");

			if (!(handler.isSupported("WPDepotList") || handler.isSupported("WPDepotUms"))) {
				throw new ApplicationException("Die Bank unterstützt keine Depots via HBCI!");
			}

			// Bestimmen, ob die Erzeugung der Umsätze aus den Bestandsveränderungen aktiviert werden soll
			boolean simulateOrders = Boolean.parseBoolean(konto.getMeta(UMSAETZEERGAENZEN, "false"))
					&& !handler.isSupported("WPDepotUms");

			// Bestand abrufen
			if (handler.isSupported("WPDepotList")) {
				List<GenericObjectSQL> lastBestand = null;
				if (simulateOrders) {
					lastBestand = SQLUtils.getResultSet("select * from depotviewer_bestand where kontoid = " + konto.getID(),
							"depotviewer_bestand", "id");
				}
				getDepotBestand(handler, konto);
				if (simulateOrders) {
					erzeugeUmsaetzeFuerBestandsdifferenz(konto, lastBestand);
				}

			}

			// Umsatz abrufen
			if (handler.isSupported("WPDepotUms")) {
				getDepotUmsaetze(handler, konto);
			}
			handler.close();
			handle.close();

		} catch (Exception e) {
			e.printStackTrace();
			throw new ApplicationException(e);
		}

	}

	private void getDepotBestand(HBCIHandler handler, Konto konto) throws ApplicationException {
		try {
			HBCIJob auszug = handler.newJob("WPDepotList");
			auszug.setParam("my", Converter.HibiscusKonto2HBCIKonto(konto));
			auszug.addToQueue();

			HBCIExecStatus ret=handler.execute();
			if (!ret.isOK()) {
				throw new ApplicationException(ret.getErrorString());
			}

			GVRWPDepotList result=(GVRWPDepotList)auszug.getJobResult();
			if (!result.isOK()) {
				throw new ApplicationException(result.getJobStatus().getErrorString());
			}
			if (result.getEntries().length > 1) {
				throw new ApplicationException("Zuviele Depots wurden zurückgeliefert");
			}


			Utils.clearBestand(konto);
			Entry depot = result.getEntries()[0];
			konto.setSaldo((depot.total != null) ? depot.total.getValue().doubleValue() : 0); // Bei der DKB ist depot.total == null, wenn das Depot leer ist
			konto.store();
			for (Gattung  g : depot.getEntries()) {
				Utils.addBestand(Utils.getORcreateWKN(g.wkn, g.isin, g.name), konto, g.saldo.getValue().doubleValue(), g.price.getValue().doubleValue(), 
						g.price.getCurr(), g.depotwert.getValue().doubleValue(),  g.depotwert.getCurr(), depot.timestamp, g.timestamp_price);
			}


		} catch (Exception e) {
			e.printStackTrace();
			throw new ApplicationException(e);
		}
	}


	private void getDepotUmsaetze(HBCIHandler handler, Konto konto) throws ApplicationException {
		try {

			HBCIJob auszug = handler.newJob("WPDepotUms");
			auszug.setParam("my", Converter.HibiscusKonto2HBCIKonto(konto));
			auszug.addToQueue();

			HBCIExecStatus ret=handler.execute();
			if (!ret.isOK()) {
				throw new ApplicationException(ret.getErrorString());
			}
			GVRWPDepotUms result =(GVRWPDepotUms) auszug.getJobResult();
			if (!result.isOK()) {
				throw new ApplicationException(result.getJobStatus().getErrorString());
			}
			parseDepotUmsatz(result, konto);
			Logger.warn("So weit so gut! Umsatz");

		} catch (Exception e) {
			throw new ApplicationException(e);
		}
	}

	protected void parseDepotUmsatz(GVRWPDepotUms ret, Konto konto) throws ApplicationException {
		if (ret.getEntries().length > 1) {
			throw new ApplicationException("Zuviele Depots wurden zurückgeliefert");
		}
		List<Transaction> unbekannte = new ArrayList<Transaction>(); 
		GVRWPDepotUms.Entry entries = ret.getEntries()[0];
		for (FinancialInstrument i : entries.instruments) {
			for (Transaction t : i.transactions) {
				// Einlage Betrag = null; transaction_indicator: 2: Kapitalmassnahme; richtung: 2 Erhalt; bezahlung 2: frei
				// Kauf Betrag = -9999, transaction_indicator: : 1: Settlement/Clearing; richtung: 2 Erhalt; bezahlung 2: frei
				// Verkauf Betrag = 9999, transaction_indicator :1: Settlement/Clearing; richtung 1: Lieferung bezahlung 2: frei

				if (t.bezahlung != Transaction.BEZAHLUNG_FREI
						|| t.anzahl.getType() != TypedValue.TYPE_STCK
						|| t.storno) {
					unbekannte.add(t);
					de.willuhn.logging.Logger.error("Unbekannte Transaktion. Bitte nehmen sie Kontakt zum Author auf.\n"
							+ t.toString());
					continue;
				}
				String aktion = "";
				if (t.transaction_indicator == Transaction.INDICATOR_CORPORATE_ACTION 
						&& t.richtung == Transaction.RICHTUNG_ERHALT) {
					aktion = "EINLAGE";
				} else if (t.transaction_indicator == Transaction.INDICATOR_SETTLEMENT_CLEARING 
						&& t.richtung == Transaction.RICHTUNG_ERHALT) {
					aktion = "KAUF";
				}  else if (t.transaction_indicator == Transaction.INDICATOR_SETTLEMENT_CLEARING 
						&& t.richtung == Transaction.RICHTUNG_LIEFERUNG) {
					aktion = "VERKAUF";
				} else {
					de.willuhn.logging.Logger.error("Unbekannte Transaktion. Bitte nehmen sie Kontakt zum Author auf.\n"
							+ t.toString());
					continue;
				}
				String orderid = i.wkn + i.isin + aktion + t.datum + t.anzahl + t.betrag; 
				try {
					String waehrung = "";
					double gesamtbetrag = 0.0d;
					double einzelbetrag = 0.0d;
					if (t.betrag != null) {
						gesamtbetrag = t.betrag.getValue().doubleValue();
						waehrung = t.betrag.getCurr();
						einzelbetrag = gesamtbetrag / t.anzahl.getValue().doubleValue(); 
					}
					Utils.addUmsatz(konto.getID(), 
							Utils.getORcreateWKN(i.wkn, i.isin, i.name), aktion,
							i.toString() + "\n" + t.toString(),
							t.anzahl.getValue().doubleValue(),
							einzelbetrag, waehrung,
							gesamtbetrag, waehrung,
							t.datum,
							String.valueOf(orderid.hashCode()),
							""
							);
				} catch (RemoteException e) {
					e.printStackTrace();
					throw new ApplicationException(e);
				}
			}

		}
		if (unbekannte.size() > 0) {
			Logger.warn("Unbekannte Transactionen");
			for (Transaction x : unbekannte) {
				Logger.warn(x.toString());
			}
			throw new ApplicationException("Es wurden Transactionen von einem unbekannten Typ gefunden.\nBitte kontaktieren sie den Autor (depotviewer@open4me.de) und senden sie ihm, falls es für sie akzeptabel ist, bitte das Logfiles (jameica.log) zu!");
		}
	}

	@Override
	public boolean isSupported(Konto konto) throws ApplicationException,
	RemoteException {
		return 	!isOffine(konto) && isBackendSelected(konto);
	}

	/**
	 * Ermittelt die Bestandsänderung zwischen dem vorherigen und dem aktuellen Bestand und 
	 * erzeugt hieraus Kauf und Verkauf Umsätze
	 * 
	 * @param konto Konto
	 * @param lastBestand alter Bestand
	 * @throws ApplicationException Fehler
	 */
	private void erzeugeUmsaetzeFuerBestandsdifferenz(Konto konto, List<GenericObjectSQL> lastBestand) throws ApplicationException {
		try {
			List<GenericObjectSQL> currentBestand = SQLUtils.getResultSet("select * from depotviewer_bestand where kontoid = " + konto.getID(),
					"depotviewer_bestand", "id");

			// Liste mit allen Wertpapier-ID erstellen
			ArrayList<Integer> wpids = new ArrayList<Integer>();
			for (GenericObjectSQL x : lastBestand) {
				if (!wpids.contains(x.getAttribute("wpid"))) {
					wpids.add((Integer) x.getAttribute("wpid"));
				}
			}
			for (GenericObjectSQL x : currentBestand) {
				if (!wpids.contains(x.getAttribute("wpid"))) {
					wpids.add((Integer) x.getAttribute("wpid"));
				}
			}

			// For jede Wertpaier-ID die Differenz bestimmen
			for (Integer wpid : wpids) {
				// Bestandsdaten zusammensuchen
				GenericObjectSQL lastdata = null;
				BigDecimal last = new BigDecimal("0");
				BigDecimal current = new BigDecimal("0");
				GenericObjectSQL currentdata = null;
				for (GenericObjectSQL x : lastBestand) {
					if (wpid.equals((Integer) x.getAttribute("wpid"))) {
						lastdata = x;
						last = (BigDecimal) x.getAttribute("anzahl");
					}
				}
				for (GenericObjectSQL x : currentBestand) {
					if (wpid.equals((Integer) x.getAttribute("wpid"))) {
						currentdata = x;
						current = (BigDecimal) x.getAttribute("anzahl");
					}
				}

				// Differenz zwischen beiden Beständen bilden
				BigDecimal diff = current.subtract(last);
				if (diff.compareTo(BigDecimal.ZERO) == 0) {
					continue;
				}

				// In Abhängigkeit davon, ob es ein Kauf oder Verkauf war, die Referenzdaten passen setzen 
				boolean isKauf = (diff.compareTo(BigDecimal.ZERO) > 0);
				GenericObjectSQL ref;
				if (isKauf) {
					ref = currentdata;
				} else {
					ref = lastdata;
				}

				// Umsatz hinzufügen
				Utils.addUmsatz(konto.getID(), 
						"" + wpid,
						(isKauf) ? "KAUF" : "VERKAUF",
								"",
								diff.abs().doubleValue(),
								((BigDecimal) ref.getAttribute("kurs")).doubleValue(),
								(String) ref.getAttribute("kursw"),
								(isKauf) ? ((BigDecimal) ref.getAttribute("wert")).negate().doubleValue() : ((BigDecimal) ref.getAttribute("wert")).doubleValue(),
										(String) ref.getAttribute("kursw"),
										(Date) ref.getAttribute("datum"),
										null, "aus Bestandsänderungen generiert");
			}
		} catch (Exception e) {
			throw new ApplicationException(e);
		}

	}

}

