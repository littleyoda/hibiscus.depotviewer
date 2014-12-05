package de.open4me.depot.gui.action;

import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.util.ApplicationException;

public class DepotViewerMainAction implements Action
{

  public void handleAction(Object context) throws ApplicationException
  {
  	GUI.startView(de.open4me.depot.gui.view.DepotViewerMainView.class.getName(),null);
  }

}

