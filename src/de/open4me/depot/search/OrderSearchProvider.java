package de.open4me.depot.search;

import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.action.UmsatzEditorAction;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.open4me.depot.tools.VarDecimalFormat;
import de.willuhn.jameica.search.Result;
import de.willuhn.jameica.search.SearchProvider;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

/**
 * Extension to the jameica search service. If you implement the
 * "SearchProvider" interface, jameica automatically detects the provider.
 */
public class OrderSearchProvider implements SearchProvider {
	@Override
	public String getName() {
		return "Depot-Viewer: " + Settings.i18n().tr("Orderbuch");
	}

	@Override
	public List<Result> search(String search) throws RemoteException, ApplicationException {
		// We have to return a list of "Result" objects
		List<Result> result = new ArrayList<Result>();
		if (search == null || search.length() < 3)
			return result;

		String s = search.toLowerCase();
		List<GenericObjectSQL> orders = SQLUtils.getResultSet("select *, "
				+ "concat(wertpapiername , ' (' , wkn , ' / ' , isin , ')') as nicename " 
				+ "from depotviewer_umsaetze "
				+ "	left join depotviewer_wertpapier on  depotviewer_umsaetze.wpid = depotviewer_wertpapier.id "
				+ "	left join konto on  konto.id = depotviewer_umsaetze.kontoid " 
				+ "order by buchungsdatum desc",
				"depotviewer_umsaetze", "id");
		for (GenericObjectSQL order : orders) {
			// "nicename" beinhaltet Name, WKN, ISIN
			if (((String) order.getAttribute("nicename")).toLowerCase().contains(s)) {
				result.add(new OrderSearchResult(order));
			}
		}
		return result;
	}

	/**
	 * Our implementation of the search result items.
	 */
	public class OrderSearchResult implements Result {
		private GenericObjectSQL order = null;

		private OrderSearchResult(GenericObjectSQL order) {
			this.order = order;
		}

		@Override
		public void execute() throws RemoteException, ApplicationException {
			new UmsatzEditorAction(false).handleAction(this.order);
		}

		@Override
		public String getName() {
			try {
				DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
				VarDecimalFormat kf = new VarDecimalFormat(5);
				return df.format((Date) this.order.getAttribute("buchungsdatum")) + ": "
						+ this.order.getAttribute("aktion") + " " + kf.format(this.order.getAttribute("anzahl"))
						+ " Stück " + (String) this.order.getAttribute("nicename") + ", Kurs: "
						+ kf.format(this.order.getAttribute("kurs")) + " " + this.order.getAttribute("kursw");
			} catch (Exception e) {
				Logger.error("unable to determine order details", e);
				return "";
			}
		}

	}

}
