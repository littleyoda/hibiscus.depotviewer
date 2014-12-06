package de.open4me.depot.gui.control;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.open4me.depot.tools.VarDecimalFormat;
import de.willuhn.jameica.gui.AbstractControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.input.DateInput;
import de.willuhn.jameica.gui.input.DecimalInput;
import de.willuhn.jameica.gui.input.Input;
import de.willuhn.jameica.gui.input.SelectInput;
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

	public UmsatzEditorControl(AbstractView view) throws Exception {
		super(view);
		if (view.getCurrentObject() != null) {
			GenericObjectSQL b = (GenericObjectSQL) view.getCurrentObject();
			getAnzahl().setValue(b.getAttribute("anzahl"));
			getEinzelkurs().setValue(b.getAttribute("kurs"));
			getDate().setValue(b.getAttribute("buchungsdatum"));
			
			String id = b.getAttribute("wpid").toString();
			for (Object o : getWertpapiere().getList()) {
				GenericObjectSQL obj = (GenericObjectSQL) o;
				if (obj.getID().equals(id)) {
					getWertpapiere().setValue(obj);
				}
			}
			
			for (Object o : getKonto().getList()) {
				Konto k = (Konto) ((GenericObjectHashMap) o).getAttribute("kontoobj");
				if (b.getAttribute("kontoid").toString().equals(k.getID())) {
					getKonto().setValue(o);
				}
			}
			
			getAktionAuswahl().setValue((String) b.getAttribute("aktion")); 
		}
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
	    betrag = new DecimalInput(d, new VarDecimalFormat(5));
	    betrag.setMandatory(true);

	    return betrag;
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
	    einzelkurs = new DecimalInput(d, new VarDecimalFormat(5));
	    einzelkurs.setMandatory(true);
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
		  List<String> liste = new ArrayList<String>();
		  liste.add("VERKAUF");
		  liste.add("KAUF");
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
			List<GenericObjectSQL> list = SQLUtils.getResultSet("select * from depotviewer_wertpapier", 
					"depotviewer_wertpapier", "id", "wertpapiername");
		  wp = new SelectInput(list, null);
		  wp.setMandatory(true);
		  return wp;
	  }


	  public void handleStore() throws RemoteException, ApplicationException {
		int faktor = -1;
		if (getAktionAuswahl().getValue().toString().equals("VERKAUF")) {
			faktor = 1;
		}
		if (getEinzelkurs().getValue() == null || getAnzahl().getValue() == null || getDate().getValue() ==null) {
			throw new ApplicationException("Bitte vervollständigen sie die Eingabe.");
		}
		if ((Double) getAnzahl().getValue() <=0 || ((Double) getEinzelkurs().getValue() < 0)) {
			throw new ApplicationException("Die Anzahl und der Kurs mmüssen positiv sein.");
		}
		if (view.getCurrentObject() != null) {
			GenericObjectSQL b = (GenericObjectSQL) view.getCurrentObject();
			SQLUtils.delete(b);
		}
		Konto k = (Konto) ((GenericObjectHashMap) getKonto().getValue()).getAttribute("kontoobj");

		Utils.addUmsatz(k.getID(), 
						((GenericObjectSQL) getWertpapiere().getValue()).getID() , 
							getAktionAuswahl().getValue().toString(), 
							"", 
							(Double) getAnzahl().getValue(), 
							(Double) getEinzelkurs().getValue(), 
							"EUR", 
							faktor * Math.abs((Double) getEinzelkurs().getValue() * (Double) getAnzahl().getValue()), 
							"EUR", 
							(Date) getDate().getValue(), 
							
							""  + (((GenericObjectSQL) getWertpapiere().getValue()).getID() + getAktionAuswahl().getValue().toString() +
							getAnzahl().getValue().toString() + getEinzelkurs().getValue().toString() + "EUR" + getDate().getValue()
							
									).hashCode(),
									""
							); 
		if (view.getCurrentObject() != null) {
			GenericObjectSQL b = (GenericObjectSQL) view.getCurrentObject();
			SQLUtils.delete(b);
		}
		
	}
}
