package de.open4me.depot.gui.menu;

import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.gui.action.UmsatzEditorAction;
import de.open4me.depot.gui.action.UmsatzImportAction;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.parts.CheckedContextMenuItem;
import de.willuhn.jameica.gui.parts.ContextMenu;
import de.willuhn.jameica.gui.parts.ContextMenuItem;
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
				Sicherheitsabfrage dialog = new Sicherheitsabfrage();
				try {
					boolean ok = dialog.open();
					if (ok) {
						if (context instanceof GenericObjectSQL) {
							GenericObjectSQL b = (GenericObjectSQL) context;
							deleteOrderItem(b);
						} else if ( context instanceof GenericObjectSQL[]) {
							deleteOrderItems((GenericObjectSQL[]) context);
						}
					}
					Utils.markRecalc(null);
				}
				catch (Exception e)
				{
					e.printStackTrace();
					throw new ApplicationException("Fehler beim Löschen");
				}
			}

		}));
		addItem(new ContextMenuItem("Hinzufügen", new UmsatzEditorAction(true)));
		addItem(new CheckedSingleContextMenuItem("Bearbeiten", new UmsatzEditorAction(false)));
		addItem(new ContextMenuItem("Importieren", new UmsatzImportAction()));
	}

	private void deleteOrderItem(GenericObjectSQL b) {
		SQLUtils.delete(b);
		tablePart.removeItem(b);
	}

	private void deleteOrderItems(GenericObjectSQL[] a) {
		Stream.of(a).forEach(this::deleteOrderItem);
	}


}

