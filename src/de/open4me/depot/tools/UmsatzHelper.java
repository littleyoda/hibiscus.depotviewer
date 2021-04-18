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
import de.willuhn.datasource.rmi.DBService;
import de.willuhn.jameica.hbci.rmi.DBProperty;
import de.willuhn.jameica.hbci.server.DBPropertyUtil.Prefix;
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
		VarDecimalFormat df = new VarDecimalFormat(2);
		VarDecimalFormat kf = new VarDecimalFormat(5);
		
		// abfragen, ob Umsatz bereits existiert anhand der meta-depotviewer_id
		de.willuhn.jameica.hbci.rmi.Umsatz hu = getHibiscusUmsatzByDepotViewerId(u.getID());
		if(hu == null) {
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

			hu.setKommentar("Automatisch erzeugt von DepotViewer");
			hu.setSaldo(0);
		}
		
		BigDecimal gegenwert = u.getKosten().negate();
		hu.setBetrag(gegenwert.doubleValue());
	    hu.setDatum(u.getBuchungsdatum());
	    hu.setValuta(u.getBuchungsdatum());
	    
	    Wertpapier w = Utils.getWertPapierByID(u.getWPid().toString());
	    hu.setZweck(u.getAktion().toString() + " " + kf.format(u.getAnzahl()) + " STK");
	    hu.setZweck2("WKN: " + w.getWkn() + " ISIN: " + w.getIsin());
	    
	    gegenwert = gegenwert.add(u.getTransaktionsgebuehren()).add(u.getSteuern()); // gegenwert hat passendes Vorzeichen, daher kann man hier addieren
	    String ertragsart = "Kosten";
	    if(u.getAktion().equals(DepotAktion.VERKAUF) || u.getAktion().equals(DepotAktion.AUSBUCHUNG)){
	    	ertragsart = "Erlös";
	    }
	    
	    hu.setWeitereVerwendungszwecke(VerwendungszweckUtil.parse(w.getWertpapiername() + "\n"
	    		+ "Kurs: " + kf.format(u.getKurs()) + " " + u.getKursW() + "\n" 
	    		+ "Betrag: " + df.format(u.getKosten().abs()) + " " + u.getKostenW() + "\n"
	    		+ "Steuern: " + df.format(u.getSteuern()) + " " + u.getSteuernW() + "\n"
	    		+ "Gebühren: " + df.format(u.getTransaktionsgebuehren()) + " " + u.getTransaktionsgebuehrenW() + "\n"
	    		+ ertragsart + ": " + df.format(gegenwert.abs())  + " " + u.getKostenW() + "\n"));	    
	    
	    // Saldo berechnen, falls möglich
	    Bestandsabfragen abfragen = new Bestandsabfragen();
	    List<Value> bestand = abfragen.getBalanceData(Utils.getKontoByID(u.getKontoid().toString()), u.getBuchungsdatum(), u.getBuchungsdatum());
	    if(bestand.size() > 0)
	    	hu.setSaldo(bestand.get(0).getValue());
	    
	    
	    hu.store();
	    
	    hu.setMeta("depotviewer_id", u.getID()); // Meta-Daten werden getrennt vom eigentlichen Umsatz (nicht mit store()) gespeichert.
	}
	
	/**
	 * Ermittelt den Hibiscus-Umsatz, der zu einem DepotViewer-Umsatz gehört.
	 * Aktuell gehen wir davon aus, dass es eine 1:1 Beziehung ist.
	 * @param depotViewerId
	 * @return Der Umsatz aus Hibiscus oder null, falls es keinen passenden gibt.
	 * @throws RemoteException
	 */
	public static de.willuhn.jameica.hbci.rmi.Umsatz getHibiscusUmsatzByDepotViewerId(String depotViewerId) throws RemoteException{
		String query = Prefix.META.value() + ".umsatz.%.depotviewer_id";
		DBService service = Settings.getDBService();
		DBIterator<DBProperty> i = service.createList(DBProperty.class);
		i.addFilter("name like ?", query);
		i.addFilter("content=?", depotViewerId);
		
		if (i.hasNext())
		{
			DBProperty p = i.next();
			// p ist der passende Meta-Datensatz mit der ID des Depot-Umsatzes.
			// Name sieht typischerweise so aus: "meta.umsatz.1234.depoviewer_id"
			// "1234" ist die ID des Hibiscus-Umsatzes.
			String[] parts = org.apache.commons.lang.StringUtils.split(p.getName(), '.');
			if (parts.length == 4) {
				String id = parts[2];
				return service.createObject(de.willuhn.jameica.hbci.rmi.Umsatz.class, id);
			}
		}
		return null;
	}
}
