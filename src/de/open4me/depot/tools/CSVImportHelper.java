package de.open4me.depot.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.StringSubstitutor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

import de.open4me.depot.gui.dialogs.CSVImportStage1;
import de.open4me.depot.gui.dialogs.CSVImportStage2;
import de.open4me.depot.gui.dialogs.WWWLinksDialog;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.open4me.depot.sql.SQLUtils;
import de.open4me.depot.tools.io.CSVImportTool;
import de.open4me.depot.tools.io.FeldDefinitionen;
import de.open4me.depot.tools.io.feldConverter.options.FeldConverterAuswahl;
import de.open4me.depot.tools.io.feldConverter.options.FeldConverterOption;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.hbci.gui.dialogs.DebugDialog;
import de.willuhn.jameica.system.OperationCanceledException;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

public class CSVImportHelper {

	private String optionsPrefix;
	private int sourceType;
	private String source;

	public CSVImportHelper(String savename, int source) {
		optionsPrefix = "csv." + savename + "." + source + ".";
		this.sourceType = source;
	}


	public List<GenericObjectHashMap> run(ArrayList<FeldDefinitionen> fd, boolean headless) throws Exception {
		CSVImportTool tool = new CSVImportTool(fd);
		loadOptions(tool);
		File file  = null;
		if (sourceType == 0) {
			file = getFilename();
			if (file == null) {
				return null;
			}
			source = file.getCanonicalPath();
		} else {

			//source = "xhttps://www.ariva.de/quote/historic/historic.csv?secu=101542057&boerse_id=8&clean_split=1&clean_payout=1&clean_bezug=1&currency=EUR&min_time=1.1.2000&max_time=2.9.2020&trenner=%3B&go=Download";
			Map<String, Object> valuesMap = new HashMap<String, Object>();
			LocalDate now = LocalDate.now();
			valuesMap.put("day", Integer.toString(now.getDayOfMonth()));
			valuesMap.put("month", Integer.toString(now.getMonthValue()));
			valuesMap.put("year", Integer.toString(now.getYear()));
			StringSubstitutor sub = new StringSubstitutor(valuesMap);

			WWWLinksDialog dialog = new WWWLinksDialog(DebugDialog.POSITION_CENTER, "", source, sub,valuesMap);
			source = (String) dialog.open();

			String subsource = sub.replace(source);
			Logger.info("Url für CSV-Abruf: " + subsource);
			URL url = new URL(subsource);
			ReadableByteChannel rbc = Channels.newChannel(url.openStream());
			file = File.createTempFile("depotviewer-", "csv");
			FileOutputStream fos = new FileOutputStream(file);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			file.deleteOnExit();
			fos.close();
		}
		tool.setFile(file);
		Logger.debug("csv-Headless: " + headless);
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
				String key = optionsPrefix + "csv." + x.getId();
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
			String key = optionsPrefix + "base.source";
			SQLUtils.saveCfg(key, source);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("Error", e);
		}					
	}

	private void loadOptions(CSVImportTool tool) {
		try {
			for (FeldConverterAuswahl<?> x : tool.getCsvOptions()) {
				String key = optionsPrefix + "csv." + x.getId();
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
			String key = optionsPrefix + "base.source";
			source = SQLUtils.getCfg(key);
		} catch (Exception e) {
			Logger.error("Fehler beim Zugriff auf cfg", e);
			e.printStackTrace();
		}
	}

}
