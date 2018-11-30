package de.open4me.depot.abruf.www;

import javax.annotation.Resource;

import de.open4me.depot.abruf.impl.BasisDepotAbruf;
import de.open4me.depot.abruf.impl.DepotAbrufFabrik;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.hbci.synchronize.jobs.SynchronizeJobKontoauszug;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

/**
 * Implementierung des Kontoauszugsabruf fuer AirPlus.
 * Von der passenden Job-Klasse ableiten, damit der Job gefunden wird.
 */
public class DVSynchronizeJobKontoauszug extends SynchronizeJobKontoauszug implements DVSynchronizeJob
{

	@Resource
	private DVSynchronizeBackend backend = null;

	/**
	 * @see org.jameica.hibiscus.barclaystg.AirPlusSynchronizeJob#execute()
	 */
	@Override
	public void execute() throws Exception
	{
		Konto konto = (Konto) this.getContext(CTX_ENTITY);

		Logger.info("Rufe Umsätze ab für " + backend.getName());

		BasisDepotAbruf x = DepotAbrufFabrik.getDepotAbruf(konto);
		if (x == null) {
			throw new ApplicationException("Unbekanntes Depot");
		}
		Logger.info("Genutztes DepotAbruf-Backend " + x.getName());
		x.run(konto);
	}


}




