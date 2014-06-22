package de.open4me.depot.gui.control;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.List;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.action.OrderList;
import de.open4me.depot.gui.menu.BestandsListMenu;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.jameica.gui.AbstractControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Part;
import de.willuhn.jameica.gui.formatter.DateFormatter;
import de.willuhn.jameica.gui.parts.TablePart;
import de.willuhn.logging.Logger;


public class BestandTableControl extends AbstractControl
{

  private TablePart bestandsList;

	public BestandTableControl(AbstractView view)
	{
		super(view);
	}

  public Part getProjectsTable() throws RemoteException
  {
    if (bestandsList != null) {
      return bestandsList;
    }
    
    List<GenericObjectSQL> list = SQLUtils.getResultSet("select *, concat(kurs,' ', kursw) as joinkurs, concat(wert,' ', wertw) as joinwert from depotviewer_bestand left join depotviewer_wertpapier on  depotviewer_bestand.wpid = depotviewer_wertpapier.id"
    		     				+ "	left join konto on  konto.id = depotviewer_bestand.kontoid", 
    		"depotviewer_bestand", "id");
    bestandsList = new TablePart(list ,new OrderList()) {

		@SuppressWarnings("unchecked")
		@Override
		protected String getSummary() {
			try {

		      double sum = 0.0d;
		      for (GenericObjectSQL k: (List<GenericObjectSQL>) getItems())
		      {
		    	  sum += ((BigDecimal) k.getAttribute("wert")).doubleValue();
		      }

		      return String.format("Gesamt-Saldo: %.2f", sum);
		    }
		    catch (Exception e)
		    {
		      Logger.error("Kann Gesamt-Saldo nicht berechnen",e);
		    }
		    return super.getSummary();
		}
    	
    };

    bestandsList.addColumn(Settings.i18n().tr("Depot"), "bezeichnung");
    bestandsList.addColumn(Settings.i18n().tr("wkn"),"wkn");
    bestandsList.addColumn(Settings.i18n().tr("Name"),"wertpapiername");
    bestandsList.addColumn(Settings.i18n().tr("Anzahl"),"anzahl"); 
    bestandsList.addColumn(Settings.i18n().tr("Kurs"),"joinkurs"); 
    bestandsList.addColumn(Settings.i18n().tr("Wert"),"joinwert"); 
    bestandsList.addColumn(Settings.i18n().tr("Bewertungsdatum"),"bewertungszeitpunkt", new DateFormatter(Settings.DATEFORMAT)); 
    bestandsList.addColumn(Settings.i18n().tr("Abrufdatum"),"datum", new DateFormatter(Settings.DATEFORMAT)); 
    bestandsList.setContextMenu(new BestandsListMenu(bestandsList));
    return bestandsList;
  }

}

