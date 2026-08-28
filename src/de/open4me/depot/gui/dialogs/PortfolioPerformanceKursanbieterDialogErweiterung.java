package de.open4me.depot.gui.dialogs;

import java.rmi.RemoteException;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;

import de.open4me.depot.kursprovider.portfolio.PortfolioPerformanceKursProvider;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.input.LinkInput;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.gui.util.Container;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.util.ApplicationException;

/** Verbindungssteuerung und Quellenhinweis für Portfolio Performance. */
public class PortfolioPerformanceKursanbieterDialogErweiterung implements KursanbieterDialogErweiterung
{
	private final PortfolioPerformanceKursProvider provider;
	private final Action connect;

	public PortfolioPerformanceKursanbieterDialogErweiterung(PortfolioPerformanceKursProvider provider, Action connect)
	{
		this.provider = provider;
		this.connect = connect;
	}

	@Override
	public void paint(Composite parent) throws Exception
	{
		Container container = new SimpleContainer(parent);
		container.addText("Die Kurse werden von Portfolio Performance bereitgestellt.", false);
		LinkInput link = new LinkInput("<a href=\"" + PortfolioPerformanceKursProvider.WEBSITE + "\">"
				+ PortfolioPerformanceKursProvider.WEBSITE + "</a>");
		link.addListener(event -> Program.launch(PortfolioPerformanceKursProvider.WEBSITE));
		container.addInput(link);
		container.addText("Portfolio-Performance-Verbindung: "
				+ (provider.isConnected() ? "verbunden" : "nicht verbunden"), false);

		ButtonArea buttons = new ButtonArea();
		buttons.addButton(new ProviderButton(provider.isConnected() ? "Neu verbinden" : "Verbinden", connect));
		if (provider.isConnected())
		{
			buttons.addButton(new ProviderButton("Trennen", context -> {
				try { provider.disconnect(); }
				catch (Exception e) { throw new ApplicationException("Portfolio Performance konnte nicht getrennt werden.", e); }
				for (org.eclipse.swt.widgets.Control child : parent.getChildren()) child.dispose();
				try { paint(parent); }
				catch (Exception e) { throw new ApplicationException("Portfolio-Performance-Steuerung konnte nicht aktualisiert werden.", e); }
				parent.layout(true, true);
			}));
		}
		container.addButtonArea(buttons);
	}

	private static final class ProviderButton extends Button
	{
		ProviderButton(String title, Action action) { super(title, action); }

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
