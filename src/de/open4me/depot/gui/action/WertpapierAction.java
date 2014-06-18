package de.open4me.depot.gui.action;

import de.open4me.depot.gui.view.WertpapierView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.util.ApplicationException;

public class WertpapierAction implements Action
{

	 public void handleAction(Object context) throws ApplicationException
	 {
	 	GUI.startView(WertpapierView.class.getName(),null);
	 }

	}
