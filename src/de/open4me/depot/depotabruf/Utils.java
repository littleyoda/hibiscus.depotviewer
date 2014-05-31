package de.open4me.depot.depotabruf;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import de.open4me.depot.Settings;
import de.open4me.depot.rmi.Bestand;
import de.open4me.depot.rmi.Umsatz;
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
	public static
	void addUmsatz(String wkn, String name, String aktion, String info, Double anzahl, 
			Double kurs, String kursW, Double kosten, String kostenW, Date date, String orderid) throws ApplicationException {
		try {
			DBIterator liste = Settings.getDBService().createList(Umsatz.class);
			liste.addFilter("orderid=?", orderid);
			if (liste.hasNext()) {
				Logger.info("Skipping Buchung");
				return;
			}
			// create new project
			Umsatz p = (Umsatz) Settings.getDBService().createObject(Umsatz.class,null);
			p.setAktion(aktion);
			p.setBuchungsinformationen(info);
			p.setWertPapierName(name);
			p.setWKN(wkn);
			p.setAnzahl(anzahl);
			p.setKurz(kurs);
			p.setKurzW(kursW);
			p.setKosten(kosten);
			p.setKostenW(kostenW);
			p.setBuchungsdatum(date);
			p.setOrderid(orderid);
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
	public static void addBestand(Konto konto, Double anzahl, String wkn,
			Double kurs, String kursw, double wert, String wertw, Date date) throws ApplicationException {
		try {
			Bestand p = (Bestand) Settings.getDBService().createObject(Bestand.class,null);
			p.setAnzahl(anzahl);
			p.setKontoid(Integer.parseInt(konto.getID()));
			p.setKurs(kurs);
			p.setKursw(kursw);
			p.setWert(wert);
			p.setWertw(wertw);
			p.setWkn(wkn);
			p.setDatum(date);
			p.store();
		}
		catch (RemoteException e)
		{
			System.out.println(e.toString());
			e.printStackTrace();
			throw new ApplicationException(Settings.i18n().tr("error while creating new Umsatz"),e);
		}

	}

	public static void clearBestand(Konto konto) throws RemoteException, ApplicationException {
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


	public static void main(String [] args)
	{
		String[] s = new String[]{
				"1", "1 2", "123456789012345678901234567890",
				"123456789012345678901234567 890",
				"1234567890123456789012345678 90",
				"123456789012345678901234567 890",
				"123456789012345678901234567 890 123456789012345678901234567 3342",
		};
		for (String t : s) {
			System.out.println(t + ": " + Arrays.toString(parse(t)));
		}
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


}
