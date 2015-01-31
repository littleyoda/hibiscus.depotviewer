package de.open4me.depot.gui.view;

import de.open4me.depot.gui.control.LizenzinformationControl;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.util.ApplicationException;

public class LizenzinformationView extends AbstractView
{

	public void bind() throws Exception
	  {
			GUI.getView().setTitle("Lizenzinformationen");
	    
			LizenzinformationControl control = new LizenzinformationControl(this);

			control.getTextPart().paint(getParent());
			if ("depotviewerdebug".equals(System.getenv().get("depotviewerdebug"))) {
				ButtonArea buttons = new ButtonArea();
				buttons.addButton("Leere alles", new Action() {
					public void handleAction(Object context) throws ApplicationException {
						for (String s : new String[] { "truncate table depotviewer_umsaetze;", "truncate table depotviewer_bestand;", "truncate table depotviewer_wertpapier;", "truncate table depotviewer_kurse;" } ) {
							SQLUtils.exec(s);
						}
					}

				},null,true,"ok.png");
				buttons.addButton("Leere Kursinformationen", new Action() {
					public void handleAction(Object context) throws ApplicationException {
						for (String s : new String[] { "truncate table depotviewer_kurse;" } ) {
							SQLUtils.exec(s);
						}
					}

				},null,true,"ok.png");
				buttons.paint(getParent());
			}
	  }
	}