package de.open4me.depot.gui.control;

import java.io.IOException;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.htmlunit.FailingHttpStatusCodeException;

import de.open4me.depot.Settings;
import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.datenobj.rmi.Wertpapier;
import de.open4me.depot.gui.view.ModifyWertpapierView;
import de.open4me.depot.gui.view.WertpapierView;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.open4me.depot.tools.WertpapierSuche;
import de.willuhn.jameica.gui.AbstractControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.input.Input;
import de.willuhn.jameica.gui.input.TextInput;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.gui.parts.TablePart;
import de.willuhn.util.ApplicationException;

public class AddWertpapierControl  extends AbstractControl {

	private Input suchbox;
	private TablePart trefferListe;
	
	public AddWertpapierControl(AbstractView view) {
		super(view);
	}

	public Input getSuchbox() {
		if (this.suchbox == null)
		{
			this.suchbox = new TextInput("");
		}
		return this.suchbox;
	}
	
	public TablePart getTab() {
		if (this.trefferListe != null) {
			return trefferListe;
		}
		
		trefferListe = new TablePart(getDummy(),null);
		trefferListe.setSummary(false);
		trefferListe.addColumn(Settings.i18n().tr("Name"), "Name");
		trefferListe.addColumn(Settings.i18n().tr("Typ"),"Typ");
		trefferListe.addColumn(Settings.i18n().tr("ISIN"),"Isin");
		trefferListe.addColumn(Settings.i18n().tr("WKN"),"Wkn");
		trefferListe.addColumn(Settings.i18n().tr("Quelle"),"Source");
		return trefferListe;
	}
	
	public List<GenericObjectHashMap> getDummy() {
		HashMap<String, Object> h = new HashMap<String, Object>();
		h.put("Name", "                           ");
		h.put("Isin", "                           ");
		h.put("Typ", "                           ");
		h.put("Wkn", "                           ");
		h.put("Source", "                           ");
		List<GenericObjectHashMap> x = new ArrayList<GenericObjectHashMap>();
		x.add(new GenericObjectHashMap(h));
		return x;

	}
	public ButtonArea getSearchButton() {
		ButtonArea buttons = new ButtonArea();
		buttons.addButton("Suchen",new Action() {

			@Override
			public void handleAction(Object context)
					throws ApplicationException {
				try {
					List<HashMap<String, String>> x = WertpapierSuche.search(((String) getSuchbox().getValue()).trim()); 
					TablePart ziel = getTab();
					ziel.removeAll();
					for (HashMap<String, String> map : x) {
						if (map.get("Isin") == null) {
							continue;
						}
						GenericObjectHashMap obj = new GenericObjectHashMap(new HashMap<String, Object>(map));
						ziel.addItem(obj);
					}
				} catch (FailingHttpStatusCodeException | IOException e) {
					e.printStackTrace();
					throw new ApplicationException(e);
				}

			}

		},null ,false,"edit-copy.png");
		return buttons;
	}

	public ButtonArea getAddButton() {
		ButtonArea buttons = new ButtonArea();
		buttons.addButton("Manuell hinzufügen",new Action() {

			@Override
			public void handleAction(Object context)
					throws ApplicationException {
				Wertpapier wp;
				try {
					wp = (Wertpapier) Settings.getDBService().createObject(Wertpapier.class, null);
				} catch (RemoteException e) {
					e.printStackTrace();
					throw new ApplicationException(e);
				}
			  	GUI.startView(ModifyWertpapierView.class, wp);
			}

		}, null, false, "edit-copy.png");
		
		buttons.addButton("Übernehmen",new Action() {

			@Override
			public void handleAction(Object context)
					throws ApplicationException {
				Object selection = getTab().getSelection();
				if (selection == null || !(selection instanceof GenericObjectHashMap)) {
					return;
				}
				GenericObjectHashMap sel = (GenericObjectHashMap) selection;
				Wertpapier wp;
				try {
					String wpid = Utils.getORcreateWKN((String) sel.getAttribute("Wkn"), (String) sel.getAttribute("Isin"), (String) sel.getAttribute("Name"));
					wp = Utils.getWertPapierByID(wpid);
				} catch (RemoteException e) {
					e.printStackTrace();
					throw new ApplicationException(e);
				}
			  	GUI.startView(WertpapierView.class, wp);
			}

		},null ,false,"edit-copy.png");
		return buttons;
	}

}


