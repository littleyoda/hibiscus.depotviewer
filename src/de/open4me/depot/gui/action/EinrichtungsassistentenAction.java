package de.open4me.depot.gui.action;

import de.open4me.depot.gui.dialogs.EinrichtungsAssistenten;
import de.willuhn.jameica.gui.Action;

public class EinrichtungsassistentenAction  implements Action
{

	public void handleAction(Object context) 
	{
		EinrichtungsAssistenten a = new EinrichtungsAssistenten(EinrichtungsAssistenten.POSITION_CENTER);
		try {
			a.open();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

