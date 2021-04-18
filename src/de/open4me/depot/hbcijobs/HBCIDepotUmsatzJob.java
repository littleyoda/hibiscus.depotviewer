package de.open4me.depot.hbcijobs;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.kapott.hbci.GV_Result.GVRWPDepotUms;
import org.kapott.hbci.GV_Result.GVRWPDepotUms.Entry;
import org.kapott.hbci.GV_Result.GVRWPDepotUms.Entry.FinancialInstrument;
import org.kapott.hbci.GV_Result.GVRWPDepotUms.Entry.FinancialInstrument.Transaction;
import org.kapott.hbci.structures.TypedValue;

import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.datenobj.DepotAktion;
import de.open4me.depot.datenobj.rmi.Umsatz;
import de.open4me.depot.tools.UmsatzHelper;
import de.willuhn.jameica.hbci.HBCIProperties;
import de.willuhn.jameica.hbci.rmi.HibiscusDBObject;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.hbci.rmi.Protokoll;
import de.willuhn.jameica.hbci.server.Converter;
import de.willuhn.jameica.hbci.server.hbci.AbstractHBCIJob;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

/**
 * Job fuer "Umsatz-Abfrage".
 */
public class HBCIDepotUmsatzJob extends AbstractHBCIJob
{
	//	private final static Settings settings = Application.getPluginLoader().getPlugin(HBCI.class).getResources().getSettings();

	private Konto konto     = null;

	/**
	 * ct.
	 * @param konto Konto, fuer das die Umsaetze abgerufen werden sollen.
	 * @throws ApplicationException
	 * @throws RemoteException
	 */
	public HBCIDepotUmsatzJob(Konto konto) throws ApplicationException, RemoteException
	{
		try
		{
			//		PluginResources res = Application.getPluginLoader().getPlugin(HBCI.class).getResources();
			if (konto == null)
				throw new ApplicationException("Bitte wählen Sie ein Konto aus"); 

			if (konto.isNewObject())
				konto.store();

			this.konto = konto;

			String curr = konto.getWaehrung();
			if (curr == null || curr.length() == 0)
				konto.setWaehrung(HBCIProperties.CURRENCY_DEFAULT_DE);
			setJobParam("my",Converter.HibiscusKonto2HBCIKonto(konto));

		}
		catch (RemoteException e)
		{
			throw e;
		}
		catch (ApplicationException e2)
		{
			throw e2;
		}
		catch (Throwable t)
		{
			Logger.error("error while executing job " + getIdentifier(),t);
			throw new ApplicationException(i18n.tr("Fehler beim Erstellen des Auftrags. Fehlermeldung: {0}",t.getMessage()),t);
		}
	}

	/**
	 * @see de.willuhn.jameica.hbci.server.hbci.AbstractHBCIJob#getIdentifier()
	 */
	public String getIdentifier() {
		return "WPDepotUms";
	}

	/**
	 * @see de.willuhn.jameica.hbci.server.hbci.AbstractHBCIJob#getName()
	 */
	public String getName() throws RemoteException
	{
		return "DepotUmsatzabruf " + konto.getLongName();
	}

	/**
	 * @see de.willuhn.jameica.hbci.server.hbci.AbstractHBCIJob#markExecuted()
	 */
	protected void markExecuted() throws RemoteException, ApplicationException
	{

		try {
			GVRWPDepotUms result =(GVRWPDepotUms) getJobResult();
			if (!result.isOK()) {
				throw new ApplicationException(result.getJobStatus().getErrorString());
			}

			if (result.getEntries().length > 1) {
				String out = "";
				for (int idx = 0; idx < result.getEntries().length; idx++) {
					Entry depot = result.getEntries()[idx];
					if (depot.depot != null && depot.depot.iban != null) {
						out = out + " " + depot.depot.iban;
					} else {
						out = out + " NULL";
					}
				}
				Logger.error("Folgende Depots wurden zurückgeliefert:" + out);
				throw new ApplicationException("Zuviele Depots wurden zurückgeliefert (Umsatz)");
			}
			parseDepotUmsatz(result, konto);

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
				// Auslieferung Betrag = null; transaction_indicator: 2: Corporate Action; richtung: 1 Lieferung; bezahlung 2: frei
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
					aktion = DepotAktion.EINBUCHUNG.internal();
				} else if (t.transaction_indicator == Transaction.INDICATOR_CORPORATE_ACTION 
						&& t.richtung == Transaction.RICHTUNG_LIEFERUNG) {
					aktion = DepotAktion.AUSBUCHUNG.internal();
				} else if (t.transaction_indicator == Transaction.INDICATOR_SETTLEMENT_CLEARING 
						&& t.richtung == Transaction.RICHTUNG_ERHALT) {
					aktion = DepotAktion.KAUF.internal();
				}  else if (t.transaction_indicator == Transaction.INDICATOR_SETTLEMENT_CLEARING 
						&& t.richtung == Transaction.RICHTUNG_LIEFERUNG) {
					aktion = DepotAktion.VERKAUF.internal();
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
						if ("BIWBDE33XXX".equals(konto.getBic()) || "10130800".equals(konto.getBLZ()))  {
							// Hack für FlatEx
							gesamtbetrag = -gesamtbetrag;
						}
						waehrung = t.betrag.getCurr();
						einzelbetrag = Math.abs(gesamtbetrag) / t.anzahl.getValue().doubleValue();
					}
					Umsatz u = Utils.addUmsatz(konto.getID(), 
							Utils.getORcreateWKN(i.wkn, i.isin, i.name), aktion,
							i.toString() + "\n" + t.toString(),
							t.anzahl.getValue().doubleValue(),
							einzelbetrag, waehrung,
							gesamtbetrag, waehrung,
							t.datum,
							String.valueOf(orderid.hashCode()),
							"",0.0d, "EUR", 0.0d, "EUR"
							);
					UmsatzHelper.storeUmsatzInHibiscus(u);
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

	/**
	 * @see de.willuhn.jameica.hbci.server.hbci.AbstractHBCIJob#markFailed(java.lang.String)
	 */
	protected String markFailed(String error) throws RemoteException, ApplicationException
	{
		String msg = i18n.tr("Fehler beim Abrufen der Umsätze: {0}",error);
		konto.addToProtokoll(msg,Protokoll.TYP_ERROR);
		return msg;
	}

	@Override
	protected HibiscusDBObject getContext() {
		return null;
	}


}
