package de.open4me.depot.server;

import java.rmi.RemoteException;
import java.util.Date;

import de.open4me.depot.rmi.Kursevents;
import de.willuhn.datasource.db.AbstractDBObject;

public class KurseventsImpl extends AbstractDBObject implements Kursevents
{

	/**
	 * @throws RemoteException
	 */
	public KurseventsImpl() throws RemoteException
	{
		super(); 

	}

	/**
	 * We have to return the name of the sql table here.
	 * @see de.willuhn.datasource.db.AbstractDBObject#getTableName()
	 */
	protected String getTableName()
	{
		return "depotviewer_kursevent";
	}

	/**
	 * Sometimes you can display only one of the projects attributes (in combo boxes).
	 * Here you can define the name of this field.
	 * Please dont confuse this with the "primary KEY".
	 * @see de.willuhn.datasource.GenericObject#getPrimaryAttribute()
	 */
	public String getPrimaryAttribute() throws RemoteException
	{
		return "id";
	}

	public Integer getWpid() throws RemoteException
	{
		return (Integer) getAttribute("wpid");
	}

	public void setWpid(Integer name) throws RemoteException
	{
		setAttribute("wpid",name);
	}
	public String getRatio() throws RemoteException
	{
		return (String) getAttribute("ratio");
	}

	public void setRatio(String name) throws RemoteException
	{
		setAttribute("ratio",name);
	}
	public Double getValue() throws RemoteException
	{
		return (Double) getAttribute("value");
	}

	public void setValue(Double name) throws RemoteException
	{
		setAttribute("value",name);
	}
	public String getAktion() throws RemoteException
	{
		return (String) getAttribute("aktion");
	}

	public void setAktion(String name) throws RemoteException
	{
		setAttribute("aktion",name);
	}
	public Date getDatum() throws RemoteException
	{
		return (Date) getAttribute("datum");
	}

	public void setDatum(Date name) throws RemoteException
	{
		setAttribute("datum",name);
	}
	public String getWaehrung() throws RemoteException
	{
		return (String) getAttribute("waehrung");
	}

	public void setWaehrung(String name) throws RemoteException
	{
		setAttribute("waehrung",name);
	}

}
