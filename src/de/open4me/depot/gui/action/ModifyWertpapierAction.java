package de.open4me.depot.gui.action;

import java.rmi.RemoteException;

import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.datenobj.rmi.Wertpapier;
import de.open4me.depot.gui.view.ModifyWertpapierView;
import de.open4me.depot.sql.GenericObjectSQL;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.util.ApplicationException;

public class ModifyWertpapierAction implements Action
{


	@Override
	public void handleAction(Object context) throws ApplicationException
	  {
		GenericObjectSQL obj = (GenericObjectSQL) context;
		Wertpapier wp;
		try {
			wp = Utils.getWertPapierByID(obj.getAttribute("id").toString());
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new ApplicationException(e);
		}
	  	GUI.startView(ModifyWertpapierView.class, wp);
	  }


}