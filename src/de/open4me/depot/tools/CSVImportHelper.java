package de.open4me.depot.tools;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;


import de.open4me.depot.gui.dialogs.CSVImportConfigDialog;
import de.open4me.depot.gui.dialogs.CSVImportFeldDefinitionenDialog;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.open4me.depot.sql.SQLUtils;
import de.open4me.depot.tools.io.FeldDefinitionen;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.system.OperationCanceledException;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

public class CSVImportHelper {

	private HashMap<String, String> options;
	private String fullname;

	public CSVImportHelper(String savename) {
		fullname = "csv." + savename + ".";
	}


	public List<GenericObjectHashMap> run(ArrayList<FeldDefinitionen> fd) throws Exception {
		File file = getFilename();
		if (file == null) {
			return null;
		}
		options = new HashMap<String, String>();
		load(options, fullname);
		// CSV Daten in ein passendes Format bringen
		CSVImportConfigDialog dialog = new CSVImportConfigDialog(file, options);
		try {
			dialog.open();
		} catch (OperationCanceledException e) {
			return null;
		}
		List<GenericObjectHashMap> csvdata = dialog.getCSVData();


		// Und nun die Daten der CSV Datei den gewünschten Feldern zu weisen
		List<String> header = dialog.getCSVHeader();
		CSVImportFeldDefinitionenDialog fdDialog = new CSVImportFeldDefinitionenDialog(fd, csvdata, header, options);
		try {
			fdDialog.open();
		} catch (OperationCanceledException e) {
			return null;
		}
		if (fdDialog.isSave()) {
			save();
		}
		return fdDialog.getCSVData();

	}

	private void load(HashMap<String, String> options2, String fullname) {
		try {
			PreparedStatement pre = SQLUtils.getPreparedSQL("select key, value from depotviewer_cfg where key like concat(?,'%')");
			pre.setString(1, fullname);
			ResultSet ret =  pre.executeQuery();
			while (ret.next()) {
				options.put(ret.getString(1).replace(fullname, ""), ret.getString(2));
			}
		} catch (Exception e) {
			Logger.error("Fehler beim Zugriff auf cfg", e);
			e.printStackTrace();
		}
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

	public void save() {
		System.out.println(options);
		try {
			PreparedStatement pre = SQLUtils.getPreparedSQL("delete from depotviewer_cfg where key like concat(?,'%')");
			pre.setString(1, fullname);
			pre.execute();
			for (Entry<String, String> x : options.entrySet()) {
				SQLUtils.saveCfg(fullname + x.getKey(), x.getValue());
			}
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("Error", e);
		}					
	}
}
