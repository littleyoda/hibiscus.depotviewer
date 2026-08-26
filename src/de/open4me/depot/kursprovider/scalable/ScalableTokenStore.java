package de.open4me.depot.kursprovider.scalable;

import de.willuhn.jameica.store.BeanContainer;
import de.willuhn.jameica.store.BeanStore;
import de.willuhn.util.ApplicationException;

/** Kapselt Jameicas verschlüsselten BeanStore für genau eine OAuth-Sitzung. */
public class ScalableTokenStore
{
	private final BeanStore store = new BeanStore();

	public synchronized ScalableOAuthData load() throws ApplicationException
	{
		BeanContainer<ScalableOAuthData> container = store.load(ScalableOAuthData.class, true);
		return container.getBeans().isEmpty() ? null : container.getBeans().get(0);
	}

	public synchronized void save(ScalableOAuthData data) throws ApplicationException
	{
		BeanContainer<ScalableOAuthData> container = store.load(ScalableOAuthData.class, true);
		container.getBeans().clear();
		container.getBeans().add(data);
		store.store(container);
	}

	public synchronized void clear() throws ApplicationException
	{
		BeanContainer<ScalableOAuthData> container = store.load(ScalableOAuthData.class, true);
		container.getBeans().clear();
		store.store(container);
	}

	public synchronized boolean hasSession()
	{
		try
		{
			ScalableOAuthData data = load();
			return data != null && data.getRefreshToken() != null && !data.getRefreshToken().isBlank();
		}
		catch (Exception e)
		{
			return false;
		}
	}
}
