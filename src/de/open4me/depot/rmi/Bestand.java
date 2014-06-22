package de.open4me.depot.rmi;

import java.rmi.RemoteException;
import java.util.Date;

import de.willuhn.datasource.rmi.DBObject;


public interface Bestand extends DBObject
{
	public Integer getKontoid() throws RemoteException;
	public void setKontoid(Integer name) throws RemoteException;
	public String getWPid() throws RemoteException;
	public void setWPid(String name) throws RemoteException;
	public Double getAnzahl() throws RemoteException;
	public void setAnzahl(Double name) throws RemoteException;
	public Double getKurs() throws RemoteException;
	public void setKurs(Double name) throws RemoteException;
	public String getKursw() throws RemoteException;
	public void setKursw(String name) throws RemoteException;
	public Double getWert() throws RemoteException;
	public void setWert(Double name) throws RemoteException;
	public String getWertw() throws RemoteException;
	public void setWertw(String name) throws RemoteException;
	public Date getDatum() throws RemoteException;
	public void setDatum(Date name) throws RemoteException;
	public Date getBewertungsDatum() throws RemoteException;
	public void setBewertungsDatum(Date name) throws RemoteException;
}