package de.open4me.depot.abruf.impl;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.UnexpectedPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import de.open4me.depot.abruf.utils.HtmlUtils;
import de.open4me.depot.gui.dialogs.DebugDialogWithTextarea;
import de.willuhn.jameica.hbci.gui.dialogs.DebugDialog;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.system.Application;
import de.willuhn.jameica.system.OperationCanceledException;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

/***
 * Genutzt wird hierbei die Schnittstelle, welche auch die Consorsbank-APP nutzt.
 * Leider gibt es keine Dokumentation für diese Schnittstelle.
 * Informationen muss man sich selber zusammenreihmen, indem man die Kommunikation zwischen der APP und dem Server belauscht. 
 * 
 * @author sven
 *
 */

public class CortalConsorsMitHBCI extends BasisHBCIDepotAbruf {

	//	private final static I18N i18n = Application.getPluginLoader().getPlugin(DepotViewerPlugin.class).getResources().getI18N();
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
		List<String> fehlerhafteOrder = new ArrayList<String>();
		String depotnummer = null;
		try {
			depotnummer = konto.getKontonummer();
		} catch (RemoteException e2) {
			throw new ApplicationException("Kontonummer nicht gefunden", e2);
		}
		ArrayList<String> seiten = new ArrayList<String>(); 
		try {
			String username = konto.getMeta(PROP_KUNDENNUMMER, null);
			if (username == null || username.length() == 0) {
				throw new ApplicationException("Bitte geben ihre Kundenummer in den Synchronisationsoptionen ein");
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
			HtmlUtils.setProxyCfg(webClient, "https://webservices.consorsbank.de/");
			webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);


			String url = "https://webservices.consorsbank.de/WebServicesDe/services/restful/login";

			// Login und "sessionID" speichern
			// TODO Password und Username entsprechend escapen
			String request = "{\"1\":{\"2\":\"" + password + "\",\"3\":\"" + username + "\",\"0\":{\"5\":\"MOOTWINA\",\"6\":\"0\",\"2\":\"DE\",\"1\":\"DE\",\"0\":\"CCLogin\",\"3\":\"\",\"4\":\"1\"}}}";
			String json = getRequest(webClient, url, request);
			seiten.add(json);
			// Prüfen, ob Login Fehlgeschlagen ist
			if (!"CCLogin".equals(jsonRead(json,"$.2.0.0"))) {
				Logger.debug(json);
				String msg = jsonRead(json,"$.20[0].1");
				if (msg == null || msg.isEmpty()) {
					msg = "Login aus unbekannten Grund nicht möglich!";
				}
				throw new ApplicationException(msg);
			}
			String sessionID = jsonRead(json,"$.2.0.3");

			// Request: Alle Order
			String allordersRequest = "{\"6\":{\"1\":\"" + depotnummer + "\",\"600\":{\"2\":\"30\",\"3\":\"orderNo DESC\",\"1\":\"1\",\"0\":\"Order\"},\"601\":\"Q\",\"0\":{\"5\":\"MOOTWINA\",\"6\":\"0\",\"2\":\"DE\",\"1\":\"DE\",\"0\":\"CCOrderAllInquiry\",\"3\":\"" + sessionID + "\",\"4\":\"1\"}}}";
			json = getRequest(webClient, "https://webservices.consorsbank.de/WebServicesDe/services/restful/getAllOrders", allordersRequest);
			seiten.add(json);


			// Für alle Order, die Detailinformationen requesten und Umsatz-Eintrag generieren
			try {
				Logger.debug("JSON für Order: " + json.replace(depotnummer, "000111222333"));
				Integer anzahlOrders = JsonPath.parse(json).read("$.7.602.2");
				List<Map<String, Object>> orders = null;
				if (anzahlOrders == 1) {
					orders = new ArrayList<Map<String, Object>>(); 
					orders.add(JsonPath.parse(json).read("$.7.2", Map.class));
				} else if (anzahlOrders > 1){
					orders = JsonPath.parse(json).read("$.7.2", List.class);
				}

				if (orders == null) {
					Logger.info("Es wurden keine Order gefunden!");
				} else {
					Logger.info("Es wurden " + orders.size() + " Order gefunden!");
					for (Map<String, Object> orderinfo : orders) {
						if (orderinfo.get("6").toString().equals("0")) {
							Logger.info("Offene Order übersprungen!");
							continue;
						}
						String orderRequest = "{\"101\":{\"1\":\"" + depotnummer + "\",\"2\":\"" + orderinfo.get("4").toString() + "\",\"0\":{\"5\":\"MOOTWINA\",\"6\":\"0\",\"2\":\"DE\",\"1\":\"DE\",\"0\":\"CCOrderDetailInquiry\",\"3\":\"" + sessionID + "\",\"4\":\"1\"}}}";
						String order = getRequest(webClient, "https://webservices.consorsbank.de/WebServicesDe/services/restful/getOrderDetail", orderRequest);
						seiten.add(order);
						parseOrder(depotnummer, konto, fehlerhafteOrder, orderinfo, order);
					}
				}
			} catch (PathNotFoundException pnfe) {
				Logger.error("Fehler bei der Verarbeitung der JSON", pnfe);
				fehlerhafteOrder.add(pnfe + json.replace(depotnummer, "000111222333"));
			}


			// Logout
			String logoutRequest = "{\"17\":{\"0\":{\"5\":\"MOOTWINA\",\"6\":\"0\",\"2\":\"DE\",\"1\":\"DE\",\"0\":\"CCLogout\",\"3\":\"" + sessionID + "\",\"4\":\"1\"}}}";
			json = getRequest(webClient, "https://webservices.consorsbank.de/WebServicesDe/services/restful/logout", logoutRequest);
			seiten.add(json);
			// @TODO Anwort verifizieren

		} catch (IOException e) {
			throw new ApplicationException(e);
		} finally {
			try {
				debug(seiten, konto);
			} catch (RemoteException e) {
				throw new ApplicationException(e);
			}
		}


		try
		{
			if (fehlerhafteOrder.size() > 0) {
				DebugDialogWithTextarea dialog = new DebugDialogWithTextarea(DebugDialog.POSITION_CENTER, fehlerhafteOrder);
				dialog.open();
			}
		} catch (OperationCanceledException oce) {
			//
		} catch (Exception e) {
			Logger.error("unable to display debug dialog",e);
		}

	}

	private void parseOrder(String depotnummer, Konto konto, List<String> fehlerhafteOrder, Map<String, Object> orderinfo, String order)
			throws RemoteException {
		Map<String, Object> detailInfo = null;
		try {
			detailInfo = JsonPath.parse(order).read("$.102.50", Map.class);

			CortalConsorsMitHBCIJSONWrapper wrapper = new CortalConsorsMitHBCIJSONWrapper(orderinfo, detailInfo);
			if (!wrapper.addUmsatz(konto.getID())) {
				fehlerhafteOrder.add(CortalConsorsMitHBCIJSONWrapper.getAnnoymisierterBuchungstext(orderinfo, detailInfo));
			}
		} catch (PathNotFoundException pnfe) {
			Logger.error("Fehler bei der Verarbeitung der JSON", pnfe);
			String out = CortalConsorsMitHBCIJSONWrapper.getAnnoymisierterBuchungstext(orderinfo, detailInfo).replace(depotnummer, "000111222333") + "\n" + order;
			Logger.info("Orderjson: " + out);
			fehlerhafteOrder.add(pnfe + out);
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


	/**
	 * Führt ein JSON-Request auf
	 * @param webClient htmlunit-client
	 * @param url URL
	 * @param request Request im JSON Format
	 * @return Antwort des Server, auch in JSON Format
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private String getRequest(final WebClient webClient, String url, String request)
			throws MalformedURLException, IOException {
		WebRequest requestSettings = new WebRequest(new URL(url), HttpMethod.POST);
		requestSettings.setRequestBody(request);
		requestSettings.setAdditionalHeader("Content-Type", "application/json");

		UnexpectedPage p = (UnexpectedPage) webClient.getPage(requestSettings);
		StringWriter writer = new StringWriter();
		IOUtils.copy(p.getInputStream(), writer, Charset.forName("UTF-8"));
		String json = writer.toString();
		return json;
	}

	private String jsonRead(String json, String xpath) {
		try {
			return JsonPath.parse(json).read(xpath);
		} catch (com.jayway.jsonpath.PathNotFoundException e) {
			return null;
		}
	}
}
