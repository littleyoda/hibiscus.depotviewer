package de.open4me.depot.depotabruf;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import de.open4me.depot.Settings;
import de.open4me.depot.rmi.Bestand;
import de.open4me.depot.rmi.Umsatz;
import de.open4me.depot.rmi.Wertpapier;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.datasource.rmi.DBIterator;
import de.willuhn.jameica.gui.dialogs.YesNoDialog;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.plugin.Plugin;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

public class Utils {

	public static void debug(String path, String dateiname, String options, List<String> seiten) {
		try {
			if (options.toLowerCase().contains("save")) {
				YesNoDialog d = new YesNoDialog(YesNoDialog.POSITION_CENTER);
				d.setTitle("Speichern");
				d.setText("Sollen die Debug-Informationen gespeichert werden?\n"
						+ "Ziel: " + (new File(path, dateiname + ".zip")));
				try {
					Boolean choice = (Boolean) d.open();
					if (!choice.booleanValue()) {
						return;
					}
				}	catch (Exception e) {
					return;
				}
				try { 
					ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(
							new File(path, dateiname + ".zip")));
					int i = 0;
					for (String x : seiten) {
						ZipEntry ze = new ZipEntry(dateiname + "." + i + ".html");
						zip.putNextEntry(ze);
						zip.write(x.getBytes(Charset.forName("UTF-8")));
						zip.closeEntry();
						i++;
					}
					zip.close();
				} catch (IOException e) {
					throw new ApplicationException(e);

				}
			}
		} catch (Exception e) {
			Logger.error("Zusammenstellung der Debug-Informationen fehlgeschlagen", e);
		}
	}

	public static String getWorkingDir(Class<? extends Plugin> class1) {
		return Application.getPluginLoader().getPlugin(class1).getResources().getWorkPath();
	}

	public static Double getDoubleFromZahl(String s) {
		return Double.parseDouble(s.replace(".", "").replace(",","."));
	}

	public static void addUmsatz(String kontoid, String wpid, String aktion, String info, Double anzahl, 
			Double kurs, String kursW, Double kosten, String kostenW, Date date, String orderid, String kommentar) throws ApplicationException {
		try {
			DBIterator liste = Settings.getDBService().createList(Umsatz.class);
			liste.addFilter("orderid=?", orderid);
			if (liste.hasNext()) {
				Logger.info("Skipping Buchung");
				return;
			}
			if (!(aktion.toUpperCase().equals("KAUF") || aktion.toUpperCase().equals("VERKAUF") || aktion.toUpperCase().equals("EINLAGE"))) {
				Logger.error("Unbekannte Buchungsart" + aktion);
				return;
			}
			if ((aktion.toUpperCase().equals("KAUF") && (kosten >= 0.0f))
				|| (aktion.toUpperCase().equals("VERKAUF") && (kosten <= 0.0f))) {
				throw new ApplicationException("Bei Käufen muss der Gesamtbetrag negativ sein, beim Verkauf positiv.");
			}
			if (kurs <=  0.0f) {
					throw new ApplicationException("Der Kurs muss immer positiv sein.");
			}
			if (orderid == null) {
				orderid = "" + ("" + kontoid + wpid + aktion + date + anzahl + kurs + kursW).hashCode(); 
			}
			markRecalc();
			// create new project
			Umsatz p = (Umsatz) Settings.getDBService().createObject(Umsatz.class,null);
			p.setKontoid(Integer.parseInt(kontoid));
			p.setAktion(aktion.toUpperCase());
			p.setBuchungsinformationen(info);
			p.setWPid(wpid);
			p.setAnzahl(anzahl);
			p.setKurz(kurs);
			p.setKurzW(kursW);
			p.setKosten(kosten);
			p.setKostenW(kostenW);
			p.setBuchungsdatum(date);
			p.setOrderid(orderid);
			p.setKommentar(kommentar);
			p.store();
			// Liste der Objekte aus der Datenbank laden
		}
		catch (RemoteException e)
		{
			System.out.println(e.toString());
			e.printStackTrace();
			throw new ApplicationException(Settings.i18n().tr("error while creating new Umsatz"),e);
		}

	}

	/**
	 * Wird aufgerufen, wenn sich im Bestand oder bei den Umästzen etwas geändert hat
	 * 
	 * Dient als Trigger, um notwendige Aktualisierungen anzustoßen
	 * @throws ApplicationException 
	 */
	public static void markRecalc() throws ApplicationException {
		setUmsatzBetsandTest(null);
		
	}
	
	public static void setUmsatzBetsandTest(Boolean value) throws ApplicationException {
		SQLUtils.exec("update depotviewer_cfg set value = " + ((value == null) ? "NULL" : value.toString()) + " where key='status_bestand_order'");
	}
	
	public static Boolean getUmsatzBestandTest() {
		List<GenericObjectSQL> rs = SQLUtils.getResultSet("select value from depotviewer_cfg where key ='status_bestand_order'", "", "");
		try {
			Object ret = rs.get(0).getAttribute("value");
			if (ret == null) {
				return null;
			}
			return Boolean.parseBoolean(ret.toString());
		} catch (RemoteException e) {
			e.printStackTrace();
			return null;
		}
	}


	public static void addBestand(String wpid, Konto konto, Double anzahl, 
			Double kurs, String kursw, double wert, String wertw, Date date, Date bewertungsZeitpunkt) throws ApplicationException {
		try {
			markRecalc();
			Bestand p = (Bestand) Settings.getDBService().createObject(Bestand.class,null);
			p.setAnzahl(anzahl);
			p.setKontoid(Integer.parseInt(konto.getID()));
			p.setKurs(kurs);
			p.setKursw(kursw);
			p.setWert(wert);
			p.setWertw(wertw);
			p.setWPid(wpid);
			p.setDatum(date);
			p.setBewertungsDatum(bewertungsZeitpunkt);
			p.store();
		}
		catch (RemoteException e)
		{
			System.out.println(e.toString());
			e.printStackTrace();
			throw new ApplicationException(Settings.i18n().tr("error while creating new Umsatz"),e);
		}

	}

	/**
	 * Löscht für ein Konto den gesamten Bestand
	 * 
	 * @param konto Konto
	 * @throws RemoteException
	 * @throws ApplicationException
	 */
	public static void clearBestand(Konto konto) throws RemoteException, ApplicationException {
		markRecalc();
		SQLUtils.exec("delete from depotviewer_bestand where kontoid = " + konto.getID());
	}


	// Zerlegt einen String intelligent in max. 27 Zeichen lange Stücke
	public static String[] parse(String line)
	{
		if (line == null || line.length() == 0)
			return new String[0];
		List<String> out = new ArrayList<String>();
		String rest = line.trim();
		int lastpos = 0;
		while (rest.length() > 0) {
			if (rest.length() < 28) {
				out.add(rest);
				rest = "";
				continue;
			}
			int pos = rest.indexOf(' ', lastpos + 1);
			boolean zulang = (pos > 28) || pos == -1;
			// 1. Fall: Durchgehender Text mit mehr als 27 Zeichen ohne Space
			if (lastpos == 0 && zulang) {
				out.add(rest.substring(0, 27));
				rest = rest.substring(27).trim();
				continue;
			} 
			// 2. Fall Wenn der String immer noch passt, weitersuchen
			if (!zulang) {
				lastpos = pos;
				continue;
			}
			// Bis zum Space aus dem vorherigen Schritt den String herausschneiden
			out.add(rest.substring(0, lastpos));
			rest = rest.substring(lastpos + 1).trim();
			lastpos = 0;
		}
		return out.toArray(new String[0]);
	}



	public static void writePage(HtmlPage page, String s) throws Exception {
		BufferedWriter out = new BufferedWriter(new FileWriter("/tmp/" + s + ".txt"));
		out.write(page.getUrl() + "\n");
		out.write("===============================================\n");
		out.write(page.asText() + "\n");
		out.write("===============================================\n");
		out.close();

		out = new BufferedWriter(new FileWriter("/tmp/" + s + ".xml"));
		out.write(page.asXml());
		out.close();
	}

	public static void report(List<? extends HtmlElement> byXPath) {
		Logger.warn("Mögliche alternative Elemente:");
		for (HtmlElement  x : byXPath) {
			Logger.warn("- " + x.getTagName() + " Attribute: " + x.getAttributesMap().entrySet().toString());
		}
	}

	public static String getWertPapierByWkn(String suche) throws RemoteException {
		DBIterator liste = Settings.getDBService().createList(Wertpapier.class);
		liste.addFilter("wkn=?", suche.toUpperCase());
		if (!liste.hasNext()) {
			return null; // Unbekanntes Wertpapier
		}
		return ((Wertpapier) liste.next()).getWpid();
	}

	public static String getWertPapierByIsin(String suche) throws RemoteException {
		DBIterator liste = Settings.getDBService().createList(Wertpapier.class);
		liste.addFilter("isin=?", suche.toUpperCase());
		if (!liste.hasNext()) {
			return null; // Unbekanntes Wertpapier
		}
		return ((Wertpapier) liste.next()).getWpid();
	}

	public static void addWertPapier(String wkn, String isin,
			String name) throws ApplicationException {
		try {
			Wertpapier p = (Wertpapier) Settings.getDBService().createObject(Wertpapier.class,null);
			p.setWkn(wkn.toUpperCase());
			p.setIsin(isin.toUpperCase());
			p.setWertpapiername(name);
			p.store();
		} catch (RemoteException e) {
			System.out.println(e.toString());
			e.printStackTrace();
			throw new ApplicationException(Settings.i18n().tr("error while creating new Umsatz"),e);
		}
	}

	
	public static String getORcreateWKN(String wkn, String isin, String name) throws ApplicationException, RemoteException {
		if (wkn == null) {
			wkn = "";
		}
		if (isin == null) {
			isin = "";
		}
		if (wkn.isEmpty() && isin.isEmpty()) {
			throw new ApplicationException("Entweder WKN oder ISIN muss angegeben werden!");
		}
		String wpid = null;
		if (!wkn.isEmpty()) {
			wpid = Utils.getWertPapierByWkn(wkn);
		}
		if (!isin.isEmpty() && wpid == null) {
			wpid = Utils.getWertPapierByIsin(isin);
		}
		if (wpid == null) {
			Utils.addWertPapier(wkn, isin, name);
			return getORcreateWKN(wkn, isin, name);
		}
		return wpid;
	}

	public static List<Konto> getKonten() throws RemoteException, ApplicationException {
		List<Konto> list = new ArrayList<Konto>();
		DBIterator liste = Settings.getDBService().createList(Konto.class);
		while (liste.hasNext()) {
			Konto k = (Konto) liste.next();
			if (DepotAbrufFabrik.getDepotAbruf(k) != null) {
				list.add(k);
			}
		}
		return list;
	}
	
}
