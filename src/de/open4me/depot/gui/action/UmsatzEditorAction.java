package de.open4me.depot.gui.action;

import de.open4me.depot.gui.view.UmsatzEditorView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.util.ApplicationException;

public class UmsatzEditorAction implements Action
{

	  public void handleAction(Object context) throws ApplicationException
	  {
		  System.out.println(context.getClass() + " " + context.toString());
//	    Konto k = null;
//	    if (context instanceof Konto)
//	    {
//	      k = (Konto) context;
//	    }
//	    
			GUI.startView(UmsatzEditorView.class, null);
	  }

	}
