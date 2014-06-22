package de.open4me.depot.gui.control;

import java.rmi.RemoteException;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.action.OrderList;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.open4me.depot.tools.UpdateStock;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.parts.Button;
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

		List<GenericObjectSQL> list = SQLUtils.getResultSet("select * from depotviewer_wertpapier", 
				"depotviewer_wertpapier", "id", "wertpapiername");

		orderList = new TablePart(list,new OrderList());
		orderList.addColumn(Settings.i18n().tr("wkn"),"wkn");
		orderList.addColumn(Settings.i18n().tr("ISIN"),"isin");
		orderList.addColumn(Settings.i18n().tr("Name"),"wertpapiername");
		orderList.addSelectionListener(new Listener() {

			@Override
			public void handleEvent(Event event) {
				if (event.data == null || !(event.data instanceof GenericObjectSQL)) {
					return;
				}
				GenericObjectSQL d = (GenericObjectSQL) event.data;
				history.update(d);

			}

		});

		return orderList;
	}

	public Composite getWepierControl(Composite comp) throws RemoteException
	{

		Composite rest = new Composite(comp ,SWT.BORDER);
		GridLayout grid1 = new GridLayout();
		grid1.numColumns = 1;
		rest.setLayout(grid1);


		getTable().paint(rest);


		Button updateButton = new Button("Aktualisieren",new Action() {

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

		});
		updateButton.paint(rest);
		return rest;
	}
}