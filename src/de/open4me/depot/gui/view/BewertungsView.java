package de.open4me.depot.gui.view;

import java.rmi.RemoteException;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.control.BewertungsControl;
import de.open4me.depot.tools.Bestandspruefung;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.util.ColumnLayout;
import de.willuhn.jameica.gui.util.Container;
import de.willuhn.jameica.gui.util.LabelGroup;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.util.ApplicationException;

public class BewertungsView extends AbstractView
{

	/**
	 * @see de.willuhn.jameica.gui.AbstractView#bind()
	 */
	public void bind() throws Exception {

		GUI.getView().setTitle(Settings.i18n().tr("Gewinn/Verlust"));

		if (!Bestandspruefung.isOK()) {
			LabelGroup group = new LabelGroup(this.getParent(),
					Settings.i18n().tr("Inkonsistenzen zwischen Umsätzen und Beständen"));
			group.addText("Der Abgleich zwischen Umsatz und Bestand hat Inkonsistenz ergeben.\n"
						+ "Aus diesem Grund ist die Auswertung wahrscheinlich fehlerhaft.\nFalls sie eine Transaktion vor wenigen Tagen getätig haben, hat die Bank sie evtl. noch nicht als Umsatz und im Bestand gebucht.\nBitte korrigieren sie die Fehler, falls nötig!", true);
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
		BewertungsControl control = new BewertungsControl(this);

		// Filter-Gruppe mit horizontalem Layout hinzufügen
		LabelGroup filterGroup = new LabelGroup(this.getParent(), Settings.i18n().tr("Filter"));
		ColumnLayout filterLayout = new ColumnLayout(filterGroup.getComposite(), 3);
		
		Container depotContainer = new SimpleContainer(filterLayout.getComposite());
		depotContainer.addPart(control.getDepotFilter());
		
		Container wpContainer = new SimpleContainer(filterLayout.getComposite());
		wpContainer.addPart(control.getWertpapierFilter());
		
		Container bestandContainer = new SimpleContainer(filterLayout.getComposite());
		bestandContainer.addPart(control.getNurBestandFilter());

		// Setup dispose listeners after controls are properly initialized
		control.setupDisposeListeners();

		control.getOrderInfoTable().paint(this.getParent());


	}
}
