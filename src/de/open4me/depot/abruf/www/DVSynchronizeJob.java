package de.open4me.depot.abruf.www;

import de.willuhn.jameica.hbci.synchronize.jobs.SynchronizeJob;

/**
 * MarkerInterface fuer die vom Plugin unterstuetzten Jobs.
 */
public interface DVSynchronizeJob extends SynchronizeJob
{
       // Hier koennen wir jetzt eigene Funktionen definieren, die dann von
       // der JobGroup im AirPlusSynchronizeBackend ausgefuehrt werden.

       /**
        * Fuehrt den Job aus.
        * @throws Exception
        */
       public void execute() throws Exception;

}
