package de.open4me.depot.gui.menu;

import java.rmi.RemoteException;

import de.open4me.depot.gui.action.AddWertpapierAction;
import de.open4me.depot.gui.action.DeleteWertpapierAction;
import de.open4me.depot.gui.action.ModifyWertpapierAction;
import de.open4me.depot.gui.action.WertpapiereAktualisierenAction;
import de.open4me.depot.gui.control.WertpapiereControl;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.tools.Wertpapier;
import de.willuhn.jameica.gui.parts.CheckedContextMenuItem;
import de.willuhn.jameica.gui.parts.ContextMenu;
import de.willuhn.jameica.gui.parts.ContextMenuItem;

public class WertpapierMenu extends ContextMenu
{
	String[][] allgseiten ={ 
			{ "Ariva.de", "http://www.ariva.de/search/search.m?searchname={}"},
			{ "Finanzen.net", "http://www.finanzen.net/suchergebnis.asp?frmAktiensucheTextfeld={}" },
			{ "Finanztreff.de", "http://www.finanztreff.de/kurse_einzelkurs_suche.htn?suchbegriff={}"},
			{ "Onvista.de", "http://www.onvista.de/suche/{}"},
			{ "Yahoo Finance DE", "http://de.finance.yahoo.com/q?s={}"},
					
				};

	String[][] fondsseiten ={ 
			{ "Fondsweb.de", "http://www.fondsweb.de/suche?QUERY={}" },
			{ "Justetf.com", "https://www.justetf.com/de/etf-profile.html?isin={}"},
			{ "Morningstar.de", "http://www.morningstar.de/de/funds/SecuritySearchResults.aspx?type=ALL&search={}" },
		//	{ "Moneymeets.de", "https://moneymeets.depotstand.de/app.php/produkt/Factsheet/{isin}" }
		};
	

	public WertpapierMenu(WertpapiereControl controller) {
		addItem(new ContextMenuItem("Wertpapier hinzufügen...", new AddWertpapierAction()));
		addItem(new CheckedContextMenuItem("Wertpapier bearbeiten...", new ModifyWertpapierAction()));
		addItem(new CheckedContextMenuItem("Wertpapier löschen", new DeleteWertpapierAction()) {
			/**
			 * @see de.willuhn.jameica.gui.parts.ContextMenuItem#isEnabledFor(java.lang.Object)
			 */
			public boolean isEnabledFor(Object o)
			{
				if (o == null || !(o instanceof GenericObjectSQL) ) {
					return false;
				}
				GenericObjectSQL b = (GenericObjectSQL) o;	
				try {
					return !Wertpapier.isInUse(b.getID());
				} catch (RemoteException e) {
					e.printStackTrace();
					return false;
				}
			}			
		});
		addItem(ContextMenuItem.SEPARATOR);
		addItem(new ContextMenuItem("Aktualisieren...", new WertpapiereAktualisierenAction(false)));
		addItem(new ContextMenuItem("Aktualisieren (Einstellungen wählen)...", new WertpapiereAktualisierenAction(true)));
		addItem(ContextMenuItem.SEPARATOR);
		addMenu(new OpenInBrowserMenu("Webseiten (allgemein)", allgseiten));
		addMenu(new OpenInBrowserMenu("Webseiten (Fonds)", fondsseiten));
	}
}

