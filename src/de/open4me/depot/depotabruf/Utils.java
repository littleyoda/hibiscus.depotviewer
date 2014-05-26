package de.open4me.depot.depotabruf;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import de.open4me.depot.Settings;
import de.open4me.depot.rmi.Bestand;
import de.open4me.depot.rmi.Umsatz;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.datasource.rmi.DBIterator;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.util.ApplicationException;

public class Utils {

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
				System.out.println("Skipping Buchung");
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


}
