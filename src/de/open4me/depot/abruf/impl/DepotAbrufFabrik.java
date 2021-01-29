package de.open4me.depot.abruf.impl;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.util.ApplicationException;

public class DepotAbrufFabrik {

	private static List<BasisDepotAbruf> depotAbrufs;
	private static ArrayList<BasisDepotAbruf> depotHBCIAbrufs;

	/**
	 * Liefert alle Backends zurück
	 * @return Liste aller Backends
	 */
	public static  List<BasisDepotAbruf> getDepotAbrufs() {
		if (depotAbrufs == null) {
			// Die Reihenfolge ist relevant, da die erste Klasse genommen wird, die auf das Konto passt.
			// Deshalb muss die Reihenfolge von Spezial (Unterstützung einzelner Banken) zu Allgemein (HBCI) eingehalten werden
			depotAbrufs = new ArrayList<BasisDepotAbruf>();
		}
		return depotAbrufs;
	}

	/**
	 * Liefert alle Backends zurück
	 * @return Liste aller Backends
	 */
	public static  List<BasisDepotAbruf> getDepotAbrufsHBCISupport() {
		if (depotHBCIAbrufs == null) {
			depotHBCIAbrufs = new ArrayList<BasisDepotAbruf>();
			depotHBCIAbrufs.add(new CortalConsorsMitHBCI());
		}
		return depotHBCIAbrufs;
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
		return getMatchingDepotAbruf(konto, getDepotAbrufs());
	}

	/**
	 * Sucht nach einem Backeknd, dass für das Konto zuständig ist
	 * 
	 * @param konto Konto
	 * @return Backend, dass für das Konto zuständig ist
	 * @throws RemoteException
	 * @throws ApplicationException
	 */
	public static BasisDepotAbruf getDepotAbrufHBCI(Konto konto) throws RemoteException, ApplicationException {
		return getMatchingDepotAbruf(konto, getDepotAbrufsHBCISupport());
	}
	
	private static BasisDepotAbruf getMatchingDepotAbruf(Konto konto, List<BasisDepotAbruf> list) throws RemoteException, ApplicationException {
		if (konto == null) {
			return null;
		}
		for (BasisDepotAbruf x : list) {
			if (x.isSupported(konto)) {
				return x;
			}
		}
		return null;
	}

}
