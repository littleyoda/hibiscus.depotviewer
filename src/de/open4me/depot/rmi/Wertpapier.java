package de.open4me.depot.rmi;

import java.rmi.RemoteException;

import de.willuhn.datasource.rmi.DBObject;

public interface Wertpapier extends DBObject
{
	public String getWpid() throws RemoteException;
	public void setWpid(String name) throws RemoteException;
	
	public String getWertpapiername() throws RemoteException;
	public void setWertpapiername(String name) throws RemoteException;
	
	public String getWkn() throws RemoteException;
	public void setWkn(String name) throws RemoteException;
	
	public String getIsin() throws RemoteException;
	public void setIsin(String name) throws RemoteException;
}