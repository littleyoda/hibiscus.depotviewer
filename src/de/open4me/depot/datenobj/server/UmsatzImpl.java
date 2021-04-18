package de.open4me.depot.datenobj.server;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.Date;

import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.datenobj.DepotAktion;
import de.open4me.depot.datenobj.rmi.Umsatz;
import de.willuhn.datasource.db.AbstractDBObject;
import de.willuhn.util.ApplicationException;


public class UmsatzImpl extends AbstractDBObject implements Umsatz
{

	public UmsatzImpl() throws RemoteException
	{
		super(); 

	}

	protected String getTableName()
	{
		return "depotviewer_umsaetze";
	}

	public String getPrimaryAttribute() throws RemoteException
	{
		return "name";
	}



	public Integer getWPid() throws RemoteException
	{
		return (Integer) getAttribute("wpid");
	}

	public void setWPid(String name) throws RemoteException
	{
		setAttribute("wpid",Integer.parseInt(name));
	}

	@Override

	public BigDecimal getAnzahl() throws RemoteException
	{
		return (BigDecimal) getAttribute("anzahl");
	}

	@Override
	public void setAnzahl(BigDecimal name) throws RemoteException
	{
		setAttribute("anzahl", name);
	}
	
	@Override
	public BigDecimal getKurs() throws RemoteException
	{
		return (BigDecimal) getAttribute("kurs");
	}

	@Override
	public void setKurs(BigDecimal name) throws RemoteException
	{
		setAttribute("kurs", name);
	}
	
	@Override
	public DepotAktion getAktion() throws RemoteException
	{
		return DepotAktion.getByString((String) getAttribute("aktion"));
	}

	@Override
	public void setAktion(DepotAktion aktion) throws RemoteException
	{
		setAttribute("aktion", aktion.internal());
	}

	public String getBuchungsinformationen() throws RemoteException
	{
		return (String) getAttribute("buchungsinformationen");
	}

	public void setBuchungsinformationen(String name) throws RemoteException
	{
		setAttribute("buchungsinformationen",name);
	}

	public String toString() {
		StringBuilder b = new StringBuilder();
		try {
			for (String x : getAttributeNames()) {
				b.append(x);
				b.append(":");
				b.append(getAttribute(x));
				b.append(";");


			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return b.toString();
	}

	@Override
	public void setKursW(String kursW) throws RemoteException {
		setAttribute("kursw", kursW);
	}

	@Override
	public String getKursW() throws RemoteException
	{
		return (String) getAttribute("kursw");
	}

	@Override
	public void setKosten(BigDecimal kosten) throws RemoteException {
		setAttribute("kosten", kosten);
	}

	@Override
	public void setKostenW(String kostenW) throws RemoteException {
		setAttribute("kostenw", kostenW);
	}

	@Override
	public BigDecimal getKosten() throws RemoteException
	{
		return (BigDecimal) getAttribute("kosten");
	}
	
	@Override
	public String getKostenW() throws RemoteException
	{
		return (String) getAttribute("kostenw");
	}
	
	@Override
	public void setBuchungsdatum(Date date) throws RemoteException {
		setAttribute("buchungsdatum", date);
	}

	@Override
	public void setBuchungsdatum(int jahr, int month, int tag) throws RemoteException {
		setAttribute("buchungsdatum", new Date(jahr-1900, month-1, tag));
	}

	@Override
	public Date getBuchungsdatum() throws RemoteException {
		return (Date) getAttribute("buchungsdatum");
	}

	@Override
	public void setOrderid(String orderid) throws RemoteException {
		setAttribute("orderid", orderid);
	}

	public String getOrderid() throws RemoteException {
		return (String) getAttribute("orderid");
	}

	public Integer getKontoid() throws RemoteException
	{
		if (getAttribute("kontoid") instanceof BigDecimal) {
			return ((BigDecimal) getAttribute("kontoid")).intValueExact(); 
		}
		return (Integer) getAttribute("kontoid");
	}

	public void setKontoid(Integer name) throws RemoteException
	{
		setAttribute("kontoid",name);
	}

	@Override
	public String getKommentar() throws RemoteException {
		return (String) getAttribute("kommentar");
	}

	@Override
	public void setKommentar(String name) throws RemoteException {
		setAttribute("kommentar",name);
		
	}
	
	@Override
	public BigDecimal getSteuern() throws RemoteException
	{
		return (BigDecimal) getAttribute("steuern");
	}

	@Override
	public void setSteuern(BigDecimal name) throws RemoteException
	{
		setAttribute("steuern", name);
	}
	
	@Override
	public void setSteuernW(String kursW) throws RemoteException {
		setAttribute("steuernw", kursW);
	}

	@Override
	public String getSteuernW() throws RemoteException
	{
		return (String) getAttribute("steuernw");
	}
	
	
	@Override
	public BigDecimal getTransaktionsgebuehren() throws RemoteException
	{
		return (BigDecimal) getAttribute("transaktionskosten");
	}

	@Override
	public void setTransaktionsgebuehren(BigDecimal name) throws RemoteException
	{
		setAttribute("transaktionskosten", name);
	}
	
	@Override
	public void setTransaktionsgebuehrenW(String kursW) throws RemoteException {
		setAttribute("transaktionskostenw", kursW);
	}

	@Override
	public String getTransaktionsgebuehrenW() throws RemoteException
	{
		return (String) getAttribute("transaktionskostenw");
	}

	@Override
	public void store() throws RemoteException, ApplicationException {
		if (getAktion() == null) {
			throw new ApplicationException("Unbekannte Transaktionsart/Buchungsart: " + getAktion());
		}
		if (getAnzahl().signum() == -1) {
			throw new ApplicationException("Anzahl muss immer positiv sein.");
		}
		if (getKurs().signum() == -1) {
			throw new ApplicationException("Der Kurs muss immer positiv sein.");
		}
		
		if (getOrderid() == null) {
			throw new ApplicationException("Keine OrderID vergeben.");
		}
		super.store();
	}

	@Override
	public String generateOrderId() throws RemoteException {
		Integer hash = ("" + getKontoid() + getWPid() + getAktion() + getBuchungsdatum() +  getAnzahl() + getKurs() + getKursW()).hashCode();
		return hash.toString();
	}

}

