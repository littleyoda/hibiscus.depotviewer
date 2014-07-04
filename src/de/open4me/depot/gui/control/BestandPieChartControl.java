package de.open4me.depot.gui.control;

import java.awt.Font;
import java.math.BigDecimal;
import java.text.AttributedString;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.experimental.chart.swt.ChartComposite;

import de.open4me.de.depot.gui.input.DatumsSlider;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.tools.Bestandsabfragen;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.util.ApplicationException;

public class BestandPieChartControl implements Listener  
{


	private DatumsSlider datumsSlider;
	private Date currentdate;
	private DefaultPieDataset dataset;

	public BestandPieChartControl(AbstractView view, DatumsSlider datumsSlider) {
		this.datumsSlider = datumsSlider;
		dataset = new DefaultPieDataset();
		datumsSlider.addListener(this);
	}



	public ChartComposite getBestandChart(Composite comp) throws ApplicationException
	{
		JFreeChart chart = ChartFactory.createPieChart("Bestand", dataset, true, true, false);       
		PiePlot plot = (PiePlot) chart.getPlot();
		plot.setSectionOutlinesVisible(false);
		plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
		plot.setNoDataMessage("No data available");
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
		return new ChartComposite(comp, SWT.NONE, chart, true);
	}


	@Override
	public void handleEvent(Event event) {
		if (currentdate != datumsSlider.getDate()) {
			currentdate = datumsSlider.getDate();
			try {
				dataset.clear();
				for (GenericObjectSQL x : Bestandsabfragen.getBestand(currentdate)) {
					Object anzahl = x.getAttribute("anzahl");
					double wert;
					if (anzahl instanceof BigDecimal) {
						wert = ((BigDecimal) anzahl).doubleValue();
					} else {
						wert = (Double) anzahl;
					}
					
					dataset.setValue((String) x.getAttribute("wertpapiername"), wert);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}