package de.open4me.depot.gui.view;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.control.OrderListControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.GUI;

/**
 * View to show the list of existing projects.
 */
public class OrderView extends AbstractView
{

	/**
	 * @see de.willuhn.jameica.gui.AbstractView#bind()
	 */
	public void bind() throws Exception {

		GUI.getView().setTitle(Settings.i18n().tr("Orderliste"));

		OrderListControl control = new OrderListControl(this);

		control.getOrderInfoTable().paint(this.getParent());


	}
}
