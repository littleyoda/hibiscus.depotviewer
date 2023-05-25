package de.open4me.depot.tools.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import de.open4me.depot.sql.GenericObjectHashMap;
import de.open4me.depot.tools.io.feldConverter.options.FeldConverterAuswahl;

public class CSVImportTool {

	private List<GenericObjectHashMap> list = new ArrayList<GenericObjectHashMap>();
	private List<String> header = new ArrayList<String>();
	private boolean isIgnoredLinesr;
	private ArrayList<FeldDefinitionen> feldDefinitionen;
	private ArrayList<FeldConverterAuswahl<?>> csvOptions;
	private FeldConverterAuswahl<String> charset = new FeldConverterAuswahl<String>("charset", "Charset", Arrays.asList(new String[] { "WINDOWS-1252", "ISO-8859-1", "ISO-8859-15", "US-ASCII", "UTF-8", "UTF-16BE", "UTF-16LE", "UTF-16" }));
	private FeldConverterAuswahl<String> trennzeichen = new FeldConverterAuswahl<String>("separator", "Trennzeichen", Arrays.asList(new String[] { ";", ",", "|", "\t" }));
	private FeldConverterAuswahl<Integer> skipLines = new FeldConverterAuswahl<Integer>("rowheader", "Zeile mit den Spaltennamen", Arrays.asList(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9,10 }));
	private File file;
			
	public CSVImportTool(ArrayList<FeldDefinitionen> fd) {
		this.feldDefinitionen = fd;
		csvOptions = new ArrayList<FeldConverterAuswahl<?>>();
		csvOptions.add(charset);
		csvOptions.add(trennzeichen);
		csvOptions.add(skipLines);
	}
	
	public void setFile(File file) {
		this.file = file;
	}

	public void load() throws IOException {
		header.clear();
		long counter = 0;

		int headerline = skipLines.getAuswahl() - 1;
		FileInputStream is = new FileInputStream(file);
		InputStreamReader isr = new InputStreamReader(is, Charset.forName(charset.getAuswahl()) );
		CSVFormat format = CSVFormat.RFC4180
				.withDelimiter(trennzeichen.getAuswahl().charAt(0))
				.withSkipLines(headerline);
		CSVParser parser = new CSVParser(isr, format);
		list.clear();
		isIgnoredLinesr = false;
		boolean isheader = true;
		header.clear();
		for(CSVRecord record : parser) {
			if (isheader) {
				for (int i = 0; i < record.size(); i++) {
					String name = record.get(i);
					if (name.isEmpty()) {
						name = "Namenlos";
					}
					String neuername = name;
					counter = 1;
					while (header.contains(neuername)) { 
						neuername = name + " (" + counter + ")";
						counter++;
					}
					header.add(neuername);
				}
				isheader = false;
				continue;
			}
			GenericObjectHashMap g = new GenericObjectHashMap();
			g.setAttribute("_DEPOTVIEWER_IGNORE", "");
			g.setAttribute("_DEPOTVIEWER_IDX", "" + (record.getRecordNumber() + headerline));
			for (int i = 0; i < header.size(); i++) {
				if (i >= record.size()) {
					g.setAttribute("_DEPOTVIEWER_IGNORE", "X");
					isIgnoredLinesr = true;
					continue;
				}
				g.setAttribute(header.get(i), record.get(i));

			}
			list.add(g);
		} 
		parser.close();
	}

	public List<GenericObjectHashMap> getList() {
		return list;
	}

	public List<String> getHeader() {
		return header;
	}

	public boolean isIgnoredLines() {
		return isIgnoredLinesr;
	}

	public int transformiereDaten(List<GenericObjectHashMap> tablist) throws RemoteException {
		tablist.clear();
		int fehler = 0;
		for (GenericObjectHashMap source: list) {
			if (!source.getAttribute("_DEPOTVIEWER_IGNORE").toString().isEmpty()) {
				continue;
			}
			GenericObjectHashMap g = new GenericObjectHashMap();
			for (FeldDefinitionen fd: feldDefinitionen) {
				boolean required = fd.isRequired();
				String sourceattr = fd.getSpalte();
				String destattr = fd.getAttr();
				// Prüfen, ob überhaupt eine Spalte zugewiesen wurde
				if (sourceattr == null || sourceattr.isEmpty() ) {
					if (required) {
						g.setAttribute(destattr, "N/A");
						fehler++;
					} else {
						g.setAttribute(destattr, "");
					}
					continue;
				}

				// Ursprungsdaten konvertieren
				Object sourcedata = source.getAttribute(sourceattr);
				try {
					sourcedata = fd.getConverters().convert((String) sourcedata);
				} catch (Exception e) {
					fehler++;
					if (sourcedata == null) {
						sourcedata = "ERROR: NULL";
					} else {
						sourcedata = "ERROR: " + sourcedata.toString();
					}
				}
				g.setAttribute(destattr, sourcedata);
			}
			tablist.add(g);
		}
		return fehler;
	}

	public ArrayList<FeldConverterAuswahl<?>> getCsvOptions() {
		return csvOptions;
	}

	public ArrayList<FeldDefinitionen> getFeldDefinitionen() {
		return feldDefinitionen;
	}

}
