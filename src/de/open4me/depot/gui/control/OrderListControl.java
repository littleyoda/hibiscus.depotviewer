package de.open4me.depot.gui.control;

import java.rmi.RemoteException;
import java.util.List;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.action.OrderList;
import de.open4me.depot.gui.menu.OrderListMenu;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.jameica.gui.AbstractControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Part;
import de.willuhn.jameica.gui.formatter.DateFormatter;
import de.willuhn.jameica.gui.parts.TablePart;


public class OrderListControl extends AbstractControl
{

	private TablePart orderList;
	private TablePart umsatzList;
	public OrderListControl(AbstractView view) {
		super(view);
	}


	public Part getOrderInfoTable() throws RemoteException
	{
		if (orderList != null) {
			return orderList;
		}

		List<GenericObjectSQL> list = SQLUtils.getResultSet("select *, "
				+ "concat(kosten, ' ', kostenw) as joinkosten, "
				+ "konto.id as kontoid, "
				+ "depotviewer_umsaetze.id as umsatzid, "
				+ "concat(steuern, ' ', steuernw) as joinsteuern, "
				+ "concat(transaktionskosten, ' ', transaktionskostenw) as jointransaktionskosten "
				+ "from depotviewer_umsaetze "
				+ "	left join depotviewer_wertpapier on  depotviewer_umsaetze.wpid = depotviewer_wertpapier.id"
				+ "	left join konto on  konto.id = depotviewer_umsaetze.kontoid order by buchungsdatum desc"
				, 
				"depotviewer_umsaetze", "id");

		orderList = new TablePart(list,new OrderList());
		orderList.addColumn(Settings.i18n().tr("Depot"), "bezeichnung");
		orderList.addColumn(Settings.i18n().tr("wkn"),"wkn");
		orderList.addColumn(Settings.i18n().tr("Wertpapiername"),"wertpapiername"); 
		orderList.addColumn(Settings.i18n().tr("Anzahl"),"anzahl"); 
		orderList.addColumn(Settings.i18n().tr("Kurs"),"kurs"); 
		orderList.addColumn(Settings.i18n().tr("Kosten"),"joinkosten"); 
		orderList.addColumn(Settings.i18n().tr("Gebühren"),"jointransaktionskosten"); 
		orderList.addColumn(Settings.i18n().tr("Steuern"),"joinsteuern"); 
		orderList.addColumn(Settings.i18n().tr("Aktion"),"aktion"); 
		orderList.addColumn(Settings.i18n().tr("Datum"),"buchungsdatum", new DateFormatter(Settings.DATEFORMAT)); 
		orderList.addColumn(Settings.i18n().tr("Kommentar"), "kommentar"); 
		orderList.setContextMenu(new OrderListMenu(orderList));

		return orderList;
	}


}