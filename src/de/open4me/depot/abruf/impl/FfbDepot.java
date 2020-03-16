package de.open4me.depot.abruf.impl;

import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import de.bmarwell.ffb.depot.client.FfbMobileClient;
import de.bmarwell.ffb.depot.client.json.FfbDepotInfo;
import de.bmarwell.ffb.depot.client.json.FfbFondsbestand;
import de.bmarwell.ffb.depot.client.json.FfbUmsatz;
import de.bmarwell.ffb.depot.client.json.MyFfbResponse;
import de.bmarwell.ffb.depot.client.value.FfbAuftragsTyp;
import de.bmarwell.ffb.depot.client.value.FfbDepotNummer;
import de.bmarwell.ffb.depot.client.value.FfbLoginKennung;
import de.bmarwell.ffb.depot.client.value.FfbPin;
import de.open4me.depot.DepotViewerPlugin;
import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.datenobj.DepotAktion;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;

public class FfbDepot extends BasisDepotAbruf {

	final static String PROP_PASSWORD = "Passwort";
	private final static I18N i18n = Application.getPluginLoader().getPlugin(DepotViewerPlugin.class).getResources().getI18N();

	@Override
	public String getName() {
		return "FFB (FIL Fondsbank GmbH)";
	}

	public void run(Konto konto) throws ApplicationException {
		String username;
		try {
			username = konto.getKundennummer();
			String depotnummer = konto.getKontonummer();
			String password = konto.getMeta(PROP_PASSWORD, null);
			if (username == null || username.length() == 0) {
				throw new ApplicationException(i18n.tr("Bitte geben Sie Ihren Karten-Nummer in den Synchronisationsoptionen ein"));
			}

			try {
				if (password == null || password.length() == 0) {
					password = Application.getCallback().askPassword(getName());
				}
			} catch (Exception e1) {
				e1.printStackTrace();
				throw new ApplicationException("Password-Eingabe:" + e1.getMessage());
			}

			System.out.println("FFB Runed");
			FfbLoginKennung LOGIN = FfbLoginKennung.of(username);
			FfbPin PIN = FfbPin.of(password);
			FfbDepotNummer depotNummer = FfbDepotNummer.of(depotnummer); // Login ohne -01.

			FfbMobileClient mobileAgent = new FfbMobileClient(LOGIN, PIN);
			mobileAgent.logon();

			// Log Depots
			MyFfbResponse accountData = mobileAgent.fetchAccountData();
			for (FfbDepotInfo x: accountData.getDepots()) {
				Logger.info("Depot " + x.getDepotname() + " " + x.getDepotNummer() + " gefunden!");
			}
			
			// Bestand
		    FfbDepotInfo[] depotData = accountData.getDepots().stream().filter(depot -> depotNummer.equals(depot.getDepotNummer())).toArray(FfbDepotInfo[]::new);
		    if (depotData.length != 1) {
				throw new ApplicationException(i18n.tr("Probleme bei der Identifizierung des Accounts [" + depotData.length + "]"));
		    }
			Utils.clearBestand(konto);
			Logger.info("Bestände: " + depotData[0].getFondsbestaende().size());
			double depotwert = 0.0;
		    for (FfbFondsbestand x : depotData[0].getFondsbestaende()) {
		    	process(x, konto);
				depotwert += x.getBestandWertInEuro().doubleValue();
		    }
			
		    // Umsätze
			FfbUmsatz[] umsaetze = mobileAgent
									.getUmsaetze(FfbAuftragsTyp.ALLE, LocalDate.now().minusMonths(5).minusDays(0), LocalDate.now())
									.getUmsaetze()
									.stream().filter(p -> p.getDepotnummer().equals(depotnummer))
									.toArray(FfbUmsatz[]::new);
			Logger.info("Bestände: " + depotData[0].getFondsbestaende().size());
			for (FfbUmsatz x: umsaetze) {
				process(x, konto);
			}
			

			konto.setSaldo(depotwert);
			konto.store();


		} catch (RemoteException e) {
			throw new ApplicationException("Fehler beim Abruf der Daten\n" 
					+ e.getMessage(), e);
		}

	}

	private void process(FfbFondsbestand x, Konto konto) throws RemoteException, ApplicationException {
			Utils.addBestand(
					Utils.getORcreateWKN(x.getWkn(), x.getIsin(), x.getFondsname()), 
					konto, 
					x.getBestandStueckzahl().doubleValue(),
					x.getRuecknahmePreis().doubleValue(),
					x.getFondswaehrung(),
					
					x.getBestandWertInEuro().doubleValue(),
					"EUR",
					new Date(), 
					Date.from(x.getPreisDatum().atStartOfDay(ZoneId.systemDefault()).toInstant())
					);
	}

	
	
	public void process(FfbUmsatz x, Konto konto) throws RemoteException, ApplicationException {
		DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
		String id = Utils.getORcreateWKN(x.getWkn(), x.getIsin(), x.getFondsname());
		DepotAktion a = Utils.checkTransaktionsBezeichnung(x.getTransaktionArt());
		if (a == null) {
			Logger.error("Unbekannte Buchungsart: " + x.getTransaktionArt());;
			return;
		}
		Double kosten = Utils.getDoubleFromZahl(x.getAbrechnungBetragInEuro());
		if (a.equals(DepotAktion.KAUF)) {
			kosten = -kosten; 
		}
		Date d;
		try {
			d = df.parse(x.getBuchungDatum());
		} catch (ParseException e) {
			throw new ApplicationException("Unbekanntes Datumsformat: [" + x.getBuchungDatum() + "]");	
		}

		Utils.addUmsatz(
				konto.getID(),/// kontoid, 
				id, // wpid, 
				x.getTransaktionArt(), // aktion
				"info", // info, 
				Utils.getDoubleFromZahl(x.getAbrechnungAnteile()), // anzahl, 
				Utils.getDoubleFromZahl(x.getAbrechnungpreis()),// kurs, 
				"EUR", // kursW,
				kosten,
				"EUR", // kostenW, 
				d,//Date date, 
				null, // orderid, 
				"", // kommentar,
				0.0, // gebuehren, 
				"EUR",// gebuehrenW, 
				0.0,// steuern, 
				"EUR"// steuernW);
				);
	}
	
	@Override
	public List<String> getPROP(Konto konto) {
		List<String> result = super.getPROP(konto);
		result.add(0, PROP_PASSWORD + "(pwd)");
		return result;
	}

	@Override
	public boolean isSupported(Konto konto) throws ApplicationException, RemoteException {
		if (!Utils.hasRightKontoType(konto)) {
			return false;
		}
		String unterkontoExtract = "";
		if (konto.getUnterkonto() != null && konto.getUnterkonto().toLowerCase().startsWith("depot")) {
			unterkontoExtract = konto.getUnterkonto().toLowerCase().substring(5).replace(" ", ""); 
		}

		return 	konto.getBLZ().equals("XXX") 
				|| konto.getBic().toUpperCase().equals("XXX")
				|| getName().toLowerCase().replace(" ", "").equals(unterkontoExtract)
				|| "ffb".toLowerCase().replace(" ", "").equals(unterkontoExtract);
	}

}
