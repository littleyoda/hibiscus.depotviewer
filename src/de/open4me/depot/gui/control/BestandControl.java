package de.open4me.depot.gui.control;

import java.rmi.RemoteException;
import java.util.List;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.action.OrderList;
import de.open4me.depot.rmi.Bestand;
import de.willuhn.datasource.rmi.DBIterator;
import de.willuhn.jameica.gui.AbstractControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Part;
import de.willuhn.jameica.gui.formatter.DateFormatter;
import de.willuhn.jameica.gui.parts.TablePart;
import de.willuhn.logging.Logger;


public class BestandControl extends AbstractControl
{

  private TablePart bestandsList;

	public BestandControl(AbstractView view)
	{
		super(view);
	}

  public Part getProjectsTable() throws RemoteException
  {
    if (bestandsList != null) {
      return bestandsList;
    }
   
    DBIterator projects = Settings.getDBService().createList(Bestand.class);
    bestandsList = new TablePart(projects,new OrderList()) {

		@Override
		protected String getSummary() {
			try {

		      double sum = 0.0d;
		      for (Bestand k:(List<Bestand>) getItems())
		      {
		        sum += k.getWert();
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

    bestandsList.addColumn(Settings.i18n().tr("wkn"),"wkn");
    
    bestandsList.addColumn(Settings.i18n().tr("Anzahl"),"anzahl"); 
    bestandsList.addColumn(Settings.i18n().tr("Kurs"),"kurs"); 
    bestandsList.addColumn(Settings.i18n().tr("Wert"),"wert"); 
    bestandsList.addColumn(Settings.i18n().tr("Datum"),"datum", new DateFormatter(Settings.DATEFORMAT)); 
    return bestandsList;
  }

}

