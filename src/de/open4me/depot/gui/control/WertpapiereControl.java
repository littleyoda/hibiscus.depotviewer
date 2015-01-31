package de.open4me.depot.gui.control;

import java.rmi.RemoteException;

import de.open4me.depot.messaging.KursUpdatesMsg;
import de.open4me.depot.sql.GenericObjectSQL;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.messaging.Message;
import de.willuhn.jameica.messaging.MessageConsumer;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;

public class WertpapiereControl {

	private WertpapiereDatenControl unten;
	private WertpapiereTableControl oben;
	private KursUpdatesMsgConsumer mcNew;

	public WertpapiereControl(WertpapiereDatenControl unten,
			WertpapiereTableControl oben) {
		this.oben = oben;
		this.unten = unten;
	    mcNew     = new KursUpdatesMsgConsumer();
	    Application.getMessagingFactory().registerMessageConsumer(mcNew);
		
	}

	public void aktualisiereTablle() throws RemoteException {
		oben.aktualisiere();
	}
	public void aktualisieren(GenericObjectSQL[] selection) {
		unten.update(selection);
		
	}
	
	
	  /**
	   * Hilfsklasse damit wir ueber importierte Umsaetze informiert werden.
	   */
	  public class KursUpdatesMsgConsumer implements MessageConsumer
	  {
	    /**
	     * @see de.willuhn.jameica.messaging.MessageConsumer#getExpectedMessageTypes()
	     */
	    public Class[] getExpectedMessageTypes()
	    {
	      return new Class[]{
	        KursUpdatesMsg.class,
	      };
	    }

	    /**
	     * @see de.willuhn.jameica.messaging.MessageConsumer#handleMessage(de.willuhn.jameica.messaging.Message)
	     */
	    public void handleMessage(final Message message) throws Exception
	    {
	      if (message == null)
	        return;
	      GUI.getDisplay().syncExec(new Runnable() {
	          public void run()
	          {
	        	  try {
					aktualisiereTablle();
				} catch (RemoteException e) {
					Logger.error("Fehler beim Aktualisieren", e);
				}
	        	  aktualisieren(oben.getSelection());
	          }
	      });
	    }

	    /**
	     * @see de.willuhn.jameica.messaging.MessageConsumer#autoRegister()
	     */
	    public boolean autoRegister()
	    {
	      return false;
	    }
	  }


	public void unlisten() {
	    Application.getMessagingFactory().unRegisterMessageConsumer(mcNew);
	}
	


}
