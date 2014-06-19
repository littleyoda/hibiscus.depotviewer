package de.open4me.depot.gui.action;

import de.open4me.depot.tools.Bestandspruefung;
import de.willuhn.jameica.gui.Action;
import de.willuhn.util.ApplicationException;

public class BestandspruefungAction implements Action {



	@Override
	public void handleAction(Object context) throws ApplicationException {
		try {
		Bestandspruefung.exec();
		} catch (Exception e)
		{
			e.printStackTrace();
			throw new ApplicationException("Fehler beim Abgleich: " + e.getMessage(), e);
		}

	}

}