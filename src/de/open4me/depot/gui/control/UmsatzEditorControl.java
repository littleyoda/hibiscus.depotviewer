package de.open4me.depot.gui.control;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import de.open4me.depot.Settings;
import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.datenobj.DepotAktion;
import de.open4me.depot.datenobj.rmi.Umsatz;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLQueries;
import de.open4me.depot.sql.SQLUtils;
import de.open4me.depot.tools.UmsatzHelper;
import de.open4me.depot.tools.VarDecimalFormat;
import de.willuhn.jameica.gui.AbstractControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Part;
import de.willuhn.jameica.gui.input.AbstractInput;
import de.willuhn.jameica.gui.input.CheckboxInput;
import de.willuhn.jameica.gui.input.DateInput;
import de.willuhn.jameica.gui.input.DecimalInput;
import de.willuhn.jameica.gui.input.Input;
import de.willuhn.jameica.gui.input.SelectInput;
import de.willuhn.jameica.gui.input.TextAreaInput;
import de.willuhn.jameica.gui.parts.TablePart;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.util.ApplicationException;

public class UmsatzEditorControl extends AbstractControl
{

	private Input betrag                       = null;
	private SelectInput aktion;
	private SelectInput wp;
	private DateInput datum;
	private SelectInput konto;
	private DecimalInput einzelkurs;
	private TablePart umsatzList;
	private DecimalInput gesamt;
	private DecimalInput kurswert;
	private CheckboxInput kurswertberechnen;
	private AbstractInput transaktionskosten;
	private DecimalInput steuern;
	private Umsatz umsatz = null;
	private Input kommentar;

	public UmsatzEditorControl(AbstractView view) throws Exception {
		super(view);
		if (view.getCurrentObject() == null) {
			getCBKurswertBerechnen().setValue(true);
			calc();
			return;
		}
		umsatz = Utils.getUmsatzByID(((GenericObjectSQL) view.getCurrentObject()).getID());
		getCBKurswertBerechnen().setValue(false);
		getAnzahl().setValue(umsatz.getAnzahl());
		getEinzelkurs().setValue(umsatz.getKurs());
		getDate().setValue(umsatz.getBuchungsdatum());
		getSteuern().setValue((umsatz.getSteuern() != null) ? umsatz.getSteuern() : 0.0d);
		getTransaktionskosten().setValue((umsatz.getTransaktionsgebuehren() != null) ? umsatz.getTransaktionsgebuehren()  : 0.0d);
		getKurswert().setValue(Math.abs(umsatz.getKosten().doubleValue()));
		getKommentar().setValue(umsatz.getKommentar());
		
		String id = umsatz.getWPid().toString();
		boolean found = false;
		for (Object o : getWertpapiere().getList()) {
			GenericObjectSQL obj = (GenericObjectSQL) o;
			if (obj.getID().equals(id)) {
				getWertpapiere().setValue(obj);
				found = true;
			}
		}
		if (!found) {
			throw new ApplicationException("Keine Änderungen möglich.\nZugehöriges Wertpapier nicht gefunden!");
		}

		found = false;
		Integer kontoid = umsatz.getKontoid();
		for (Object o : getKonto().getList()) {
			Konto k = (Konto) ((GenericObjectHashMap) o).getAttribute("kontoobj");
			if (kontoid.toString().equals(k.getID())) {
				getKonto().setValue(o);
				found = true;
			}
		}
		if (!found) {
			throw new ApplicationException("Keine Änderungen möglich.\nZugehöriges Konto nicht gefunden!");
		}

		getAktionAuswahl().setValue(umsatz.getAktion());
		calc();

	}

	/**
	 * Liefert das Eingabe-Feld fuer den Betrag.
	 * @return Eingabe-Feld.
	 * @throws RemoteException
	 */
	public Input getAnzahl() throws RemoteException
	{
		if (betrag != null)
			return betrag;
		double d = Double.NaN;
		betrag = new DecimalInput(d, new VarDecimalFormat(2, 3));
		betrag.setMandatory(true);
		betrag.addListener(new Listener() {

			@Override
			public void handleEvent(org.eclipse.swt.widgets.Event event) {
				calc();
			}

		});

		return betrag;
	}

	protected void calc()  {
		try {
			if ((Boolean) getCBKurswertBerechnen().getValue()) {
				getKurswert().setValue(null);
				getKurswert().setEnabled(false);
			} else {
				getKurswert().setEnabled(true);
			}
			getGesamtSumme().setValue(Double.NaN);
			if (getEinzelkurs().getValue() == null || getAnzahl().getValue() == null) {
				return;
			}
			if ((Double) getAnzahl().getValue() <=0 || ((Double) getEinzelkurs().getValue() < 0)) {
				return;
			}
			if ((Boolean) getCBKurswertBerechnen().getValue()) {
				Double d = (Double) getAnzahl().getValue() * (Double) getEinzelkurs().getValue();
				getKurswert().setValue(d);
			} else {
			}
			
			int faktor = -1;
			if (getAktionAuswahl().getValue().equals(DepotAktion.VERKAUF) || getAktionAuswahl().getValue().equals(DepotAktion.AUSBUCHUNG)) {
				faktor = 1;
			}
			Double d = faktor * (Double) getKurswert().getValue();
			if (getTransaktionskosten().getValue() != null) {
				d = d  - (Double) getTransaktionskosten().getValue();
			}
			if (getSteuern().getValue() != null) {
				d = d  - (Double) getSteuern().getValue();
			}
			getGesamtSumme().setValue(d);
		} catch (RemoteException re) {

		}

	}

	/**
	 * Liefert das Eingabe-Feld fuer den Betrag.
	 * @return Eingabe-Feld.
	 * @throws RemoteException
	 */
	public Input getEinzelkurs() throws RemoteException
	{
		if (einzelkurs != null)
			return einzelkurs;
		double d = Double.NaN;
		einzelkurs = new DecimalInput(d, new VarDecimalFormat(2, 3));
		einzelkurs.setMandatory(true);
		einzelkurs.addListener(new Listener() {

			@Override
			public void handleEvent(org.eclipse.swt.widgets.Event event) {
				calc();
			}

		});
		return einzelkurs;
	}

	public SelectInput getKonto() throws RemoteException, ApplicationException 
	{
		if (konto != null) {
			return konto;
		}
		konto = new SelectInput(Utils.getDepotKonten(), null);
		konto.setAttribute("bezeichnung");
		return konto;
	}

	public Input getAktionAuswahl() throws RemoteException 
	{
		if (aktion != null) {
			return aktion;
		}
		List<DepotAktion> liste = new ArrayList<DepotAktion>();
		liste.add(DepotAktion.KAUF);
		liste.add(DepotAktion.VERKAUF);
		liste.add(DepotAktion.EINBUCHUNG);
		liste.add(DepotAktion.AUSBUCHUNG);
		aktion = new SelectInput(liste, null);
		aktion.setMandatory(true);
		return aktion;
	}

	public DateInput getDate() throws RemoteException {
		if (datum != null) 
			return datum;
		datum = new DateInput();
		datum.setMandatory(true);
		return datum;
	}


	public SelectInput getWertpapiere() throws RemoteException 
	{
		if (wp != null) {
			return wp;
		}

		List<GenericObjectSQL> list = SQLQueries.getWertpapiere();
		wp = new SelectInput(list, null);
		wp.setMandatory(true);
		return wp;
	}


	public void handleStore() throws RemoteException, ApplicationException {
		int faktor = -1;
		if (getAktionAuswahl().getValue().equals(DepotAktion.VERKAUF) || getAktionAuswahl().getValue().equals(DepotAktion.AUSBUCHUNG)) {
			faktor = 1;
		}
		if (getEinzelkurs().getValue() == null || getAnzahl().getValue() == null || getDate().getValue() ==null) {
			throw new ApplicationException("Bitte vervollständigen Sie die Eingabe.");
		}
		if ((Double) getAnzahl().getValue() <=0 || ((Double) getEinzelkurs().getValue() < 0)) {
			throw new ApplicationException("Die Anzahl und der Kurs müssen positiv sein.");
		}
		if (umsatz == null) {
			umsatz = (Umsatz) Settings.getDBService().createObject(Umsatz.class,null);
			umsatz.setBuchungsinformationen("");
			umsatz.setOrderid("" + (((GenericObjectSQL) getWertpapiere().getValue()).getID() + getAktionAuswahl().getValue().toString() +
						getAnzahl().getValue().toString() + getEinzelkurs().getValue().toString() + "EUR" + getDate().getValue()
						).hashCode());
			umsatz.setKursW("EUR");
			umsatz.setKostenW("EUR");
			umsatz.setSteuernW("EUR");
			umsatz.setTransaktionsgebuehrenW("EUR");
		}
		Konto k = (Konto) ((GenericObjectHashMap) getKonto().getValue()).getAttribute("kontoobj");
		umsatz.setKontoid(Integer.parseInt(k.getID()));
		umsatz.setWPid(((GenericObjectSQL) getWertpapiere().getValue()).getID());
		umsatz.setAktion((DepotAktion) getAktionAuswahl().getValue());
		umsatz.setAnzahl(BigDecimal.valueOf((Double) getAnzahl().getValue()));
		umsatz.setKurs(BigDecimal.valueOf((Double) getEinzelkurs().getValue()));
		umsatz.setKosten(BigDecimal.valueOf(faktor * (Double) getKurswert().getValue()));
		umsatz.setBuchungsdatum((Date) getDate().getValue());
		umsatz.setSteuern(BigDecimal.valueOf((Double) getSteuern().getValue()));
		umsatz.setTransaktionsgebuehren(BigDecimal.valueOf((Double) getTransaktionskosten().getValue()));
		umsatz.setKommentar((String)getKommentar().getValue());
		umsatz.store();
		
		UmsatzHelper.storeUmsatzInHibiscus(umsatz);
	}

	public Part getBuchungen() throws RemoteException {
		if (umsatzList != null) {
			return umsatzList;
		}

		List<GenericObjectSQL> list = SQLUtils.getResultSet("select *, concat(zweck, ' ', zweck2, ' ', zweck3) as zweckconcat from umsatz"
				, "umsatz", "id");

		umsatzList = new TablePart(list, null);
		umsatzList.addColumn(Settings.i18n().tr("Art"), "art");
		umsatzList.addColumn(Settings.i18n().tr("Datum"), "datum");
		umsatzList.addColumn(Settings.i18n().tr("Betrag"),"betrag");
		umsatzList.addColumn(Settings.i18n().tr("Zweck"),"zweckconcat");
		//umsatzList.setContextMenu(new umsatzListMenu(orderList));
		return umsatzList;
	}

	public DecimalInput getGesamtSumme() {
		if (gesamt != null)
			return gesamt;
		double d = Double.NaN;
		gesamt = new DecimalInput(d, new VarDecimalFormat(2));
		gesamt.setMandatory(true);
		gesamt.setEnabled(false);
		return gesamt;
	}
	
	public Input getKommentar() {
		if (kommentar != null)
			return kommentar;
		kommentar = new TextAreaInput(null, 2000);
		((TextAreaInput)kommentar).setHeight(50);
		kommentar.setMandatory(false);
		return kommentar;
	}

	public Input getKurswert() {
		if (kurswert != null)
			return kurswert;
		double d = Double.NaN;
		kurswert = new DecimalInput(d, new VarDecimalFormat(2, 3));
		kurswert.setMandatory(true);
		return kurswert;
	}

	public Input getTransaktionskosten() {
		if (transaktionskosten != null)
			return transaktionskosten;
		double d = Double.valueOf("0");
		transaktionskosten = new DecimalInput(d, new VarDecimalFormat(2, 3));
		transaktionskosten.setMandatory(true);
		transaktionskosten.addListener(new Listener() {

			@Override
			public void handleEvent(org.eclipse.swt.widgets.Event event) {
				calc();
			}

		});
		return transaktionskosten;
	}

	public CheckboxInput getCBKurswertBerechnen() {
		if (kurswertberechnen != null)
			return kurswertberechnen;
		kurswertberechnen = new CheckboxInput(false);
		kurswertberechnen.addListener(new Listener() {

			@Override
			public void handleEvent(Event event) {
				calc();
			}
		});
		return kurswertberechnen;
	}

	public Input getSteuern() {
		if (steuern != null)
			return steuern;
		double d = Double.valueOf("0");
		steuern = new DecimalInput(d, new VarDecimalFormat(2, 3));
		steuern.setMandatory(true);
		steuern.addListener(new Listener() {

			@Override
			public void handleEvent(org.eclipse.swt.widgets.Event event) {
				calc();
			}

		});
		return steuern;
	}

}
