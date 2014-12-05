/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus/src/de/willuhn/jameica/hbci/gui/dialogs/SynchronizeOptionsDialog.java,v $
 * $Revision: 1.11 $
 * $Date: 2011/05/20 16:22:31 $
 * $Author: willuhn $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by willuhn.webdesign
 * All rights reserved
 *
 **********************************************************************/

package de.open4me.depot.gui.dialogs;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import de.open4me.depot.abruf.hbci.DVHBCISynchronizeJobDepotKontoauszug;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.dialogs.AbstractDialog;
import de.willuhn.jameica.gui.input.CheckboxInput;
import de.willuhn.jameica.gui.input.Input;
import de.willuhn.jameica.gui.input.TextInput;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.gui.util.Container;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.jameica.hbci.HBCI;
import de.willuhn.jameica.hbci.SynchronizeOptions;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.hbci.synchronize.SynchronizeBackend;
import de.willuhn.jameica.hbci.synchronize.SynchronizeEngine;
import de.willuhn.jameica.hbci.synchronize.jobs.SynchronizeJobKontoauszug;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.jameica.services.BeanService;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;

/**
 * Ein Dialog, ueber den die Synchronisierungs-Optionen fuer ein Konto eingestellt werden koennen.
 * 
 * Solange Hibiscus für HBCI Plugin keine Addon-Properties unterstützt, muss ich mir so behelfen
 */
public class ExtendedSynchronizeOptionsDialog extends AbstractDialog
{
  private final static I18N i18n = Application.getPluginLoader().getPlugin(HBCI.class).getResources().getI18N();
  
  private final static int WINDOW_WIDTH = 400;
  
  private Konto konto                 = null;
  private boolean offline             = false;
  private boolean syncAvail           = false;
  private SynchronizeOptions options  = null;
  private CheckboxInput syncSaldo     = null;
  private CheckboxInput syncUmsatz    = null;
  private Button apply                = null;
  
  private List<Input> properties = new ArrayList<Input>();

  /**
   * ct.
   * @param obj. das Konto.
   * @param position
   * @throws RemoteException
   */
  public ExtendedSynchronizeOptionsDialog(GenericObjectHashMap obj, int position) throws RemoteException
  {
    super(position);
    this.setTitle(i18n.tr("Synchronisierungsoptionen"));
    this.setSize(WINDOW_WIDTH,SWT.DEFAULT);
    this.konto = (Konto) obj.getAttribute("kontoobj");
    this.options = new SynchronizeOptions(konto);
    
      BeanService service = Application.getBootLoader().getBootable(BeanService.class);
      SynchronizeEngine engine = service.get(SynchronizeEngine.class);
      this.syncAvail = engine.supports(SynchronizeJobKontoauszug.class,konto);
      
      // checken, ob wir Addon-Properties haben
      if (this.syncAvail)
      {
        try
        {
          SynchronizeBackend backend = engine.getBackend(SynchronizeJobKontoauszug.class,konto);
          List<String> names = backend.getPropertyNames(konto);
          if (names != null && names.size() > 0)
          {
            for (String name:names)
            {
              this.createCustomProperty(name);
            }
          }
          
          Object x = backend.create(SynchronizeJobKontoauszug.class, konto);
          if (x instanceof DVHBCISynchronizeJobDepotKontoauszug) {
        	  DVHBCISynchronizeJobDepotKontoauszug sj = (DVHBCISynchronizeJobDepotKontoauszug) x;
        	  names = sj.getProf(konto);
              if (names != null && names.size() > 0)
              {
                for (String name:names)
                {
                	System.out.println(name);
                  this.createCustomProperty(name);
                }
              }
          }
          System.out.println(x.getClass());

        }
        catch (ApplicationException ae)
        {
          Logger.error(ae.getMessage());
        }
      }
  }
  
  /**
   * Erzeugt ein Custom-Property-Input fuer den angegebenen Property-Namen.
   * @param name der Name des Custom-Property.
   * @throws RemoteException
   */
  private void createCustomProperty(String name) throws RemoteException
  {
    Input t = null;
    if (name.endsWith("(true/false)"))
    {
      String newName = name.replace("(true/false)","").trim();
      String value = konto.getMeta(newName,null);
      t = new CheckboxInput(value != null && Boolean.valueOf(value).booleanValue());
      t.setName(newName);
    }
    else
    {
      t = new TextInput(konto.getMeta(name,null));
      t.setName(name);
    }
    this.properties.add(t);
  }

  /**
   * @see de.willuhn.jameica.gui.dialogs.AbstractDialog#paint(org.eclipse.swt.widgets.Composite)
   */
  protected void paint(Composite parent) throws Exception
  {
    Container group = new SimpleContainer(parent);

    group.addText(i18n.tr("Bitte wählen Sie aus, welche Geschäftsvorfälle bei der " +
    		                  "Synchronisierung des Kontos ausgeführt werden sollen."),true);
    
    group.addHeadline(this.konto.getLongName());
    
    this.apply = new Button(i18n.tr("Übernehmen"),new Action() {
      public void handleAction(Object context) throws ApplicationException
      {
        
        if (!offline || syncAvail) // Entweder bei Online-Konten oder bei welchen mit neuem Scripting-Support
        {
          options.setSyncSaldo(((Boolean)getSyncSaldo().getValue()).booleanValue());
          options.setSyncKontoauszuege(((Boolean)getSyncUmsatz().getValue()).booleanValue());
        }

        
        try
        {
          for (Input prop:properties)
          {
            Object value = prop.getValue();
            System.out.println(prop.getName() + " ");
            konto.setMeta(prop.getName(),value != null ? value.toString() : null);
          }
        }
        catch (Exception e)
        {
          Logger.error("unable to apply properties",e);
          Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Übernehmen der Optionen fehlgeschlagen: {0}",e.getMessage()),StatusBarMessage.TYPE_ERROR));
        }
        close();
      }
    },null,true,"ok.png");
    
    
    if (!offline || syncAvail)
    {
      group.addInput(getSyncSaldo());
      group.addInput(getSyncUmsatz());
    }

    
    if (this.properties.size() > 0)
    {
      group.addHeadline(i18n.tr("Erweiterte Einstellungen"));
      for (Input prop:this.properties)
      {
        group.addInput(prop);
      }
    }
    

    ButtonArea buttons = new ButtonArea();
    buttons.addButton(this.apply);
    buttons.addButton(i18n.tr("Abbrechen"), new Action() {
      public void handleAction(Object context) throws ApplicationException
      {
        close();
      }
    },null,false,"process-stop.png");
    
    group.addButtonArea(buttons);
    getShell().setMinimumSize(getShell().computeSize(WINDOW_WIDTH,SWT.DEFAULT));
  }
  
  /**
   * Liefert eine Checkbox fuer die Aktivierung der Synchronisierung der Salden.
   * @return Checkbox.
   */
  private CheckboxInput getSyncSaldo()
  {
    if (this.syncSaldo == null)
    {
      this.syncSaldo = new CheckboxInput(options.getSyncSaldo());
      this.syncSaldo.setName(i18n.tr("Bestand/Saldo abrufen"));
    }
    return this.syncSaldo;
  }

  /**
   * Liefert eine Checkbox fuer die Aktivierung der Synchronisierung der Umsaetze.
   * @return Checkbox.
   */
  private CheckboxInput getSyncUmsatz()
  {
    if (this.syncUmsatz == null)
    {
      this.syncUmsatz = new CheckboxInput(options.getSyncKontoauszuege());
      this.syncUmsatz.setName(i18n.tr("Umsätze abrufen"));
    }
    return this.syncUmsatz;
  }

  /**
   * @see de.willuhn.jameica.gui.dialogs.AbstractDialog#getData()
   */
  protected Object getData() throws Exception
  {
    return null;
  }
  
}
