package de.open4me.depot.server;

import java.rmi.RemoteException;
import java.util.Date;

import de.open4me.depot.rmi.Umsatz;
import de.willuhn.datasource.db.AbstractDBObject;


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

	public Double getAnzahl() throws RemoteException
	{
		return (Double) getAttribute("anzahl");
	}

	public void setAnzahl(Double name) throws RemoteException
	{
		setAttribute("anzahl", name);
	}
	public Double getKurz() throws RemoteException
	{
		return (Double) getAttribute("kurs");
	}

	public void setKurz(Double name) throws RemoteException
	{
		setAttribute("kurs", name);
	}
	public String getAktion() throws RemoteException
	{
		return (String) getAttribute("aktion");
	}

	public void setAktion(String name) throws RemoteException
	{
		setAttribute("aktion",name);
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
	public void setKurzW(String kursW) throws RemoteException {
		setAttribute("kursw", kursW);
	}

	public String getKurzW() throws RemoteException
	{
		return (String) getAttribute("kursw");
	}

	@Override
	public void setKosten(Double kosten) throws RemoteException {
		setAttribute("kosten", kosten);
	}

	@Override
	public void setKostenW(String kostenW) throws RemoteException {
		setAttribute("kostenw", kostenW);
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

	public Integer getKontoid() throws RemoteException
	{
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
}

