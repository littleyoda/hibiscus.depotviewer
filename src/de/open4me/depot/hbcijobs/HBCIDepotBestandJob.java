package de.open4me.depot.hbcijobs;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.kapott.hbci.GV_Result.GVRWPDepotList;
import org.kapott.hbci.GV_Result.GVRWPDepotList.Entry;
import org.kapott.hbci.GV_Result.GVRWPDepotList.Entry.Gattung;

import de.open4me.depot.abruf.impl.BasisDepotAbruf;
import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.datenobj.DepotAktion;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.jameica.hbci.HBCIProperties;
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
			throw new ApplicationException("Zuviele Depots wurden zurückgeliefert");
		}

		List<GenericObjectSQL> lastBestand = null;
		if (simulateOrders) {
			lastBestand = SQLUtils.getResultSet("select * from depotviewer_bestand where kontoid = " + konto.getID(),
					"depotviewer_bestand", "id");
		}



		Utils.clearBestand(konto);
		Entry depot = result.getEntries()[0];
		konto.setSaldo((depot.total != null) ? depot.total.getValue().doubleValue() : 0); // Bei der DKB ist depot.total == null, wenn das Depot leer ist
		konto.store();
		Utils.clearBestand(konto);
		for (Gattung  g : depot.getEntries()) {
			Utils.addBestand(Utils.getORcreateWKN(g.wkn, g.isin, g.name), konto, g.saldo.getValue().doubleValue(), g.price.getValue().doubleValue(), 
					g.price.getCurr(), g.depotwert.getValue().doubleValue(),  g.depotwert.getCurr(), depot.timestamp, g.timestamp_price);
		}
		if (simulateOrders) {
			erzeugeUmsaetzeFuerBestandsdifferenz(konto, lastBestand);
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
						(isKauf) ? DepotAktion.KAUF.internal() : DepotAktion.VERKAUF.internal(),
								"",
								diff.abs().doubleValue(),
								((BigDecimal) ref.getAttribute("kurs")).doubleValue(),
								(String) ref.getAttribute("kursw"),
								(isKauf) ? ((BigDecimal) ref.getAttribute("wert")).negate().doubleValue() : ((BigDecimal) ref.getAttribute("wert")).doubleValue(),
										(String) ref.getAttribute("kursw"),
										(Date) ref.getAttribute("datum"),
										null, "aus Bestandsänderungen generiert"
										,0.0d, "EUR", 0.0d, "EUR");
			}
		} catch (Exception e) {
			throw new ApplicationException(e);
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

}
