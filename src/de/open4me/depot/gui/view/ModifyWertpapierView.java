package de.open4me.depot.gui.view;

import de.open4me.depot.gui.control.ModifyWertpapierControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.util.ColumnLayout;
import de.willuhn.jameica.gui.util.Container;
import de.willuhn.jameica.gui.util.SimpleContainer;

public class ModifyWertpapierView extends AbstractView {

	@Override
	public void bind() throws Exception {
		ModifyWertpapierControl  control = new ModifyWertpapierControl(this);
		ColumnLayout columns = new ColumnLayout(getParent(),2);
		Container left = new SimpleContainer(columns.getComposite());
		left.addLabelPair("Name", control.getWertpapiername());
		left.addLabelPair("ISIN", control.getIsin());
		left.addLabelPair("WKN", control.getWkn());
		left.addButtonArea(control.getButtons());
		
	}

}
