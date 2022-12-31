package de.open4me.depot.search;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import de.open4me.depot.Settings;
import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.gui.dialogs.ExtendedSynchronizeOptionsDialog;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.willuhn.jameica.hbci.gui.dialogs.SynchronizeOptionsDialog;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.jameica.search.Result;
import de.willuhn.jameica.search.SearchProvider;
import de.willuhn.jameica.system.Application;
import de.willuhn.jameica.system.OperationCanceledException;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

/**
 * Extension to the jameica search service. If you implement the
 * "SearchProvider" interface, jameica automatically detects the provider.
 */
public class DepotSearchProvider implements SearchProvider {
	@Override
	public String getName() {
		return "Depot-Viewer: " + Settings.i18n().tr("Depots");
	}

	@Override
	public List<Result> search(String search) throws RemoteException, ApplicationException {
		// We have to return a list of "Result" objects
		List<Result> result = new ArrayList<Result>();
		if (search == null || search.length() < 3)
			return result;

		String s = search.toLowerCase();
		List<GenericObjectHashMap> depots = Utils.getDepotKonten();
		for (GenericObjectHashMap depot : depots) {
			if (((String) depot.getAttribute("bezeichnung")).toLowerCase().contains(s)
					|| ((String) depot.getAttribute("kontonummer")).toLowerCase().contains(s)) {
				result.add(new DepotSearchResult(depot));
			}
		}
		return result;
	}

	/**
	 * Our implementation of the search result items.
	 */
	public class DepotSearchResult implements Result {
		private GenericObjectHashMap depot = null;

		private DepotSearchResult(GenericObjectHashMap depot) {
			this.depot = depot;
		}

		@Override
		public void execute() throws RemoteException, ApplicationException {
			try {
				ExtendedSynchronizeOptionsDialog d = new ExtendedSynchronizeOptionsDialog(depot,
						SynchronizeOptionsDialog.POSITION_CENTER);
				d.open();
			} catch (OperationCanceledException oce) {
				Logger.info(oce.getMessage());
				return;
			} catch (ApplicationException ae) {
				throw ae;
			} catch (Exception e) {
				Logger.error("unable to configure synchronize options");
				Application.getMessagingFactory().sendMessage(new StatusBarMessage(
						"Fehler beim Konfigurieren der Synchronisierungsoptionen", StatusBarMessage.TYPE_ERROR));
			}
		}

		@Override
		public String getName() {
			try {
				return (String) this.depot.getAttribute("bezeichnung");
			} catch (Exception e) {
				Logger.error("unable to determine depot name", e);
				return "";
			}
		}

	}

}
