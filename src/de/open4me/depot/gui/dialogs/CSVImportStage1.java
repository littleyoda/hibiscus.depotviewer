package de.open4me.depot.gui.dialogs;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import de.open4me.depot.sql.GenericObjectHashMap;
import de.open4me.depot.tools.io.CSVImportTool;
import de.open4me.depot.tools.io.feldConverter.options.FeldConverterAuswahl;
import de.open4me.depot.tools.io.feldConverter.options.FeldConverterOption;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.dialogs.AbstractDialog;
import de.willuhn.jameica.gui.input.AbstractInput;
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
public class CSVImportStage1 extends AbstractDialog
{	  
	private Composite comp;
	private LabelInput error;
	private Button weiterbutton;
	private List<GenericObjectHashMap> list = new ArrayList<GenericObjectHashMap>();
	private List<String> header = new ArrayList<String>();
	private CSVImportTool tool;


	public CSVImportStage1(CSVImportTool tool)
	{
		super(POSITION_CENTER);
		setTitle("CSV Einstellungen");
		this.tool = tool;
	}


	/**
	 * @see de.willuhn.jameica.gui.dialogs.AbstractDialog#paint(org.eclipse.swt.widgets.Composite)
	 */
	protected void paint(Composite parent) throws Exception
	{
		Container group = new SimpleContainer(parent);
		group.addText("Bitte nehmen sie die notwendigen Einstellungen so vor, dass alle Daten der CSV-Datei inkl. korrekter Spaltennamen in der Tabelle unten angezeigt werden.", true);
		group.addText("Notwendige Einstellungen:", false);
		
		for (FeldConverterOption opt: tool.getCsvOptions()) {
				FeldConverterAuswahl aca = (FeldConverterAuswahl) opt;
				
				final AbstractInput c = new SelectInput(aca.getListe(), aca.getListe().get(0));
				c.addListener(new Listener() {

					@Override
					public void handleEvent(Event event) {
						aca.setAuswahl(c.getValue());
					}
					
				});
				group.addLabelPair(aca.getName(), c);
		}
		comp  = new Composite(parent,SWT.NONE);
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		comp.setLayout(new GridLayout());
		reload();
		SimpleContainer sc = new SimpleContainer(parent);
		sc.addInput(this.getError());

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
		group.addButtonArea(buttons);
	}

	
	
	private void reload() throws RemoteException {
		this.getError().setValue("");
		boolean enable = true;
		list.clear();
		header.clear();
		try
		{
			SWTUtil.disposeChildren(this.comp);
			comp.setLayoutData(new GridData(GridData.FILL_BOTH));
			this.comp.setLayout(new GridLayout());
			tool.load();
			list = tool.getList();
			header = tool.getHeader();
			if (tool.isIgnoredLines()) {
				getError().setValue("Die mit X markierten Zeilen werden ignoriert.");
			}


		}
		catch (Exception e)
		{
			Logger.error("unable to read file",e);
			this.getError().setValue("Fehler beim Lesen der Datei:\n" + e.getMessage());
			enable = false;
		}
		TablePart tab = new TablePart(list, null);
// TODO		tab.addColumn("" + headerline, "_DEPOTVIEWER_IDX");
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

	@Override
	protected Object getData() throws Exception {
		return list;
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
			return x.getName().toLowerCase().trim().replace(" ", "");
	}
	
}
