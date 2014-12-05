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

import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.ThreadedRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;

import de.open4me.depot.DepotViewerPlugin;
import de.open4me.depot.abruf.utils.Utils;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;

public class CortalConsorsMitHBCI extends BasisHBCIDepotAbruf {

	private final static I18N i18n = Application.getPluginLoader().getPlugin(DepotViewerPlugin.class).getResources().getI18N();
	final static String PROP_KUNDENNUMMER = "Kundennummer (Webseite)";
	final static String PROP_PASSWORD = "Passwort (Webseite)";

	@Override
	public String getName() {
		return "HBCI CortalConsors";
	}

	@Override
	public void run(Konto konto) throws ApplicationException {
		super.run(konto); // Bestand via HBCI
		runUmsaetze(konto);

	}

	@SuppressWarnings("unchecked")
	public void runUmsaetze(Konto konto) throws ApplicationException {
		ArrayList<String> seiten = new ArrayList<String>(); 
		try {
			String username = konto.getMeta(PROP_KUNDENNUMMER, null);
			if (username == null || username.length() == 0) {
				throw new ApplicationException(i18n.tr("Bitte geben ihre Kundenummer in den Synchronisationsoptionen ein"));
			}
			String password = konto.getMeta(PROP_PASSWORD, null);

			try {
				if (password == null || password.length() == 0) {
					password = Application.getCallback().askPassword(getName());
				}
			} catch (Exception e1) {
				e1.printStackTrace();
				throw new ApplicationException("Password-Eingabe:" + e1.getMessage());
			}

			final WebClient webClient = new WebClient();
			webClient.setCssErrorHandler(new SilentCssErrorHandler());
			webClient.setRefreshHandler(new ThreadedRefreshHandler());
			webClient.getOptions().setJavaScriptEnabled(false); 
			HtmlPage page = webClient.getPage("https://mobile.cortalconsors.de/euroWebDe/-?$part=sslm.login&s_requestedURL=https://mobile.cortalconsors.de/euroWebDe/-?$part=sslm.MobileDefault.depot&$event=orderInfoEntry");
			seiten.add(page.asXml());
			List<HtmlForm> forms = (List<HtmlForm>) page.getByXPath("//form[@id='login']");
			if (forms.size() != 1) {
				Utils.report((List<? extends HtmlElement>) page.getByXPath("//form"));
				throw new ApplicationException("Konnte das Login-Formular nicht finden.");
			}
			HtmlForm form = forms.get(0);
			form.getInputByName("userId").setValueAttribute(username);
			form.getInputByName("nip").setValueAttribute(password);
			HtmlInput input = form.getInputByName("$$event_contentAreaCustomerLoginMobile");
			page = input.click();
			seiten.add(page.asXml());

			// Nach dem Link für das passende Depot suchen. Wichtig, wenn man mehr als ein depot hat
			HtmlAnchor link = null;
			for (HtmlAnchor x : page.getAnchors()) {
				if (x.asText().equals("" + konto.getKontonummer())) {
					link = x;
				}
			}
			if (link == null) {
				throw new ApplicationException("Link zum Konto " + konto.getKontonummer() + " nicht gefunden!");
			}
			page = link.click();
			seiten.add(page.asXml());

			try {
				page = page.getAnchorByText("Orderinfo").click();
			} catch (com.gargoylesoftware.htmlunit.ElementNotFoundException e) {
				Utils.report(page.getAnchors());
				throw new ApplicationException("Orderinfo nicht gefunden!");
			}

			HashMap<String, String> infos = new HashMap<String, String>();
			DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
			boolean missingOrderDate = false;
			for (HtmlAnchor x : page.getAnchors()) {
				if (x.getAttribute("href").contains("orderInfoPageNumber")) {
					infos.clear();
					page =  x.click();
					seiten.add(page.asXml());
					DomNodeList<DomElement> list = page.getElementsByTagName("table");
					if (list.size() != 3) {
						//throw new ApplicationException("Anzahl der Tabelen in Orderinfo stimmt nicht. Ist: " + list.size());
					}
					tohash(infos, (HtmlTable) list.get(2), false);
					tohash(infos, (HtmlTable) list.get(3), true);
					String[] kurs = ((String) infos.get("kurs")).replaceAll("  *", " ").split(" ");
					if (!infos.containsKey("order vom")) {
						missingOrderDate = true;
						continue;
					}
					Date d;
					try {
						d = df.parse(infos.get("order vom").substring(0,10));

						//						d = df.parse(infos.get("zeitpunkt der abrechnung").substring(0,10));
					} catch (ParseException e) {
						throw new ApplicationException("Unbekanntes Datumsformat beim Abrechnungszeitpunkt: " + infos.get("zeitpunkt der abrechnung"));
					}


					Utils.addUmsatz(konto.getID(), Utils.getORcreateWKN(infos.get("wkn"), "", infos.get("wertpapiername")), infos.get("orderart"), 
							infos.toString(),
							Utils.getDoubleFromZahl(infos.get("stück")),
							Utils.getDoubleFromZahl(kurs[0]), kurs[1],

							((infos.get("orderart").toUpperCase().equals("KAUF")) ? -1 : 1) *
							Math.rint(Utils.getDoubleFromZahl(kurs[0]) * Utils.getDoubleFromZahl(infos.get("stück")) * 100) / 100,
							kurs[1],
							d,
							infos.get("ordernummer"), ""
							);
				}
			}

			if (missingOrderDate) {
				throw new ApplicationException("Nicht alle Order könnten übernommen werden, da das Orderdatum fehlt");
			}


		} catch (IOException e) {
			throw new ApplicationException(e);
		} finally {
			try {
				debug(seiten, konto);
			} catch (RemoteException e) {
				throw new ApplicationException(e);
			}
		}

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
					Logger.info("Warnung. Ungültige Anzahl an Zellen: " + cells.size() + " " + row.asText());
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
				Logger.info("Warnung. Ungültige Anzahl an Zellen: " + r1.toString());
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
	public List<String> getPROP() {
		List<String> result = super.getPROP();
		result.add(0, PROP_PASSWORD);
		result.add(0, PROP_KUNDENNUMMER);
		//result.remove(PropHelper.UMSAETZEERGAENZENINCLFORMAT);
		return result;
	}

	@Override
	public boolean isSupported(Konto konto) throws ApplicationException,
	RemoteException {
		return 	super.isSupported(konto) && 
				(konto.getBLZ().equals("76030080") 
						|| konto.getBic().toUpperCase().equals("CSDBDE71XXX"));
	}

}
