package de.open4me.depot.tools.io;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

import jsq.config.Config;
import jsq.datastructes.Datacontainer;
import jsq.fetcher.history.BaseFetcher;
import de.open4me.depot.gui.dialogs.CSVImportConfigDialog;
import de.open4me.depot.gui.dialogs.CSVImportFeldDefinitionenDialog;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.system.OperationCanceledException;
import de.willuhn.util.ApplicationException;

public class KurseViaCSV extends BaseFetcher {

	@Override
	public String getName() {
		return "CSV Import";
	}

	@Override
	public String getURL() {
		return "";
	}

	@Override
	public void prepare(String search, int beginYear, int beginMon,
			int beginDay, int stopYear, int stopMon, int stopDay)
					throws Exception {
		super.prepare(search, beginYear, beginMon, beginDay, stopYear, stopMon, stopDay);
		try {
		    FileDialog fileopen = new FileDialog(GUI.getShell(),SWT.OPEN);
		    fileopen.setText("Bitte wählen Sie die CSV Datei aus:");
		    String filename = fileopen.open();
		    if (filename == null || filename.isEmpty()) {
		      return;
		    }
		    File file = new File(filename);
		    if (!file.exists() || !file.isFile()) {
		      throw new ApplicationException("Datei existiert nicht oder ist nicht lesbar");
		    }
			
			// CSV Daten in ein passendes Format bringen
			CSVImportConfigDialog dialog = new CSVImportConfigDialog(file);
			dialog.open();
			List<GenericObjectHashMap> csvdata = dialog.getCSVData();
			List<String> header = dialog.getCSVHeader();
			
			// FeldDefinitionen anwenden 
			ArrayList<FeldDefinitionen> fd = new ArrayList<FeldDefinitionen>();
			fd.add(new FeldDefinitionen("Datum", java.util.Date.class, "date", true));
			fd.add(new FeldDefinitionen("Kurs", BigDecimal.class, "last", true));
			CSVImportFeldDefinitionenDialog fdDialog = new CSVImportFeldDefinitionenDialog(fd, csvdata, header);
			fdDialog.open();
			List<GenericObjectHashMap> daten = fdDialog.getCSVData();
			
			// Und die letzte Umwandlung
			List<Datacontainer> dc = new ArrayList<Datacontainer>();
			for (GenericObjectHashMap x : daten) {
				Datacontainer obj = new Datacontainer((Map<String, Object>) x.getMap());
				obj.put("currency", "EUR");
				dc.add(obj);
			}
			setHistQuotes(dc);
			setHistEvents(new ArrayList<Datacontainer>());
		} catch (OperationCanceledException e) {
			
		}


	}

	@Override
	public void process(List<Config> options) {
	}





}
