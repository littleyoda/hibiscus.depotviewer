package de.open4me.depot.depotabruf;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import de.open4me.depot.DepotViewerPlugin;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.util.ApplicationException;

public abstract class BasisDepotAbruf {

	final static String PROP_OPTIONS = "Optionen (kann leer bleiben)";

	public abstract String getName();
	public abstract void run(Konto konto) throws ApplicationException;
	public abstract boolean isSupported(Konto konto) throws ApplicationException, RemoteException;
	
	public List<String> getPROP() {
		List<String> result = new ArrayList<String>();
		result.add(getName());
		result.add(PROP_OPTIONS);
		return result;
		
	}
	
	public void debug(List<String> seiten, Konto konto) throws RemoteException {
		Utils.debug(Utils.getWorkingDir(DepotViewerPlugin.class), 
				getName(), konto.getMeta(PROP_OPTIONS, ""), seiten);
	}

	public boolean isBackendSelected(Konto konto) throws RemoteException {
		return konto.getBackendClass() != null && !(konto.getBackendClass().isEmpty());
	}
	
	public boolean isOffine(Konto konto) throws RemoteException {
		return konto.hasFlag(Konto.FLAG_OFFLINE);
	}

}
