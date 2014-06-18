package de.open4me.depot.depotabruf;

import java.rmi.RemoteException;
import java.util.ArrayList;
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
			if (handler.isSupported("WPDepotList")) {
				getDepotBestand(handler, konto);
			}
			if (handler.isSupported("WPDepotUms")) {
				getDepotUmsaetze(handler, konto);
			}
			handler.close();
			handle.close();
			Logger.warn("So weit so gut!");

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
			Utils.clearBestand(konto);
			if (result.getEntries().length > 1) {
				throw new ApplicationException("Zuviele Depots wurden zurückgeliefert");
			}
			Entry depot = result.getEntries()[0];
			konto.setSaldo((depot.total != null) ? depot.total.getValue().doubleValue() : 0); // Bei der DKB ist depot.total == null, wenn das Depot leer ist
			konto.store();
			for (Gattung  g : depot.getEntries()) {
				Utils.addBestand(Utils.getORcreateWKN(g.wkn, g.isin, g.name), konto, g.saldo.getValue().doubleValue(), g.price.getValue().doubleValue(), 
						g.price.getCurr(), g.depotwert.getValue().doubleValue(),  g.depotwert.getCurr(), depot.timestamp);
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
			//
			//			GVRWPDepotList result=(GVRWPDepotList)auszug.getJobResult();
			//			Utils.clearBestand(konto);
			//
			////			double bestandswert = 0;
			////			for (HashMap<String, String> i : liste) {
			////				String[] bk = i.get("bewertungs­kurs").split(" ");
			////				Utils.addBestand(konto, Utils.getDoubleFromZahl(i.get("stück/nominale")), i.get("wkn"),
			////						Utils.getDoubleFromZahl(bk[0]), bk[1], 
			////						Utils.getDoubleFromZahl(i.get("stück/nominale"))*Utils.getDoubleFromZahl(bk[0]),
			////						bk[1], new Date());
			////				bestandswert += Utils.getDoubleFromZahl(i.get("stück/nominale"))*Utils.getDoubleFromZahl(bk[0]);
			////
			////			}
			//			if (!result.isOK()) {
			//				throw new ApplicationException(result.getJobStatus().getErrorString());
			//			}
			//			if (result.getEntries().length > 1) {
			//				throw new ApplicationException("Zuviele Depots wurden zurückgeliefert");
			//			}
			//			Entry depot = result.getEntries()[0];
			//			konto.setSaldo(depot.total.getValue().doubleValue());
			//			konto.store();
			//			for (Gattung  g : depot.getEntries()) {
			//				Utils.addBestand(Utils.getORcreateWKN(g.wkn, g.isin, g.name), konto, g.saldo.getValue().doubleValue(), g.price.getValue().doubleValue(), 
			//						g.price.getCurr(), g.depotwert.getValue().doubleValue(),  g.depotwert.getCurr(), depot.timestamp);
			//		}
			//
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

				t.betrag = null;
				t.transaction_indicator = Transaction.INDICATOR_KAPITALMASSNAHME;
				t.richtung = Transaction.RICHTUNG_ERHALT;

				if (t.bezahlung != Transaction.BEZAHLUNG_FREI
						|| t.anzahl.getType() != TypedValue.TYPE_STCK
						|| t.storno) {
					unbekannte.add(t);
					de.willuhn.logging.Logger.error("Unbekannte Transaktion. Bitte nehmen sie Kontakt zum Author auf.\n"
							+ t.toString());
					continue;
				}
				String aktion = "";
				if (t.transaction_indicator == Transaction.INDICATOR_KAPITALMASSNAHME 
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
							String.valueOf(orderid.hashCode())
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
}

