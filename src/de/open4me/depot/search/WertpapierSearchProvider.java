package de.open4me.depot.search;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.action.ModifyWertpapierAction;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLQueries;
import de.willuhn.jameica.search.Result;
import de.willuhn.jameica.search.SearchProvider;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

/**
 * Extension to the jameica search service. If you implement the
 * "SearchProvider" interface, jameica automatically detects the provider
 */
public class WertpapierSearchProvider implements SearchProvider {
	@Override
	public String getName() {
		return "Depot-Viewer: " + Settings.i18n().tr("Wertpapiere");
	}

	@Override
	public List<Result> search(String search) throws RemoteException, ApplicationException {
		// We have to return a list of "Result" objects
		List<Result> result = new ArrayList<Result>();
		if (search == null || search.length() < 3)
			return result;

		String s = search.toLowerCase();
		List<GenericObjectSQL> wertpapiere = SQLQueries.getWertpapiere();
		for (GenericObjectSQL wp : wertpapiere) {
			// "nicename" beinhaltet Name, WKN, ISIN
			if (((String) wp.getAttribute("nicename")).toLowerCase().contains(s)) {
				result.add(new WertpapierSearchResult(wp));
			}
		}
		return result;
	}

	/**
	 * Our implementation of the search result items.
	 */
	public class WertpapierSearchResult implements Result {
		private GenericObjectSQL wertpapier = null;

		private WertpapierSearchResult(GenericObjectSQL wertpapier) {
			this.wertpapier = wertpapier;
		}

		@Override
		public void execute() throws RemoteException, ApplicationException {
			new ModifyWertpapierAction().handleAction(wertpapier);
		}

		@Override
		public String getName() {
			try {
				return (String) this.wertpapier.getAttribute("nicename");
			} catch (Exception e) {
				Logger.error("unable to determine Wertpapier name", e);
				return "";
			}
		}

	}

}
