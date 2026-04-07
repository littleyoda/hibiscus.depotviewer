package de.open4me.depot.messageconsumer;

import java.util.Date;
import java.util.List;
import java.util.Map;

import de.willuhn.jameica.hbci.rmi.Konto;
import de.open4me.depot.abruf.utils.Utils;
import de.willuhn.jameica.messaging.Message;
import de.willuhn.jameica.messaging.MessageConsumer;
import de.willuhn.jameica.messaging.QueryMessage;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.util.ApplicationException;


public class PortfolioMessageConsumer implements MessageConsumer
{
  @Override
  public boolean autoRegister()
  {
    // wird explizit per plugin.xml registriert
    return false;
  }

  @Override
  public Class[] getExpectedMessageTypes()
  {
    return new Class[]{QueryMessage.class};
  }

  @Override
  public void handleMessage(Message message) throws Exception
  {
    if (message == null || !(message instanceof QueryMessage))
      return;

    QueryMessage qm = (QueryMessage) message;
    Map data = (Map) qm.getData();
    System.out.println("Portfolio");
    System.out.println(data);
    Utils.clearBestand((Konto) data.get("konto"));
    for (Map<String,Object> h : (List<Map<String, Object>>)data.get("portfolio")) {
    	Utils.addBestand(
    			Utils.getORcreateWKN((String) h.get("wkn"),(String)h.get("isin"),(String)h.get("name")),
    			(Konto) h.get("konto"),
    			(Double) h.get("anzahl"),
    			(Double) h.get("kurs"),
    			(String) h.get("kursw"),
    			(Double) h.get("wert"),
    			(String) h.get("wertw"),
    			(Date) h.get("datum"),
    			(Date) h.get("bewertungszeitpunkt"));
    }
    

  }
} 