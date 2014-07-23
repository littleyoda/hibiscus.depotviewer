package de.open4me.depot.depotabruf;

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

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.ThreadedRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

import de.open4me.depot.DepotViewerPlugin;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;

public class Fondsdepotbank extends BasisDepotAbruf {

	private final static I18N i18n = Application.getPluginLoader().getPlugin(DepotViewerPlugin.class).getResources().getI18N();

	final static String PROP_PASSWORD = "Passwort";


	public void run(Konto konto) throws ApplicationException {
		ArrayList<String> seiten = new ArrayList<String>(); 
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
			
			
			final WebClient webClient = new WebClient(BrowserVersion.INTERNET_EXPLORER_8);
			webClient.setCssErrorHandler(new SilentCssErrorHandler());
			webClient.setRefreshHandler(new ThreadedRefreshHandler());
			HtmlPage page = webClient.getPage("https://banking.fondsdepotbank.de/i3/fodb/public/login_init.do");
			seiten.add(page.asXml());
			List<HtmlForm> forms = (List<HtmlForm>) page.getByXPath( "//form[@name='loginForm']");
			if (forms.size() != 1) {
				throw new ApplicationException("Konnte das Login-Formular nicht finden.");
			}
			HtmlForm form = forms.get(0);
			((HtmlTextInput) form.getInputByName("authentificationNo")).setValueAttribute(username);
			((HtmlPasswordInput) form.getInputByName("pin")).setValueAttribute(password);
			page = form.getInputByValue("Anmelden").click();
			seiten.add(page.asXml());
			try {
				page = page.getAnchorByText("Umsätze").click();
				seiten.add(page.asXml());
			} catch (ElementNotFoundException e) {
				throw new ApplicationException("Login fehlgeschlagen! Falsches Password?");
			}

			// Abfrage der Umsätze
			forms = (List<HtmlForm>) page.getByXPath( "//form[@name='transactionsForm']");
			if (forms.size() != 1) {
				throw new ApplicationException("Konnte das Umsätze-Formular nicht finden.");
			}
			form = forms.get(0);

			HtmlCheckBoxInput cb = form.getInputByName("showAllTransactions");
			cb.setChecked(true);
			page = form.getInputByValue("Suchen").click();
			seiten.add(page.asXml());
			//		System.out.println(page.asText());
			//	System.out.println(page.asXml());
			TextPage text = page.getAnchorByHref("/i3/fodb/transaction_list_export_csv.do").click();
			//System.out.println();

			Scanner scanner = new Scanner(text.getContent());
			String[] header = null;
			DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
			HashMap<String, String> infos = new HashMap<String, String>();
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (header == null) {
					if (!line.startsWith("Abrechnung")) {
						continue;
					}
					header = (line + "_").replace(";;", ";_;").split(";");
					continue;
				}
				infos.clear();
				String[] data = line.split(";");
				String pre = "";
				for (int i = 0; i < data.length; i++) {
					String headername = header[i].toLowerCase();
					if (headername.equals("_")) {
						headername = pre + "2";
					}
					infos.put(headername, data[i]);
					pre = header[i].toLowerCase();
				}

				String orderid = infos.get("wkn") + infos.get("transaktion") + infos.get("ausführungspreis") + infos.get("umsatz") + infos.get("abrechnung") + infos.get("stücke");  
				Date d;
				try {
					d = df.parse(infos.get("ausführungsdatum").substring(0,10));
				} catch (ParseException e) {
					throw new ApplicationException("Unbekanntes Datumsformat: " + infos.get("zeitpunkt der abrechnung"));	
				}
				Utils.addUmsatz(konto.getID(), Utils.getORcreateWKN(infos.get("wkn"), "", infos.get("fondsname")), infos.get("transaktion"), 
						infos.toString(),
						Utils.getDoubleFromZahl(infos.get("stücke")),
						Utils.getDoubleFromZahl(infos.get("ausführungspreis")), infos.get("ausführungspreis2"),
						((infos.get("transaktion").toUpperCase().equals("KAUF")) ? -1 : 1) * Utils.getDoubleFromZahl(infos.get("umsatz")), infos.get("umsatz2"),
						d,
						String.valueOf(orderid.hashCode()), ""
						);



			}
			scanner.close();

			// Depot Bestand
			page = page.getAnchorByText("Depotübersicht").click();
			seiten.add(page.asText());
			text = page.getAnchorByHref("/i3/fodb/sec_account_asset_overview_export_csv.do").click();
			ArrayList<HashMap<String, String>> x = parseCSV(text.getContent(), "Bestand");
			Utils.clearBestand(konto);
			double depotwert = 0.0;
			for (HashMap<String, String> i : x) {
				Utils.addBestand(
						Utils.getORcreateWKN(i.get("wkn"), "", ""), konto, Utils.getDoubleFromZahl(i.get("bestand")),
						Utils.getDoubleFromZahl(i.get("akt. preis")), 
						i.get("akt. preis1"), 
						Utils.getDoubleFromZahl(i.get("akt. wert")),
						i.get("akt. preis1"), new Date(), df.parse(i.get("preisdatum")));
				depotwert += Utils.getDoubleFromZahl(i.get("akt. wert"));
			}
			konto.setSaldo(depotwert);
			konto.store();

		} catch (IOException e) {
			e.printStackTrace();
			throw new ApplicationException("Fehler beim Abruf der Daten", e);
		} catch (ParseException e) {
			e.printStackTrace();
			throw new ApplicationException("Ungültiges Datum", e);
		}finally{
			try {
				debug(seiten, konto);
			} catch (RemoteException e) {
				throw new ApplicationException(e);
			}
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
				header = (line + "_").replace(";;", ";_;").split(";");
				String pre = "";
				int nr = 1;
				for (int i = 0; i < header.length; i++) {
					System.out.print(header[i]);
					header[i] = header[i].toLowerCase();
					String orig = header[i];
					if (header[i].trim().equals("_") || header[i].trim().isEmpty()) {
						header[i] = pre + nr;
					} else {
						nr = 1;
					}
					pre = orig;
					System.out.println("  =>  " + header[i]);
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
	public List<String> getPROP() {
		List<String> result = super.getPROP();
		result.add(0, PROP_PASSWORD);
		return result;
	}

	@Override
	public boolean isSupported(Konto konto) throws ApplicationException, RemoteException {
		String unterkontoExtract = "";
		if (konto.getUnterkonto() != null && konto.getUnterkonto().toLowerCase().startsWith("depot")) {
			unterkontoExtract = konto.getUnterkonto().toLowerCase().substring(5).replace(" ", ""); 
		}

		return 	konto.getBLZ().equals("77322200") 
				|| konto.getBic().toUpperCase().equals("FODBDE77XXX")
				|| getName().toLowerCase().replace(" ", "").equals(unterkontoExtract);
	}

}
