package de.open4me.depot.gui.dialogs;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import jsq.fetch.factory.Factory;
import jsq.fetcher.history.BaseFetcher;

import org.eclipse.swt.widgets.Composite;

import de.open4me.depot.gui.action.JSQweitereQuellenAction;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.dialogs.AbstractDialog;
import de.willuhn.jameica.gui.input.CheckboxInput;
import de.willuhn.jameica.gui.input.SelectInput;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.gui.util.Container;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.jameica.system.OperationCanceledException;
import de.willuhn.util.ApplicationException;

public class KursAktualisierenAnbieterAuswahlDialog extends AbstractDialog
{	  
	private SelectInput anbieter;
	protected BaseFetcher bf;
	private Boolean speich;


	public KursAktualisierenAnbieterAuswahlDialog(int position, String name)
	{
		super(position);
		setTitle(name);
	}

	/**
	 * @see de.willuhn.jameica.gui.dialogs.AbstractDialog#paint(org.eclipse.swt.widgets.Composite)
	 */
	protected void paint(Composite parent) throws Exception
	{
		Container group = new SimpleContainer(parent);
		group.addText("Bitte wählen Sie den Anbeiter für die Kursaktualisierung:", false);
		group.addInput(getHistoryKurse());
		
		final CheckboxInput erlaubnis = new CheckboxInput(false);
		group.addCheckbox(erlaubnis, 
				  "Hiermit bestätigen Sie, dass Sie die \n"
				+ "Nutzungsbedingungen des Anbieters gelesen haben \n"
				+ "und Sie zu einer Nutzung berechtigt sind!\n\n");
		
		final CheckboxInput speichern = new CheckboxInput(false);
		group.addCheckbox(speichern, 
				  "Aktualisierungseinstellungen speichern und \nbeim nächsten Abruf automatisch nutzen\n");
		
		ButtonArea buttons1 = new ButtonArea();
		buttons1.addButton(i18n.tr("Weitere Datenquellen installieren"), new Action() {

			@Override
			public void handleAction(Object context)
					throws ApplicationException {
				(new JSQweitereQuellenAction()).handleAction(context);
				throw new OperationCanceledException("Abgebrochen");
			}
			
		}
		,null,true,"dialog-information.png");

		buttons1.addButton(i18n.tr("Anbieter Webseite öffnen"), new Action() {
			public void handleAction(Object context) throws ApplicationException
			{
				String url = ((BaseFetcher) anbieter.getValue()).getURL();
				try {
					Desktop.getDesktop().browse(new URI(url));
				} catch (IOException | URISyntaxException e) {
					e.printStackTrace();
				}
			}

		},null,true,"dialog-information.png");
		group.addButtonArea(buttons1);
		
		final Button weiterButton = new Button("Weiter", new Action() {
				public void handleAction(Object context) throws ApplicationException
				{
					if ((Boolean) erlaubnis.getValue()) {
						bf = (BaseFetcher) anbieter.getValue();
						speich = (Boolean) speichern.getValue();
						close();
					}
				}},null ,false, "ok.png");

		ButtonArea buttons = new ButtonArea();
		buttons.addButton(weiterButton);
		buttons.addButton(i18n.tr("Abbrechen"), new Action() {
			public void handleAction(Object context) throws ApplicationException
			{
				throw new OperationCanceledException("Abgebrochen");
			}

		},null,true,"process-stop.png");
		group.addButtonArea(buttons);
	}
	

	private SelectInput getHistoryKurse()
	{
		anbieter = new SelectInput(Factory.getHistoryFetcher(), null);
		anbieter.setName("Anbieter");
		anbieter.setMandatory(true);
		return anbieter;
	}

	public boolean getSpeichernSetting() {
		return speich;
	}
	
	@Override
	protected Object getData() throws Exception {
		return bf;
	}


}
