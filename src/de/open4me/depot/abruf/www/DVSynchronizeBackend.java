package de.open4me.depot.abruf.www;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import de.open4me.depot.DepotViewerPlugin;
import de.open4me.depot.abruf.impl.DepotAbrufFabrik;
import de.willuhn.annotation.Lifecycle;
import de.willuhn.annotation.Lifecycle.Type;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.hbci.synchronize.AbstractSynchronizeBackend;
import de.willuhn.jameica.hbci.synchronize.SynchronizeJobProvider;
import de.willuhn.jameica.hbci.synchronize.jobs.SynchronizeJob;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;
import de.willuhn.util.ProgressMonitor;

/**
 * Implementierung eines Sync-Backends.
 */
@Lifecycle(Type.CONTEXT)
public class DVSynchronizeBackend extends AbstractSynchronizeBackend
{
	private final static I18N i18n = Application.getPluginLoader().getPlugin(DepotViewerPlugin.class).getResources().getI18N();


	/**
	 * @see de.willuhn.jameica.hbci.synchronize.AbstractSynchronizeBackend#createJobGroup(de.willuhn.jameica.hbci.rmi.Konto)
	 */
	@Override
	protected JobGroup createJobGroup(Konto k)
	{
		return new MMTgJobGroup(k);
	}

	/**
	 * @see de.willuhn.jameica.hbci.synchronize.AbstractSynchronizeBackend#getJobProviderInterface()
	 */
	@Override
	protected Class<? extends SynchronizeJobProvider> getJobProviderInterface()
	{
		return DVSynchronizeJobProvider.class;
	}

	/**
	 * @see de.willuhn.jameica.hbci.synchronize.AbstractSynchronizeBackend#getPropertyNames(de.willuhn.jameica.hbci.rmi.Konto)
	 */
	@Override
	public List<String> getPropertyNames(Konto konto)
	{

		try
		{
			if (konto == null || konto.hasFlag(Konto.FLAG_DISABLED)) {
				return null;
			}
			return DepotAbrufFabrik.getDepotAbruf(konto).getPROP();
		} catch (RemoteException re) {
			Logger.error("unable to determine property-names",re);
			return null;
		} catch (ApplicationException e) {
			Logger.error("unable to determine property-names", e);
			return null;
		}
	}

	/**
	 * @see de.willuhn.jameica.hbci.synchronize.SynchronizeBackend#supports(java.lang.Class, de.willuhn.jameica.hbci.rmi.Konto)
	 */
	@Override
	public boolean supports(Class<? extends SynchronizeJob> type, Konto konto)
	{
		boolean b = super.supports(type,konto);
		if (!b)
			return false;

		try
		{

			return DepotAbrufFabrik.getDepotAbruf(konto) != null;
		}
		catch (RemoteException re)
		{
			Logger.error("unable to check for Depotsupport",re);
		} catch (ApplicationException e) {
			Logger.error("unable to check for Depotsupport", e);
		}
		return false;
	}

	/**
	 * @see de.willuhn.jameica.hbci.synchronize.AbstractSynchronizeBackend#getSynchronizeKonten(de.willuhn.jameica.hbci.rmi.Konto)
	 */
	public List<Konto> getSynchronizeKonten(Konto k)
	{
		List<Konto> list = super.getSynchronizeKonten(k);
		List<Konto> result = new ArrayList<Konto>();

		for (Konto konto:list)
		{
					result.add(konto);
		}

		return result;
	}

	

	/**
	 * @see de.willuhn.jameica.hbci.synchronize.SynchronizeBackend#getName()
	 */
	@Override
	public String getName()
	{
		return i18n.tr("Depot Viewer");
	}

	/**
	 * Hier findet die eigentliche Ausfuehrung des Jobs statt.
	 */
	protected class MMTgJobGroup extends JobGroup
	{
		/**
		 * ct.
		 * @param k
		 */
		protected MMTgJobGroup(Konto k)
		{
			super(k);
		}

		/**
		 * @see de.willuhn.jameica.hbci.synchronize.AbstractSynchronizeBackend.JobGroup#sync()
		 */
		@Override
		protected void sync() throws Exception
		{
			////////////////////////////////////////////////////////////////////
			// lokale Variablen
			ProgressMonitor monitor = worker.getMonitor();
			String kn               = this.getKonto().getLongName();

			int step = 100 / worker.getSynchronization().size();
			////////////////////////////////////////////////////////////////////

			try
			{
				this.checkInterrupted();

				monitor.log(" ");
				monitor.log(i18n.tr("Synchronisiere Konto: {0}",kn));

				Logger.info("processing jobs");
				for (SynchronizeJob job:this.jobs)
				{
					this.checkInterrupted();
					DVSynchronizeJob j = (DVSynchronizeJob) job;
					j.execute();

					monitor.addPercentComplete(step);
				}
			}
			catch (Exception e)
			{
				throw e;
			}
		}

	}

}


