package de.open4me.depot.gui.action;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;

import jsq.config.Config;

import org.jfree.util.Log;

import de.open4me.depot.Settings;
import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.datenobj.DepotAktion;
import de.open4me.depot.datenobj.rmi.BigDecimalWithCurrency;
import de.open4me.depot.datenobj.rmi.Umsatz;
import de.open4me.depot.gui.dialogs.KursAktualisierenDialog;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.open4me.depot.tools.CSVImportHelper;
import de.open4me.depot.tools.UmsatzHelper;
import de.open4me.depot.tools.io.FeldDefinitionen;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

public class UmsatzImportAction implements Action {

	@Override
	public void handleAction(Object context) throws ApplicationException {
		String kontoid;
		try {
			kontoid = askUserForKonto();
		} catch (Exception e1) {
			e1.printStackTrace();
			Logger.error("Kontoauswahl beim CSV-Import", e1);
			return;
		}			
		// FeldDefinitionen anwenden 
		ArrayList<FeldDefinitionen> fd = new ArrayList<FeldDefinitionen>();
//		fd.add(new FeldDefinitionen("Währung (global)", Currency.class, "waehrung", false));
		fd.add(new FeldDefinitionen("Datum/Valuta", java.util.Date.class, "date", true));
		fd.add(new FeldDefinitionen("Wertpapiername", String.class, "name", true));
		fd.add(new FeldDefinitionen("ISIN", String.class, "isin", false));
		fd.add(new FeldDefinitionen("WKN", String.class, "wkn", false));
		fd.add(new FeldDefinitionen("Transaktionsart", DepotAktion.class, "aktion", true));
		fd.add(new FeldDefinitionen("Anzahl", BigDecimal.class, "anzahl", true));
		fd.add(new FeldDefinitionen("Einzelkosten", BigDecimalWithCurrency.class, "kurs", false));
		fd.add(new FeldDefinitionen("Einzelkosten (Währung)", Currency.class, "kursW", false));
		
		fd.add(new FeldDefinitionen("Gesamtkosten (Anzahl x E.kosten)", BigDecimalWithCurrency.class, "kosten", false));
		fd.add(new FeldDefinitionen("Gesamtkosten (Währung)", Currency.class, "kostenW", false));
		
		fd.add(new FeldDefinitionen("Steuern", BigDecimalWithCurrency.class, "steuern", false));
		fd.add(new FeldDefinitionen("Steuern (Währung)", Currency.class, "steuernW", false));
		
		fd.add(new FeldDefinitionen("Gebühren", BigDecimalWithCurrency.class, "gebuehren", false));
		fd.add(new FeldDefinitionen("Gebühren (Währung)", Currency.class, "gebuehrenW", false));
		fd.add(new FeldDefinitionen("Ordernummer", String.class, "orderid", false));

		List<GenericObjectHashMap> daten;
		try {
			CSVImportHelper csv = new CSVImportHelper("umsatz." + kontoid, 0);
			daten = csv.run(fd, false);
		} catch (Exception e) {
			Logger.error("Fehler beim CSV-Import", e);
			throw new ApplicationException(e);
		}
		if (daten == null) {
			return;
		}

		String fehlt = "";
		// Und die letzte Umwandlung
		try {
			for (GenericObjectHashMap x : daten) {
				for (FeldDefinitionen f : fd) {
					Object value = x.getAttribute(f.getAttr());
					if (f.isRequired() && (value == null  || value.toString().isEmpty())) {
						fehlt += ", " + f.getBeschreibung();
					}
					if (value instanceof BigDecimalWithCurrency) {
						BigDecimalWithCurrency b = (BigDecimalWithCurrency) value;
						x.setAttribute(f.getAttr(), b.getZahl());
						x.setAttribute(f.getAttr() + "W", b.getWaehrung());
					}
					
				}
				if (x.getAttribute("isin").toString().isEmpty() && x.getAttribute("wkn").toString().isEmpty()) {
					fehlt += ", ISIN oder WKN";
				}
				if (x.getAttribute("kurs").toString().isEmpty() && x.getAttribute("kosten").toString().isEmpty()) {
					fehlt += ", Einzelkosten oder Gesamtkosten";
				}
				
				if (x.getAttribute("gebuehren").toString().isEmpty()) {
					x.setAttribute("gebuehren", BigDecimal.ZERO);
				}
				if (x.getAttribute("steuern").toString().isEmpty()) {
					x.setAttribute("steuern", BigDecimal.ZERO);
				}
				if (x.getAttribute("gebuehrenW").toString().isEmpty()) {
					x.setAttribute("gebuehrenW", x.getAttribute("_depotviewer_default_curr")); 
				}
				if (x.getAttribute("kursW").toString().isEmpty()) {
					x.setAttribute("kursW", x.getAttribute("_depotviewer_default_curr")); 
				}
				if (x.getAttribute("steuernW").toString().isEmpty()) {
					x.setAttribute("steuernW", x.getAttribute("_depotviewer_default_curr"));
				}
				if (x.getAttribute("kostenW").toString().isEmpty()) {
					x.setAttribute("kostenW", x.getAttribute("_depotviewer_default_curr")); 
				}
				if (((BigDecimal) x.getAttribute("anzahl")).signum() == -1) {
					x.setAttribute("anzahl", ((BigDecimal) x.getAttribute("anzahl")).abs()); 
				}
				if (x.getAttribute("kurs").toString().isEmpty()  && !x.getAttribute("kosten").toString().isEmpty()) {
					BigDecimal d = ((BigDecimal) x.getAttribute("kosten")).divide((BigDecimal) x.getAttribute("anzahl"),5, RoundingMode.HALF_UP);
					x.setAttribute("kurs", d); 
				}
				if (!x.getAttribute("kurs").toString().isEmpty()  && x.getAttribute("kosten").toString().isEmpty()) {
					BigDecimal d = ((BigDecimal) x.getAttribute("kurs")).multiply((BigDecimal) x.getAttribute("anzahl"));
					x.setAttribute("kosten", d); 
				}
				DepotAktion aktion = Utils.checkTransaktionsBezeichnung(x.getAttribute("aktion").toString().toUpperCase());
				if (aktion.equals(DepotAktion.KAUF)) {
					x.setAttribute("kosten", ((BigDecimal) x.getAttribute("kosten")).abs().negate());
				}
				if (aktion.equals(DepotAktion.VERKAUF)) {
					x.setAttribute("kosten", ((BigDecimal) x.getAttribute("kosten")).abs());
				}
				
				// Nochmal prüfen. Evtl. haben wir ja etwas übersehen
				for (FeldDefinitionen f : fd) {
					if (f.getAttr().equals("isin")) {
						continue;
					}
					if (f.getAttr().equals("wkn")) {
						continue;
					}
					if (f.getAttr().equals("orderid")) {
						continue;
					}
					if (x.getAttribute(f.getAttr()).toString().isEmpty()) {
						fehlt += ", " + f.getBeschreibung();
					}
					
				}
				if (!fehlt.isEmpty()) {
					Logger.error("Fehler beim CSV-Import. Es fehlt der Inhalft für folgende Felder: " + fehlt);
					throw new ApplicationException("Es fehlt Werte für die folgenden Felder: " + fehlt.substring(1));
				}
			}
			
			for (GenericObjectHashMap x : daten) {
				if (UmsatzHelper.existsOrder((String) x.getAttribute("orderid"))) {
					Log.warn("Überspringe Buchung, da sie bereits existiert");
					continue;
				}
				if (x.getAttribute("aktion") == null) {
					Log.error("Aktion fehlt");
				}
				Umsatz p = (Umsatz) Settings.getDBService().createObject(Umsatz.class,null);
				p.setKontoid(Integer.parseInt(kontoid));
				p.setAktion((DepotAktion) x.getAttribute("aktion"));
				p.setBuchungsinformationen("CSV Import");
				p.setWPid(Utils.getORcreateWKN(x.getAttribute("wkn").toString(), x.getAttribute("isin").toString(), x.getAttribute("name").toString()));
				p.setAnzahl((BigDecimal) x.getAttribute("anzahl"));
				p.setKurs((BigDecimal) x.getAttribute("kurs"));
				p.setKursW(x.getAttribute("kursW").toString());
				p.setKosten((BigDecimal) x.getAttribute("kosten"));
				p.setKostenW(x.getAttribute("kostenW").toString());
				p.setBuchungsdatum((Date) x.getAttribute("date"));
				p.setKommentar("");
				p.setSteuern((BigDecimal) x.getAttribute("steuern"));
				p.setSteuernW(x.getAttribute("steuernW").toString());
				p.setTransaktionsgebuehren((BigDecimal) x.getAttribute("gebuehren"));
				p.setTransaktionsgebuehrenW(x.getAttribute("gebuehrenW").toString());
				String orderid = (String) x.getAttribute("orderid");
				if (orderid.isEmpty()) {
					orderid = p.generateOrderId();
				}
				p.setOrderid(orderid);
				p.store();
			}
		} catch (RemoteException e) {
			throw new ApplicationException(e);
		}
		Application.getMessagingFactory().sendMessage(new StatusBarMessage(Application.getI18n().tr("Import beendet!"),StatusBarMessage.TYPE_INFO));



	}


	private String askUserForKonto() throws RemoteException, Exception {
		List<Config> cfg = new ArrayList<Config>();
		Config c = new Config("Konto für den Import");
		List<GenericObjectHashMap> list = Utils.getDepotKonten();
		for (GenericObjectHashMap obj : list) {
			c.addAuswahl(obj.getAttribute("bezeichnung").toString(), obj.getAttribute("id"));
		}
		cfg.add(c);
		KursAktualisierenDialog dialog= new KursAktualisierenDialog(KursAktualisierenDialog.POSITION_CENTER, cfg);
		dialog.open();
		String kontoid = c.getSelected().get(0).getObj().toString();
		return kontoid;
	}

}
