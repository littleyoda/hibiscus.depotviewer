package de.open4me.depot.gui.view;

import de.open4me.depot.gui.control.AddWertpapierControl;
import de.open4me.depot.tools.WertpapierSuche;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.util.Container;
import de.willuhn.jameica.gui.util.SimpleContainer;

public class AddWertpapierView extends AbstractView {

	@Override
	public void bind() throws Exception {
		final AddWertpapierControl control = new AddWertpapierControl(this);
		Container left = new SimpleContainer(getParent());
		left.addLabelPair("Nach Wertpapiername, WKN oder ISIN suchen:",              control.getSuchbox());
		if (!WertpapierSuche.isXetaAvail()) {
			left.addText("Um die Suchergebnisse zu verbessern,  sollten sie die 'Gesamtliste aller Xetra-handelbaren Wertpapiere' über den Menüpunkt Depot-Viewer herunterladen lassen.", true);
		}

		control.getSearchButton().paint(getParent());

		control.getTab().paint(getParent());

		control.getAddButton().paint(getParent());
	}

}
