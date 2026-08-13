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
import de.open4me.depot.tools.InconsistencyData;
import de.open4me.depot.tools.UmsatzHelper;
import de.open4me.depot.tools.VarDecimalFormat;
import de.open4me.depot.tools.Zahlen;
import de.willuhn.jameica.gui.AbstractControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Part;
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

	private DecimalInput betrag                = null;
	private SelectInput aktion;
	private SelectInput wp;
	private DateInput datum;
	private SelectInput konto;
	private DecimalInput einzelkurs;
	private TablePart umsatzList;
	private DecimalInput gesamt;
	private DecimalInput kurswert;
	private CheckboxInput kurswertberechnen;
	private DecimalInput transaktionskosten;
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
		
		// Prüfe ob es sich um InconsistencyData handelt (neuer Umsatz basierend auf Inkonsistenz)
		if (view.getCurrentObject() instanceof InconsistencyData) {
			initializeFromInconsistency((InconsistencyData) view.getCurrentObject());
			return;
		}
		
		// Bestehender Umsatz wird bearbeitet
		umsatz = Utils.getUmsatzByID(((GenericObjectSQL) view.getCurrentObject()).getID());
		getCBKurswertBerechnen().setValue(false);
		getAnzahl().setValue(umsatz.getAnzahl());
		getEinzelkurs().setValue(umsatz.getKurs());
		getDate().setValue(umsatz.getBuchungsdatum());
		getSteuern().setValue((umsatz.getSteuern() != null) ? umsatz.getSteuern() : BigDecimal.ZERO);
		getTransaktionskosten().setValue((umsatz.getTransaktionsgebuehren() != null) ? umsatz.getTransaktionsgebuehren()  : BigDecimal.ZERO);
		getKurswert().setValue(umsatz.getKosten().abs());
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
	public DecimalInput getAnzahl() throws RemoteException
	{
		if (betrag != null)
			return betrag;
		// 8 statt 6 optionale Nachkommastellen: Stueckzahlen liegen als
		// decimal(20,10) in der Datenbank, die Anzeige darf nicht frueher runden.
		betrag = new DecimalInput((Number) null, new VarDecimalFormat(2, 8));
		betrag.setMandatory(true);
		betrag.addListener(new Listener() {

			@Override
			public void handleEvent(org.eclipse.swt.widgets.Event event) {
				calc();
			}

		});

		return betrag;
	}

	/**
	 * Liest den Wert eines Eingabefeldes als BigDecimal.
	 *
	 * Bewusst über {@link DecimalInput#getNumber()} statt getValue():
	 * getValue() wandelt intern immer nach Double und wäre verlustbehaftet.
	 * Dank {@link VarDecimalFormat} (setParseBigDecimal) liefert getNumber()
	 * bereits einen BigDecimal; {@link Zahlen#toBigDecimal(Object)} deckt nur
	 * Alt-/Sonderfälle ab.
	 *
	 * @return Wert oder null, wenn das Feld leer bzw. nicht lesbar ist
	 */
	private static BigDecimal toDecimal(DecimalInput input) {
		Number n = input.getNumber();
		if (n == null) {
			return null;
		}
		// NaN/Infinity kaeme aus einem leeren bzw. kaputten Feld und liesse sich
		// nicht in einen BigDecimal ueberfuehren - das gilt als "kein Wert".
		double d = n.doubleValue();
		if (Double.isNaN(d) || Double.isInfinite(d)) {
			return null;
		}
		return Zahlen.toBigDecimal(n);
	}

	protected void calc()  {
		try {
			if ((Boolean) getCBKurswertBerechnen().getValue()) {
				getKurswert().setValue(null);
				getKurswert().setEnabled(false);
			} else {
				getKurswert().setEnabled(true);
			}
			getGesamtSumme().setValue(null);
			BigDecimal anzahl = toDecimal(getAnzahl());
			BigDecimal kurs = toDecimal(getEinzelkurs());
			if (kurs == null || anzahl == null) {
				return;
			}
			if (anzahl.signum() <= 0 || kurs.signum() < 0) {
				return;
			}
			if ((Boolean) getCBKurswertBerechnen().getValue()) {
				getKurswert().setValue(anzahl.multiply(kurs));
			}

			BigDecimal kurswert = toDecimal(getKurswert());
			if (kurswert == null) {
				return;
			}
			boolean istErloes = getAktionAuswahl().getValue().equals(DepotAktion.VERKAUF)
					|| getAktionAuswahl().getValue().equals(DepotAktion.AUSBUCHUNG);
			BigDecimal d = istErloes ? kurswert : kurswert.negate();
			BigDecimal kosten = toDecimal(getTransaktionskosten());
			if (kosten != null) {
				d = d.subtract(kosten);
			}
			BigDecimal st = toDecimal(getSteuern());
			if (st != null) {
				d = d.subtract(st);
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
	public DecimalInput getEinzelkurs() throws RemoteException
	{
		if (einzelkurs != null)
			return einzelkurs;
		einzelkurs = new DecimalInput((Number) null, new VarDecimalFormat(2, 6));
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
		boolean istErloes = getAktionAuswahl().getValue().equals(DepotAktion.VERKAUF)
				|| getAktionAuswahl().getValue().equals(DepotAktion.AUSBUCHUNG);
		BigDecimal anzahl = toDecimal(getAnzahl());
		BigDecimal kurs = toDecimal(getEinzelkurs());
		if (kurs == null || anzahl == null || getDate().getValue() ==null) {
			throw new ApplicationException("Bitte vervollständigen Sie die Eingabe.");
		}
		if (anzahl.signum() <= 0 || kurs.signum() < 0) {
			throw new ApplicationException("Die Anzahl und der Kurs müssen positiv sein.");
		}
		BigDecimal kurswert = toDecimal(getKurswert());
		if (kurswert == null) {
			throw new ApplicationException("Bitte vervollständigen Sie die Eingabe.");
		}
		// Leere Gebühren-/Steuerfelder wie 0 behandeln (so sind sie auch vorbelegt)
		BigDecimal gebuehren = toDecimal(getTransaktionskosten());
		if (gebuehren == null) {
			gebuehren = BigDecimal.ZERO;
		}
		BigDecimal st = toDecimal(getSteuern());
		if (st == null) {
			st = BigDecimal.ZERO;
		}
		if (umsatz == null) {
			umsatz = (Umsatz) Settings.getDBService().createObject(Umsatz.class,null);
			umsatz.setBuchungsinformationen("");
			umsatz.setOrderid(Zahlen.berechneManuelleOrderId(
						((GenericObjectSQL) getWertpapiere().getValue()).getID(),
						getAktionAuswahl().getValue().toString(),
						anzahl, kurs, "EUR", (Date) getDate().getValue()));
			umsatz.setKursW("EUR");
			umsatz.setKostenW("EUR");
			umsatz.setSteuernW("EUR");
			umsatz.setTransaktionsgebuehrenW("EUR");
		}
		Konto k = (Konto) ((GenericObjectHashMap) getKonto().getValue()).getAttribute("kontoobj");
		umsatz.setKontoid(Integer.parseInt(k.getID()));
		umsatz.setWPid(((GenericObjectSQL) getWertpapiere().getValue()).getID());
		umsatz.setAktion((DepotAktion) getAktionAuswahl().getValue());
		umsatz.setAnzahl(anzahl);
		umsatz.setKurs(kurs);
		umsatz.setKosten(istErloes ? kurswert : kurswert.negate());
		umsatz.setBuchungsdatum((Date) getDate().getValue());
		umsatz.setSteuern(st);
		umsatz.setTransaktionsgebuehren(gebuehren);
		umsatz.setKommentar((String)getKommentar().getValue());
		umsatz.store();
		
		UmsatzHelper.storeUmsatzInHibiscus(umsatz);
		Utils.setUmsatzBetsandTest(false);
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
		gesamt = new DecimalInput((Number) null, new VarDecimalFormat(2));
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

	public DecimalInput getKurswert() {
		if (kurswert != null)
			return kurswert;
		kurswert = new DecimalInput((Number) null, new VarDecimalFormat(2, 6));
		kurswert.setMandatory(true);
		return kurswert;
	}

	public DecimalInput getTransaktionskosten() {
		if (transaktionskosten != null)
			return transaktionskosten;
		transaktionskosten = new DecimalInput(BigDecimal.ZERO, new VarDecimalFormat(2, 6));
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

	public DecimalInput getSteuern() {
		if (steuern != null)
			return steuern;
		steuern = new DecimalInput(BigDecimal.ZERO, new VarDecimalFormat(2, 6));
		steuern.setMandatory(true);
		steuern.addListener(new Listener() {

			@Override
			public void handleEvent(org.eclipse.swt.widgets.Event event) {
				calc();
			}

		});
		return steuern;
	}
	
	/**
	 * Initialisiert das Control mit Daten aus einer Inkonsistenz
	 */
	private void initializeFromInconsistency(InconsistencyData inconsistency) throws Exception {
		getCBKurswertBerechnen().setValue(true);
		
		// Setze Aktion (Kauf oder Verkauf)
		String aktion = inconsistency.getRequiredAction();
		DepotAktion depotAktion = DepotAktion.getByString(aktion);
		if (depotAktion != null) {
			getAktionAuswahl().setValue(depotAktion);
		}
		
		// Setze Anzahl
		getAnzahl().setValue(inconsistency.getRequiredAmount());
		
		// Setze Wertpapier
		String wpId = inconsistency.getWpId().toString();
		for (Object o : getWertpapiere().getList()) {
			GenericObjectSQL obj = (GenericObjectSQL) o;
			if (obj.getID().toString().equals(wpId)) {
				getWertpapiere().setValue(obj);
				break;
			}
		}
		
		// Setze Konto
		String kontoId = inconsistency.getKontoId();
		for (Object o : getKonto().getList()) {
			Konto k = (Konto) ((GenericObjectHashMap) o).getAttribute("kontoobj");
			if (kontoId.equals(k.getID())) {
				getKonto().setValue(o);
				break;
			}
		}
		
	
		// Setze Kommentar
		getKommentar().setValue("Automatisch erstellt zur Behebung von Inkonsistenz");
		
		calc();
	}

}
