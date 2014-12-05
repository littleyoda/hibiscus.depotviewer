package de.open4me.depot.abruf.impl;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import de.open4me.depot.DepotViewerPlugin;
import de.open4me.depot.abruf.utils.PropHelper;
import de.open4me.depot.abruf.utils.Utils;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.util.ApplicationException;

public abstract class BasisDepotAbruf {


	public abstract String getName();

	public abstract void run(Konto konto) throws ApplicationException;
	
	public abstract boolean isSupported(Konto konto) throws ApplicationException, RemoteException;

//	public SecurePwdCfgImpl getConfig(Konto konto) throws ApplicationException {
//		SecurePwdCfgImpl config = null;
//		try {
//			PassportImpl p = new PassportImpl();
//			p.init(konto, DepotViewerPlugin.class, "Depot-Viewer: " + getName());
//			config = p.getConfig();
//			if (config == null) {
//				config = p.createConfig();
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new ApplicationException("Fehler beim Zugriff auf die Config: ", e);
//		}
//		return config;
//	}
	public List<String> getPROP() {
		List<String> result = new ArrayList<String>();
		result.add(getName());
		result.addAll(PropHelper.getPROP());
		return result;

	}

	public void debug(List<String> seiten, Konto konto) throws RemoteException {
		Utils.debug(Utils.getWorkingDir(DepotViewerPlugin.class), 
				getName(), konto.getMeta(PropHelper.PROP_OPTIONS, ""), seiten);
	}



}
