package de.open4me.depot.gui.dialogs;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.sql.PreparedStatement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import de.open4me.depot.Settings;
import de.open4me.depot.datenobj.DepotAktion;
import de.open4me.depot.datenobj.rmi.BigDecimalWithCurrency;
import de.open4me.depot.gui.parts.ReplaceableComposite;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.open4me.depot.sql.SQLUtils;
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
import de.willuhn.jameica.gui.util.ScrolledContainer;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.jameica.system.OperationCanceledException;
import de.willuhn.util.ApplicationException;


// TODO: Trennung in View und Controller
public class CSVImportFeldDefinitionenDialog extends AbstractDialog
{	  
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
	private ReplaceableComposite rc;
	private String savename;
	private SelectInput zahlenFormat;
	private TextInput datumsFormat;
	private SelectInput waehrung;



	public CSVImportFeldDefinitionenDialog(ArrayList<FeldDefinitionen> fd, List<GenericObjectHashMap> liste, List<String> header, String savename)
	{
		super(POSITION_CENTER);
		setTitle("CSV Felddefinitionen");
		this.feldDefinitionen = fd;
		this.savename = "csvimport." + savename + ".";
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

		

		SimpleContainer columns = new SimpleContainer(parent, true, 2);
		ScrolledContainer left = new ScrolledContainer(columns.getComposite());
		SimpleContainer right = new SimpleContainer(columns.getComposite());
		
		left.addText("Bitte ordnen sie die einzelnen Spalten zu:", false);
		zahlenFormat = new SelectInput(Arrays.asList(fcBigDecimal), null);
		left.addLabelPair("Format für Zahlen", zahlenFormat);
		
		datumsFormat = new TextInput("dd.MM.yyyy");
		left.addLabelPair("Format für Datum (yyyy-MM-dd)", datumsFormat);

		Locale locale = Locale.getDefault();
		ArrayList<Currency> currencies = new ArrayList<Currency>(Currency.getAvailableCurrencies());
		Collections.sort(currencies, new Comparator<Currency>() {

			@Override
			public int compare(Currency o1, Currency o2) {
				return o1.toString().compareTo(o2.toString());
			}
			
		});
		waehrung = new SelectInput(currencies, Currency.getInstance(locale));
		left.addLabelPair("Standard Währung", waehrung);

		left.addSeparator();
		for (FeldDefinitionen x : feldDefinitionen) {
//			String saved = SQLUtils.getCfg(getSaveKey(x, "ext"));
//			if (saved == null) {
//				saved = "";
//			}
			AbstractInput control = new SelectInput(header, SQLUtils.getCfg(getSaveKey(x, "")));
			String desc = x.getBeschreibung();
			if (x.isRequired()) {
				desc = desc + " (*)";
			}
			left.addLabelPair(desc, control);
			controls.put(x, control);
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
		buttons.addButton("Einstellungen speichern", new Action() {
			public void handleAction(Object context) throws ApplicationException
			{
				try {
				PreparedStatement pre = SQLUtils.getPreparedSQL("delete from depotviewer_cfg where key like concat(?,'%')");
				pre.setString(1, savename);
				pre.execute();
				for (Entry<FeldDefinitionen, AbstractInput> x : controls.entrySet()) {
					Object value = x.getValue().getValue();
					if (value != null) {
							SQLUtils.saveCfg(getSaveKey(x.getKey(), ""), value.toString());
					}
				}
				for (Entry<FeldDefinitionen, AbstractInput> x : extendedControls.entrySet()) {
					Object value = x.getValue().getValue();
					if (value != null) {
							SQLUtils.saveCfg(getSaveKey(x.getKey(), "ext"), value.toString());
					}
				}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}					
			}

		},null,false,"document-save.png");
		right.addButtonArea(buttons);
		rc = new ReplaceableComposite(right.getComposite(), SWT.None);
		reload();
		right.addInput(getError());

	}

	private String getSaveKey(FeldDefinitionen x, String erweitert) {
		if (erweitert.isEmpty()) {
			return savename + x.getAttr();
		}
		return savename + erweitert + "." + x.getAttr(); 
	}
	
	private void reload() throws RemoteException {
		this.getError().setValue("");
		transformiereDaten(tablist);
		TablePart tab = new TablePart(tablist, null);
		for (FeldDefinitionen h : feldDefinitionen) {
			if (h.getFeldtype().equals(Date.class)) {
				tab.addColumn(h.getAttr(), h.getAttr(), new DateFormatter(Settings.DATEFORMAT));
			} else {
				tab.addColumn(h.getAttr(), h.getAttr());
			}
		}
		rc.replace(tab);
	}
	

	private void transformiereDaten(List<GenericObjectHashMap> tablist) throws RemoteException {
		tablist.clear();
		int fehler = 0;
		for (GenericObjectHashMap source: quellDaten) {
			if (!source.getAttribute("_DEPOTVIEWER_IGNORE").toString().isEmpty()) {
				continue;
			}
			GenericObjectHashMap g = new GenericObjectHashMap();
			for (Entry<FeldDefinitionen, AbstractInput> entry : controls.entrySet()) {
				boolean required = entry.getValue().isMandatory();
				String sourceattr = (String) entry.getValue().getValue();
				String destattr = entry.getKey().getAttr();
				
				// Prüfen, ob überhaupt eine Spalte zugewiesen wurde
				if (sourceattr.isEmpty() ) {
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
				Class<?> feldtype = entry.getKey().getFeldtype();
				try {
//					AbstractInput ec = extendedControls.get(entry.getKey());
					if (feldtype.equals(BigDecimal.class)) {
						// Converter nutzen
						FeldConverter fc = (FeldConverter) zahlenFormat.getValue();
						sourcedata = fc.convert(sourcedata.toString());
					} else if (feldtype.equals(BigDecimalWithCurrency.class)) {
							FeldConverter fc = (FeldConverter) zahlenFormat.getValue();
							String data = sourcedata.toString().trim();
							int spaceCount = StringUtils.countMatches(data, " ");
							if (spaceCount == 0) {
								// Vermutlich nur eine Zahl
								sourcedata = fc.convert(data);
							} else if (spaceCount == 1) {
								// Zahl mit Währung
								String[] splitted = data.split(" ");
								BigDecimalWithCurrency bdwc = new BigDecimalWithCurrency((BigDecimal) fc.convert(splitted[0]), splitted[1]);
								sourcedata = bdwc;
							} else {
								// Augen zu und durch. Ich habe keine Ahnung, was es sein kann.
								sourcedata = fc.convert(data);
							}
					} else if (feldtype.equals(Date.class)) {
						// Date-Format ermitteln
						SimpleDateFormat dp = getDateParser(datumsFormat.getValue().toString());
						// Sourcedaten entsprechend parsen
						sourcedata = dp.parse(sourcedata.toString());
					} else if (feldtype.equals(DepotAktion.class)) {
						sourcedata = DepotAktion.getByString(sourcedata.toString());
					} else if (feldtype.equals(String.class)) {
						//sourcedata = sourcedata.toString();
					} else {
						sourcedata = "FELDTYPE UNBEKANNT!";
					}
				} catch (NumberFormatException | ParseException e) {
					fehler++;
					sourcedata = "ERROR: " + sourcedata.toString();
				}
				g.setAttribute(destattr, sourcedata);
			}
			g.setAttribute("_depotviewer_default_curr", waehrung.getValue().toString());
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
