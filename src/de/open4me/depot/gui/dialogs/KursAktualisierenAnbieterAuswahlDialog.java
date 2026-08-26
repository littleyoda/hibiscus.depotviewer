package de.open4me.depot.gui.dialogs;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import de.open4me.depot.gui.action.JSQweitereQuellenAction;
import de.open4me.depot.kursprovider.KursProvider;
import de.open4me.depot.kursprovider.KursProviderRegistry;
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
	protected KursProvider auswahl;
	private Boolean speich;
	private boolean forceConnect;
	private final KursProviderRegistry registry;
	private Composite anbieterBereich;


	public KursAktualisierenAnbieterAuswahlDialog(int position, String name, KursProviderRegistry registry)
	{
		super(position);
		setTitle(name);
		this.registry = registry;
	}

	/**
	 * @see de.willuhn.jameica.gui.dialogs.AbstractDialog#paint(org.eclipse.swt.widgets.Composite)
	 */
	protected void paint(Composite parent) throws Exception
	{
		Container group = new SimpleContainer(parent);
		final CheckboxInput erlaubnis = new CheckboxInput(false);
		final CheckboxInput speichern = new CheckboxInput(false);
		Action connect = new Action() {
			@Override
			public void handleAction(Object context) throws ApplicationException
			{
				if (!Boolean.TRUE.equals(erlaubnis.getValue()))
					throw new ApplicationException("Bitte bestätigen Sie zuerst die Nutzungsbedingungen des Anbieters.");
				auswahl = (KursProvider) anbieter.getValue();
				speich = (Boolean) speichern.getValue();
				forceConnect = true;
				close();
			}
		};

		group.addText("Bitte wählen Sie den Anbieter für die Kursaktualisierung:", false);
		SelectInput auswahlInput = getHistoryKurse();
		// Jameica bindet Input-Listener nur beim Painten an das SWT-Control.
		auswahlInput.addListener(event -> updateAnbieterBereich(connect));
		group.addInput(auswahlInput);

		group.addCheckbox(erlaubnis, 
				  "Hiermit bestätigen Sie, dass Sie die \n"
				+ "Nutzungsbedingungen des Anbieters gelesen haben \n"
				+ "und Sie zu einer Nutzung berechtigt sind!\n\n");
		
		group.addCheckbox(speichern, 
				  "Aktualisierungseinstellungen speichern und \nbeim nächsten Abruf automatisch nutzen\n");
		
		anbieterBereich = new Composite(group.getComposite(), 0);
		anbieterBereich.setLayout(new GridLayout(1, false));
		GridData providerGrid = new GridData(GridData.FILL_HORIZONTAL);
		providerGrid.horizontalSpan = 2;
		anbieterBereich.setLayoutData(providerGrid);
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
				KursProvider selected = (KursProvider) anbieter.getValue();
				if (selected == null) return;
				String url = selected.getWebsite();
				try {
					Desktop.getDesktop().browse(new URI(url));
				} catch (IOException | URISyntaxException e) {
					e.printStackTrace();
				}
			}

		},null,true,"dialog-information.png");
		group.addButtonArea(buttons1);
		updateAnbieterBereich(connect);
		
		final Button weiterButton = new Button("Weiter", new Action() {
				public void handleAction(Object context) throws ApplicationException
				{
					if ((Boolean) erlaubnis.getValue()) {
						auswahl = (KursProvider) anbieter.getValue();
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
		anbieter = new SelectInput(registry.getAll(), null);
		anbieter.setName("Anbieter");
		anbieter.setMandatory(true);
		return anbieter;
	}

	public boolean getSpeichernSetting() {
		return speich;
	}

	public boolean isForceConnect()
	{
		return forceConnect;
	}

	private KursanbieterDialogErweiterung getErweiterung(KursProvider selected, Action connect)
	{
		return selected == null ? KursanbieterDialogErweiterung.KEINE : selected.createDialogErweiterung(connect);
	}

	private void updateAnbieterBereich(Action connect)
	{
		for (org.eclipse.swt.widgets.Control child : anbieterBereich.getChildren()) child.dispose();
		KursanbieterDialogErweiterung erweiterung = getErweiterung((KursProvider) anbieter.getValue(), connect);
		boolean vorhanden = erweiterung.isVisible();
		anbieterBereich.setVisible(vorhanden);
		((GridData) anbieterBereich.getLayoutData()).exclude = !vorhanden;
		try
		{
			erweiterung.paint(anbieterBereich);
		}
		catch (Exception e)
		{
			throw new RuntimeException("Anbieterspezifischer Dialogbereich konnte nicht erstellt werden.", e);
		}
		anbieterBereich.layout(true, true);
		anbieterBereich.getParent().layout(true, true);
		anbieterBereich.getShell().pack();
	}
	
	@Override
	protected Object getData() throws Exception {
		return auswahl;
	}


}
