package de.open4me.depot.gui.control;

import java.awt.Font;
import java.math.BigDecimal;
import java.text.AttributedString;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.jfree.experimental.swt.SWTUtils;
import org.jfree.util.Rotation;
import org.jfree.util.SortOrder;

import de.open4me.depot.gui.DatumsSlider;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.tools.Bestandsabfragen;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

public class BestandPieChartControl implements Listener  
{


	private DatumsSlider datumsSlider;
	private Date currentdate = new Date();
	private DefaultPieDataset dataset;

	public BestandPieChartControl(AbstractView view, DatumsSlider datumsSlider) {
		this.datumsSlider = datumsSlider;
		dataset = new DefaultPieDataset();
		datumsSlider.addListener(this);
	}



	public ChartComposite getBestandChart(Composite comp) throws ApplicationException
	{
		Font font = SWTUtils.toAwtFont(Display.getCurrent(), Display.getCurrent().getSystemFont());
		JFreeChart chart = ChartFactory.createPieChart3D("Bestand", dataset, true, true, false);
		chart.getLegend().setItemFont(font.deriveFont(9.0f));
		PiePlot3D plot = (PiePlot3D) chart.getPlot();
		plot.setBackgroundAlpha(0);
		plot.setStartAngle(270);
		plot.setDirection(Rotation.CLOCKWISE);
		plot.setForegroundAlpha(0.8f);
		plot.setOutlineVisible(false);
		plot.setSectionOutlinesVisible(false);
		plot.setLabelFont(font.deriveFont(9.0f));
		plot.setNoDataMessage("Kein Bestand verfügbar. Wahrscheinlich fehlen die Kursdaten.");
		plot.setCircular(false);
		plot.setLabelGap(0.02);
		plot.setLabelGenerator(new PieSectionLabelGenerator() {

			@Override
			public AttributedString generateAttributedSectionLabel(
					PieDataset arg0, Comparable arg1) {
				// TODO Auto-generated method stub
				return null;
			}

			 public String generateSectionLabel(final PieDataset dataset, final Comparable key) {
		            String result = null;    
		            if (dataset != null) {
		                    result = key.toString();   
		            }
		            return result;
		        }			
		}); 
		handleEvent(null);
		return new ChartComposite(comp, SWT.NONE, chart, true);
	}


	@Override
	public void handleEvent(Event event) {
		if (currentdate != datumsSlider.getDate()) {
			currentdate = datumsSlider.getDate();
			try {
				dataset.clear();
				for (GenericObjectSQL x : Bestandsabfragen.getBestand(currentdate)) {
					Object anzahl = x.getAttribute("wert");
					if (anzahl == null) {
						Logger.warn("Keine Kursdaten für " + (String) x.getAttribute("wertpapiername"));
						dataset.clear();
						break;
					}
					double wert;
					if (anzahl instanceof BigDecimal) {
						wert = ((BigDecimal) anzahl).doubleValue();
					} else {
						wert = (Double) anzahl;
					}
					
					dataset.setValue((String) x.getAttribute("wertpapiername"), wert);
				}
				dataset.sortByValues(SortOrder.DESCENDING);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}