package de.open4me.depot.gui.action;

import java.rmi.RemoteException;

import de.open4me.depot.gui.view.WertpapierView;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.tools.Wertpapier;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.util.ApplicationException;

public class DeleteWertpapierAction  implements Action {

	@Override
	public void handleAction(Object context) throws ApplicationException {
		try {
			GenericObjectSQL o = (GenericObjectSQL) context;
			if (!Wertpapier.isInUse(o.getID())) {
				Wertpapier.deleteWertpapier(o.getID());
			}
			GUI.startView(WertpapierView.class.getName(),null);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
	}

}
