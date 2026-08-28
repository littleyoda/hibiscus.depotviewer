package de.open4me.depot.kursprovider.scalable;

import java.util.Collections;

import de.open4me.depot.gui.dialogs.KursanbieterDialogErweiterung;
import de.open4me.depot.gui.dialogs.ScalableKursanbieterDialogErweiterung;
import de.open4me.depot.kursprovider.KursProvider;
import de.open4me.depot.kursprovider.KursProviderErgebnis;
import de.open4me.depot.kursprovider.KursProviderKontext;
import de.willuhn.jameica.gui.Action;
import de.willuhn.util.ApplicationException;

/** Vollständige Einbindung von Scalable MCP in den allgemeinen Providervertrag. */
public final class ScalableKursProvider implements KursProvider
{
	public static final String ID = "scalable-capital-mcp";
	private final ScalableMcpProvider provider;

	public ScalableKursProvider(ScalableMcpProvider provider)
	{
		this.provider = provider;
	}

	@Override public String getId() { return ID; }
	@Override public String getName() { return "Scalable Capital (MCP)"; }
	@Override public String getWebsite() { return "https://mcp.scalable.capital/"; }
	@Override public String toString() { return getName(); }

	@Override
	public KursProviderErgebnis abrufen(KursProviderKontext context) throws Exception
	{
		if (context.isVorbereiten()) provider.connect();
		if (context.getWertpapier().isEmpty("isin"))
			throw new ApplicationException("Scalable Capital benötigt eine ISIN; für dieses Wertpapier ist keine hinterlegt.");
		context.getMonitor().setStatusText("Scalable Capital: Chart-Historie abrufen");
		return new KursProviderErgebnis(
				provider.fetch(context.getWertpapier().getAttribute("isin").toString()), Collections.emptyList());
	}

	@Override
	public KursanbieterDialogErweiterung createDialogErweiterung(Action vorbereiten)
	{
		return createDialogErweiterung(vorbereiten, () -> {});
	}

	@Override
	public KursanbieterDialogErweiterung createDialogErweiterung(Action vorbereiten, Runnable statusGeaendert)
	{
		return new ScalableKursanbieterDialogErweiterung(provider, vorbereiten, statusGeaendert);
	}

	@Override public boolean istAbrufbereit() { return provider.isConnected(); }
	@Override public boolean ersetztBestandBeimWechsel() { return true; }
	@Override public void close() { provider.close(); }
}
