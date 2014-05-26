package de.open4me.depot.depotabruf;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import de.willuhn.jameica.hbci.rmi.Konto;

public class DepotAbrufFabrik {

	private static List<BasisDepotAbruf> depotAbrufs;

	public static  List<BasisDepotAbruf> getDepotAbrufs() {
		if (depotAbrufs == null) {
			depotAbrufs = new ArrayList<BasisDepotAbruf>();
			depotAbrufs.add(new CortalConsors());
			depotAbrufs.add(new Fondsdepotbank());

		}
		return depotAbrufs;
	}


	public static BasisDepotAbruf getDepotAbruf(Konto konto) throws RemoteException {
		if ((konto.getBLZ().equals("0000000")
				|| konto.getBLZ().equals("0")) && konto.getUnterkonto().toLowerCase().startsWith("depot")) {
			String name = konto.getUnterkonto().toLowerCase().substring(5).replace(" ", "");
			for (BasisDepotAbruf x : getDepotAbrufs()) {
				if (name.equals(x.getName().toLowerCase().replace(" ", ""))) {
					return x;
				}
			}
		}
		return null;
	}

}
