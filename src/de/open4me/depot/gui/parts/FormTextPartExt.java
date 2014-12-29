/**********************************************************************
 * based on  /cvsroot/jameica/jameica/src/de/willuhn/jameica/gui/parts/FormTextPart.java,v $
 * Copyright (c) by willuhn.webdesign
 * All rights reserved
 *
 **********************************************************************/
package de.open4me.depot.gui.parts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.rmi.RemoteException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;

import de.willuhn.io.IOUtil;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.Part;
import de.willuhn.jameica.gui.internal.action.Program;
import de.willuhn.jameica.gui.util.Color;
import de.willuhn.jameica.gui.util.Font;
import de.willuhn.jameica.gui.util.SWTUtil;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.jameica.services.BeanService;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

/**
 * Freiformatierbarer Text.
 */
public class FormTextPartExt implements Part {

  private StringBuffer content        = new StringBuffer();
  private ScrolledComposite container = null;
  private FormText text               = null;

  /**
   * ct.
   */
  public FormTextPartExt()
  {
  }

  /**
   * ct.
   * @param text der anzuzeigenden Text.
   */
  public FormTextPartExt(String text)
  {
    setText(text);
  }
  
  /**
   * ct.
   * @param text die PlainText-Datei.
   * @throws IOException Wenn beim Lesen der Datei Fehler auftreten.
   */
  public FormTextPartExt(Reader text) throws IOException
  {
    setText(text);
  }

  /**
   * Zeigt den Text aus der uebergebenen Datei an.
   * @param text anzuzeigender Text.
   * @throws IOException
   */
  public void setText(Reader text) throws IOException
  {
    BufferedReader br =  null;
    
    try {
      br = new BufferedReader(text);

      String thisLine = null;
      StringBuffer buffer = new StringBuffer();
      while ((thisLine =  br.readLine()) != null)
      {
        if (thisLine.length() == 0) // Leerzeile
        {
          buffer.append("\n\n");
          continue;
        }
        buffer.append(thisLine.trim() + " "); // Leerzeichen am Ende einfuegen.


      }

      content = buffer; // machen wir erst wenn die gesamte Datei gelesen werden konnte
      refresh();
    }
    catch (IOException e)
    {
      throw e;
    }
    finally
    {
      IOUtil.close(br);
    }
  }

  /**
   * Zeigt den uebergebenen Hilfe-Text an.
   * @param s anzuzeigender Hilfe-Text.
   */
  public void setText(String s)
  {
    content = new StringBuffer(s);
    refresh();
  }


  /**
   */
  public void refresh()
  {
    if (text == null || content == null)
      return;
    String s = content.toString();
    boolean b = s != null && s.startsWith("<form>");
    try {
    text.setText(s == null ? "" : s,b,b);
    } catch (java.lang.IllegalArgumentException e) {
    	e.printStackTrace();
    	text.setText("Fehlerhafter HTML Code", false, false);
    }
    resize();
  }

  /**
   * Passt die Groesse des Textes an die Umgebung an.
   */
  private void resize()
  {
    if (text == null || container == null)
      return;
    text.setSize(text.computeSize(text.getParent().getClientArea().width,SWT.DEFAULT));
  }

	public void hyperlinkPressed(String action) {
        // Fallback
        try
        {
          new Program().handleAction(action);
        }
        catch (ApplicationException ae)
        {
          Application.getMessagingFactory().sendMessage(new StatusBarMessage(ae.getMessage(),StatusBarMessage.TYPE_ERROR));
        }
	}

  /**
   * @see de.willuhn.jameica.gui.Part#paint(org.eclipse.swt.widgets.Composite)
   */
  public void paint(Composite parent) throws RemoteException {

    container = new ScrolledComposite(parent,SWT.H_SCROLL | SWT.V_SCROLL);
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.horizontalIndent = 4;
    container.setLayoutData(gd);
    container.setLayout(SWTUtil.createGrid(1,true));
    container.addListener(SWT.Resize, new Listener() {
      public void handleEvent(Event event) {
        resize();
      }
    });

    text = new FormText(container, SWT.WRAP);

    // Die HyperlinkSettings muessen zwingend an den
    // FormText uebergeben werden, BEVOR der eigentliche Text uebergeben
    // wird. Das gibts sonst eine NPE. Und das laesst sich extrem
    // schlecht debuggen.
    // Den Quellcode von eclipse.ui.forms hab ich hier gefunden:
    // http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.ui.forms/src/org/eclipse/ui/internal/forms/widgets/?hideattic=0
    
    // Daher machen wir das gleich als erstes. Sicher ist sicher.
    HyperlinkSettings hs = new HyperlinkSettings(GUI.getDisplay());
    hs.setBackground(Color.BACKGROUND.getSWTColor());
    hs.setForeground(Color.LINK.getSWTColor());
    hs.setActiveBackground(Color.BACKGROUND.getSWTColor());
    hs.setActiveForeground(Color.LINK_ACTIVE.getSWTColor());
    text.setHyperlinkSettings(hs);

    text.setFont(Font.DEFAULT.getSWTFont());

    text.setColor("header",Color.COMMENT.getSWTColor());
    text.setColor("error",Color.ERROR.getSWTColor());
    text.setColor("success",Color.SUCCESS.getSWTColor());
    text.setFont("header", Font.H1.getSWTFont());

    container.setContent(text);

    text.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        Object href = e.getHref();
        if (href == null)
        {
          Logger.info("got hyperlink event, but data was null. nothing to do");
          return;
        }
        if (!(href instanceof String))
        {
          Logger.info("got hyperlink event, but data is not a string, skipping");
          return;
        }
        String action = (String) href;
        
        // Wir versuchen die Action als Klasse zu laden. Wenn das fehlschlaegt,
        // starten wir die Action einfach als Programm
        Logger.info("executing action \"" + action);
        try
        {
          Logger.debug("trying to load class " + action);
          Class<Action> c = Application.getClassLoader().load(action);
          BeanService beanService = Application.getBootLoader().getBootable(BeanService.class);
          Action a = beanService.get(c);
          a.handleAction(e);
          return;
        }
        
        catch (Throwable t)
        {
          // ignore
        }
        hyperlinkPressed(action);
        
      }

    });

    refresh();
  }
}

