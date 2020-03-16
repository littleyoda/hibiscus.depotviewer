package de.open4me.depot.gui.action;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;

import jsq.config.Config;
import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.datenobj.rmi.BigDecimalWithCurrency;
import de.open4me.depot.gui.dialogs.KursAktualisierenDialog;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.open4me.depot.tools.CSVImportHelper;
import de.open4me.depot.tools.UmsatzeAusBestandsAenderung;
import de.open4me.depot.tools.io.FeldDefinitionen;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

public class BestandImportAction implements Action {


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
		fd.add(new FeldDefinitionen("Bewertungszeitpunkt", java.util.Date.class, "date", true));
		fd.add(new FeldDefinitionen("Wertpapiername", String.class, "name", false));
		fd.add(new FeldDefinitionen("ISIN", String.class, "isin", false));
		fd.add(new FeldDefinitionen("WKN", String.class, "wkn", false));
		
		fd.add(new FeldDefinitionen("Anzahl", BigDecimal.class, "anzahl", true));
		
		fd.add(new FeldDefinitionen("Kurs", BigDecimalWithCurrency.class, "kurs", false));
		fd.add(new FeldDefinitionen("Kurs (Währung)", Currency.class, "kursW", false));

		fd.add(new FeldDefinitionen("Gesamtwert (Anzahl x Kurs)", BigDecimalWithCurrency.class, "wert", false));
		fd.add(new FeldDefinitionen("Gesamtwert (Währung)", Currency.class, "wertW", false));

		
		List<GenericObjectHashMap> daten;
		try {
			CSVImportHelper csv = new CSVImportHelper("bestandsimport." + kontoid);
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
					if (f.isRequired() &&  value.toString().isEmpty()) {
						fehlt += ", " + f.getBeschreibung();
					}
					// BigDecimalWithCurrency wieder trennen
					if (value instanceof BigDecimalWithCurrency) {
						BigDecimalWithCurrency b = (BigDecimalWithCurrency) value;
						x.setAttribute(f.getAttr(), b.getZahl());
						x.setAttribute(f.getAttr() + "W", b.getWaehrung());
					}

				}
				if (x.getAttribute("isin").toString().isEmpty() && x.getAttribute("wkn").toString().isEmpty()) {
					fehlt += ", ISIN oder WKN";
				}
				if (x.getAttribute("kurs").toString().isEmpty() && x.getAttribute("wert").toString().isEmpty()) {
					fehlt += ", Kurs oder Gesamtwert";
				}

				if (x.getAttribute("kursW").toString().isEmpty()) {
					x.setAttribute("kursW", x.getAttribute("_depotviewer_default_curr")); 
				}
				if (x.getAttribute("wertW").toString().isEmpty()) {
					x.setAttribute("wertW", x.getAttribute("_depotviewer_default_curr")); 
				}

				if (x.getAttribute("kurs").toString().isEmpty()  && !x.getAttribute("wert").toString().isEmpty()) {
					BigDecimal d = ((BigDecimal) x.getAttribute("wert")).divide((BigDecimal) x.getAttribute("anzahl"),5, RoundingMode.HALF_UP);
					x.setAttribute("kurs", d); 
				}
				if (!x.getAttribute("kurs").toString().isEmpty()  && x.getAttribute("wert").toString().isEmpty()) {
					BigDecimal d = ((BigDecimal) x.getAttribute("kurs")).multiply((BigDecimal) x.getAttribute("anzahl"));
					x.setAttribute("wert", d); 
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
			Konto konto = Utils.getKontoByID(kontoid);
			
			UmsatzeAusBestandsAenderung umsaetzeAusBestaenden = new UmsatzeAusBestandsAenderung(konto);
			Utils.clearBestand(konto);
			for (GenericObjectHashMap x : daten) {
				// Alle Einträge sind vorläufig und werden bei der Neuberechnung des Offline-Kontos gelöscht
				Object kursW = x.getAttribute("kursW");
				Object wertW = x.getAttribute("wertW");
				System.out.println(x);
				Utils.addBestand(
						Utils.getORcreateWKN((String) x.getAttribute("wkn"), (String) x.getAttribute("isin"), (String) x.getAttribute("name")),
						konto,
						((BigDecimal) x.getAttribute("anzahl")).doubleValue(),
						((BigDecimal) x.getAttribute("kurs")).doubleValue(),
						handleCurrency(kursW),
						((BigDecimal) x.getAttribute("wert")).doubleValue(),
						handleCurrency(wertW),
						(Date) x.getAttribute("date"),
						(Date) x.getAttribute("date"));
			}
			umsaetzeAusBestaenden.erzeugeUmsaetze();
		} catch (RemoteException e) {
			throw new ApplicationException(e);
		}
		Application.getMessagingFactory().sendMessage(new StatusBarMessage(Application.getI18n().tr("Import beendet!"),StatusBarMessage.TYPE_INFO));

	}

	private String handleCurrency(Object wert) {
		if (wert instanceof Currency) {
			return ((Currency) wert).getCurrencyCode();
		}
		return wert.toString();
	}

	private String askUserForKonto() throws RemoteException, Exception {
		List<Config> cfg = new ArrayList<Config>();
		Config c = new Config("Konto für den Import");
		List<GenericObjectHashMap> list = Utils.getDepotKonten(true);
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
