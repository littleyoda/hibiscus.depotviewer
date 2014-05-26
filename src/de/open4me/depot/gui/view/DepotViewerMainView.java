package de.open4me.depot.gui.view;

import de.open4me.depot.Settings;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.util.LabelGroup;
import de.willuhn.util.ApplicationException;


public class DepotViewerMainView extends AbstractView
{

	public void bind() throws Exception {
		GUI.getView().setTitle(Settings.i18n().tr("Depot-Viewer"));
		
		LabelGroup group = new LabelGroup(this.getParent(),Settings.i18n().tr("welcome"));
		
		group.addText(Settings.i18n().tr(""),false);

	}

	public void unbind() throws ApplicationException {
	}

}
