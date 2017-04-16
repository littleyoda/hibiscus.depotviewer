package de.open4me.depot.abruf.impl;

import java.io.IOException;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import com.gargoylesoftware.htmlunit.TextPage;

import de.open4me.depot.DepotViewerPlugin;
import de.open4me.depot.abruf.utils.Utils;
import de.open4me.ly.webscraper.runner.Runner;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;

public class Fondsdepotbank extends BasisDepotAbruf {
	final static String logout = "https://finanzportal.fondsdepotbank.de/fdb/abaxx-?$part=Home.short-login-info&$event=logout"; 
	final static String[] basescript = new String[] { 
			"#Login", 
			"open \"https://finanzportal.fondsdepotbank.de/\"", 
			"set getbyid(\"Form2073790314_1_j_username\") to value \"${user}\"", 
			"set getbyid(\"Form2073790314_1_j_password\") to value \"${pwd}\"", 
			"click getbyxpath(\"//button[contains(@class,'evt-login')]\")",
			"assertExists \"Login nicht möglich. Zugangsdaten falsch?\" getbyxpath(\"//p[contains(.,'Zeit bis zur Abmeldung: ')]\")" + 
			"", 
			"#Main Page", 
			"open \"https://finanzportal.fondsdepotbank.de/fdb/abaxx-?$part=Home.content.Welcome\"",
			"",
			"#Umsätze", 
			"click getbytext(\"Depotumsätze\")",
			"click getbyxpath(\"//button[normalize-space(.)='Aktionen']\")",
			"download getbytext(\"CSV-Export\")",
			"assertExists \"CSV Export für Umsätze nicht gefunden\" getbytext(\"CSV-Export\")",
			"", 
			"#Back to Main Page", 
			"open \"https://finanzportal.fondsdepotbank.de/fdb/abaxx-?$part=Home.content.Welcome\"", 
			"", 
			"#Bestand", 
			"click getbytext(\"Depotbestand\")", 
			"click getbyxpath(\"//button[normalize-space(.)='Aktionen']\")",
			"download getbytext(\"CSV-Export\")", 
			"assertExists \"CSV Export für Umsätze nicht gefunden\" getbytext(\"CSV-Export\")",
			"", 
			"#Logout", 
			"click getbytext(\"Abmelden\")"};
	
	private final static I18N i18n = Application.getPluginLoader().getPlugin(DepotViewerPlugin.class).getResources().getI18N();

	final static String PROP_PASSWORD = "Passwort";


	public void run(Konto konto) throws ApplicationException {
		try {
			String username = konto.getKundennummer();
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
			
			HashMap<String, String> info = getKontoInformationen(konto);

			Runner r = new Runner() {

				
				
			};
			info.put("user", username);
			info.put("pwd", password);
			r.setInfo(info);
			r.setCode(basescript);
			r.run();
			umsaetze(konto, (TextPage) r.getDownloads().get(0));
			bestaende(konto, (TextPage) r.getDownloads().get(1));
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new ApplicationException("Fehler beim Abruf der Daten\n" 
					+ e.getMessage(), e);
		}
	}

	private void bestaende(Konto konto, TextPage page) throws IOException, RemoteException,
			ApplicationException, ParseException {
		DateFormat df;
		df = new SimpleDateFormat("dd.MM.yyyy");
		
		ArrayList<HashMap<String, String>> buchungen = parseCSV(page.getContent(), "Wertpapier");
		Utils.clearBestand(konto);
		double depotwert = 0.0;
		for (HashMap<String, String> buchung : buchungen) {
			String[] kurs = buchung.get("akt. preis").split(" ");
			String[] wert = buchung.get("akt. wert").split(" ");

			Utils.addBestand(
					Utils.getORcreateWKN(buchung.get("wkn"), buchung.get("isin"), buchung.get("produkt")), 
					konto, 
					Utils.getDoubleFromZahl(buchung.get("stück").replace(" Stück", "")),

					Utils.getDoubleFromZahl(kurs[0]), 
					kurs[1],

					Utils.getDoubleFromZahl(wert[0]), 
					wert[1],

					new Date(), 
					df.parse(buchung.get("datum")));
			depotwert += Utils.getDoubleFromZahl(wert[0]);
			
		}
		konto.setSaldo(depotwert);
		konto.store();
	}

	@SuppressWarnings("unchecked")
	private void umsaetze(Konto konto, TextPage page) throws ApplicationException, IOException,
			RemoteException {
		DateFormat df = new SimpleDateFormat("dd.MM.yyyy");

		ArrayList<HashMap<String, String>> buchungen = parseCSV(page.getContent(), "Buchung");
		for (HashMap<String, String> buchung : buchungen) {
			String[] wertpapier = buchung.get("wertpapier").split(" / ");
			String id = Utils.getORcreateWKN(wertpapier[1], wertpapier[0], wertpapier[2]);
			Date d;
			if (buchung.get("buchungsdatum") == null) {
				continue; // erstmal nicht behandeln
			}
			try {
				d = df.parse(buchung.get("buchungsdatum"));
			} catch (ParseException e) {
				Logger.error("Aktuelle Zeile: " + buchungen.toString());
				throw new ApplicationException("Unbekanntes Datumsformat: [" + buchung.get("buchungsdatum") + "] " + buchungen.toString());	
			} catch (NullPointerException e) {
				Logger.error("Aktuelle Zeile: " + buchungen.toString());
				throw new ApplicationException("Unbekanntes Datumsformat: [" + buchung.get("buchungsdatum") + "] " + buchungen.toString());	
			}
			if (buchung.get("geschäftsart").equals("Erträgnis")) {
				continue;
//				throw new ApplicationException("Unbekannte Geschäftsart (" + buchung.get("Geschäftsart") + "). Bitte den Entwickler kontaktieren!" );	
			}
			String[] kurs = buchung.get("kurs").split(" ");
			String[] umsatz = buchung.get("umsatz").split(" ");
			String orderid = wertpapier[0] + buchung.get("buchungsdatum") + buchung.get("geschäftsart")  
					         + buchung.get("umsatz") + buchung.get("kurz") + buchung.get("stück");  
			Utils.addUmsatz(konto.getID(), id, 
					buchung.get("geschäftsart"), 
					buchung.toString(),
					Math.abs(Utils.getDoubleFromZahl(buchung.get("stück").replace(" Stück", ""))), // Anzahl
					Utils.getDoubleFromZahl(kurs[0]), // Kurs
					kurs[1],
					-1 * Utils.getDoubleFromZahl(umsatz[0]), // Kosten 
					umsatz[1],
					d,
					String.valueOf(orderid.hashCode()), "",0.0d, "EUR", 0.0d, "EUR"
					);
			
		}
	}

	private ArrayList<HashMap<String, String>> parseCSV(String csv, String search) {
		ArrayList<HashMap<String, String>> liste = new ArrayList<HashMap<String, String>>();
		Scanner scanner = new Scanner(csv);
		String[] header = null;
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (header == null) {
				if (!line.startsWith(search)) {
					continue;
				}
				header = line.replace(";;", ";_;").split(";");
				String pre = "";
				int nr = 1;
				for (int i = 0; i < header.length; i++) {
//					System.out.print(header[i]);
					header[i] = header[i].toLowerCase();
					String orig = header[i];
					if (header[i].trim().equals("_") || header[i].trim().isEmpty()) {
						header[i] = pre + nr;
					} else {
						nr = 1;
					}
					pre = orig;
	//				System.out.println("  =>  " + header[i]);
				}
				continue;
			}
			HashMap<String, String> infos = new HashMap<String, String>();
			String[] data = line.split(";");
			for (int i = 0; i < data.length; i++) {
				infos.put(header[i], data[i]);
			}
			liste.add(infos);
		}
		scanner.close();
		return liste;

	}

	@Override
	public String getName() {
		return "Fondsdepot Bank";
	}

	@Override
	public List<String> getPROP(Konto konto) {
		List<String> result = super.getPROP(konto);
		result.add(0, PROP_PASSWORD);
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

		return 	konto.getBLZ().equals("77322200") 
				|| konto.getBic().toUpperCase().equals("FODBDE77XXX")
				|| getName().toLowerCase().replace(" ", "").equals(unterkontoExtract);
	}

	  public static HashMap<String, String> getKontoInformationen(Konto konto) throws RemoteException {
			HashMap<String, String> kontoInfo = new HashMap<String, String>();
			kontoInfo.put("blz", konto.getBLZ());
			kontoInfo.put("bic", konto.getBic());
			kontoInfo.put("iban", konto.getIban());
			kontoInfo.put("userid", konto.getKundennummer());
			kontoInfo.put("subid", konto.getUnterkonto());
			kontoInfo.put("nummer", konto.getKontonummer());
			return kontoInfo;
		  
	  }
	
}
