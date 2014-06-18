package de.open4me.depot.gui.view;


import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.control.WertpaperHistoryControl;
import de.open4me.depot.gui.control.WertpapiereTableControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.GUI;

public class WertpapierView extends AbstractView
{



	/**
	 * @see de.willuhn.jameica.gui.AbstractView#bind()
	 */
	public void bind() throws Exception {

		GUI.getView().setTitle(Settings.i18n().tr("Wertpapier"));
		getParent().setLayout(new FillLayout());

		SashForm sashForm = new SashForm(getParent(), SWT.VERTICAL);

		WertpaperHistoryControl history = new WertpaperHistoryControl();
		WertpapiereTableControl control = new WertpapiereTableControl(history);
		
		control.getWepierControl(sashForm);
		history.getKursChart(sashForm);

	}
}