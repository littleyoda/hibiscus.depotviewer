package de.open4me.depot.gui.dialogs;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import de.open4me.depot.Settings;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.open4me.depot.tools.io.FeldDefinitionen;
import de.open4me.depot.tools.io.feldConverter.BigDecimalDezimaltrennzeichenKomma;
import de.open4me.depot.tools.io.feldConverter.BigDecimalDezimaltrennzeichenPunkt;
import de.open4me.depot.tools.io.feldConverter.FeldConverter;
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
import de.willuhn.jameica.gui.util.Container;
import de.willuhn.jameica.gui.util.SWTUtil;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.jameica.system.OperationCanceledException;
import de.willuhn.util.ApplicationException;

public class CSVImportFeldDefinitionenDialog extends AbstractDialog
{	  
	private Composite comp;
	private LabelInput error;
	private Button weiterbutton;
	List<GenericObjectHashMap> tablist = new ArrayList<GenericObjectHashMap>();
	private List<GenericObjectHashMap> quellDaten;
	private ArrayList<FeldDefinitionen> feldDefinitionen;
	private Map<FeldDefinitionen, AbstractInput> controls;
	private Map<FeldDefinitionen, AbstractInput> extendedControls;
	private Map<String, SimpleDateFormat> dateparser = new HashMap<String, SimpleDateFormat>();

	private FeldConverter[] fcBigDecimal = new FeldConverter[]{ new BigDecimalDezimaltrennzeichenKomma(), new BigDecimalDezimaltrennzeichenPunkt()}; 

	private List<String> header;



	public CSVImportFeldDefinitionenDialog(ArrayList<FeldDefinitionen> fd, List<GenericObjectHashMap> liste, List<String> header)
	{
		super(POSITION_CENTER);
		setTitle("CSV Felddefinitionen");
		this.feldDefinitionen = fd;
		this.quellDaten = liste;
		this.header = new ArrayList<String>();
		this.header.add("");
		this.header.addAll(header);
	}

	/**
	 * @see de.willuhn.jameica.gui.dialogs.AbstractDialog#paint(org.eclipse.swt.widgets.Composite)
	 */
	protected void paint(Composite parent) throws Exception
	{
		controls = new HashMap<FeldDefinitionen, AbstractInput>();
		extendedControls = new HashMap<FeldDefinitionen, AbstractInput>();
		Container group = new SimpleContainer(parent);
		group.addText("Bitte ordnen sie die einzelnen Spalten zu:", false);
		for (FeldDefinitionen x : feldDefinitionen) {
			AbstractInput control = new SelectInput(header, null);
			String desc = x.getBeschreibung();
			if (x.isRequired()) {
				desc = desc + " (*)";
			}
			group.addLabelPair(desc, control);
			controls.put(x, control);
			if (x.getFeldtype().equals(Date.class)) {
				control = new TextInput("yyyy-MM-dd");
				extendedControls.put(x, control);
				group.addLabelPair("Datumsformat (yyyy-MM-dd)", control);
			} else if (x.getFeldtype().equals(BigDecimal.class)) {
				control = new SelectInput(Arrays.asList(fcBigDecimal), null);
				extendedControls.put(x, control);
				group.addLabelPair("Zahlenformat", control);
			}

		}

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

		comp  = new Composite(parent,SWT.NONE);
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		comp.setLayout(new GridLayout());
		reload();
		SimpleContainer sc = new SimpleContainer(parent);
		sc.addInput(this.getError());

	}

	private void reload() throws RemoteException {
		this.getError().setValue("");
		tablist.clear();
		SWTUtil.disposeChildren(this.comp);
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		this.comp.setLayout(new GridLayout());

		transformiereDaten(tablist);
		TablePart tab = new TablePart(tablist, null);
		for (FeldDefinitionen h : feldDefinitionen) {
			if (h.getFeldtype().equals(Date.class)) {
				tab.addColumn(h.getAttr(), h.getAttr(), new DateFormatter(Settings.DATEFORMAT));
			} else {
				tab.addColumn(h.getAttr(), h.getAttr());
			}
		}
		tab.paint(comp);
		comp.layout(true);
	}

	private void transformiereDaten(List<GenericObjectHashMap> tablist) throws RemoteException {
		int fehler = 0;
		for (GenericObjectHashMap source: quellDaten) {
			GenericObjectHashMap g = new GenericObjectHashMap();
			for (Entry<FeldDefinitionen, AbstractInput> entry : controls.entrySet()) {
				String sourceattr = (String) entry.getValue().getValue();
				String destattr = entry.getKey().getAttr();
				
				// Prüfen, ob überhaupt eine Spalte zugewiesen wurde
				if (sourceattr.isEmpty()) {
					g.setAttribute(destattr, "N/A");
					fehler++;
					continue;
				}
				
				// Ursprungsdaten konvertieren
				Object sourcedata = source.getAttribute(sourceattr);
				Class<?> feldtype = entry.getKey().getFeldtype();
				try {
					AbstractInput ec = extendedControls.get(entry.getKey());
					if (feldtype.equals(BigDecimal.class)) {
						// Converter nutzen
						FeldConverter fc = (FeldConverter) ec.getValue();
						sourcedata = fc.convert(sourcedata.toString());
					} else if (feldtype.equals(Date.class)) {
						// Date-Format ermitteln
						SimpleDateFormat dp = getDateParser(ec.getValue().toString());
						// Sourcedaten entsprechend parsen
						sourcedata = dp.parse(sourcedata.toString());
					} else {
						sourcedata = "FELDTYPE UNBEKANNT!";
					}
				} catch (NumberFormatException | ParseException e) {
					fehler++;
					sourcedata = "ERROR: " + sourcedata.toString();
				}
				g.setAttribute(destattr, sourcedata);
			}
			tablist.add(g);
		}
		if (fehler == 0) {
			error.setValue("");
		} else {
			error.setValue("Fehlerhafte Zellen: " + fehler);
		}
		weiterbutton.setEnabled(fehler == 0);
	}


	private SimpleDateFormat getDateParser(String dateFormat) {
		SimpleDateFormat df = dateparser.get(dateFormat);
		if (df == null) {
			try {
				df = new SimpleDateFormat(dateFormat, Locale.GERMAN);
				dateparser.put(dateFormat, df);
			} catch (java.lang.IllegalArgumentException e) {
				//
			}
		}
		return df;
	}

	private SelectInput getSelectInput(String beschreibung, List list) {
		SelectInput selectInput = new SelectInput(list, null);
		selectInput.setName(beschreibung);
		selectInput.setMandatory(true);
		return selectInput;
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

}
