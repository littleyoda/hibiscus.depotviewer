package de.open4me.depot.gui.view;

import java.rmi.RemoteException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.TabFolder;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.DatumsSlider;
import de.open4me.depot.gui.control.BestandPieChartControl;
import de.open4me.depot.gui.control.BestandTableControl;
import de.open4me.depot.gui.control.BestandsControl;
import de.open4me.depot.tools.Bestandspruefung;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.util.Container;
import de.willuhn.jameica.gui.util.LabelGroup;
import de.willuhn.jameica.gui.util.ScrolledContainer;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.jameica.gui.util.TabGroup;
import de.willuhn.util.ApplicationException;

public class BestandView extends AbstractView
{

	/**
	 * @see de.willuhn.jameica.gui.AbstractView#bind()
	 */
	public void bind() throws Exception {
		if (!Bestandspruefung.isOK()) {
			LabelGroup group = new LabelGroup(this.getParent(),
					Settings.i18n().tr("Inkonsistenzen zwischen Umsätzen und Beständen"));
			group.addText("Der Abgleich zwischen Umsatz und Bestand hat Inkonsistenz ergeben.\n"
					+ "Falls sie eine Transaktion vor wenigen Tagen getätig haben, hat die Bank sie evtl. noch nicht als Umsatz und im Bestand gebucht.\nBitte korrigieren sie die Fehler, falls nötig!", true);
			group.addPart(new Button("Inkonsistenzen anzeigen",new Action() {

				@Override
				public void handleAction(Object context)
						throws ApplicationException {
					String output;
					try {
						output = Bestandspruefung.exec();
						GUI.startView(BestandsAbgleichView.class,output);
					} catch (RemoteException e) {
						e.printStackTrace();
						throw new ApplicationException(e);
					}
				}

			}));


		}
		BestandsControl bestandsControl = new BestandsControl(this); 
		GUI.getView().setTitle(Settings.i18n().tr("Bestand"));

		DatumsSlider datumsSlider = new DatumsSlider(bestandsControl.getDates());

		final TabFolder folder = new TabFolder(getParent(), SWT.CENTER);
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));

		TabGroup tabellenTab = new TabGroup(folder, "Tabellarisch");
		BestandTableControl control = new BestandTableControl(this, datumsSlider);
		Container container = new ScrolledContainer(tabellenTab.getComposite());
		container.addPart(control.getBestandsTabelle());

		final TabGroup piechartTab = new TabGroup(folder, "Graphisch");
		piechartTab.getComposite().setLayout(new FillLayout());
		BestandPieChartControl chart = new BestandPieChartControl(this, datumsSlider);
		chart.getBestandChart(piechartTab.getComposite());

		datumsSlider.paint(getParent());

	}
}