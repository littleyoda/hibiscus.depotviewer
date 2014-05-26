package de.open4me.depot.depotabruf;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.ThreadedRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;

import de.open4me.depot.DepotViewerPlugin;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;

public class CortalConsors implements BasisDepotAbruf {

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

			final WebClient webClient = new WebClient(BrowserVersion.INTERNET_EXPLORER_8);
			webClient.setCssErrorHandler(new SilentCssErrorHandler());
			webClient.setRefreshHandler(new ThreadedRefreshHandler());
			webClient.getOptions().setJavaScriptEnabled(false); 
			HtmlPage page = webClient.getPage("https://mobile.cortalconsors.de/euroWebDe/-?$part=sslm.login&s_requestedURL=https://mobile.cortalconsors.de/euroWebDe/-?$part=sslm.MobileDefault.depot&$event=orderInfoEntry");
			//		System.out.println(page.asText());
			List<HtmlForm> forms = (List<HtmlForm>) page.getByXPath( "//form[@id='login']");
			if (forms.size() != 1) {
				throw new ApplicationException("Konnte das Login-Formular nicht finden.");
			}
			HtmlForm form = forms.get(0);
			//		System.out.println(form.asXml());
			form.getInputByName("userId").setValueAttribute(username);
			form.getInputByName("nip").setValueAttribute(password);
			HtmlInput input = form.getInputByName("$$event_contentAreaCustomerLoginMobile");
			page = input.click();
			try {
				page = page.getAnchorByText("Orderinfo").click();
			} catch (com.gargoylesoftware.htmlunit.ElementNotFoundException e) {
				return;
			}

			HashMap<String, String> infos = new HashMap<String, String>();
			DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
			for (HtmlAnchor x : page.getAnchors()) {
				if (x.getAttribute("href").contains("orderInfoPageNumber")) {
					infos.clear();
					page =  x.click();
					DomNodeList<DomElement> list = page.getElementsByTagName("table");
					if (list.size() != 3) {
						//throw new ApplicationException("Anzahl der Tabelen in Orderinfo stimmt nicht. Ist: " + list.size());
					}
					tohash(infos, (HtmlTable) list.get(2), false);
					tohash(infos, (HtmlTable) list.get(3), true);
					String[] kurs = ((String) infos.get("kurs")).replaceAll("  *", " ").split(" ");

					Date d;
					try {
						d = df.parse(infos.get("order vom").substring(0,10));

						//						d = df.parse(infos.get("zeitpunkt der abrechnung").substring(0,10));
					} catch (ParseException e) {
						throw new ApplicationException("Unbekanntes Datumsformat beim Abrechnungszeitpunkt: " + infos.get("zeitpunkt der abrechnung"));
					}



					Utils.addUmsatz(infos.get("wkn"), infos.get("wertpapiername"), infos.get("orderart"), 
							infos.toString(),
							Utils.getDoubleFromZahl(infos.get("stück")),
							Utils.getDoubleFromZahl(kurs[0]), kurs[1],
							Math.rint(
									Utils.getDoubleFromZahl(kurs[0]) * Utils.getDoubleFromZahl(infos.get("stück")) * 100 / 100),
									kurs[1],
									d,
									infos.get("ordernummer")
							);
				}
			}

			infos = null;
			try {
				page = page.getAnchorByText("Depot").click();
			} catch (com.gargoylesoftware.htmlunit.ElementNotFoundException e) {
				return;
			}

			List<HtmlTable> tabs = (List<HtmlTable>) page.getByXPath( "//table[@class='transactions']");
			ArrayList<HashMap<String, String>> liste = tableRowsToHashs(tabs.get(0));
			Utils.clearBestand(konto);

			double bestandswert = 0;
			for (HashMap<String, String> i : liste) {
				String[] bk = i.get("bewertungs­kurs").split(" ");
				Utils.addBestand(konto, Utils.getDoubleFromZahl(i.get("stück/nominale")), i.get("wkn"),
						Utils.getDoubleFromZahl(bk[0]), bk[1], 
						Utils.getDoubleFromZahl(i.get("stück/nominale"))*Utils.getDoubleFromZahl(bk[0]),
						bk[1], new Date());
				bestandswert += Utils.getDoubleFromZahl(i.get("stück/nominale"))*Utils.getDoubleFromZahl(bk[0]);

			}
			konto.setSaldo(bestandswert);
			konto.store();



		} catch (IOException e) {
			throw new ApplicationException(e);
		}

	}

	private ArrayList<HashMap<String, String>> tableRowsToHashs(HtmlTable tab) {
		ArrayList<HashMap<String, String>> liste = new ArrayList<HashMap<String, String>>();
		List<HtmlTableRow> rows = tab.getRows();
		List<HtmlTableCell> headerRow = rows.get(0).getCells();
		for (int zeile = 1; zeile < rows.size(); zeile++) {
			HashMap<String, String> infos = new HashMap<String, String>(); 
			List<HtmlTableCell> r2 = rows.get(zeile).getCells();
			if (headerRow.size() != r2.size()) {
				System.out.println("Warnung. Ungültige Anzahl an Zellen: " + headerRow.toString());
				continue;
			}
			int missing=0;
			for (int i = 0; i < headerRow.size(); i++) {
				String header = headerRow.get(i).asText().toLowerCase();
				if ("".equals(header)) {
					header = "Missing" + missing;
					missing++;
				}

				infos.put(header, r2.get(i).asText());
			}
			liste.add(infos);
		}
		return liste;
	}

	private void tohash(HashMap<String, String> infos, HtmlTable tab,
			boolean b) {
		if (!b) {
			for (HtmlTableRow row :tab.getRows()) {
				List<HtmlTableCell> cells = row.getCells();
				if (cells.size() == 1 && row.asText().toLowerCase().contains("zurück")) {
					continue;
				}
				if (cells.size() != 2) {
					System.out.println("Warnung. Ungültige Anzahl an Zellen: " + cells.size() + " " + row.asText());
					System.out.println(cells.get(0).toString().toLowerCase());
					continue;
				}
				infos.put(cells.get(0).asText().toLowerCase(), cells.get(1).asText());
			}
			return;
		}
		List<HtmlTableRow> rows = tab.getRows();
		if (rows.size() < 2) {
			System.out.println("Warnung. Ungültige Anzahl an Zeilen: " + rows.toString());
			return;
		}
		List<HtmlTableCell> r1 = rows.get(0).getCells();
		for (int zeile = 1; zeile < rows.size(); zeile++) {
			List<HtmlTableCell> r2 = rows.get(zeile).getCells();
			if (r1.size() != r2.size()) {
				System.out.println("Warnung. Ungültige Anzahl an Zellen: " + r1.toString());
				continue;
			}
			int missing=0;
			for (int i = 0; i < r1.size(); i++) {
				String header = r1.get(i).asText().toLowerCase();
				if ("".equals(header)) {
					header = "Missing" + missing;
					missing++;
				}
				infos.put(header, r2.get(i).asText());
			}
		}

	}


	@Override
	public String getName() {
		return "Cortal Consors";
	}

	@Override
	public List<String> getPROP() {
		List<String> result = new ArrayList<String>();
		result.add(PROP_PASSWORD);
		return result;
	}


}