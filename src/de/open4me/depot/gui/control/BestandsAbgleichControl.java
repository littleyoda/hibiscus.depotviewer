package de.open4me.depot.gui.control;

import de.willuhn.jameica.gui.AbstractControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Part;
import de.willuhn.jameica.gui.parts.FormTextPart;

public class BestandsAbgleichControl extends AbstractControl {

	  private Part libList = null;

	  public BestandsAbgleichControl(AbstractView view) {
	    super(view);
	  }

	  /**
	   * Liefert eine Liste mit allen direkt von Hibiscus verwendeten Komponenten.
	   * @return Liste der verwendeten Komponenten
	   */
	  public Part getLibList()
	  {
	    if (libList != null)
	      return libList;


	    StringBuffer buffer = new StringBuffer();
	    buffer.append("<form>" +
	    (String) view.getCurrentObject()
	    + "</form>");

	    libList = new FormTextPart(buffer.toString());
	    return libList;
	  }
	}
