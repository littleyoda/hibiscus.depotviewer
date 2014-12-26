package de.open4me.depot.gui.control;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.jdbc.JDBCXYDataset;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.jfree.ui.Layer;

import de.open4me.depot.Settings;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.jameica.gui.Part;
import de.willuhn.jameica.gui.parts.TablePart;
import de.willuhn.jameica.gui.util.TabGroup;
import de.willuhn.logging.Logger;

public class WertpaperHistoryControl {


	private JFreeChart chart;
	private JDBCXYDataset data = null;
	private XYItemRenderer renderer;
	private Connection conn;
	private TablePart bestandsList;

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
			// Normale Graphen zeichen
			String sql = "SELECT kursdatum, kurs, kursperf from depotviewer_kurse where wpid = " + nummer + " order by 1";
			data.executeQuery(sql);
			
			
			// Darstellung der Events. Damit die Events auf der passenden Höhe sind, wird ein passender Kurs zum Zeitpunkt des Events bestimmt.
			String inner = SQLUtils.addTop(1, "select kurs from depotviewer_kurse where wpid=depotviewer_kursevent.wpid and kursdatum >= datum order by kursdatum");
			String sql2 = "select *, ( " + inner + " ) as kurs from depotviewer_kursevent where wpid = " + nummer;
			renderer.removeAnnotations();
			List<GenericObjectSQL> list = SQLUtils.getResultSet(sql2,
					"", "id");
			for (GenericObjectSQL x: list) {
				XYAnnotation annotation = new XYTextAnnotation((String) x.getAttribute("aktion"), ((Date) x.getAttribute("datum")).getTime(), ((BigDecimal) x.getAttribute("kurs")).doubleValue());
				renderer.addAnnotation(annotation, Layer.FOREGROUND);
			}
			
			// Kennzahlen
			calcKennzahlen(nummer);

		} catch (Exception e) {
			System.err.print("Exception: ");
			System.err.println(e.getMessage());
		}
	}


	private void calcKennzahlen(String nummer) throws Exception {
		Date d = new Date();
		BigDecimal refKurs = null;
		bestandsList.removeAll();


		PreparedStatement pre = SQLUtils.getPreparedSQL(SQLUtils.addTop(1, "select * from depotviewer_kurse where wpid = " + nummer + " order by kursdatum desc"));
		List<GenericObjectSQL> referenz = SQLUtils.getResultSet(pre, "depotviewer_kurse", "", "");
		if (referenz.size() > 0) {
			//d = (Date) referenz.get(0).getAttribute("kursdatum");
			refKurs = (BigDecimal) referenz.get(0).getAttribute("kurs");
		}

		PreparedStatement getperf = SQLUtils.getPreparedSQL(SQLUtils.addTop(1, "select *, abs(" + SQLUtils.getDateDiff("kursdatum", "?") + ") as diff from depotviewer_kurse where wpid = " + nummer + " order by diff"));
		for (int i = 0; i < 6; i++) {
			GenericObjectHashMap zeile = new GenericObjectHashMap();
			String zeitraum = "";
			Calendar calendar = Calendar.getInstance(); 
			calendar.setTime(d);
			switch (i) {
				case 0: zeitraum = "Letzer Monat"; calendar.add(Calendar.MONTH, -1); break;
				case 1: zeitraum = "Letzes Jahr"; calendar.add(Calendar.YEAR, -1); break;
				case 2: zeitraum = "Letzten 2 Jahre"; calendar.add(Calendar.YEAR, -2); break;
				case 3: zeitraum = "Letzten 3 Jahre"; calendar.add(Calendar.YEAR, -3); break;
				case 4: zeitraum = "Letzten 4 Jahre"; calendar.add(Calendar.YEAR, -4); break;
				case 5: zeitraum = "Letzten 5 Jahre"; calendar.add(Calendar.YEAR, -5); break;
			}
			zeile.setAttribute("zeitraum", zeitraum);
			if (refKurs != null) {
				getperf.setDate(1, new java.sql.Date(calendar.getTime().getTime()));
				List<GenericObjectSQL> x = SQLUtils.getResultSet(getperf, "depotviewer_kurse", "", "");
				if (x.size() == 1) {
					GenericObjectSQL v = x.get(0);
					BigDecimal kurs = (BigDecimal) v.getAttribute("kursperf");
					BigDecimal einprozent = kurs.divide(new BigDecimal("100.0"), 10, RoundingMode.HALF_UP);
					BigDecimal performance = refKurs.subtract(kurs).divide(einprozent, 10, RoundingMode.HALF_UP);
//					(refkurs - kurs)/(kurs/100)
					zeile.setAttribute("performance",  performance.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "%");
				}
			}
			bestandsList.addItem(zeile);
			
		}
	}




	public void getKursChart(Composite comp) throws Exception
	{
		final TabFolder folder = new TabFolder(comp, SWT.CENTER);
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));

		final TabGroup tabellenTab = new TabGroup(folder, "Kennzahlen");
		tabellenTab.addPart(getProjectsTable());
		

		final TabGroup piechartTab = new TabGroup(folder, "Graphisch");
		piechartTab.getComposite().setLayout(new FillLayout());

		chart = ChartFactory.createTimeSeriesChart(
				"",  "Datum",
				"Kurs", data, false, true, false);
		renderer = chart.getXYPlot().getRenderer();
		new ChartComposite(piechartTab.getComposite(), SWT.NONE, chart, true);
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

	public Part getProjectsTable() throws Exception
	{
		if (bestandsList != null) {
			return bestandsList;
		}
		bestandsList = new TablePart(null);
		bestandsList.addColumn(Settings.i18n().tr("Zeitraum"), "zeitraum");
		bestandsList.addColumn(Settings.i18n().tr("Performance"),"performance");
		calcKennzahlen("-1");
		return bestandsList;
	}


}
