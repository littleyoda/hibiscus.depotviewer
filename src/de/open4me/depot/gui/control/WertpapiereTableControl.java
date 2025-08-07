package de.open4me.depot.gui.control;

import java.rmi.RemoteException;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.action.AddWertpapierAction;
import de.open4me.depot.gui.action.ModifyWertpapierAction;
import de.open4me.depot.gui.action.OrderList;
import de.open4me.depot.gui.action.WertpapiereAktualisierenAction;
import de.open4me.depot.gui.menu.WertpapierMenu;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLQueries;
import de.willuhn.jameica.gui.input.CheckboxInput;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.gui.parts.TablePart;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.logging.Logger;

public class WertpapiereTableControl 
{

	private TablePart orderList;
	private WertpapiereControl controller;
	private CheckboxInput nurBestandFilter;

	public WertpapiereTableControl() {
	}

	private TablePart getTable() {
		if (orderList != null) {
			return orderList;
		}

		// Initial alle Wertpapiere laden, Filter wird später angewendet
		List<GenericObjectSQL> list = SQLQueries.getWertpapiereMitKursdatum();

		orderList = new TablePart(list,new ModifyWertpapierAction());
		orderList.setRememberColWidths(true);
		orderList.setRememberOrder(true);
		orderList.addColumn(Settings.i18n().tr("WKN"),"wkn");
		orderList.addColumn(Settings.i18n().tr("ISIN"),"isin");
		orderList.addColumn(Settings.i18n().tr("Name"),"wertpapiername");
		orderList.addColumn(Settings.i18n().tr("Letzter Kurs"),"kurs");
		orderList.addColumn(Settings.i18n().tr("Letzter Kurs Datum"),"kursdatum");
		orderList.addSelectionListener(new Listener() {

			@Override
			public void handleEvent(Event event) {
				GenericObjectSQL[] d = getSelection();
				if (d == null) {
					return;
				}
				controller.aktualisieren(d);

			}

		});
		orderList.setMulti(true);
		orderList.setContextMenu(new WertpapierMenu(controller));
		return orderList;
	}

	public GenericObjectSQL[] getSelection() {
		if (orderList.getSelection() == null) {
			return null; 
		}
		GenericObjectSQL[] d;
		if (orderList.getSelection() instanceof Object[]) {
			d = ((GenericObjectSQL[]) orderList.getSelection());
		} else {
			d = new GenericObjectSQL[] {(GenericObjectSQL) orderList.getSelection()};
		}
		return d;
	}
	
	public Composite getWertpapierControl(Composite comp) throws RemoteException
	{

		Composite rest = new Composite(comp ,SWT.BORDER);
		GridLayout grid1 = new GridLayout();
		grid1.numColumns = 1;
		rest.setLayout(grid1);

		// Filter hinzufügen
		SimpleContainer filterContainer = new SimpleContainer(rest);
		filterContainer.addPart(getNurBestandFilter());

		// Setup dispose listener after control is properly initialized
		setupDisposeListeners();

		getTable().paint(rest);
		
		// Apply saved filter after table is painted
		try {
			refreshTable();
		} catch (Exception e) {
			Logger.error("Error applying saved filter", e);
		}

		ButtonArea buttons = new ButtonArea();

		buttons.addButton(new Button("Hinzufügen", new AddWertpapierAction()));
		buttons.addButton(new Button("Aktualisieren", new WertpapiereAktualisierenAction(getTable())));
		
		buttons.paint(rest);
		return rest;
	}

	public void setController(WertpapiereControl controller) {
		this.controller = controller;
		
	}

	public void aktualisiere() throws RemoteException {
		getTable().removeAll();
		for (GenericObjectSQL x : getFilteredWertpapiere()) {
			getTable().addItem(x);
		}
	}

	/**
	 * Nur-Bestand-Filter
	 */
	public CheckboxInput getNurBestandFilter() throws RemoteException {
		if (nurBestandFilter != null)
			return nurBestandFilter;

		nurBestandFilter = new CheckboxInput(false);
		nurBestandFilter.setName(Settings.i18n().tr("Nur Wertpapiere im Bestand anzeigen"));
		// Load saved checkbox from settings
		try {
			de.willuhn.jameica.system.Settings settings = new de.willuhn.jameica.system.Settings(WertpapiereTableControl.class);
			boolean savedNurBestand = settings.getBoolean("wertpapiere.nurbestand", false);
			nurBestandFilter.setValue(savedNurBestand);
		} catch (Exception e) {
			// Ignore errors, use default
		}
		nurBestandFilter.addListener(new Listener() {
			public void handleEvent(Event event) {
				try {
					refreshTable();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		return nurBestandFilter;
	}

	/**
	 * Setup dispose listeners for filter persistence
	 */
	private void setupDisposeListeners() {
		if (nurBestandFilter != null) {
			nurBestandFilter.getControl().addDisposeListener(e -> {
				try {
					de.willuhn.jameica.system.Settings settings = new de.willuhn.jameica.system.Settings(WertpapiereTableControl.class);
					Boolean value = (Boolean) nurBestandFilter.getValue();
					settings.setAttribute("wertpapiere.nurbestand", value != null ? value : false);
				} catch (Exception ex) {
					// Ignore save errors
				}
			});
		}
	}

	/**
	 * Filtert die Wertpapiere basierend auf dem Filter
	 */
	private List<GenericObjectSQL> getFilteredWertpapiere() {
		Boolean nurBestand = false;
		try {
			nurBestand = nurBestandFilter != null ? (Boolean) nurBestandFilter.getValue() : false;
		} catch (Exception e) {
			// Ignorieren, Default-Wert verwenden
		}

		Logger.info("Filter Status: nurBestand = " + nurBestand);
		
		if (nurBestand) {
			List<GenericObjectSQL> owned = SQLQueries.getOwnedWertpapiereMitKursdatum();
			Logger.info("Owned Wertpapiere: " + owned.size());
			return owned;
		} else {
			List<GenericObjectSQL> all = SQLQueries.getWertpapiereMitKursdatum();
			Logger.info("Alle Wertpapiere: " + all.size());
			return all;
		}
	}

	/**
	 * Aktualisiert die Tabelle mit gefilterten Daten
	 */
	private void refreshTable() throws RemoteException {
		if (orderList != null) {
			orderList.removeAll();
			List<GenericObjectSQL> filteredData = getFilteredWertpapiere();
			for (GenericObjectSQL item : filteredData) {
				orderList.addItem(item);
			}
		}
	}
}