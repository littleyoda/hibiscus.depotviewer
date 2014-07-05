package de.open4me.depot.depotabruf;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.util.ApplicationException;

public class DepotAbrufFabrik {

	private static List<BasisDepotAbruf> depotAbrufs;

	/**
	 * Liefert alle Backends zurück
	 * @return Liste aller Backends
	 */
	public static  List<BasisDepotAbruf> getDepotAbrufs() {
		if (depotAbrufs == null) {
			// Die Reihenfolge ist relevant, da die erste Klasse genommen wird, die auf das Konto passt.
			// Deshalb muss die Reihenfolge von Spezial (Unterstützung einzelner Banken) zu Allgemein (HBCI) eingehalten werden
			depotAbrufs = new ArrayList<BasisDepotAbruf>();
			depotAbrufs.add(new MusterDepot());
			depotAbrufs.add(new Fondsdepotbank());
			depotAbrufs.add(new CortalConsorsMitHBCI());
			depotAbrufs.add(new HBCIDepot());

		}
		return depotAbrufs;
	}


	/**
	 * Sucht nach einem Backeknd, dass für das Konto zuständig ist
	 * 
	 * @param konto Konto
	 * @return Backend, dass für das Konto zuständig ist
	 * @throws RemoteException
	 * @throws ApplicationException
	 */
	public static BasisDepotAbruf getDepotAbruf(Konto konto) throws RemoteException, ApplicationException {
		if (konto == null) {
			return null;
		}
		for (BasisDepotAbruf x : getDepotAbrufs()) {
			if (x.isSupported(konto)) {
				return x;
			}
		}
		return null;
	}

}
