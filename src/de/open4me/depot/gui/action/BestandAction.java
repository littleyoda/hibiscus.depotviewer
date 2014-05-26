package de.open4me.depot.gui.action;

import de.open4me.depot.gui.view.BestandView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.util.ApplicationException;

public class BestandAction implements Action
{

 public void handleAction(Object context) throws ApplicationException
 {
 	GUI.startView(BestandView.class.getName(),null);
 }

}

