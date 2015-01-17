package de.open4me.depot.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

import de.open4me.depot.gui.dialogs.CSVImportConfigDialog;
import de.open4me.depot.gui.dialogs.CSVImportFeldDefinitionenDialog;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.open4me.depot.tools.io.FeldDefinitionen;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.system.OperationCanceledException;
import de.willuhn.util.ApplicationException;

public class CSVImportHelper {

	public static List<GenericObjectHashMap> run(ArrayList<FeldDefinitionen> fd) throws Exception {
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

		// CSV Daten in ein passendes Format bringen
		CSVImportConfigDialog dialog = new CSVImportConfigDialog(file);
		try {
			dialog.open();
		} catch (OperationCanceledException e) {
			return null;
		}

		List<GenericObjectHashMap> csvdata = dialog.getCSVData();
		List<String> header = dialog.getCSVHeader();

		CSVImportFeldDefinitionenDialog fdDialog = new CSVImportFeldDefinitionenDialog(fd, csvdata, header);
		fdDialog.open();
		return fdDialog.getCSVData();

	}

}
