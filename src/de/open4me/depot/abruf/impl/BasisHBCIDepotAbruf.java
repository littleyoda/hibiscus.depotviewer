package de.open4me.depot.abruf.impl;

import java.rmi.RemoteException;

import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.util.ApplicationException;

public class BasisHBCIDepotAbruf extends BasisDepotAbruf {

	@Override
	public String getName() {
		return "HBCI";
	}

	@Override
	public void run(Konto konto) throws ApplicationException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isSupported(Konto konto) throws ApplicationException,
			RemoteException {
		return true;
	}
	
}
