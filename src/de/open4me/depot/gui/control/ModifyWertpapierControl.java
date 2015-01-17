package de.open4me.depot.gui.control;

import java.rmi.RemoteException;

import de.open4me.depot.Settings;
import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.gui.view.WertpapierView;
import de.open4me.depot.rmi.Wertpapier;
import de.open4me.depot.sql.GenericObjectSQL;
import de.willuhn.datasource.rmi.DBIterator;
import de.willuhn.jameica.gui.AbstractControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.input.TextInput;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.ApplicationException;

public class ModifyWertpapierControl extends AbstractControl {

	private TextInput wertpapiername;
	private TextInput isin;
	private TextInput wkn;
	private Wertpapier wp;

	public ModifyWertpapierControl(AbstractView view) throws RemoteException {
		super(view);
		GenericObjectSQL obj = (GenericObjectSQL) view.getCurrentObject();
		wp = Utils.getWertPapierByID(obj.getAttribute("id").toString());
		wertpapiername = new TextInput(wp.getWertpapiername());
		isin = new TextInput(wp.getIsin());
		wkn = new TextInput(wp.getWkn());
	}

	public TextInput getWertpapiername() {
		return wertpapiername;
	}

	public TextInput getIsin() {
		return isin;
	}

	public TextInput getWkn() {
		return wkn;
	}

	public ButtonArea getButtons() {

		ButtonArea buttonArea = new ButtonArea();
		buttonArea.addButton("Speichern",new Action()
		{
			public void handleAction(Object context) throws ApplicationException
			{
				try {
					if (handleStore()) {
						Application.getMessagingFactory().sendMessage(new StatusBarMessage("Wertpapier gespeichert.",StatusBarMessage.TYPE_SUCCESS));
						GUI.startView(WertpapierView.class, null);
					}
				} catch (RemoteException e) {
					e.printStackTrace();
					Application.getMessagingFactory().sendMessage(new StatusBarMessage("Fehler beim Speichern.",StatusBarMessage.TYPE_ERROR));
				}


			}
		},null,false,"document-save.png");
		return buttonArea;
	}

	protected boolean handleStore() throws RemoteException, ApplicationException {
		String isin = getIsin().getValue().toString().toUpperCase();
		String name = getWertpapiername().getValue().toString();
		String wkn = getWkn().getValue().toString().toUpperCase();
		if (name.isEmpty()) {
			Application.getMessagingFactory().sendMessage(new StatusBarMessage("Wertpapiername fehlt.",StatusBarMessage.TYPE_ERROR));
			return false;
		}
		DBIterator liste = Settings.getDBService().createList(Wertpapier.class);
		liste.addFilter("(isin=? OR wkn=?) AND id <> ? ", isin, wkn, wp.getID());
		if (liste.hasNext()) {
			Application.getMessagingFactory().sendMessage(new StatusBarMessage("Die WKN oder ISIN wird bereits bei einem anderen Eintrag genutzt.",StatusBarMessage.TYPE_ERROR));
			return false;
		}
		wp.setWertpapiername(name);
		wp.setIsin(isin);
		wp.setWkn(wkn);
		wp.store();
		return true;
	}

}
