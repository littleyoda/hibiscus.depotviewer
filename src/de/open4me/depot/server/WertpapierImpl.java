package de.open4me.depot.server;

import java.rmi.RemoteException;

import de.open4me.depot.rmi.Wertpapier;
import de.willuhn.datasource.db.AbstractDBObject;

public class WertpapierImpl extends AbstractDBObject implements Wertpapier
{

	/**
	 * @throws RemoteException
	 */
	public WertpapierImpl() throws RemoteException
	{
		super(); 

	}

	/**
	 * We have to return the name of the sql table here.
	 * @see de.willuhn.datasource.db.AbstractDBObject#getTableName()
	 */
	protected String getTableName()
	{
		return "depotviewer_wertpapier";
	}

	/**
	 * Sometimes you can display only one of the projects attributes (in combo boxes).
	 * Here you can define the name of this field.
	 * Please dont confuse this with the "primary KEY".
	 * @see de.willuhn.datasource.GenericObject#getPrimaryAttribute()
	 */
	public String getPrimaryAttribute() throws RemoteException
	{
		return "wertpapiername";
	}

	public String getWpid() throws RemoteException
	{
		return (String) getAttribute("id");
	}

	public void setWpid(String name) throws RemoteException
	{
		setAttribute("id",name);
	}

	public String getWertpapiername() throws RemoteException
	{
		return (String) getAttribute("wertpapiername");
	}

	public void setWertpapiername(String name) throws RemoteException
	{
		setAttribute("wertpapiername",name);
	}
	public String getWkn() throws RemoteException
	{
		return (String) getAttribute("wkn");
	}

	public void setWkn(String name) throws RemoteException
	{
		setAttribute("wkn",name);
	}
	public String getIsin() throws RemoteException
	{
		return (String) getAttribute("isin");
	}

	public void setIsin(String name) throws RemoteException
	{
		setAttribute("isin",name);
	}
}
