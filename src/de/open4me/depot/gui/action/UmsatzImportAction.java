package de.open4me.depot.gui.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jsq.config.Config;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

import de.open4me.depot.depotabruf.Utils;
import de.open4me.depot.gui.dialogs.KursAktualisierenDialog;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.util.ApplicationException;

public class UmsatzImportAction implements Action {

	@Override
	public void handleAction(Object context) throws ApplicationException {
		try {
			
			File file = askUserForFile();
			if (file == null) {
				return;
			}
			String kontoid = askUserForKonto();			
			
			CSVFormat format = CSVFormat.RFC4180.withHeader().withDelimiter(',').withIgnoreEmptyLines(true);
			CSVParser parser;
			
			// erstmal ein Probedurchgang ohne Import
			InputStreamReader stream = new InputStreamReader(new FileInputStream(file), "UTF-8");
			parser = new CSVParser(stream, format);
			prüfeSpaltenAufVollstaendigkeit(parser);
			importiere(kontoid, parser, false); 
			parser.close();
			
			// Und jetzt erst Importieren
			stream = new InputStreamReader(new FileInputStream(file), "UTF-8");
			parser = new CSVParser(stream, format);
			importiere(kontoid, parser, true); 
			parser.close();
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new ApplicationException("Fehler beim Import: " + e.getMessage() , e);
			// TODO Auto-generated catch block
		}

	}

	private void importiere(String kontoid, CSVParser parser, boolean doImport) throws ApplicationException,
			RemoteException {
		DateFormat df = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);
		for(CSVRecord record : parser){
			String wpid = Utils.getORcreateWKN(
					record.isMapped("wkn") ? record.get("wkn") : "",
					record.isMapped("isin") ? record.get("isin") : "",
					record.isMapped("name") ? record.get("name") : "");
			if (wpid == null) {
				throw new ApplicationException("Weder die Spalte 'wkn' noch die Spalte 'isin' enthalten einen Wert.");
			}
			String aktion = record.get("aktion").toUpperCase();
			if (!(aktion.equals("KAUF") || aktion.equals("VERKAUF"))) {
				throw new ApplicationException("In der Spalte 'aktion' nur nur die Werte 'KAUF' oder 'VERKAUF' zulässig. \"" + aktion + "\"");
			}
			Date date = null;
			try {
				date = df.parse(record.get("datum"));
			} catch (ParseException e) {
				throw new ApplicationException("Fehler beim Verarbeiten des Datums \"" + record.get("Datum") + "\"" );
			}
			Double stueck =  Double.parseDouble(record.get("anzahl").replace(".", "").replace(",","."));
			Double kurs = Double.parseDouble(record.get("kurs").replace(".", "").replace(",","."));
			String waehrung = record.get("währung");
			if (doImport) {
				Utils.addUmsatz(kontoid, wpid, aktion, 
						"", stueck,
						kurs,
						waehrung,
						((aktion.toUpperCase().equals("VERKAUF")) ? 1 : -1) * kurs * stueck,
						waehrung,
						date,
						"" + record.toString().hashCode()
						);
			}
		}
	}

	private void prüfeSpaltenAufVollstaendigkeit(CSVParser parser)
			throws ApplicationException {
		for (String s : new String[]{ "datum", "kurs", "anzahl", "währung", "aktion", "name" }) {
			if (parser.getHeaderMap().get(s) == null) {
				throw new ApplicationException("Die Spalte '" + s + "' fehlt." );
			}
		}
		if (parser.getHeaderMap().get("wkn") == null && parser.getHeaderMap().get("isin") == null) {
			throw new ApplicationException("Weder die Spalte 'wkn' noch die Spalte 'isin' existiert." );
		}
	}

	private File askUserForFile() {
	    FileDialog fd = new FileDialog(GUI.getShell(),SWT.OPEN);
	    fd.setFilterExtensions(new String[]{"*.csv"});
	    fd.setText("Bitte wählen Sie die CSV-Datei aus");
	    String f = fd.open();
	    if (f == null || f.length() == 0) {
	      return null;
	    }
	    
	    File file = new File(f);
	    if (!file.exists()) {
	      return null;
	    }
	    return file;
	}

	private String askUserForKonto() throws RemoteException, Exception {
		List<Config> cfg = new ArrayList<Config>();
		Config c = new Config("Konto für den Import");
		List<GenericObjectSQL> list = SQLUtils.getResultSet("select * from konto",
				"konto", "id");
		for (GenericObjectSQL obj : list) {
			c.addAuswahl(obj.getAttribute("bezeichnung").toString(), obj.getAttribute("id"));
		}
		cfg.add(c);
		KursAktualisierenDialog dialog= new KursAktualisierenDialog(KursAktualisierenDialog.POSITION_CENTER, cfg);
		dialog.open();
		String kontoid = c.getSelected().get(0).getObj().toString();
		return kontoid;
	}

}
