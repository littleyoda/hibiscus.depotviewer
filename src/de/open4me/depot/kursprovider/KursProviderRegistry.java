package de.open4me.depot.kursprovider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.open4me.depot.kursprovider.scalable.ScalableKursProvider;
import de.open4me.depot.kursprovider.scalable.ScalableMcpProvider;
import jsq.fetch.factory.Factory;
import jsq.fetcher.history.BaseFetcher;

/** Zentrale, geordnete Registrierung aller verfügbaren Kursprovider. */
public final class KursProviderRegistry implements AutoCloseable
{
	private final List<KursProvider> provider = new ArrayList<KursProvider>();

	public KursProviderRegistry() {}

	public static KursProviderRegistry createDefault()
	{
		KursProviderRegistry registry = new KursProviderRegistry();
		registry.register(new ScalableKursProvider(new ScalableMcpProvider()));
		for (BaseFetcher fetcher : Factory.getHistoryFetcher())
			registry.register(new JavaStockQuotesKursProvider(fetcher));
		return registry;
	}

	public void register(KursProvider kursProvider)
	{
		if (findById(kursProvider.getId()) != null)
			throw new IllegalArgumentException("Kursprovider bereits registriert: " + kursProvider.getId());
		provider.add(kursProvider);
	}

	public List<KursProvider> getAll()
	{
		return Collections.unmodifiableList(provider);
	}

	public KursProvider findById(String id)
	{
		for (KursProvider candidate : provider)
			if (candidate.getId().equals(id)) return candidate;
		return null;
	}

	@Override
	public void close()
	{
		for (KursProvider candidate : provider)
		{
			try { candidate.close(); }
			catch (Exception ignored) {}
		}
	}
}
