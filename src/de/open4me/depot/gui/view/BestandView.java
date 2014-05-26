package de.open4me.depot.gui.view;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.control.BestandControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.GUI;

public class BestandView extends AbstractView
{

	/**
	 * @see de.willuhn.jameica.gui.AbstractView#bind()
	 */
	public void bind() throws Exception {

		GUI.getView().setTitle(Settings.i18n().tr("Bestand"));

		BestandControl control = new BestandControl(this);

		control.getProjectsTable().paint(this.getParent());


	}
}