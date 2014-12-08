package de.open4me.depot.abruf.impl;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.ThreadedRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

import de.open4me.depot.DepotViewerPlugin;
import de.open4me.depot.abruf.utils.HtmlUtils;
import de.open4me.depot.abruf.utils.PropHelper;
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

			// Viel Ajax auf der Webseite, mit dem HTMLUnit nicht zu recht kommt.
			// Also brauchen wir uns den Login-Request selber zusammen
			HtmlPage page = webClient.getPage("https://www.consorsbank.de/ev/System/Login?showEVLoginForm=true");
			seiten.add(page.asXml());
			WebRequest requestSettings = new WebRequest(new URL("https://www.consorsbank.de/euroWebDe/-?$part=login.json&$event=login"), HttpMethod.POST);
			requestSettings.setRequestParameters(new ArrayList<NameValuePair>());
			requestSettings.getRequestParameters().add(new NameValuePair("userId", username));
			requestSettings.getRequestParameters().add(new NameValuePair("nip", password));
			requestSettings.getRequestParameters().add(new NameValuePair("sender", ""));
			requestSettings.getRequestParameters().add(new NameValuePair("referer", ""));
			requestSettings.setAdditionalHeader("Referer", "https://www.consorsbank.de/ev/System/Login?showEVLoginForm=true");
			requestSettings.setAdditionalHeader("X-Requested-With", "XMLHttpRequest");
			requestSettings.setAdditionalHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
			com.gargoylesoftware.htmlunit.UnexpectedPage p = webClient.getPage(requestSettings);
			StringWriter writer = new StringWriter();
			IOUtils.copy(p.getInputStream(), writer, Charset.forName("UTF-8"));
			// TODO Json Antwort prüfen
			seiten.add(writer.toString());



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
				System.out.println(infos.entrySet());
				String[] kurs = ((String) infos.get("kurs")).replaceAll("  *", " ").split(" ");
				if (!infos.containsKey("zeitpunkt der abrechnung")) {
					missingOrderDate = true;
					continue;
				}
				Date d;
				try {
					d = df.parse(infos.get("zeitpunkt der abrechnung").substring(0,10));

					//						d = df.parse(infos.get("zeitpunkt der abrechnung").substring(0,10));
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
						infos.get("ordernummer"), ""
						);
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



	@Override
	public List<String> getPROP() {
		List<String> result = super.getPROP();
		result.add(0, PROP_PASSWORD);
		result.add(0, PROP_KUNDENNUMMER);
		result.add(PropHelper.NURBESTANDINKLFORMAT);
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
