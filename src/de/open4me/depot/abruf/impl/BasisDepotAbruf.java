package de.open4me.depot.abruf.impl;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import de.open4me.depot.DepotViewerPlugin;
import de.open4me.depot.abruf.utils.PropHelper;
import de.open4me.depot.abruf.utils.Utils;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

public abstract class BasisDepotAbruf {


	public abstract String getName();

	public void run(Konto konto) throws ApplicationException {
		handlePropertiesChanges(konto);
	}
	
	public boolean isSupported(Konto konto) throws ApplicationException, RemoteException {
		return Utils.hasRightKontoType(konto);
	}

	public List<String> getPROP(Konto konto) {
		handlePropertiesChanges(konto);
		List<String> result = new ArrayList<String>();
		result.addAll(PropHelper.getPROP());
		return result;
	}

	/**
	 * Korrigiert die im Laufe der Zeit umbenannten bzw. gelöschten Properties-Einträge
	 * @param konto
	 */
	protected void handlePropertiesChanges(Konto konto) {
		try {
			// Änderungsliste anfordern
			int version = Integer.parseInt(konto.getMeta("depotviewerversion", "0"));
			List<String[]> changes = getPropertiesChanges(version);


			// Änderungen durchführen
			if (changes != null && changes.size() > 0) {
				Logger.warn("Passe Properties an");
				for (String[] change : changes) {
					// Wenn der Eintrag nicht nur gelöscht werden soll, Wert in die neue Properties kopieren
					if (!change[1].isEmpty()) {
						String wert = konto.getMeta(change[0], null);
						konto.setMeta(change[1], wert);
					}
					// Alten Wert löschen
					konto.setMeta(change[0], null);
					// Version korrigieren
					konto.setMeta("depotviewerversion", change[2]);
				}
			}
		} catch (RemoteException e) {
			Logger.error("Fehler beim Aktualisieren der Properties.", e);
			e.printStackTrace();
		}
	}

	public void debug(List<String> seiten, Konto konto) throws RemoteException {
		Utils.debug(Utils.getWorkingDir(DepotViewerPlugin.class), 
				getName(), konto.getMeta(PropHelper.PROP_OPTIONS, ""), seiten);
	}

	public List<String[]> getPropertiesChanges(int version) {
		return null;
	}


}
