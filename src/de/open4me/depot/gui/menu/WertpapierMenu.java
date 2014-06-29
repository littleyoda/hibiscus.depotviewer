package de.open4me.depot.gui.menu;

import de.willuhn.jameica.gui.parts.ContextMenu;

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
		};
	

	public WertpapierMenu() {
		addMenu(new OpenInBrowserMenu("Webseiten (allgemein)", allgseiten));
		addMenu(new OpenInBrowserMenu("Webseiten (Fonds)", fondsseiten));
	}
}

