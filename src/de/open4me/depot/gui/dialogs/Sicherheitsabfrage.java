package de.open4me.depot.gui.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.dialogs.AbstractDialog;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.gui.util.Container;
import de.willuhn.jameica.gui.util.SWTUtil;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.util.ApplicationException;

public class Sicherheitsabfrage extends AbstractDialog<Boolean>{

	boolean jaIchWill = false;
	private String message = "Wollen Sie wirklich die Einträge löschen?";

	public Sicherheitsabfrage() {
		super(AbstractDialog.POSITION_CENTER, false);
		setTitle(i18n.tr("Sicherheitsabfrage"));
		setSize(480, SWT.DEFAULT);
		setSideImage(SWTUtil.getImage("dialog-warning-large.png"));
	}

	@Override
	protected void paint(Composite parent) throws Exception {
	    Container cb = new SimpleContainer(parent);
	    cb.addText(message, false);
	    ButtonArea buttons = new ButtonArea();
	    paintButtons(buttons);
	    cb.addButtonArea(buttons);
	}

	@Override
	protected Boolean getData() throws Exception {
		return jaIchWill;
	}

	  /**
	   * copied from de.willuhn.jameica.gui.dialogs.AbstractCertificateDialog#paintButtons(de.willuhn.jameica.gui.parts.ButtonArea)
	   */
	  protected void paintButtons(ButtonArea buttons)
	  {
	    buttons.addButton("   " + i18n.tr("Ja") + "   ", new Action()
	    {
	      public void handleAction(Object context) throws ApplicationException
	      {
		    jaIchWill = Boolean.TRUE;
	        close();
	      }
	    },null,false,"ok.png");
	    buttons.addButton("   " + i18n.tr("Nein") + "   ", new Action()
	    {
	      public void handleAction(Object context) throws ApplicationException
	      {
	        jaIchWill = Boolean.FALSE;
	        close();
	      }
	    },null,false,"window-close.png");
	  }

}
