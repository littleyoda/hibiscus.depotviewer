package de.open4me.depot.messageconsumer;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.datenobj.rmi.Umsatz;
import de.open4me.depot.tools.Zahlen;
import de.open4me.depot.tools.UmsatzHelper;
import de.willuhn.jameica.messaging.Message;
import de.willuhn.jameica.messaging.MessageConsumer;
import de.willuhn.jameica.messaging.QueryMessage;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.logging.Logger;


public class TransactionMessageConsumer implements MessageConsumer {
  @Override
  public boolean autoRegister() {
    // wird explizit per plugin.xml registriert
    return false;
  }

  @Override
  public Class[] getExpectedMessageTypes() {
    return new Class[] { QueryMessage.class };
  }

  @Override
  public void handleMessage(Message message) throws Exception
  {
    if (message == null || !(message instanceof QueryMessage))
      return;

    QueryMessage qm = (QueryMessage) message;
    Map<String, Object> data = (Map<String, Object>) qm.getData();
    ArrayList<Map<String, Object>> liste = (ArrayList<Map<String, Object>>) data.get("transactions");
   	Logger.debug("Transaction via MessageConsumer: " + liste.size());
    for (Map<String, Object> t : liste) {
      try {
    	String wpid = Utils.getORcreateWKN((String) t.get("wkn"), (String) t.get("isin"), (String) t.get("name"));
   			Logger.debug(t.toString());
        Umsatz u = Utils.addUmsatz(
    			((Konto) t.get("konto")).getID(), 
    			wpid, 
    			(String) t.get("aktion"),
    			"", 
    			Zahlen.toBigDecimal(t.get("anzahl")),
    			Zahlen.toBigDecimal(t.get("kurs")),
    			(String) t.get("kursw"),
    			Zahlen.toBigDecimal(t.get("kosten")),
    			(String) t.get("kostenw"), 
    			(Date) t.get("datetime"),
    			(String) t.get("orderid"),
    			"",
    			Zahlen.optionalerBetrag(t.get("gebuehren")),
    			(String) t.get("gebuehrenw"),
    			Zahlen.optionalerBetrag(t.get("steuern")),
    			(String) t.get("steuernw"));
    	UmsatzHelper.storeUmsatzInHibiscus(u);
    } catch (Exception e) {
 			Logger.error("Fehler beim der Verabreitung von Transaktionen", e);
//      throw e;
    }
  }
  }
}