package de.open4me.depot.rmi;

import java.rmi.RemoteException;
import java.util.Date;

import de.willuhn.datasource.rmi.DBObject;


public interface Umsatz extends DBObject
{

	public Integer getKontoid() throws RemoteException;
	public void setKontoid(Integer name) throws RemoteException;

	public Integer getWPid() throws RemoteException;
	
	public void setWPid(String name) throws RemoteException;
	

	public String getAktion() throws RemoteException;
	
	public void setAktion(String name) throws RemoteException;

	
	public String getBuchungsinformationen() throws RemoteException;
	
	public void setBuchungsinformationen(String name) throws RemoteException;
	
	
	public Double getAnzahl() throws RemoteException;
	
	public void setAnzahl(Double name) throws RemoteException;
	
	
	public Double getKurz() throws RemoteException;
	
	public void setKurz(Double name) throws RemoteException;

	
	public void setKurzW(String kursW) throws RemoteException;

	public void setKosten(Double kosten) throws RemoteException;

	public void setKostenW(String kostenW) throws RemoteException;

	public void setBuchungsdatum(Date date) throws RemoteException;
	
	public Date getBuchungsdatum()  throws RemoteException;

	void setBuchungsdatum(int jahr, int month, int tag) throws RemoteException;

	void setOrderid(String orderid) throws RemoteException;

	public String getKommentar() throws RemoteException;
	
	public void setKommentar(String name) throws RemoteException;

}
