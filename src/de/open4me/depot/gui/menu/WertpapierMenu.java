package de.open4me.depot.gui.menu;

import de.open4me.depot.gui.action.AddWertpapierAction;
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
	

	public WertpapierMenu() {
		addItem(new CheckedContextMenuItem("Wertpapier hinzufügen...", new AddWertpapierAction()));
		addItem(ContextMenuItem.SEPARATOR);
		addMenu(new OpenInBrowserMenu("Webseiten (allgemein)", allgseiten));
		addMenu(new OpenInBrowserMenu("Webseiten (Fonds)", fondsseiten));
	}
}

