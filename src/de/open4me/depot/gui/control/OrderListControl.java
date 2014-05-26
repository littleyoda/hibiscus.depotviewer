 package de.open4me.depot.gui.control;

import java.rmi.RemoteException;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.action.OrderList;
import de.open4me.depot.rmi.Umsatz;
import de.willuhn.datasource.rmi.DBIterator;
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
   
    DBIterator orders = Settings.getDBService().createList(Umsatz.class);
    orderList = new TablePart(orders,new OrderList());
    orderList.addColumn(Settings.i18n().tr("wkn"),"wkn");
    orderList.addColumn(Settings.i18n().tr("Name"),"wertpapiername"); 
    orderList.addColumn(Settings.i18n().tr("Anzahl"),"anzahl"); 
//    orderList.addColumn(Settings.i18n().tr("wkn"),"kurz"); 
    orderList.addColumn(Settings.i18n().tr("Kosten"),"kosten"); 
    orderList.addColumn(Settings.i18n().tr("Aktion"),"aktion"); 
    orderList.addColumn(Settings.i18n().tr("Datum"),"buchungsdatum", new DateFormatter(Settings.DATEFORMAT)); 
    
    return orderList;
  }
}