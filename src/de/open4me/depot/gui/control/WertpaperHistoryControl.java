package de.open4me.depot.gui.control;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.jdbc.JDBCXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.jfree.ui.Layer;

import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

public class WertpaperHistoryControl {

	
	private JFreeChart chart;
	private JDBCXYDataset data = null;

	public WertpaperHistoryControl() {
		try {
		    Connection  conn = SQLUtils.getConnection();
  	        data = new JDBCXYDataset(conn);
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}



	
	private XYDataset readData(String nummer) {

		    try {
		      String sql = "SELECT kursdatum, kurs from depotviewer_kurse where wpid = " + nummer + " order by 1";
		      data.executeQuery(sql);
		    } catch (Exception e) {
		      System.err.print("Exception: ");
		      System.err.println(e.getMessage());
		    }
		    return data;
		  }


	public ChartComposite getKursChart(Composite comp) throws ApplicationException
	{
         chart = ChartFactory.createTimeSeriesChart(
             "",  "Datum",
             "Kurs", data, false, true, false);
         XYPlot x = chart.getXYPlot();
         x.getRenderer().removeAnnotations();
//       XYAnnotation annotation = new XYTextAnnotation("Hello World!", (new Date(110, 0, 1)).getTime(), 100.0);
//       x.getRenderer().addAnnotation(annotation, Layer.FOREGROUND);
		return new ChartComposite(comp, SWT.NONE, chart, true);
	}
	

//    public static JFreeChart createTimeSeriesChart(String title,
//            String timeAxisLabel, String valueAxisLabel, XYDataset dataset,
//            boolean legend, boolean tooltips, boolean urls) {
//
//        ValueAxis timeAxis = new DateAxis(timeAxisLabel);
//        timeAxis.setLowerMargin(0.02);  // reduce the default margins
//        timeAxis.setUpperMargin(0.02);
//        NumberAxis valueAxis = new NumberAxis(valueAxisLabel);
//        valueAxis.setAutoRangeIncludesZero(false);  // override default
//        XYPlot plot = new XYPlot(dataset, timeAxis, valueAxis, null);
////        plot.addAnnotation(bestBid);
//        XYToolTipGenerator toolTipGenerator = null;
//        if (tooltips) {
//            toolTipGenerator
//                = StandardXYToolTipGenerator.getTimeSeriesInstance();
//        }
//
//        XYURLGenerator urlGenerator = null;
//        if (urls) {
//            urlGenerator = new StandardXYURLGenerator();
//        }
//
//        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true,
//                false);
//        renderer.setBaseToolTipGenerator(toolTipGenerator);
//        renderer.setURLGenerator(urlGenerator);
//        XYAnnotation annotation = new XYTextAnnotation("Hello World!", (new Date(110, 0, 1)).getTime(), 100.0);
//        renderer.addAnnotation(annotation, Layer.FOREGROUND);
//////        final CircleDrawer cd = new CircleDrawer(Color.green, new BasicStroke(1.0f), null);
//////        final XYAnnotation bestBid = new XYDrawableAnnotation((new Date(110, 0, 1)).getTime(), 100.0, 1000*60*60*24, 1000*60*60*24, cd);
////        renderer.addAnnotation(bestBid);
//        plot.setRenderer(renderer);
//
//        JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT,
//                plot, legend);
//        chart.getPlot()
//        (new StandardChartTheme("JFree")).apply(chart);
//        return chart;
//
//    }
//
	
	/**
	 * Wird aufgerufen, wenn ein neues Wertpapier ausgesucht wurde.
	 * Anschließend muss das Diagramm aktualisiert werden
	 */
	public void update(GenericObjectSQL d) {
		try {
			chart.setTitle((String) d.getAttribute("wertpapiername"));
			readData(d.getAttribute("id").toString());
		} catch (RemoteException e) {
			e.printStackTrace();
			Logger.error("Fehler beim aktualisieren des Wertpapierdiagrammes", e);
		}
	}


}
