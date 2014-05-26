package de.open4me.depot.server;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.Date;

import de.open4me.depot.rmi.Bestand;
import de.willuhn.datasource.db.AbstractDBObject;

public class BestandImpl extends AbstractDBObject implements Bestand
{

	public BestandImpl() throws RemoteException
	{
		super(); 

	}

	protected String getTableName()
	{
		return "depotviewer_bestand";
	}

	public String getPrimaryAttribute() throws RemoteException
	{
		return "name";
	}
	public Integer getKontoid() throws RemoteException
	{
		return (Integer) getAttribute("kontoid");
	}

	public void setKontoid(Integer name) throws RemoteException
	{
		setAttribute("kontoid",name);
	}
	public String getWkn() throws RemoteException
	{
		return (String) getAttribute("wkn");
	}

	public void setWkn(String name) throws RemoteException
	{
		setAttribute("wkn",name);
	}
	public Double getAnzahl() throws RemoteException
	{
		return (Double) getAttribute("anzahl");
	}

	public void setAnzahl(Double name) throws RemoteException
	{
		setAttribute("anzahl",name);
	}
	public Double getKurs() throws RemoteException
	{
		return (Double) getAttribute("kurs");
	}

	public void setKurs(Double name) throws RemoteException
	{
		setAttribute("kurs",name);
	}
	public String getKursw() throws RemoteException
	{
		return (String) getAttribute("kursw");
	}

	public void setKursw(String name) throws RemoteException
	{
		setAttribute("kursw",name);
	}
	public Double getWert() throws RemoteException
	{
		return ((BigDecimal) getAttribute("wert")).doubleValue();
	}

	public void setWert(Double name) throws RemoteException
	{
		setAttribute("wert",name);
	}
	public String getWertw() throws RemoteException
	{
		return (String) getAttribute("wertw");
	}

	public void setWertw(String name) throws RemoteException
	{
		setAttribute("wertw",name);
	}
	public Date getDatum() throws RemoteException
	{
		return (Date) getAttribute("datum");
	}

	public void setDatum(Date name) throws RemoteException
	{
		setAttribute("datum",name);
	}
}
