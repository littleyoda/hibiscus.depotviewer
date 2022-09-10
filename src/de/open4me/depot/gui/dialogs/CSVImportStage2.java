package de.open4me.depot.gui.dialogs;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.parts.ReplaceableComposite;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.open4me.depot.tools.io.CSVImportTool;
import de.open4me.depot.tools.io.FeldDefinitionen;
import de.open4me.depot.tools.io.feldConverter.options.FeldConverterAuswahl;
import de.open4me.depot.tools.io.feldConverter.options.FeldConverterOption;
import de.open4me.depot.tools.io.feldConverter.options.FeldConverterText;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.dialogs.AbstractDialog;
import de.willuhn.jameica.gui.formatter.DateFormatter;
import de.willuhn.jameica.gui.input.AbstractInput;
import de.willuhn.jameica.gui.input.LabelInput;
import de.willuhn.jameica.gui.input.SelectInput;
import de.willuhn.jameica.gui.input.TextInput;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.gui.parts.TablePart;
import de.willuhn.jameica.gui.util.Color;
import de.willuhn.jameica.gui.util.ScrolledContainer;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.jameica.system.OperationCanceledException;
import de.willuhn.util.ApplicationException;


public class CSVImportStage2 extends AbstractDialog
{	  
	private LabelInput error;
	private Button weiterbutton;
	List<GenericObjectHashMap> tablist = new ArrayList<GenericObjectHashMap>();
	private ArrayList<FeldDefinitionen> feldDefinitionen;
	//	private Map<FeldDefinitionen, AbstractInput> controls;

	private List<String> header;
	private ReplaceableComposite rc;
	//	private Map<String, String> options;
	private CSVImportTool tool;
	private boolean save = false;

	public CSVImportStage2(ArrayList<FeldDefinitionen> fd, CSVImportTool tool) {
		super(POSITION_CENTER);
		setTitle("CSV Felddefinitionen");

		this.feldDefinitionen = fd;
		this.tool = tool;
		this.header = new ArrayList<String>();
		this.header.add("");
		this.header.addAll(tool.getHeader());
	}

	/**
	 * @see de.willuhn.jameica.gui.dialogs.AbstractDialog#paint(org.eclipse.swt.widgets.Composite)
	 */
	protected void paint(Composite parent) throws Exception
	{
		//		controls = new HashMap<FeldDefinitionen, AbstractInput>();

		SimpleContainer columns = new SimpleContainer(parent, true, 2);
		ScrolledContainer left = new ScrolledContainer(columns.getComposite());
		SimpleContainer right = new SimpleContainer(columns.getComposite());

		left.addText("Bitte ordnen Sie die einzelnen Spalten zu:", false);

//		ArrayList<Currency> currencies = new ArrayList<Currency>(Currency.getAvailableCurrencies());
//		Collections.sort(currencies, new Comparator<Currency>() {
//
//			@Override
//			public int compare(Currency o1, Currency o2) {
//				return o1.toString().compareTo(o2.toString());
//			}
//
//		});

		left.addSeparator();
		for (FeldDefinitionen x : feldDefinitionen) {
			AbstractInput control = new SelectInput(header, x.getSpalte()); 
			control.addListener(new Listener() {

				@Override
				public void handleEvent(Event event) {
					x.setSpalte((String) control.getValue());
				}

			});
			String desc = x.getBeschreibung();
			if (x.isRequired()) {
				desc = desc + " (*)";
			}
			left.addLabelPair(desc, control);
			//			controls.put(x, control);

			for (FeldConverterOption opt: x.getConverters().getOptions()) {
				if (opt instanceof FeldConverterAuswahl) {
					FeldConverterAuswahl aca = (FeldConverterAuswahl) opt;

					final AbstractInput c = new SelectInput(aca.getListe(), aca.getAuswahl());
					c.addListener(new Listener() {

						@Override
						public void handleEvent(Event event) {
							aca.setAuswahl(c.getValue());
						}

					});
					left.addLabelPair(aca.getName(), c);
				} else if (opt instanceof FeldConverterText) {
					FeldConverterText aca = (FeldConverterText) opt;
					final TextInput c = new TextInput(aca.getAuswahl());
					c.addListener(new Listener() {

						@Override
						public void handleEvent(Event event) {
							aca.setAuswahl((String) c.getValue());
						}

					});
					left.addLabelPair(aca.getName(), c);
				} else {
					throw new Exception("Mist");
				}
			}
			left.addSeparator();
		}

		ButtonArea buttons = new ButtonArea();
		buttons.addButton("Testen", new Action() {
			public void handleAction(Object context) throws ApplicationException {
				try {
					reload();
					for (FeldConverterAuswahl<?> x : tool.getCsvOptions()) {
						System.out.println("CSV Config " + x.getName() + " => " + x.getAuswahl());
					}
					for (FeldDefinitionen x : tool.getFeldDefinitionen()) {
						System.out.println(x.getAttr());
						for (FeldConverterOption y : x.getConverters().getOptions()) {
							System.out.println(y.getName() + " " +  y.getAuswahl());
						}
					}
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
				save = true;
			}

		},null,false,"document-save.png");
		right.addButtonArea(buttons);
		rc = new ReplaceableComposite(right.getComposite(), SWT.None);
		reload();
		right.addInput(getError());

	}


	private void reload() throws RemoteException {
		getError().setValue("");
		int fehler = tool.transformiereDaten(tablist);
		TablePart tab = new TablePart(tablist, null);
		for (FeldDefinitionen h : feldDefinitionen) {
			if (h.getFeldtype().equals(Date.class)) {
				tab.addColumn(h.getAttr(), h.getAttr(), new DateFormatter(Settings.DATEFORMAT));
			} else {
				tab.addColumn(h.getAttr(), h.getAttr());
			}
		}
		if (fehler == 0) {
			getError().setValue("");
		} else {
			getError().setValue("Fehlerhafte Zellen: " + fehler);
		}
		weiterbutton.setEnabled(fehler == 0);

		rc.replace(tab);
	}



	@Override
	protected Object getData() throws Exception {
		return tablist;
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

	public List<GenericObjectHashMap> getCSVData() {
		return tablist;
	}

	public boolean isSave() {
		return save;
	}

	public void setSave(boolean save) {
		this.save = save;
	}

	private String getName(AbstractInput inp) {
		return inp.getName().toLowerCase().trim().replace(" ", "");
	}

	public ArrayList<FeldDefinitionen> getFeldDefinitionen() {
		return feldDefinitionen;
	}


}
