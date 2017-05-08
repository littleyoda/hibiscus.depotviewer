package de.open4me.depot.gui.control;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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
			System.out.println(current.getClass());
			if (last != null && (last.getYear() != current.getYear())) {
				Date jahresEnde = (Date) last.clone();
				jahresEnde.setMonth(11);
				jahresEnde.setDate(31);
				datelist.add(jahresEnde);
			}
			datelist.add(current);
			last = current;
		}
		return datelist;
	}
}
