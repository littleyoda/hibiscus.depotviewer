/**********************************************************************
 *
 * Copyright (c) 2004 Olaf Willuhn
 * All rights reserved.
 * 
 * This software is copyrighted work licensed under the terms of the
 * Jameica License.  Please consult the file "LICENSE" for details. 
 *
 **********************************************************************/

package de.open4me.depot.tools;

import java.rmi.RemoteException;
import java.util.List;

import de.willuhn.jameica.hbci.gui.chart.AbstractChartDataSaldo;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.hbci.server.Value;

/**
 * Implementierung eines Datensatzes fuer die Darstellung des Saldenverlaufs.
 */
public class ChartDataPortfolioBalanceHistory extends AbstractChartDataSaldo
{
  private Konto konto      = null;
  private List<Value> data = null;
  
  /**
   * ct.
   * @param k das Konto, fuer das das Diagramm gemalt werden soll.
   * @param data Daten für das Diagramm
   */
  public ChartDataPortfolioBalanceHistory(Konto konto, List<Value> data)
  {
	  this.konto = konto;
	  this.data = data;
  }

  /**
   * @see de.willuhn.jameica.hbci.gui.chart.ChartData#getData()
   */
  @Override
  public List<Value> getData() throws RemoteException
  {
    return this.data;  
  }

	
  /**
   * @see de.willuhn.jameica.hbci.gui.chart.ChartData#getLabel()
   */
  public String getLabel() throws RemoteException
  {
    if (this.konto != null)
      return this.konto.getBezeichnung();
    return i18n.tr("Alle Konten");
  }
}
