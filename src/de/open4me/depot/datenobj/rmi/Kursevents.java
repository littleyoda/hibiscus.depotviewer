package de.open4me.depot.datenobj.rmi;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.Date;

import de.willuhn.datasource.rmi.DBObject;

public interface  Kursevents extends DBObject {
	public Integer getWpid() throws RemoteException;
	public void setWpid(Integer name) throws RemoteException;
	public String getRatio() throws RemoteException;
	public void setRatio(String name) throws RemoteException;
	public BigDecimal getValue() throws RemoteException;
	public void setValue(BigDecimal name) throws RemoteException;
	public String getAktion() throws RemoteException;
	public void setAktion(String name) throws RemoteException;
	public Date getDatum() throws RemoteException;
	public void setDatum(Date name) throws RemoteException;
	public String getWaehrung() throws RemoteException;
	public void setWaehrung(String name) throws RemoteException;
}
