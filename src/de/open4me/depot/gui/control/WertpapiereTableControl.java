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
import de.open4me.depot.gui.action.OrderList;
import de.open4me.depot.gui.menu.WertpapierMenu;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLQueries;
import de.open4me.depot.tools.UpdateStock;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.gui.parts.TablePart;
import de.willuhn.util.ApplicationException;

public class WertpapiereTableControl 
{

	private TablePart orderList;
	private WertpaperHistoryControl history;

	public WertpapiereTableControl(WertpaperHistoryControl history) {
		this.history = history;
	}

	private TablePart getTable() {
		if (orderList != null) {
			return orderList;
		}

		List<GenericObjectSQL> list = SQLQueries.getWertpapiereMitKursdatum();

		orderList = new TablePart(list,new OrderList());
		orderList.addColumn(Settings.i18n().tr("wkn"),"wkn");
		orderList.addColumn(Settings.i18n().tr("ISIN"),"isin");
		orderList.addColumn(Settings.i18n().tr("Name"),"wertpapiername");
		orderList.addColumn(Settings.i18n().tr("Letzter Kurs"),"kursdatum");
		orderList.addSelectionListener(new Listener() {

			@Override
			public void handleEvent(Event event) {
				if (orderList.getSelection() == null) {
					return; 
				}
				if (orderList.getSelection() instanceof Object[]) {
					history.update((GenericObjectSQL[]) orderList.getSelection());
				} else {
					GenericObjectSQL d = (GenericObjectSQL) event.data;
					history.update(d);
				}

			}

		});
		orderList.setMulti(true);
		orderList.setContextMenu(new WertpapierMenu());
		return orderList;
	}

	public Composite getWepierControl(Composite comp) throws RemoteException
	{

		Composite rest = new Composite(comp ,SWT.BORDER);
		GridLayout grid1 = new GridLayout();
		grid1.numColumns = 1;
		rest.setLayout(grid1);


		getTable().paint(rest);

		ButtonArea buttons = new ButtonArea();

		buttons.addButton(new Button("Hinzufügen", new AddWertpapierAction()));
		buttons.addButton(new Button("Aktualisieren",new Action() {

			@Override
			public void handleAction(Object context)
					throws ApplicationException {
				Object obj = getTable().getSelection();
				if (obj == null || !(obj instanceof GenericObjectSQL)) {
					return;
				}
				GenericObjectSQL d = (GenericObjectSQL) obj;

					UpdateStock.update(d);
					history.update(d);

			}

		}));
		
		buttons.paint(rest);
		return rest;
	}
}