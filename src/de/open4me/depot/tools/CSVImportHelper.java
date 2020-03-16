package de.open4me.depot.tools;

import java.io.File;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

import de.open4me.depot.gui.dialogs.CSVImportStage1;
import de.open4me.depot.gui.dialogs.CSVImportStage2;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.open4me.depot.sql.SQLUtils;
import de.open4me.depot.tools.io.CSVImportTool;
import de.open4me.depot.tools.io.FeldDefinitionen;
import de.open4me.depot.tools.io.feldConverter.options.FeldConverterAuswahl;
import de.open4me.depot.tools.io.feldConverter.options.FeldConverterOption;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.system.OperationCanceledException;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

public class CSVImportHelper {

	private String optionsPrefix;

	public CSVImportHelper(String savename) {
		optionsPrefix = "csv." + savename + ".";
	}


	public List<GenericObjectHashMap> run(ArrayList<FeldDefinitionen> fd, boolean headless) throws Exception {
		File file = getFilename();
		if (file == null) {
			return null;
		}
		CSVImportTool tool = new CSVImportTool(file, fd);
		loadOptions(tool);
		Logger.debug("CVS-Headless: " + headless);
		if (!headless) {
			// 	CSV Daten in ein passendes Format bringen
			CSVImportStage1 dialog = new CSVImportStage1(tool);
			try {
				dialog.open();
			} catch (OperationCanceledException e) {
				return null;
			}
		}
		
		// Und nun die Daten der CSV Datei den gewünschten Feldern zu weisen
		CSVImportStage2 fdDialog = new CSVImportStage2(fd, tool);
		try {
			fdDialog.open();
		} catch (OperationCanceledException e) {
			return null;
		}
		if (fdDialog.isSave()) {
			saveOptions(tool);
		}
		return fdDialog.getCSVData();

	}



	public File getFilename() throws ApplicationException {
		FileDialog fileopen = new FileDialog(GUI.getShell(),SWT.OPEN);
		fileopen.setText("Bitte wählen Sie die CSV Datei aus:");
		String filename;
		try {
			filename = fileopen.open();
		} catch (OperationCanceledException e) {
			return null;
		}
		if (filename == null || filename.isEmpty()) {
			return null;
		}
		File file = new File(filename);
		if (!file.exists() || !file.isFile()) {
			throw new ApplicationException("Datei existiert nicht oder ist nicht lesbar");
		}
		return file;
	}

	public void saveOptions(CSVImportTool tool) {
		try {
			PreparedStatement pre = SQLUtils.getPreparedSQL("delete from depotviewer_cfg where key like concat(?,'%')");
			pre.setString(1, optionsPrefix);
			pre.execute();

			
			for (FeldConverterAuswahl<?> x : tool.getCsvOptions()) {
				String key = optionsPrefix + "cvs." + x.getId();
				String value = x.getAuswahl().toString();
				SQLUtils.saveCfg(key, value);
			}
			for (FeldDefinitionen x : tool.getFeldDefinitionen()) {
				String prefix = optionsPrefix + x.getAttr() + ".";
				SQLUtils.saveCfg(prefix + "_spalte", x.getSpalte());
				for (FeldConverterOption<?> o : x.getConverters().getOptions()) {
					SQLUtils.saveCfg(prefix + o.getId(), o.getAuswahl().toString());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("Error", e);
		}					
	}
	
	private void loadOptions(CSVImportTool tool) {
		try {
			for (FeldConverterAuswahl<?> x : tool.getCsvOptions()) {
				String key = optionsPrefix + "cvs." + x.getId();
				String out = SQLUtils.getCfg(key);
				x.setAuswahlByText(out);
			}
			for (FeldDefinitionen x : tool.getFeldDefinitionen()) {
				String prefix = optionsPrefix + x.getAttr() + ".";
				String out = SQLUtils.getCfg(prefix + "_spalte");
				x.setSpalte(out);
				
				for (FeldConverterOption<?> o : x.getConverters().getOptions()) {
					out = SQLUtils.getCfg(prefix + o.getId());
					o.setAuswahlByText(out);
				}
			}

		} catch (Exception e) {
			Logger.error("Fehler beim Zugriff auf cfg", e);
			e.printStackTrace();
		}
	}
	
}
