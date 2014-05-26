
package de.open4me.depot;

import de.open4me.depot.sql.SQLUtils;
import de.willuhn.jameica.plugin.AbstractPlugin;
import de.willuhn.jameica.plugin.Version;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.ApplicationException;

/**
 * You need to have at least one class wich inherits from <code>AbstractPlugin</code>.
 * If so, Jameica will detect your plugin automatically at startup.
 */
public class DepotViewerPlugin extends AbstractPlugin
{

	/**
	 * This method is invoked on every startup.
	 * You can make here some stuff to init your plugin.
	 * If you get some errors here and you dont want to activate the plugin,
	 * simply throw an ApplicationException.
	 * You dont need to implement this function.
	 * @see de.willuhn.jameica.plugin.AbstractPlugin#init()
	 */
	public void init() throws ApplicationException
	{
		super.init();
		SQLUtils.checkforupdates();
	}

	/**
	 * This method is called only the first time, the plugin is loaded (before executing init()).
	 * if your installation procedure was not successfull, throw an ApplicationException.
	 * You dont need to implement this function.
	 * @see de.willuhn.jameica.plugin.AbstractPlugin#install()
	 */
	public void install() throws ApplicationException
	{

		// If we are running in client/server mode and this instance
		// is the client, we do not need to create a database.
		// Instead of this we will get our objects via RMI from
		// the server
		if (Application.inClientMode())
			return;
	}

	/**
	 * This method will be executed on every version change.
	 * You dont need to implement this function.
	 * @see de.willuhn.jameica.plugin.AbstractPlugin#update(de.willuhn.jameica.plugin.Version)
	 */
	public void update(Version oldVersion) throws ApplicationException
	{
		super.update(oldVersion);
		SQLUtils.checkforupdates();
	}

	/**
	 * Here you can do some cleanup stuff.
	 * The method will be called on every clean shutdown of jameica.
	 * You dont need to implement this function.
	 * @see de.willuhn.jameica.plugin.AbstractPlugin#shutDown()
	 */
	public void shutDown()
	{
		super.shutDown();
	}


}

