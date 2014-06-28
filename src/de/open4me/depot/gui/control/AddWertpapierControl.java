package de.open4me.depot.gui.control;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jsq.tools.HtmlUnitTools;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.ThreadedRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;

import de.open4me.depot.Settings;
import de.open4me.depot.depotabruf.Utils;
import de.open4me.depot.gui.action.OrderList;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.willuhn.jameica.gui.AbstractControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.input.Input;
import de.willuhn.jameica.gui.input.TextInput;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.gui.parts.TablePart;
import de.willuhn.util.ApplicationException;

public class AddWertpapierControl  extends AbstractControl {

	private Input suchbox;
	private TablePart bestandsList;
	
	public AddWertpapierControl(AbstractView view) {
		super(view);
	}

	public Input getSuchbox() {
		if (this.suchbox == null)
		{
			this.suchbox = new TextInput("");
		}
		return this.suchbox;
	}
	
	public TablePart getTab() {
		if (this.bestandsList != null) {
			return bestandsList;
		}
		bestandsList = new TablePart(new ArrayList(),new OrderList());

		bestandsList.addColumn(Settings.i18n().tr("Name"), "Name");
		bestandsList.addColumn(Settings.i18n().tr("Typ"),"Typ");
		bestandsList.addColumn(Settings.i18n().tr("ISIN"),"Isin");
		return bestandsList;
	}
	public ButtonArea getSearchButton() {
		ButtonArea buttons = new ButtonArea();
		buttons.addButton("Suchen",new Action() {

			@Override
			public void handleAction(Object context)
					throws ApplicationException {
				try {
					WebClient webClient = new WebClient();
					webClient.setCssErrorHandler(new SilentCssErrorHandler());
					webClient.setRefreshHandler(new ThreadedRefreshHandler());
					webClient.getOptions().setJavaScriptEnabled(false);
					webClient.getOptions().setThrowExceptionOnScriptError(false);
					java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(java.util.logging.Level.OFF);
					HtmlPage page = webClient.getPage("https://de.finance.yahoo.com/lookup/all?s=" + getSuchbox().getValue() +  "&t=A&m=ALL&r=");
					HtmlTable tab = (HtmlTable) HtmlUnitTools.getElementByPartContent(page, "Ticker", "table");
					if (tab == null) {
						return;
					}
					List<HashMap<String, String>> x = HtmlUnitTools.analyse(tab);
					TablePart ziel = getTab();
					ziel.removeAll();
					for (HashMap<String, String> map : x) {
						if (map.get("Isin") == null) {
							continue;
						}
						GenericObjectHashMap obj = new GenericObjectHashMap(map);
						ziel.addItem(obj);
					}
				} catch (FailingHttpStatusCodeException | IOException e) {
					e.printStackTrace();
					throw new ApplicationException(e);
				}

			}

		},null ,false,"edit-copy.png");
		return buttons;
	}

	public ButtonArea getAddButton() {
		ButtonArea buttons = new ButtonArea();
		buttons.addButton("Hinzufügen",new Action() {

			@Override
			public void handleAction(Object context)
					throws ApplicationException {
				Object selection = getTab().getSelection();
				if (selection == null || !(selection instanceof GenericObjectHashMap)) {
					return;
				}
				GenericObjectHashMap sel = (GenericObjectHashMap) selection;
				try {
					Utils.getORcreateWKN("", (String) sel.getAttribute("Isin"), (String) sel.getAttribute("Name"));
				} catch (RemoteException e) {
					e.printStackTrace();
					throw new ApplicationException(e);
				}
			  	GUI.startView(de.open4me.depot.gui.view.WertpapierView.class.getName(),null);
			}

		},null ,false,"edit-copy.png");
		return buttons;
	}
}


