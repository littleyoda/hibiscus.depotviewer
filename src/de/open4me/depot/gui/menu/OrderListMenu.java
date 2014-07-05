package de.open4me.depot.gui.menu;

import de.open4me.depot.gui.action.UmsatzEditorAction;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.parts.CheckedContextMenuItem;
import de.willuhn.jameica.gui.parts.ContextMenu;
import de.willuhn.jameica.gui.parts.TablePart;
import de.willuhn.util.ApplicationException;

/**
 */
public class OrderListMenu extends ContextMenu
{
	private TablePart tablePart;

	public OrderListMenu(TablePart orderList) {
		this.tablePart = orderList;
		addItem(new CheckedContextMenuItem("Löschen",new Action() {

			@Override
			public void handleAction(Object context)
					throws ApplicationException {
				if (context == null || !(context instanceof GenericObjectSQL))
					return;
				GenericObjectSQL b = (GenericObjectSQL) context;
				try {
					SQLUtils.delete(b);
					tablePart.removeItem(b);
				}
				catch (Exception e)
				{
					e.printStackTrace();
					throw new ApplicationException("Fehler beim Löschen");
				}
			}

		}));
		addItem(new CheckedContextMenuItem("Hinzufügen", new UmsatzEditorAction(true)));
		addItem(new CheckedContextMenuItem("Bearbeiten", new UmsatzEditorAction(false)));
	}
}

