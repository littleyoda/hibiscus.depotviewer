package de.open4me.depot.gui.action;

import de.open4me.depot.gui.view.UmsatzEditorView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.util.ApplicationException;

public class UmsatzEditorAction implements Action
{

	private boolean createnew;

	public UmsatzEditorAction(boolean b) {
		createnew = b;
	}

	public void handleAction(Object context) throws ApplicationException
	{
		if (createnew) {
			GUI.startView(UmsatzEditorView.class, null);
		} else {
			GUI.startView(UmsatzEditorView.class, context);
		}
	}

}
