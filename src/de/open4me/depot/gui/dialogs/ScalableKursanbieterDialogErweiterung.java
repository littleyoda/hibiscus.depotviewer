package de.open4me.depot.gui.dialogs;

import java.rmi.RemoteException;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import de.open4me.depot.kursprovider.scalable.ScalableMcpProvider;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.gui.util.Container;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.util.ApplicationException;

/** Ausschließlich bei Scalable sichtbare Verbindungssteuerung. */
public class ScalableKursanbieterDialogErweiterung implements KursanbieterDialogErweiterung
{
	private final ScalableMcpProvider provider;
	private final Action connect;
	private final Runnable statusGeaendert;

	public ScalableKursanbieterDialogErweiterung(ScalableMcpProvider provider, Action connect, Runnable statusGeaendert)
	{
		this.provider = provider;
		this.connect = connect;
		this.statusGeaendert = statusGeaendert;
	}

	@Override
	public void paint(org.eclipse.swt.widgets.Composite parent) throws Exception
	{
		Container container = new SimpleContainer(parent);
		container.addText("Um diesen Anbieter zu nutzen, muss unter Profil / Agentic Investing\n"
				+ "die Funktion \"Scalable MCP\" aktiviert werden. Bei der ersten Nutzung\n"
				+ "ist dann eine Kontoanmeldung notwendig, die sich im Browser automatisch öffnet.", false);
		container.addText("Scalable-Verbindung: " + (provider.isConnected() ? "verbunden" : "nicht verbunden"), false);

		ButtonArea buttons = new ButtonArea();
		buttons.addButton(new ProviderButton(provider.isConnected() ? "Neu verbinden" : "Verbinden", connect));
		if (provider.isConnected())
		{
			buttons.addButton(new ProviderButton("Trennen", new Action()
			{
				@Override
				public void handleAction(Object context) throws ApplicationException
				{
					provider.disconnect();
					for (org.eclipse.swt.widgets.Control child : parent.getChildren()) child.dispose();
					try { paint(parent); }
					catch (Exception e) { throw new ApplicationException("Scalable-Steuerung konnte nicht aktualisiert werden.", e); }
					parent.layout(true, true);
					statusGeaendert.run();
				}
			}));
		}
		container.addButtonArea(buttons);
	}

	private static final class ProviderButton extends Button
	{
		ProviderButton(String title, Action action)
		{
			super(title, action);
		}

		@Override
		public void paint(Composite parent) throws RemoteException
		{
			super.paint(parent);
			GridData data = (GridData) button.getLayoutData();
			data.widthHint = 145;
			data.horizontalAlignment = GridData.FILL;
		}
	}
}
