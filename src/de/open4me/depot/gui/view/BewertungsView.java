package de.open4me.depot.gui.view;

import java.rmi.RemoteException;

import de.open4me.depot.Settings;
import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.gui.control.BewertungsControl;
import de.open4me.depot.tools.Bestandspruefung;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.util.LabelGroup;
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
						+ "Aus diesem Grund ist die Auswertung wahrscheinlich fehlerhaft.\nBitte korrigieren sie die Fehler!", true);
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

		control.getOrderInfoTable().paint(this.getParent());


	}
}
