package de.open4me.depot.gui.control;

import java.rmi.RemoteException;

import de.open4me.depot.sql.GenericObjectSQL;

public class WertpapiereControl {

	private WertpapiereDatenControl unten;
	private WertpapiereTableControl oben;

	public WertpapiereControl(WertpapiereDatenControl unten,
			WertpapiereTableControl oben) {
		this.oben = oben;
		this.unten = unten;
	}

	public void aktualisiereTablle() throws RemoteException {
		oben.aktualisiere();
	}
	public void aktualisieren(GenericObjectSQL[] selection) {
		unten.update(selection);
		
	}

	public void aktualisieren(GenericObjectSQL d) {
		unten.update(d);
	}

}
