package de.open4me.depot.tools;

import java.rmi.RemoteException;

import de.open4me.depot.Settings;
import de.open4me.depot.rmi.Umsatz;
import de.willuhn.datasource.rmi.DBIterator;

public class UmsatzHelper {

	public static boolean existsOrder(String orderid) throws RemoteException {
		DBIterator liste = Settings.getDBService().createList(Umsatz.class);
		liste.addFilter("orderid=?", orderid);
		return liste.hasNext();
	}


}
