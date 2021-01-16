package de.open4me.depot.tools;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.List;

import de.open4me.depot.Settings;
import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.datenobj.DepotAktion;
import de.open4me.depot.datenobj.rmi.Umsatz;
import de.open4me.depot.datenobj.rmi.Wertpapier;
import de.willuhn.datasource.rmi.DBIterator;
import de.willuhn.jameica.hbci.server.Value;
import de.willuhn.jameica.hbci.server.VerwendungszweckUtil;
import de.willuhn.util.ApplicationException;

public class UmsatzHelper {

	public static boolean existsOrder(String orderid) throws RemoteException {
		DBIterator liste = Settings.getDBService().createList(Umsatz.class);
		liste.addFilter("orderid=?", orderid);
		return liste.hasNext();
	}

	/**
	 * Speichert einen DepotViewer-Umsatz in der Hibiscus-Tabelle Umsatz.
	 * Dadurch steht er für Hibiscus-Auswertungen zur Verfügung. 
	 * @param u DepotViewer-Umsatz
	 * @throws RemoteException
	 * @throws ApplicationException
	 */
	public static void storeUmsatzInHibiscus(Umsatz u) throws RemoteException, ApplicationException {		
		de.willuhn.jameica.hbci.rmi.Umsatz hu = null;
		// abfragen, ob Umsatz bereits existiert anhand der EndToEndId
		DBIterator<de.willuhn.jameica.hbci.rmi.Umsatz> liste = Settings.getDBService().createList(de.willuhn.jameica.hbci.rmi.Umsatz.class);
		liste.addFilter("endtoendid=?", "DVID" + u.getID());
		if(liste.hasNext())
			hu = liste.next();
		else {
			hu = (de.willuhn.jameica.hbci.rmi.Umsatz) Settings.getDBService().createObject(de.willuhn.jameica.hbci.rmi.Umsatz.class,null);
			hu.setArt("WERTPAPIERE");
			hu.setFlags(de.willuhn.jameica.hbci.rmi.Umsatz.FLAG_NONE);
			hu.setKonto(Utils.getKontoByID(u.getKontoid().toString()));
			hu.setGegenkontoBLZ(null);
			hu.setGegenkontoName(null);
			hu.setGegenkontoName2(null);
			hu.setGegenkontoNummer(null);
			hu.setWeitereVerwendungszwecke(null);
			hu.setPrimanota(null);
			hu.setGvCode(null);
			hu.setPurposeCode(null);
			hu.setCustomerRef(null);
			hu.setMandateId(null);
			hu.setUmsatzTyp(null);
			
			hu.setEndToEndId("DVID" + u.getID()); // DVID = DepotViewer ID
			hu.setKommentar("Automatisch erzeugt von DepotViewer");
			hu.setSaldo(0); 
		}
	    hu.setBetrag(-u.getKosten().doubleValue());
	    hu.setDatum(u.getBuchungsdatum());
	    hu.setValuta(u.getBuchungsdatum());
	    
	    Wertpapier w = Utils.getWertPapierByID(u.getWPid().toString());
	    hu.setZweck(u.getAktion().toString() + " " + u.getAnzahl().toString() + " STK");
	    hu.setZweck2("WKN: " + w.getWkn() + " ISIN: " + w.getIsin());
	    
	    BigDecimal gegenwert = u.getKosten();
	    String ertragsart = "Kosten";
	    if(u.getAktion().equals(DepotAktion.KAUF)) {
	    	gegenwert = gegenwert.negate().add(u.getTransaktionsgebuehren()).add(u.getSteuern());
	    } else if(u.getAktion().equals(DepotAktion.VERKAUF)){
	    	gegenwert = gegenwert.subtract(u.getTransaktionsgebuehren()).subtract(u.getSteuern());
	    	ertragsart = "Erlös";
	    }
	    
	    hu.setWeitereVerwendungszwecke(VerwendungszweckUtil.parse(w.getWertpapiername() + "\n"
	    		+ "Kurs: " + u.getKurs().toString() + " " + u.getKursW() + "\n" 
	    		+ "Betrag: " + u.getKosten().negate().toString() + " " + u.getKostenW() + "\n"
	    		+ "Steuern: " + u.getSteuern().toString() + " " + u.getSteuernW() + "\n"
	    		+ "Gebühren: " + u.getTransaktionsgebuehren() + " " + u.getTransaktionsgebuehrenW() + "\n"
	    		+ ertragsart + ": " + gegenwert  + " " + u.getKostenW() + "\n"));	    
	    
	    // Saldo berechnen, falls möglich
	    Bestandsabfragen abfragen = new Bestandsabfragen();
	    List<Value> bestand = abfragen.getBalanceData(Utils.getKontoByID(u.getKontoid().toString()), u.getBuchungsdatum(), u.getBuchungsdatum());
	    if(bestand.size() > 0)
	    	hu.setSaldo(bestand.get(0).getValue());
	    
	    
	    hu.store();
	}
}
