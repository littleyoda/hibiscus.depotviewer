package de.open4me.depot.gui.control;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.jameica.gui.AbstractControl;
import de.willuhn.jameica.gui.AbstractView;

public class BestandsControl extends AbstractControl {

	public BestandsControl(AbstractView view) {
		super(view);
	}

	public ArrayList<Date> getDates() throws RemoteException {
		ArrayList<Date> datelist = new ArrayList<Date>(); 
		List<GenericObjectSQL> list = SQLUtils.getResultSet("select distinct buchungsdatum from depotviewer_umsaetze order by buchungsdatum", "", "");
		for (GenericObjectSQL x : list) {
			datelist.add((Date) x.getAttribute("buchungsdatum"));
		}
		return datelist;
	}
}
