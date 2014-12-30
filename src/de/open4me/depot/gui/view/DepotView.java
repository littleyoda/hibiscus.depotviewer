package de.open4me.depot.gui.view;

import java.rmi.RemoteException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import de.open4me.depot.Settings;
import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.gui.action.EinrichtungsassistentenAction;
import de.open4me.depot.gui.control.DepotListControl;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.gui.util.Container;
import de.willuhn.jameica.gui.util.SWTUtil;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.jameica.hbci.gui.action.KontoFetchUmsaetze;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.hbci.rmi.KontoType;
import de.willuhn.util.ApplicationException;

public class DepotView extends AbstractView
{

	/**
	 * @see de.willuhn.jameica.gui.AbstractView#bind()
	 */
	public void bind() throws Exception {

		GUI.getView().setTitle(Settings.i18n().tr("Depots"));

		final DepotListControl control = new DepotListControl(this);
		if (Utils.getDepotKonten().size() == 0) {
			Container container = new SimpleContainer(getParent());
		      container.addHeadline("Einrichtungsassistent:");
		      container.addText("Sie haben noch keine Konten dem Depotviewer zugewiesen.\n"
		    		  + "Möchten sie den Einrichtungsassistent starten, der Ihnen bei der Einrichtung hilft?\n"
		    		  + "Sie können den Einrichtungsassistenten auch später über den Menüpunkt \"Depot-Viewer\" starten.", true);

				ButtonArea baEinrichtung = new ButtonArea();
				baEinrichtung.addButton("Einrichtungsassistent starten", new Action() {

					@Override
					public void handleAction(Object context) {
						(new EinrichtungsassistentenAction()).handleAction(null);
					}

				}

				,null,true,"dialog-warning-large.png");
				container.addPart(baEinrichtung);
			
		}
	    getText(getParent(), control);


	}

	public Container getText(Composite composite, final DepotListControl control) throws RemoteException, ApplicationException {
		Container container = new SimpleContainer(composite, true);
	      container.addHeadline("Folgende Depots sind aktuell dem Depot-Viewer zugewiesen:");
			container.addPart(control.getDepotOverview());
			ButtonArea buttons1 = new ButtonArea();
			buttons1.addButton("Abrufen", new Action() {

				@Override
				public void handleAction(Object context)
						throws ApplicationException {
					try {
						Object obj = control.getSelectedItem();
						if (obj == null || !(obj instanceof GenericObjectHashMap)) {
							return;
						}
							GenericObjectHashMap o = (GenericObjectHashMap) obj;
							Konto k = (Konto) o.getAttribute("kontoobj");
						(new KontoFetchUmsaetze()).handleAction(k);
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}

			,null,true,"dialog-information.png");
			buttons1.addButton("Einstellungen", new Action() {

				@Override
				public void handleAction(Object context)
						throws ApplicationException {
					Object obj = control.getSelectedItem();
					if (obj == null || !(obj instanceof GenericObjectHashMap)) {
						return;
					}
					control.exec(obj);
				}

			}


			,null,true,"dialog-information.png");
			container.addPart(buttons1);

	      container.addHeadline("Unterstütze Banken:");
	      container.addText("Es werden im wesentlichen nur Banken unterstützt, die für ihre Depots HBCI Support zu Verfügung stellen.\nFür alle anderen Banken müssen handisch Erweiterungen programmiert werden.\n", true);
	      container.addHeadline("Notwendige Konto-Einstellungen für die Nutzung:");
	      container.addText("Kontoart:\n" +
	    		  "        '"  + KontoType.WERTPAPIERDEPOT.getName() + "' oder ' " + KontoType.FONDSDEPOT.getName()+ "'\n", true);
	      container.addText("Zugangsarten: \n" +
	      					"        HBCI, falls die Bank dieses tatsächlich unterstützt\n" +
	    		  			"        DepotViewer (z.Z. nur für die  Fondsdepot Bank)\n", true);

	      container.addHeadline("Geschäftsvorfall WPDepotUms wird nicht unterstützt");
	      container.addText("Teilweise unterstützen die Banken bei HBCI nur den Bestandsabruf und nicht den Abruf von Umsätzen.\n" +
	    		  			"Falls die Fehlermeldung 'Geschäftsvorfall WPDepotUms wird nicht unterstützt' erscheint, so ist unter Einstellungen der Punkt 'Nur Bestand via HBCI abrufen' zu aktivieren. " +
	    		  			"In diesem Fall wird versucht, die fehlenden Informationen aus der Differenz zwischen dem aktuellen und dem letzten Bestand zu ermitteln.\n"
	    		  			, true);
	      return container;
	}
}
