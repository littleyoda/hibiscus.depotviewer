package de.open4me.depot.messaging;

import de.willuhn.jameica.messaging.TextMessage;

public class KursUpdatesMsg extends TextMessage
{

	  /**
	   * ct.
	   * @param wpid
	   */
	  public KursUpdatesMsg(String wpid)
	  {
	    super(wpid);
	  }
	}