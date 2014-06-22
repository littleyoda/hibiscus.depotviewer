package de.open4me.depot.depotabruf;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.util.ApplicationException;

public class DepotAbrufFabrik {

	private static List<BasisDepotAbruf> depotAbrufs;

	public static  List<BasisDepotAbruf> getDepotAbrufs() {
		if (depotAbrufs == null) {
			depotAbrufs = new ArrayList<BasisDepotAbruf>();
			depotAbrufs.add(new MusterDepot());
			depotAbrufs.add(new Fondsdepotbank());
			depotAbrufs.add(new CortalConsorsMitHBCI());
			depotAbrufs.add(new HBCIDepot());

		}
		return depotAbrufs;
	}


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
