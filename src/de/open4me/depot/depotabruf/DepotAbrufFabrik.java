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
		for (BasisDepotAbruf x : getDepotAbrufs()) {
			if (x.isSupported(konto)) {
				return x;
			}
		}
		return null;
//		return new CortalConsorsMitHBCI();
//		return new HBCIDepot();
//		if ((konto.getBackendClass() != null && !konto.getBackendClass().isEmpty())
//			|| ((konto.getBLZ().equals("0000000")
//				|| konto.getBLZ().equals("0")) && konto.getUnterkonto().toLowerCase().startsWith("depot"))) {
//			String name = konto.getUnterkonto().toLowerCase().substring(5).replace(" ", "");
//			for (BasisDepotAbruf x : getDepotAbrufs()) {
//				if (name.equals(x.getName().toLowerCase().replace(" ", ""))) {
//					return x;
//				}
//			}
//		}
//		return null;
	}

}
