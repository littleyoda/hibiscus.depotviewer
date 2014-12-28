package de.open4me.depot.gui.view;

import java.rmi.RemoteException;

import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.gui.control.UmsatzEditorControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.gui.util.ColumnLayout;
import de.willuhn.jameica.gui.util.Container;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.ApplicationException;

public class UmsatzEditorView extends AbstractView {

	private UmsatzEditorControl control;

	@Override
	public void bind() throws Exception {
		this.control = new UmsatzEditorControl(this);
		Container cl = new SimpleContainer(getParent());

		cl.addHeadline("Order / Umsätze");

		ColumnLayout columns = new ColumnLayout(cl.getComposite(),2);
		Container left = new SimpleContainer(columns.getComposite());

		left.addLabelPair("Wertpapier", control.getWertpapiere());
		left.addLabelPair("Datum", control.getDate());
		left.addLabelPair("Konto", control.getKonto());
		left.addLabelPair("Aktion", control.getAktionAuswahl());

		Container right = new SimpleContainer(columns.getComposite());

		right.addLabelPair("Anzahl", control.getAnzahl());
		right.addLabelPair("Einzelkurs", control.getEinzelkurs());

		ButtonArea buttonArea = new ButtonArea();
		buttonArea.addButton("Speichern",new Action()
		{
			public void handleAction(Object context) throws ApplicationException
			{
				try {
					control.handleStore();
				} catch (RemoteException e) {
					e.printStackTrace();
					throw new ApplicationException("Fehler beim Speichern.", e);
				}
		        Application.getMessagingFactory().sendMessage(new StatusBarMessage("Order / Umsatz gespeichert.",StatusBarMessage.TYPE_SUCCESS));
				GUI.startView(OrderView.class, null);


			}
		},null,false,"document-save.png");
		buttonArea.paint(getParent());
	}

}
