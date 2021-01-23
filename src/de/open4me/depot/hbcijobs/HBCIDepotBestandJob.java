package de.open4me.depot.hbcijobs;

import java.math.BigDecimal;
import java.rmi.RemoteException;

import org.kapott.hbci.GV_Result.GVRWPDepotList;
import org.kapott.hbci.GV_Result.GVRWPDepotList.Entry;
import org.kapott.hbci.GV_Result.GVRWPDepotList.Entry.Gattung;
import org.kapott.hbci.GV_Result.GVRWPDepotList.Entry.Gattung.SubSaldo;

import de.open4me.depot.abruf.impl.BasisDepotAbruf;
import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.tools.UmsatzeAusBestandsAenderung;
import de.willuhn.jameica.hbci.HBCIProperties;
import de.willuhn.jameica.hbci.rmi.HibiscusDBObject;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.hbci.rmi.Protokoll;
import de.willuhn.jameica.hbci.server.Converter;
import de.willuhn.jameica.hbci.server.hbci.AbstractHBCIJob;
import de.willuhn.jameica.hbci.synchronize.SynchronizeSession;
import de.willuhn.jameica.hbci.synchronize.hbci.HBCISynchronizeBackend;
import de.willuhn.jameica.services.BeanService;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.ProgressMonitor;

/**
 * Job fuer "Umsatz-Abfrage".
 */
public class HBCIDepotBestandJob extends AbstractHBCIJob
{
	//	private final static Settings settings = Application.getPluginLoader().getPlugin(HBCI.class).getResources().getSettings();

	private Konto konto     = null;
	private boolean simulateOrders;
	private BasisDepotAbruf abruf;

	/**
	 * ct.
	 * @param konto Konto, fuer das die Umsaetze abgerufen werden sollen.
	 * @param simulateOrders 
	 * @param x 
	 * @throws ApplicationException
	 * @throws RemoteException
	 */
	public HBCIDepotBestandJob(Konto konto, boolean simulateOrders, BasisDepotAbruf x) throws ApplicationException, RemoteException
	{
		this.simulateOrders = simulateOrders;
		this.abruf = x;
		try
		{
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
		return "WPDepotList";
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
		GVRWPDepotList result=(GVRWPDepotList) getJobResult();
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
			throw new ApplicationException("Zuviele Depots wurden zurückgeliefert (Besand)");
		}

		UmsatzeAusBestandsAenderung umsaetzeAusBestaenden = null;
		
		if (simulateOrders) {
			umsaetzeAusBestaenden = new UmsatzeAusBestandsAenderung(konto);
		}



		Utils.clearBestand(konto);
		Entry depot = result.getEntries()[0];
		konto.setSaldo((depot.total != null) ? depot.total.getValue().doubleValue() : 0); // Bei der DKB ist depot.total == null, wenn das Depot leer ist
		konto.store();
		Utils.clearBestand(konto);
		for (Gattung  g : depot.getEntries()) {
			if (g == null) {
				Logger.error("Null Entry in depot.getEntries");
				continue;
			}
			if (g.saldo == null || g.price == null || g.depotwert == null) {
				Logger.error("Eintrag ohne Saldo oder Wert. Saldo: " + g.saldo_type + " " + g.saldo + " " + "Wert: " + g.depotwert + " Price: " + g.pricetype + " " + g.pricequalifier + " " + g.price);
				continue;
			}
			BigDecimal anzahl = g.saldo.getValue();
			if(g.getEntries().length == 1) {
				SubSaldo sub = g.getEntries()[0];
				if("TAVI".equals(sub.qualifier)) { // TAVI = Total Available. Dies ist die eigentlich verfügbare Anzahl, falls g.saldo gerundet angegeben ist, zB. bei Depots der DKB 
					anzahl = sub.saldo.getValue();
				}
			}
			Utils.addBestand(Utils.getORcreateWKN(g.wkn, g.isin, g.name), konto, anzahl.doubleValue(), g.price.getValue().doubleValue(), 
					g.price.getCurr(), g.depotwert.getValue().doubleValue(),  g.depotwert.getCurr(), depot.timestamp, g.timestamp_price);
		}
		if (simulateOrders) {
			umsaetzeAusBestaenden.erzeugeUmsaetze();
		}
		Logger.info("umsatz list fetched successfully");
		if (abruf != null) {
			BeanService service = Application.getBootLoader().getBootable(BeanService.class);
			SynchronizeSession session = service.get(HBCISynchronizeBackend.class).getCurrentSession();
			// Loggen
			ProgressMonitor monitor = session.getProgressMonitor();
			monitor.setPercentComplete(10);
			monitor.log("Umsatzabruf gestartet");
			abruf.run(konto);
			monitor.log("Umsatzabruf beendet");
		}
	}


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
