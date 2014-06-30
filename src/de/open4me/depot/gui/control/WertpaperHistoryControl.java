package de.open4me.depot.gui.control;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.util.Date;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.jdbc.JDBCXYDataset;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.jfree.ui.Layer;

import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

public class WertpaperHistoryControl {


	private JFreeChart chart;
	private JDBCXYDataset data = null;
	private XYItemRenderer renderer;
	private Connection conn;

	public WertpaperHistoryControl() {
		try {
			conn = SQLUtils.getConnection();
			data = new JDBCXYDataset(conn);
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}




	private void readData(String nummer) {

		try {
			String sql = "SELECT kursdatum, kurs, kursperf from depotviewer_kurse where wpid = " + nummer + " order by 1";
			data.executeQuery(sql);
			String sql2 = "select *, (select top 1 kurs from depotviewer_kurse where wpid=depotviewer_kursevent.wpid and kursdatum >= datum order by kursdatum) as kurs from depotviewer_kursevent where wpid = " + nummer;
			renderer.removeAnnotations();
			List<GenericObjectSQL> list = SQLUtils.getResultSet(sql2,
					"", "id");
			for (GenericObjectSQL x: list) {
				XYAnnotation annotation = new XYTextAnnotation((String) x.getAttribute("aktion"), ((Date) x.getAttribute("datum")).getTime(), ((BigDecimal) x.getAttribute("kurs")).doubleValue());
				renderer.addAnnotation(annotation, Layer.FOREGROUND);
			}

		} catch (Exception e) {
			System.err.print("Exception: ");
			System.err.println(e.getMessage());
		}
	}


	public ChartComposite getKursChart(Composite comp) throws ApplicationException
	{
		chart = ChartFactory.createTimeSeriesChart(
				"",  "Datum",
				"Kurs", data, false, true, false);
		renderer = chart.getXYPlot().getRenderer();
		return new ChartComposite(comp, SWT.NONE, chart, true);
	}

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
