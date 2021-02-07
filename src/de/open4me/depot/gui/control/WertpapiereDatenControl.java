package de.open4me.depot.gui.control;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleInsets;

import de.open4me.depot.Settings;
import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.gui.parts.PrintfColumn;
import de.open4me.depot.gui.parts.TabFolderExt;
import de.open4me.depot.gui.parts.TabGroupExt;
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
	private de.open4me.depot.jfreechart.SQLXYDataset data = null;
	private Connection conn;
	private WertpapiereControl controller;
	private TabFolderExt folder;
	private GenericObjectSQL[] currentSelection;

	public WertpapiereDatenControl() {
		try {
			conn = SQLUtils.getConnection();
			data = new de.open4me.depot.jfreechart.SQLXYDataset(conn) {

				@Override
			    public void executeQuery(String query) throws SQLException, ApplicationException {
			        super.executeQuery(query);
			        System.out.println("ItemCount: " + getItemCount());
			        System.out.println("Seriescount" + getSeriesCount());
			        for (int i = 0; i < getItemCount(); i++) {
			        //	ArrayList row = (ArrayList) rows.get(i);
			        }
			        	
	//		        return (Number) row.get(seriesIndex + 1);

			    }
				
				@Override
				public Number getX(int seriesIndex, int itemIndex) {
					// TODO Auto-generated method stub
					return super.getX(seriesIndex, itemIndex);
				}

				@Override
				public double getXValue(int series, int item) {
					double x = super.getXValue(series, item);
					//System.out.println(series + " "  + item + " " + x);
					// TODO Auto-generated method stub
					return x;
				}

				@Override
				public Number getY(int seriesIndex, int itemIndex) {
					// TODO Auto-generated method stub
					return super.getY(seriesIndex, itemIndex);
				}

				@Override
				public double getYValue(int series, int item) {
					double x = super.getYValue(series, item);
//					System.out.println(series + " "  + item + " " + x);
					// TODO Auto-generated method stub
					return x;
				}
				
			};
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}

	/**
	 * Wird aufgerufen, wenn ein neues Wertpapier ausgesucht wurde.
	 * Anschließend muss das Diagramm aktualisiert werden
	 */
	public void update(GenericObjectSQL[] selection) {
		currentSelection = selection;
		folder.doNotify();
	}



	private void readData(XYItemRenderer renderer, GenericObjectSQL[] selection) {
		try {
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
		} catch (RemoteException | SQLException | ApplicationException e) {
			e.printStackTrace();
			Logger.error("Fehler beim aktualisieren des Wertpapierdiagrammes", e);
		}
	}

	private void readData(XYItemRenderer renderer, GenericObjectSQL d) {

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
		folder = new TabFolderExt(comp, SWT.CENTER);
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));

		final TabGroup tabellenTab = new TabGroupExt(folder, "Performance") {

			String lastSelection = "";
			@Override
			public void active() {
				String newSelection = Arrays.toString(currentSelection);
				if (currentSelection == null || newSelection.equals(lastSelection)) {
					return;
				}
				lastSelection = newSelection;
				List<GenericObjectHashMap> liste = calcKennzahlen(currentSelection);

				TablePart kennzahlenTabelle = new TablePart(liste, null);
				kennzahlenTabelle.addColumn(Settings.i18n().tr("Zeitraum"), "zeitraum");
				for (GenericObjectSQL wp : currentSelection) {
					try {
						kennzahlenTabelle.addColumn(wp.getAttribute("wertpapiername").toString(), wp.getAttribute("id").toString());
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
				getReplaceableComposite().replace(kennzahlenTabelle);

			}

		};

		new TabGroupExt(folder, "Performance/Jahr") {

			String lastSelection = "";
			@Override
			public void active() {
				String newSelection = Arrays.toString(currentSelection);
				if (currentSelection == null || newSelection.equals(lastSelection)) {
					return;
				}
				lastSelection = newSelection;
				List<GenericObjectHashMap> liste = calcKennzahlen2(currentSelection);

				TablePart kennzahlenTabelle = new TablePart(liste, null);
				kennzahlenTabelle.addColumn(Settings.i18n().tr("Zeitraum"), "zeitraum");
				for (GenericObjectSQL wp : currentSelection) {
					try {
						kennzahlenTabelle.addColumn(wp.getAttribute("wertpapiername").toString(), wp.getAttribute("id").toString());
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
				getReplaceableComposite().replace(kennzahlenTabelle);

			}

		};

		final TabGroup kursTabGroup = new TabGroupExt(folder, "Kurse") {
			String lastSelection = "";
			@Override
			public void active() {
				String newSelection = Arrays.toString(currentSelection);
				if (currentSelection == null || newSelection.equals(lastSelection)) {
					return;
				}
				lastSelection = newSelection;
				try {
					String sql = "";
					String ids = "";
					String spalten = "datum.kursdatum as zeitraum";
					for (GenericObjectSQL d : currentSelection) {
						if (!ids.isEmpty()) {
							ids += ",";
						}
						String id = d.getAttribute("id").toString();
						ids += id;
						spalten += ", a" + id + ".kurs as A" + id + "k, a" + id + ".kursw as A" + id + "kw"; 
						spalten += ", a" + id + ".kursperf as A" + id + "kp, a" + id + ".kursw as A" + id + "kpw";
						sql +="left join depotviewer_kurse as A" + id + " on A" + id + ".wpid = " + id + " and A" + id + ".kursdatum = datum.kursdatum\n";
					}
					sql = "select "  + spalten + " from (select distinct kursdatum from depotviewer_kurse where wpid in (" + ids + ") order by 1 desc) as datum\n" + sql;

					List<GenericObjectSQL> liste = SQLUtils.getResultSet(sql, "depotview_kurse", "", "");
					TablePart tab = new TablePart(liste, null);
					tab.addColumn(Settings.i18n().tr("Zeitraum"), "zeitraum");
					for (GenericObjectSQL d : currentSelection) {
						String id = d.getAttribute("id").toString();
						tab.addColumn(new PrintfColumn(Settings.i18n().tr(d.getAttribute("wertpapiername").toString()), "a" + id + "k", "%.6f %s", "a" + id + "k", "a" + id + "kw"));
						tab.addColumn(new PrintfColumn(Settings.i18n().tr(d.getAttribute("wertpapiername").toString()), "a" + id + "kp", "%.6f %s", "a" + id + "kp", "a" + id + "kpw"));
					}
					getReplaceableComposite().replace(tab);
				} catch (Exception e) {
					e.printStackTrace();
					Logger.error("Fehler bei der Kursdarstellung", e);
				} 
			}
		};

		final TabGroup graphischTab = new TabGroupExt(folder, "Graphisch", false) {
			
			{
				getComposite().setLayout(new FillLayout());

				chart = ChartFactory.createTimeSeriesChart(
						"",  "Datum",
						"Kurs", data, true, true, false);
				chart.getXYPlot().setBackgroundPaint(Color.WHITE);
				chart.getXYPlot().setOutlineVisible(false);
				chart.getXYPlot().setAxisOffset(RectangleInsets.ZERO_INSETS);
				chart.getXYPlot().setDomainGridlinePaint(Color.LIGHT_GRAY);
				chart.getXYPlot().setRangeGridlinePaint(Color.LIGHT_GRAY);
				renderer = chart.getXYPlot().getRenderer();
				Rectangle maxSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
				new ChartComposite(getComposite(), SWT.NONE, chart, 
						ChartComposite.DEFAULT_WIDTH,
						ChartComposite.DEFAULT_HEIGHT,
						ChartComposite.DEFAULT_MINIMUM_DRAW_WIDTH,
						ChartComposite.DEFAULT_MINIMUM_DRAW_HEIGHT,
						maxSize.width, // ChartComposite.DEFAULT_MAXIMUM_DRAW_WIDTH,
						maxSize.height, // ChartComposite.DEFAULT_MAXIMUM_DRAW_HEIGHT,
		                true,  // useBuffer
		                true,  // properties
		                true,  // save
		                true,  // print
		                true,  // zoom
		                true   // tooltips
	                );
			}
			
			String lastSelection = "";
			private XYItemRenderer renderer;
			@Override
			public void active() {
				String newSelection = Arrays.toString(currentSelection);
				if (currentSelection == null || newSelection.equals(lastSelection)) {
					return;
				}
				if (currentSelection.length == 1) {
					chart.setTitle("Kursverlauf");
					readData(renderer, currentSelection[0]);
				} else {
					chart.setTitle("Performancevergleich");
					readData(renderer, currentSelection);
				}
			}
		};
		
	}







	private List<GenericObjectHashMap> calcKennzahlen(GenericObjectSQL[] selection) {
		List<GenericObjectHashMap> zeilen = new ArrayList<GenericObjectHashMap>();
		try {
			// Legende hinzufügen
			for (int i = 0; i < 6; i++) {
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
				zeile.setAttribute("zeitraum", zeitraum);
				zeilen.add(zeile);
			}

			// Über alle ausgewählten Wertpapiere iterieren
			for (GenericObjectSQL wp : selection) {
				String wpid = wp.getAttribute("id").toString();

				BigDecimal refKurs = getReferenzKurs(wpid, 10);

				for (int i = 0; i < 6; i++) {
						if (refKurs == null) {
							zeilen.get(i).setAttribute(wpid,  "Keine (aktuellen) Kursdaten");
							continue;
						}
						String out = getPerforamnceZahl(i, refKurs, wpid);
						zeilen.get(i).setAttribute(wpid,  out);
				} // For Zeitraum
			} // For WP



		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("Fehler bei der Berechnung der Kennzahlen", e);
		}
		return zeilen;

	}

	private List<GenericObjectHashMap> calcKennzahlen2(GenericObjectSQL[] selection) {
		List<GenericObjectHashMap> zeilen = new ArrayList<GenericObjectHashMap>();
		Date now = new Date();
		try {
			// Legende hinzufügen
			for (int i = 0; i < 10; i++) {
				GenericObjectHashMap zeile = new GenericObjectHashMap();
				int jahr = now.getYear() + 1900 - (i);
				zeile.setAttribute("zeitraum", "Jahr " + jahr);
				zeilen.add(zeile);
			}

			// Über alle ausgewählten Wertpapiere iterieren
			for (GenericObjectSQL wp : selection) {
				String wpid = wp.getAttribute("id").toString();

				BigDecimal refKurs = getReferenzKurs(wpid, 10);

				for (int i = 0; i < 10; i++) {
						String out = getPerformanceFuerJahr(now.getYear() + 1900 - (i), wpid);
						zeilen.get(i).setAttribute(wpid,  out);
				} // For Zeitraum
			} // For WP



		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("Fehler bei der Berechnung der Kennzahlen", e);
		}
		return zeilen;

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
		if (daysdiff > 7 && (jahr !=  Calendar.getInstance().get(Calendar.YEAR))) {
			return "Keine aktuellen Kursdaten (" + daysdiff + " Tage alt)";
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
			return "Keine Kursdaten des Vorjahres (" + daysdiff + " Tage alt)";
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



	public void setController(WertpapiereControl controller) {
		this.controller = controller;
	}





}
