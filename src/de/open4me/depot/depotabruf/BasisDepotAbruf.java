package de.open4me.depot.depotabruf;

import java.util.List;

import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.util.ApplicationException;

public interface BasisDepotAbruf {

	public abstract String getName();
	public abstract void run(Konto konto) throws ApplicationException;
	
	public abstract List<String> getPROP();

}
