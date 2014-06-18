package de.open4me.depot.gui.control;

import java.awt.Font;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.PieDataset;
import org.jfree.data.jdbc.JDBCPieDataset;
import org.jfree.experimental.chart.swt.ChartComposite;

import de.open4me.depot.sql.SQLUtils;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.util.ApplicationException;

public class BestandPieChartControl  
{


	public BestandPieChartControl(AbstractView view) {
	}


	/**
	 * Creates a sample dataset.
	 * 
	 * @return A sample dataset.
	 * @throws Exception 
	 */
	private static PieDataset createDataset() throws Exception {
			JDBCPieDataset dataset = new JDBCPieDataset(SQLUtils.getConnection());
  	        dataset.executeQuery("select wertpapiername as type, wert as count from depotviewer_bestand left join depotviewer_wertpapier where  depotviewer_bestand.wpid = depotviewer_wertpapier.id order by wert");
		    return dataset;			
	}

	/**
	 * Erzeugt das Diagramm
	 * 
	 * @param dataset  Datenobjekt
	 * 
	 * @return Diagramm
	 */
	private static JFreeChart createChart(PieDataset dataset) {
		JFreeChart chart = ChartFactory.createPieChart("Bestand", dataset, true, true, false);       
		PiePlot plot = (PiePlot) chart.getPlot();
		plot.setSectionOutlinesVisible(false);
		plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
		plot.setNoDataMessage("No data available");
		plot.setCircular(false);
		plot.setLabelGap(0.02);
		return chart;
	}

	public ChartComposite getBestandChart(Composite comp) throws ApplicationException
	{
		PieDataset data;
		try {
			data = createDataset();
		} catch (Exception e) {
			e.printStackTrace();
			throw new ApplicationException("Fehler bei der Zusammenstellung der Daten für das Diagramm" ,  e);
		}
		JFreeChart chart = createChart(data);
		return new ChartComposite(comp, SWT.NONE, chart, true);
	}
}