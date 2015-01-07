package de.open4me.depot.gui.control;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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
import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.gui.parts.ReplaceableComposite;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.jameica.gui.parts.TablePart;
import de.willuhn.jameica.gui.util.TabGroup;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

// TODO Tabs in einzelne Klassen auslagern
public class WertpapiereDatenControl {


	private JFreeChart chart;
	private JDBCXYDataset data = null;
	private XYItemRenderer renderer;
	private Connection conn;
	private WertpapiereControl controller;
	private ReplaceableComposite kennzahlenTab;
	private ReplaceableComposite kursTab;

	public WertpapiereDatenControl() {
		try {
			conn = SQLUtils.getConnection();
			data = new JDBCXYDataset(conn);
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}




	private void readData(GenericObjectSQL d) {

		try {
			String wpid = d.getAttribute("id").toString();
			// Normale Graphen zeichen
			String sql = "SELECT kursdatum, kurs as Kurs, kursperf as Performance from depotviewer_kurse where wpid = " + wpid + " order by 1";
			data.executeQuery(sql);


			// Darstellung der Events. Damit die Events auf der passenden Höhe sind, wird ein passender Kurs zum Zeitpunkt des Events bestimmt.
			String inner = SQLUtils.addTop(1, "select kurs from depotviewer_kurse where wpid=depotviewer_kursevent.wpid and kursdatum >= datum order by kursdatum");
			String sql2 = "select *, ( " + inner + " ) as kurs from depotviewer_kursevent where wpid = " + wpid;
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




	public void getKursChart(Composite comp) throws Exception
	{
		final TabFolder folder = new TabFolder(comp, SWT.CENTER);
		folder.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				System.out.println("Normal: " + folder.getSelectionIndex());
				
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				System.out.println("Default: " + folder.getSelectionIndex());
				
			}
			
		});
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));

		final TabGroup tabellenTab = new TabGroup(folder, "Kennzahlen");
		kennzahlenTab  = new ReplaceableComposite(tabellenTab.getComposite(),SWT.NONE);
		kennzahlenTab.replace(getKennzahlenTabelle(null));

		final TabGroup kursTabGroup = new TabGroup(folder, "Kurse");
		kursTab  = new ReplaceableComposite(kursTabGroup.getComposite(),SWT.NONE);
		kursTab.replace(getKursTabelle(null));

		final TabGroup piechartTab = new TabGroup(folder, "Graphisch");
		piechartTab.getComposite().setLayout(new FillLayout());

		chart = ChartFactory.createTimeSeriesChart(
				"",  "Datum",
				"Kurs", data, true, true, false);
		renderer = chart.getXYPlot().getRenderer();
		new ChartComposite(piechartTab.getComposite(), SWT.NONE, chart, true);
	}




	/**
	 * Wird aufgerufen, wenn ein neues Wertpapier ausgesucht wurde.
	 * Anschließend muss das Diagramm aktualisiert werden
	 */
	public void update(GenericObjectSQL[] selection) {
		try {
			calcKennzahlen(selection);
			calcKurs(selection);
			if (selection.length == 1) {
				try {
					chart.setTitle((String) selection[0].getAttribute("wertpapiername"));
					readData(selection[0]);
				} catch (RemoteException e) {
					e.printStackTrace();
					Logger.error("Fehler beim aktualisieren des Wertpapierdiagrammes", e);
				}
				return;
			}
			chart.setTitle("Performancevergleich");
			String ids = "";
			String spalten = "datum.kursdatum";
			String sql = "";
			for (GenericObjectSQL d : selection) {
				if (!ids.isEmpty()) {
					ids += ",";
				}
				String id = d.getAttribute("id").toString(); 
				ids += id;
				spalten += ", A" + id + ".kurs as \"" + StringEscapeUtils.escapeSql(d.getAttribute("wertpapiername").toString()) + "\" ";
				sql +="left join depotviewer_kurse as A" + id + " on A" + id + ".wpid = " + id + " and A" + id + ".kursdatum = datum.kursdatum\n";
			}
			sql = "select "  + spalten + " from (select distinct kursdatum from depotviewer_kurse where wpid in (" + ids + ") order by 1) as datum\n" + sql;
			renderer.removeAnnotations();
			data.executeQuery(sql);
		} catch (RemoteException | SQLException e) {
			e.printStackTrace();
			Logger.error("Fehler beim aktualisieren des Wertpapierdiagrammes", e);
		}
	}



	private void calcKurs(GenericObjectSQL[] selection) {

		try {
			String sql = "";
			String ids = "";
			String spalten = "datum.kursdatum as zeitraum";
			for (GenericObjectSQL d : selection) {
				if (!ids.isEmpty()) {
					ids += ",";
				}
				String id = d.getAttribute("id").toString();
				ids += id;
				spalten += ", concat(a" + id + ".kurs, ' ', a" + id + ".kursw) as A" + id + "k"; 
				spalten += ", concat(a" + id + ".kursperf, ' ', a" + id + ".kursw)  as A" + id + "kp";
				sql +="left join depotviewer_kurse as A" + id + " on A" + id + ".wpid = " + id + " and A" + id + ".kursdatum = datum.kursdatum\n";
			}
			sql = "select "  + spalten + " from (select distinct kursdatum from depotviewer_kurse where wpid in (" + ids + ") order by 1) as datum\n" + sql;

			List<GenericObjectSQL> liste = SQLUtils.getResultSet(sql, "depotview_kurse", "", "");
			TablePart tab = getKennzahlenTabelle(liste);
			for (GenericObjectSQL d : selection) {
				String id = d.getAttribute("id").toString();
				tab.addColumn(d.getAttribute("wertpapiername").toString(), "a" +id + "k");
				tab.addColumn(d.getAttribute("wertpapiername").toString() + " (P)", "a" +id + "kp");
			}
			kursTab.replace(tab);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}




	private void calcKennzahlen(GenericObjectSQL[] selection) {
		Date now = new Date();
		TablePart tab;
		try {
			// Legende hinzufügen
			List<GenericObjectHashMap> zeilen = new ArrayList<GenericObjectHashMap>();
			for (int i = 0; i < 16; i++) {
				GenericObjectHashMap zeile = new GenericObjectHashMap();
				String zeitraum = "";
				switch (i) {
				case 0: zeitraum = "Letzer Monat";  break;
				case 1: zeitraum = "Letzes Jahr";  break;
				case 2: zeitraum = "Letzten 2 Jahre"; break;
				case 3: zeitraum = "Letzten 3 Jahre"; break;
				case 4: zeitraum = "Letzten 4 Jahre"; break;
				case 5: zeitraum = "Letzten 5 Jahre";  break;
				}
				if (i > 5) {
					int jahr = now.getYear() + 1900 - (i -6);
					zeitraum = "Jahr " + jahr;
				}
				zeile.setAttribute("zeitraum", zeitraum);
				zeilen.add(zeile);
			}

			// Über alle ausgewählten Wertpapiere iterieren
			for (GenericObjectSQL wp : selection) {
				String wpid = wp.getAttribute("id").toString();

				BigDecimal refKurs = getReferenzKurs(wpid, 10);

				for (int i = 0; i < 16; i++) {
					if (i <= 5) {
						if (refKurs == null) {
							zeilen.get(i).setAttribute(wpid,  "Keine (aktuellen) Kursdaten");
							continue;
						}
						String out = getPerforamnceZahl(i, refKurs, wpid);
						zeilen.get(i).setAttribute(wpid,  out);
					} else {
						String out = getPerformanceFuerJahr(now.getYear() + 1900 - (i -6), wpid);
						zeilen.get(i).setAttribute(wpid,  out);
					}
				} // For Zeitraum
			} // For WP


			// Tabelle erzeugen
			tab = getKennzahlenTabelle(zeilen);
			for (GenericObjectSQL wp : selection) {
				tab.addColumn(wp.getAttribute("wertpapiername").toString(), wp.getAttribute("id").toString());
			}
			kennzahlenTab.replace(tab);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}




	private TablePart getKursTabelle(List<GenericObjectHashMap> list) {
		TablePart kursTabelle = new TablePart(list, null);
		return kursTabelle;
	}




	private String getPerformanceFuerJahr(int jahr, String wpid) throws ApplicationException, Exception {
		// letzter 
		PreparedStatement pre = SQLUtils.getPreparedSQL(SQLUtils.addTop(1, "select *, abs(" + SQLUtils.getDateDiff("kursdatum", "?") + ") as diff  from depotviewer_kurse where wpid = " + wpid + " and kursdatum <= ? order by kursdatum desc"));
		pre.setDate(1, Utils.getSQLDate(Utils.getDatum(jahr, 12, 31)));
		pre.setDate(2, Utils.getSQLDate(Utils.getDatum(jahr, 12, 31)));
		List<GenericObjectSQL> referenz = SQLUtils.getResultSet(pre, "depotviewer_kurse", "", "");
		if (referenz.size() == 0) {
			return "Keine Kursdaten";
		}
		Long daysdiff = (Long) referenz.get(0).getAttribute("diff");
		if (daysdiff > 7) {
			return "Keine aktuellen Kursdaten";
		}
		BigDecimal jahresEnde = (BigDecimal) referenz.get(0).getAttribute("kursperf");

		// Ende des Vorjahr
		pre.setDate(1, Utils.getSQLDate(Utils.getDatum(jahr - 1, 12, 31)));
		pre.setDate(2, Utils.getSQLDate(Utils.getDatum(jahr - 1, 12, 31)));
		referenz = SQLUtils.getResultSet(pre, "depotviewer_kurse", "", "");
		if (referenz.size() == 0) {
			return "Keine Kursdaten";
		}
		daysdiff = (Long) referenz.get(0).getAttribute("diff");
		if (daysdiff > 7) {
			return "Keine aktuellen Kursdaten";
		}
		BigDecimal vorjahresEnde = (BigDecimal) referenz.get(0).getAttribute("kursperf");

		BigDecimal einprozent = vorjahresEnde.divide(new BigDecimal("100.0"), 10, RoundingMode.HALF_UP);
		BigDecimal performance = jahresEnde.subtract(vorjahresEnde).divide(einprozent, 10, RoundingMode.HALF_UP);
		return performance.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "%";
	}




	private BigDecimal getReferenzKurs(String wpid, int maximalesAlter) throws Exception,
	ApplicationException, SQLException, RemoteException {
		PreparedStatement pre = SQLUtils.getPreparedSQL(SQLUtils.addTop(1, "select *, abs(" + SQLUtils.getDateDiff("kursdatum", "?") + ") as diff  from depotviewer_kurse where wpid = " + wpid + " order by kursdatum desc"));
		pre.setDate(1, new java.sql.Date((new Date()).getTime()));
		BigDecimal refKurs = null;
		List<GenericObjectSQL> referenz = SQLUtils.getResultSet(pre, "depotviewer_kurse", "", "");
		if (referenz.size() > 0) {
			Long daysdiff = (Long) referenz.get(0).getAttribute("diff");
			if (daysdiff < maximalesAlter) {
				refKurs = (BigDecimal) referenz.get(0).getAttribute("kurs");
			}
		}
		return refKurs;
	}

	public String getPerforamnceZahl(int i, BigDecimal refKurs, String wpid) throws Exception {
		Calendar calendar = Calendar.getInstance(); 
		calendar.setTime(new Date());
		switch (i) {
		case 0: calendar.add(Calendar.MONTH, -1); break;
		case 1: calendar.add(Calendar.YEAR, -1); break;
		case 2: calendar.add(Calendar.YEAR, -2); break;
		case 3: calendar.add(Calendar.YEAR, -3); break;
		case 4: calendar.add(Calendar.YEAR, -4); break;
		case 5: calendar.add(Calendar.YEAR, -5); break;
		}
		PreparedStatement getperf  = SQLUtils.getPreparedSQL(
				SQLUtils.addTop(1, 
						"select *, abs(" + SQLUtils.getDateDiff("kursdatum", "?") + ") as diff from depotviewer_kurse where wpid = " + wpid + " order by diff"));
		getperf.setDate(1, new java.sql.Date(calendar.getTime().getTime()));
		List<GenericObjectSQL> x = SQLUtils.getResultSet(getperf, "depotviewer_kurse", "", "");
		if (x.size() == 0) {
			return "Keine Kurse";
		}
		GenericObjectSQL v = x.get(0);
		Long daysdiff = (Long) v.getAttribute("diff");
		if (daysdiff > 7) {
			return "Keine aktuellen Kurse";
		}
		BigDecimal kurs = (BigDecimal) v.getAttribute("kursperf");
		BigDecimal einprozent = kurs.divide(new BigDecimal("100.0"), 10, RoundingMode.HALF_UP);
		BigDecimal performance = refKurs.subtract(kurs).divide(einprozent, 10, RoundingMode.HALF_UP);
		//					(refkurs - kurs)/(kurs/100)
		return performance.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "%";
	}


	public TablePart getKennzahlenTabelle(List list) throws Exception {
		TablePart kennzahlenTabelle = new TablePart(list, null);
		kennzahlenTabelle.addColumn(Settings.i18n().tr("Zeitraum"), "zeitraum");
		return kennzahlenTabelle;
	}




	public void setController(WertpapiereControl controller) {
		this.controller = controller;
	}





}
