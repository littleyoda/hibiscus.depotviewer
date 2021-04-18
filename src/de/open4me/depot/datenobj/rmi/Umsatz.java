package de.open4me.depot.datenobj.rmi;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.Date;

import de.open4me.depot.datenobj.DepotAktion;
import de.willuhn.datasource.rmi.DBObject;


public interface Umsatz extends DBObject
{

	public Integer getKontoid() throws RemoteException;
	public void setKontoid(Integer name) throws RemoteException;

	public Integer getWPid() throws RemoteException;
	
	public void setWPid(String name) throws RemoteException;
	

	public DepotAktion getAktion() throws RemoteException;
	
	public void setAktion(DepotAktion aktion) throws RemoteException;

	
	public String getBuchungsinformationen() throws RemoteException;
	
	public void setBuchungsinformationen(String name) throws RemoteException;
	
	
	public BigDecimal getAnzahl() throws RemoteException;
	
	
	
	public BigDecimal getKurs() throws RemoteException;
	
	public void setKurs(BigDecimal name) throws RemoteException;

	
	public void setKursW(String kursW) throws RemoteException;
	public String getKursW() throws RemoteException;

	public void setKosten(BigDecimal kosten) throws RemoteException;

	public void setKostenW(String kostenW) throws RemoteException;
	public String getKostenW() throws RemoteException;

	public void setBuchungsdatum(Date date) throws RemoteException;
	
	public Date getBuchungsdatum()  throws RemoteException;

	void setBuchungsdatum(int jahr, int month, int tag) throws RemoteException;

	void setOrderid(String orderid) throws RemoteException;

	public String getKommentar() throws RemoteException;
	
	public void setKommentar(String name) throws RemoteException;
	BigDecimal getSteuern() throws RemoteException;
	void setSteuern(BigDecimal name) throws RemoteException;
	void setSteuernW(String kursW) throws RemoteException;
	String getSteuernW() throws RemoteException;
	String getTransaktionsgebuehrenW() throws RemoteException;
	void setTransaktionsgebuehrenW(String kursW) throws RemoteException;
	void setTransaktionsgebuehren(BigDecimal name) throws RemoteException;
	BigDecimal getTransaktionsgebuehren() throws RemoteException;
	BigDecimal getKosten() throws RemoteException;
	void setAnzahl(BigDecimal name) throws RemoteException;
	String generateOrderId() throws RemoteException;

}
