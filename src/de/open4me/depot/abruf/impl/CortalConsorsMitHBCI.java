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

import org.jfree.util.Log;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.ThreadedRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;

import de.open4me.depot.DepotViewerPlugin;
import de.open4me.depot.abruf.utils.HtmlUtils;
import de.open4me.depot.abruf.utils.Utils;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;

public class CortalConsorsMitHBCI extends BasisHBCIDepotAbruf {

	private final static I18N i18n = Application.getPluginLoader().getPlugin(DepotViewerPlugin.class).getResources().getI18N();
	final static String PROP_KUNDENNUMMER = "Kontonummer / UserID (Webseite)";
	final static String PROP_PASSWORD = "PIN / Passwort (Webseite)";

	@Override
	public String getName() {
		return "HBCI CortalConsors";
	}

	@Override
	public void run(Konto konto) throws ApplicationException {
		super.run(konto); // Bestand via HBCI
		Logger.info("Starte Screen Scraping für Cortal Consors");
		runUmsaetze(konto);

	}

	@SuppressWarnings("unchecked")
	public void runUmsaetze(Konto konto) throws ApplicationException {
		String kontonummer = null;
		try {
			kontonummer = konto.getKontonummer();
		} catch (RemoteException e2) {
			throw new ApplicationException("Kontonummer nicht gefunden", e2);
		}
		ArrayList<String> seiten = new ArrayList<String>(); 
		final WebClient webClient = new WebClient();
		webClient.setCssErrorHandler(new SilentCssErrorHandler());
		webClient.setRefreshHandler(new ThreadedRefreshHandler());
		webClient.getOptions().setJavaScriptEnabled(false);
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


			// Viel Ajax auf der Webseite, mit dem HTMLUnit nicht zu recht kommt.
			HtmlPage page = webClient.getPage("https://www.consorsbank.de/ev/System/Login?showEVLoginForm=true");
			seiten.add(page.asXml());
			String url = getLoginUrl(page);
			
			//
			page = webClient.getPage(url);
			seiten.add(page.asXml());
			
			// Login
			((HtmlInput) page.getElementById("username")).setValueAttribute(username);
			((HtmlInput) page.getElementById("passwort")).setValueAttribute(username);
			page = ((HtmlInput) page.getByXPath("//input[@value='Einloggen']").get(0)).click();
			seiten.add(page.asXml());


			// Und die Orderübersicht hart reincodieren.
			page = webClient.getPage("https://www.consorsbank.de/euroWebDe/-?$part=MonalisaDE.Desks.InvestmentAccounts.Desks.OrderInfo.content.orderInfo.main.accountTabs&$event=selectAccountViaRequestParam&accountno=" + kontonummer);
			seiten.add(page.asXml());
			HashMap<String, String> infos = new HashMap<String, String>();
			DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
			boolean missingOrderDate = false;
			for (HtmlAnchor x : page.getAnchors()) {
				if (!x.getAttribute("href").contains("event=details&orderInfoPageNumber=1&orderno=")) {
					continue;
				}
				infos.clear();
				page =  x.click();
				seiten.add(page.asXml());
				DomNodeList<DomElement> list = page.getElementsByTagName("table");
//				if (list.size() != 3) {
//					//throw new ApplicationException("Anzahl der Tabelen in Orderinfo stimmt nicht. Ist: " + list.size());
//				}
				HtmlUtils.tabUntereinander2hash(infos, (HtmlTable) list.get(1), 0, 1); // Spalte 1 und 2
				HtmlUtils.tabUntereinander2hash(infos, (HtmlTable) list.get(1), 3, 4); // Spalte 4 und 5; Spalte 3 ist leer
				HtmlUtils.tabNebeneinander2hash(infos, (HtmlTable) list.get(2));
				if (!infos.containsKey("kurs")) {
					missingOrderDate = true;
					continue;
				}
				String[] kurs = ((String) infos.get("kurs")).replaceAll("  *", " ").split(" ");
				if (!infos.containsKey("zeitpunkt der abrechnung")) {
					missingOrderDate = true;
					continue;
				}
				Date d;
				try {
					d = df.parse(infos.get("zeitpunkt der abrechnung").substring(0,10));
				} catch (ParseException e) {
					throw new ApplicationException("Unbekanntes Datumsformat beim Abrechnungszeitpunkt: " + infos.get("zeitpunkt der abrechnung"));
				}


				Utils.addUmsatz(konto.getID(), Utils.getORcreateWKN(infos.get("wkn"), infos.get("isin"), infos.get("wertpapiername")), infos.get("orderart"), 
						infos.toString(),
						Utils.getDoubleFromZahl(infos.get("stück/nominale")),
						Utils.getDoubleFromZahl(kurs[0]), kurs[1],

						((infos.get("orderart").toUpperCase().equals("KAUF")) ? -1 : 1) *
						Math.rint(Utils.getDoubleFromZahl(kurs[0]) * Utils.getDoubleFromZahl(infos.get("stück/nominale")) * 100) / 100,
						kurs[1],
						d,
						infos.get("ordernummer"), "",0.0d, "EUR", 0.0d, "EUR"
						);
			}

			if (missingOrderDate) {
				Log.error("Nicht alle Order konnten übernommen werden, da das Orderdatum oder der Kurs fehlt.");
			}


		} catch (IOException e) {
			throw new ApplicationException(e);
		} finally {
			try {
				forcelogoff(webClient);
				debug(seiten, konto);
			} catch (RemoteException e) {
				throw new ApplicationException(e);
			}
		}

	}

	private String getLoginUrl(HtmlPage page) throws ApplicationException {
		// Das parsen der Seite scheitert. Also manuell im Text suchen :-(
		String url = "";
		for (String s : page.asXml().split("\n")) {
			if (s.contains("gallery.authentication.modals.Login")) {
				url = "https://www.consorsbank.de" + s.replace("  href=\"", "").replace("\"", "");
			}
		}
		if (url.isEmpty()) {
			throw new ApplicationException("Login-Link nicht gefunden");
		}
		return url;
	}



	private void forcelogoff(WebClient webClient) {
		try {
			webClient.getPage("https://www.consorsbank.de/euroWebDe/-?$part=Home.login-status&$event=logout");
		} catch (FailingHttpStatusCodeException | IOException e) {
			// Mehr kann ich dann auch nicht tun
		}
	}

	@Override
	public List<String> getPROP(Konto konto) {
		List<String> result = super.getPROP(konto);
		result.add(0, PROP_PASSWORD);
		result.add(0, PROP_KUNDENNUMMER);
		return result;
	}

	@Override 
	public List<String[]> getPropertiesChanges(int version) {
		List<String[]> liste = new ArrayList<String[]>();
		if (version < 1) {
			liste.add(new String[]{ "Kundennummer (Webseite)", "Kontonummer / UserID (Webseite)", "1"});
		}
		if (version < 2) {
			liste.add(new String[]{ "Passwort (Webseite)", "PIN / Passwort (Webseite)", "2"});
		}
		if (version < 4) {
			liste.add(new String[] { "Nur Bestand via HBCI abholen?", "", "4" });
		}
		return liste;
	}
	@Override
	public boolean isSupported(Konto konto) throws ApplicationException,
	RemoteException {
		return 	super.isSupported(konto) && 
				(konto.getBLZ().equals("76030080") 
						|| konto.getBic().toUpperCase().equals("CSDBDE71XXX"));
	}

}
