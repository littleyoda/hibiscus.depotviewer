package de.open4me.depot.gui.view;

import de.open4me.depot.gui.control.AddWertpapierControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.util.Container;
import de.willuhn.jameica.gui.util.SimpleContainer;

public class AddWertpapierView extends AbstractView {

	@Override
	public void bind() throws Exception {
		final AddWertpapierControl control = new AddWertpapierControl(this);
		Container left = new SimpleContainer(getParent());
		left.addLabelPair("Auf Yahoo Finance nach Wertpapiername, WKN oder ISIN suchen:",              control.getSuchbox());

		control.getSearchButton().paint(getParent());

		control.getTab().paint(getParent());

		control.getAddButton().paint(getParent());
	}

}
