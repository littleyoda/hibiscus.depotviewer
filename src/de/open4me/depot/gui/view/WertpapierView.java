package de.open4me.depot.gui.view;


import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.control.WertpapiereControl;
import de.open4me.depot.gui.control.WertpapiereDatenControl;
import de.open4me.depot.gui.control.WertpapiereTableControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.util.ApplicationException;

public class WertpapierView extends AbstractView
{



	private WertpapiereControl controller;

	/**
	 * @see de.willuhn.jameica.gui.AbstractView#bind()
	 */
	public void bind() throws Exception {

		GUI.getView().setTitle(Settings.i18n().tr("Wertpapier"));
		getParent().setLayout(new FillLayout());

		SashForm sashForm = new SashForm(getParent(), SWT.VERTICAL);

		WertpapiereTableControl oben = new WertpapiereTableControl();
		WertpapiereDatenControl unten = new WertpapiereDatenControl();
		controller = new WertpapiereControl(unten, oben);
		oben.setController(controller);
		unten.setController(controller);
		oben.getWertpapierControl(sashForm);
		unten.getKursChart(sashForm);

	}

	@Override
	public void unbind() throws ApplicationException {
		super.unbind();
		if (controller != null) {
			controller.unlisten();
		}
	}
}
