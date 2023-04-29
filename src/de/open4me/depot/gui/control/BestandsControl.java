package de.open4me.depot.gui.control;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
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
		Date last = null; 
		List<GenericObjectSQL> list = SQLUtils.getResultSet("select distinct buchungsdatum from depotviewer_umsaetze order by buchungsdatum", "", "");
		for (GenericObjectSQL x : list) {
			Date current = (Date) x.getAttribute("buchungsdatum");
			if (last != null) {
				final Calendar calLast = Calendar.getInstance();
				calLast.setTime(last);
				final Calendar calCurrent = Calendar.getInstance();
				calCurrent.setTime(current);
				if (calLast.get(Calendar.YEAR) != calCurrent.get(Calendar.YEAR)) {
					final Calendar jahresEnde = Calendar.getInstance();
					jahresEnde.set(calLast.get(Calendar.YEAR), 11, 31); // last day of year
					datelist.add(jahresEnde.getTime());
				}
			}
			datelist.add(current);
			last = current;
		}
		return datelist;
	}
}
