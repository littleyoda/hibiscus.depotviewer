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

	public OrderListControl(AbstractView view) {
		super(view);
	}


  public Part getOrderInfoTable() throws RemoteException
  {
    if (orderList != null) {
      return orderList;
    }
   
    List<GenericObjectSQL> list = SQLUtils.getResultSet("select *, concat(kosten, ' ', kostenw) as joinkosten from depotviewer_umsaetze left join depotviewer_wertpapier where  depotviewer_umsaetze.wpid = depotviewer_wertpapier.id", 
    		"depotviewer_umsaetze", "id");

    orderList = new TablePart(list,new OrderList());
    orderList.addColumn(Settings.i18n().tr("wkn"),"wkn");
    orderList.addColumn(Settings.i18n().tr("Name"),"wertpapiername"); 
    orderList.addColumn(Settings.i18n().tr("Anzahl"),"anzahl"); 
//    orderList.addColumn(Settings.i18n().tr("wkn"),"kurz"); 
    orderList.addColumn(Settings.i18n().tr("Kosten"),"joinkosten"); 
    orderList.addColumn(Settings.i18n().tr("Aktion"),"aktion"); 
    orderList.addColumn(Settings.i18n().tr("Datum"),"buchungsdatum", new DateFormatter(Settings.DATEFORMAT)); 
    orderList.setContextMenu(new OrderListMenu(orderList));
    
    return orderList;
  }
}