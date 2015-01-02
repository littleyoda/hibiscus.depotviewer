package de.open4me.depot.gui.action;

import java.rmi.RemoteException;

import de.open4me.depot.gui.control.WertpapiereControl;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.tools.UpdateStock;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.parts.TablePart;
import de.willuhn.util.ApplicationException;

public class WertpapiereAktualisierenAction implements Action
{

	private TablePart table;
	private WertpapiereControl controller;
	private boolean forceNewSettings;

	public WertpapiereAktualisierenAction(WertpapiereControl history, TablePart table) {
		this.table = table;
		this.controller = history;
		this.forceNewSettings = false;
		
	}

	public WertpapiereAktualisierenAction(WertpapiereControl controller, boolean forceNewSettings) {
		this.controller = controller;
		this.forceNewSettings = forceNewSettings;
	}

	public void handleAction(Object context) throws ApplicationException {
		if (context == null) {
			context = table.getSelection();
		}
		if (!(context instanceof GenericObjectSQL || context instanceof GenericObjectSQL[])) {
			return;
		}

		if (!(context instanceof GenericObjectSQL[])) {
			context = new GenericObjectSQL[] { (GenericObjectSQL) context };
		}
		for (GenericObjectSQL d : (GenericObjectSQL[]) context) { 
			UpdateStock.update(d, forceNewSettings);
		}
		
		// Aktualisieren
		controller.aktualisieren((GenericObjectSQL[]) context);
		try {
			controller.aktualisiereTablle();
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new ApplicationException(e);
		}

	}

}
