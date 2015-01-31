package de.open4me.depot.gui.action;

import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.tools.UpdateStock;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.parts.TablePart;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.ApplicationException;

public class WertpapiereAktualisierenAction implements Action
{

	private TablePart table;
	private boolean forceNewSettings;

	public WertpapiereAktualisierenAction(TablePart table) {
		this.table = table;
		this.forceNewSettings = false;

	}

	public WertpapiereAktualisierenAction(boolean forceNewSettings) {
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

		UpdateStock us = new UpdateStock((GenericObjectSQL[]) context, forceNewSettings);
		Application.getController().start(us);

	}

}

