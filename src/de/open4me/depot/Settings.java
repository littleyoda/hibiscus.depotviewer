package de.open4me.depot;

import java.rmi.RemoteException;

import java.text.DateFormat;
import java.text.DecimalFormat;

import de.willuhn.datasource.rmi.DBService;
import de.willuhn.jameica.hbci.HBCI;
import de.willuhn.jameica.hbci.rmi.HBCIDBService;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.I18N;


/**
 * This class holds some settings for our plugin.
 */
public class Settings
{

	private static DBService db;
	private static I18N i18n;

  /**
   * Our DateFormatter.
   */
  public final static DateFormat DATEFORMAT = DateFormat.getDateInstance(DateFormat.DEFAULT, Application.getConfig().getLocale());
  
  /**
   * Our decimal formatter.
   */
  public final static DecimalFormat DECIMALFORMAT = (DecimalFormat) DecimalFormat.getInstance(Application.getConfig().getLocale());

  /**
   * Our currency name.
   */
  public final static String CURRENCY = "EUR";

	static
	{
		DECIMALFORMAT.setMinimumFractionDigits(2);
		DECIMALFORMAT.setMaximumFractionDigits(2);
	}

	/**
	 * Small helper function to get the database service.
   * @return db service.
   * @throws RemoteException
   */
  public static DBService getDBService() throws RemoteException
	{
		if (db != null)
			return db;

		try
		{
			db = (HBCIDBService) Application.getServiceFactory().lookup(HBCI.class,"database");
			return db;
		}
		catch (Exception e)
		{
			throw new RemoteException("error while getting database service",e);
		}
	}

	/**
	 * Small helper function to get the translator.
   * @return translator.
   */
  public static I18N i18n()
	{
		if (i18n != null)
			return i18n;
		i18n = Application.getPluginLoader().getPlugin(DepotViewerPlugin.class).getResources().getI18N();
		return i18n; 
	}
  
}

