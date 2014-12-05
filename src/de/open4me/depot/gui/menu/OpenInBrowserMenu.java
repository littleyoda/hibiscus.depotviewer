package de.open4me.depot.gui.menu;

import de.open4me.depot.gui.action.BrowserOpenAction;
import de.willuhn.jameica.gui.parts.CheckedContextMenuItem;
import de.willuhn.jameica.gui.parts.ContextMenu;

public class OpenInBrowserMenu extends ContextMenu {
	
	public OpenInBrowserMenu(String name, String[][] fondsseiten) {
		this.setText(name);
		for (String[] seite : fondsseiten) {
			addItem(new CheckedContextMenuItem(seite[0], new BrowserOpenAction(seite[1])));
		}

	}
}
