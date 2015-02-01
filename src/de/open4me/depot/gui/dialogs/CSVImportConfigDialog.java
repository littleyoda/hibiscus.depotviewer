package de.open4me.depot.gui.dialogs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import de.open4me.depot.sql.GenericObjectHashMap;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.dialogs.AbstractDialog;
import de.willuhn.jameica.gui.input.LabelInput;
import de.willuhn.jameica.gui.input.SelectInput;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.gui.parts.TablePart;
import de.willuhn.jameica.gui.util.Color;
import de.willuhn.jameica.gui.util.Container;
import de.willuhn.jameica.gui.util.SWTUtil;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.jameica.system.OperationCanceledException;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

@SuppressWarnings("rawtypes")
public class CSVImportConfigDialog extends AbstractDialog
{	  
	private SelectInput charset;
	private SelectInput trennzeichen;
	private SelectInput skiplines;
	private Composite comp;
	private LabelInput error;
	private Button weiterbutton;
	private List<GenericObjectHashMap> list = new ArrayList<GenericObjectHashMap>();
	private List<String> header = new ArrayList<String>();
	private File file;
	private String savename;


	public CSVImportConfigDialog(File file, String savename)
	{
		super(POSITION_CENTER);
		this.file = file;
		this.savename = "csvimportfile." + savename + ".";
		setTitle("CSV Einstellungen");
	}

	/**
	 * @see de.willuhn.jameica.gui.dialogs.AbstractDialog#paint(org.eclipse.swt.widgets.Composite)
	 */
	protected void paint(Composite parent) throws Exception
	{
		Container group = new SimpleContainer(parent);
		group.addText("Bitte nehmen sie die notwendigen Einstellungen so vor, dass alle Daten der CSV-Datei inkl. korrekter Spaltennamen in der Tabelle unten angezeigt werden.", true);
		group.addText("Notwendige Einstellungen:", false);

		group.addInput(getCharset());
		group.addInput(getTrennzeichen());
		group.addInput(getSkipLines());
		comp  = new Composite(parent,SWT.NONE);
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		comp.setLayout(new GridLayout());
		reload();
		SimpleContainer sc = new SimpleContainer(parent);
		sc.addInput(this.getError());

		//		group.addPart(getTab());

		ButtonArea buttons = new ButtonArea();
		buttons.addButton("Testen", new Action() {
			public void handleAction(Object context) throws ApplicationException {
				try {
					reload();
				} catch (RemoteException e) {
					throw new ApplicationException(e);
				}

			}

		},null,true,"ok.png");
		weiterbutton = new Button("Weiter", new Action() {
			public void handleAction(Object context) throws ApplicationException
			{
				close();
			}

		},null,false,"ok.png");
		weiterbutton.setEnabled(false);
		buttons.addButton(weiterbutton);
		buttons.addButton("Abbrechen", new Action() {
			public void handleAction(Object context) throws ApplicationException
			{
				throw new OperationCanceledException("Abgebrochen");
			}

		},null,false,"process-stop.png");
		buttons.addButton("Einstellungen speichern", new Action() {
			public void handleAction(Object context) throws ApplicationException
			{
				try {
					PreparedStatement pre = SQLUtils.getPreparedSQL("delete from depotviewer_cfg where key like concat(?,'%')");
					pre.setString(1, savename);
					pre.execute();

					for (SelectInput x : new SelectInput[] { charset, trennzeichen, skiplines }) {
						SQLUtils.saveCfg(getSaveKey(x), x.getValue().toString());
					}
				} catch (Exception e) {
					Logger.error("Fehler beim Löschen der Pref", e);
				}

			}

		},null,false,"document-save.png");
		group.addButtonArea(buttons);
	}

	private void reload() throws RemoteException {
		this.getError().setValue("");
		boolean enable = true;
		list.clear();
		header.clear();
		Integer headerline = ((Integer) getSkipLines().getValue());
		try
		{
			SWTUtil.disposeChildren(this.comp);
			comp.setLayoutData(new GridData(GridData.FILL_BOTH));
			this.comp.setLayout(new GridLayout());
			 BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName((String) getCharset().getValue())));
			 String line = null;
			 int counter = 0;
			 while ((line = br.readLine()) != null && counter < 30) {
				 GenericObjectHashMap g = new GenericObjectHashMap();
				 g.setAttribute("Dateiinhalt", line);
						list.add(g);
				counter++;
			 }
			 header.add("Dateiinhalt");
			 br.close();
			 

			FileInputStream is = new FileInputStream(file);
			InputStreamReader isr = new InputStreamReader(is, Charset.forName((String) getCharset().getValue()) );
			CSVFormat format = CSVFormat.RFC4180
					.withDelimiter(((String) getTrennzeichen().getValue()).charAt(0))
					.withSkipLines(headerline - 1);
			CSVParser parser = new CSVParser(isr, format);
			list.clear();
			boolean iserror = false;
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
						iserror = true;
						continue;
					}
					g.setAttribute(header.get(i), record.get(i));
					
				}
				list.add(g);
			}
			if (iserror) {
				getError().setValue("Die mit X markierten Zeilen werden ignoriert.");
			}
			parser.close();

		}
		catch (Exception e)
		{
			Logger.error("unable to read file",e);
			this.getError().setValue("Fehler beim Lesen der Datei:\n" + e.getMessage());
			enable = false;
		}
		TablePart tab = new TablePart(list, null);
		tab.addColumn("" + headerline, "_DEPOTVIEWER_IDX");
		tab.addColumn("", "_DEPOTVIEWER_IGNORE");
		for (String h : header) {
			tab.addColumn(h, h);
		}
		tab.paint(comp);
		comp.layout(true);
		if (weiterbutton != null) {
			weiterbutton.setEnabled(enable);
		}

	}

	private SelectInput getCharset() {
		if (charset == null) {
			charset = getSelectInput("Charset", Arrays.asList( 
					new String[] { "WINDOWS-1252", "ISO-8859-1", "ISO-8859-15", "US-ASCII", "UTF-8", "UTF-16BE", "UTF-16LE", "UTF-16" }));
		}
		return charset;

	}

	private SelectInput getTrennzeichen() {
		if (trennzeichen == null) {
			trennzeichen = getSelectInput("Trennzeichen", Arrays.asList( 
					new String[] { ";", ",", "|", "\t" }));
		}
		return trennzeichen;
	}

	private SelectInput getSkipLines() {
		if (skiplines == null) {
			skiplines = getSelectInput("Zeile mit den Spaltennamen", Arrays.asList( 
					new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9,10 }));
		}
		return skiplines;
	}

	private SelectInput getSelectInput(String beschreibung, List list) {
		SelectInput selectInput = new SelectInput(list, null);
		selectInput.setName(beschreibung);
		selectInput.setMandatory(true);
		Object auswahl = SQLUtils.getCfg(getSaveKey(selectInput));
		for (Object o : list) {
			if (o.toString().equals(auswahl)) {
				selectInput.setValue(o);
			}
		}
		return selectInput;
	}



	@Override
	protected Object getData() throws Exception {
		return list;
	}

	public List<GenericObjectHashMap> getCSVData() {
		return list;
	}

	public List<String> getCSVHeader() {
		return header;
	}

	/**
	 * Liefert ein Label mit einer Fehlermeldung.
	 * @return Label.
	 */
	private LabelInput getError()
	{
		if (this.error == null)
		{
			this.error = new LabelInput("\n\n\n");
			this.error.setName("");
			this.error.setColor(Color.ERROR);
		}
		return this.error;
	}

	private String getSaveKey(SelectInput x) {
			return savename + x.getName().toLowerCase().trim().replace(" ", "");
	}
	
}
