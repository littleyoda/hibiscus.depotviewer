package de.open4me.depot.gui.view;

import java.rmi.RemoteException;

import org.eclipse.swt.widgets.Composite;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.control.DepotListControl;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.gui.util.Container;
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

	      Container container = getText(getParent());
		final DepotListControl control = new DepotListControl(this);
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


	}

	public Container getText(Composite composite) {
		Container container = new SimpleContainer(composite);
	      container.addHeadline("Unterstütze Banken:");
	      container.addText("Es werden im wesentlichen nur Banken unterstützt, die für ihre Depots HBCI Support zu Verfügung stellen.\nFür alle anderen Banken müssen handisch Erweiterungen programmiert werden.\n", true);
	      container.addHeadline("Notwendige Konto-Einstellungen für die Nutzung:");
	      container.addText("Kontoart:\n" +
	    		  "        '"  + KontoType.WERTPAPIERDEPOT.getName() + "' oder ' " + KontoType.FONDSDEPOT.getName()+ "'\n", true);
	      container.addText("Zugangsarten: \n" +
	      					"        HBCI, falls die Bank dieses tatsächlich unterstützt\n" +
	    		  			"        DepotViewer (z.Z. nur für die  Fondsdepot Bank)\n", true);
//	      FormTextPart text = new FormTextPart();
//	      text.setText("<form>Mehr Informationen: [" + Links.einrichtung.getHTML() +"]" 
//	    		  + "</form>");
//	      container.addPart(text);
	      
//	      FormTextPart text = new FormTextPart();
//	      text.setText("<form>" +
//	        "<p><b>Hibiscus - HBCI-Onlinebanking für Jameica</b></p>" +
//	        "<p>Lizenz: GPL [<a href=\"http://www.gnu.org/copyleft/gpl.html\">www.gnu.org/copyleft/gpl.html</a>]<br/>" +
//	        "Copyright by Olaf Willuhn [<a href=\"mailto:hibiscus@willuhn.de\">hibiscus@willuhn.de</a>]<br/>" +
//	        "<a href=\"http://www.willuhn.de/products/hibiscus/\">www.willuhn.de/products/hibiscus/</a></p>" +
//	        "</form>");
//
//	      container.addPart(text);

	      container.addHeadline("Geschäftsvorfall WPDepotUms wird nicht unterstützt");
	      container.addText("Teilweise unterstützen die Banken bei HBCI nur den Bestandsabruf und nicht den Abruf von Umsätzen.\n" +
	    		  			"Falls die Fehlermeldung 'Geschäftsvorfall WPDepotUms wird nicht unterstützt' erscheint, so ist unter Einstellungen der Punkt 'Nur Bestand via HBCI abrufen' zu aktivieren. " +
	    		  			"In diesem Fall wird versucht, die fehlenden Informationen aus der Differenz zwischen dem aktuellen und dem letzten Bestand zu ermitteln.\n"
	    		  			, true);
	      container.addHeadline("Folgende Depots sind aktuell dem Depot-Viewer zugewiesen:");
	      return container;
	}
}
