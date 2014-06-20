package de.open4me.depot.gui.view;

import de.open4me.depot.gui.control.LizenzinformationControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.GUI;

public class LizenzinformationView extends AbstractView
{

	public void bind() throws Exception
	  {
			GUI.getView().setTitle("Lizenzinformationen");
	    
			LizenzinformationControl control = new LizenzinformationControl(this);

			control.getTextPart().paint(getParent());
	  }
	}