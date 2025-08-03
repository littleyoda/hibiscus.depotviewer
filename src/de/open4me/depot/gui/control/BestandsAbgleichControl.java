package de.open4me.depot.gui.control;

import java.rmi.RemoteException;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import de.open4me.depot.gui.action.InconsistencyFixAction;
import de.open4me.depot.tools.Bestandspruefung;
import de.open4me.depot.tools.InconsistencyData;
import de.willuhn.jameica.gui.AbstractControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Part;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.parts.FormTextPart;
import de.willuhn.jameica.gui.util.ColumnLayout;
import de.willuhn.jameica.gui.util.Container;
import de.willuhn.jameica.gui.util.ScrolledContainer;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.jameica.gui.util.SWTUtil;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

public class BestandsAbgleichControl extends AbstractControl {

	  private Part libList = null;

	  public BestandsAbgleichControl(AbstractView view) {
	    super(view);
	  }

	  /**
	   * Liefert die strukturierte Darstellung der Inkonsistenzen
	   * @return Container mit Inkonsistenzen und Fix-Buttons
	   */
	  public Part getLibList()
	  {
	    if (libList != null)
	      return libList;

	    try {
	      List<InconsistencyData> inconsistencies = Bestandspruefung.getInconsistencies();
	      
	      if (inconsistencies.isEmpty()) {
	        // Keine Inkonsistenzen gefunden
	        StringBuffer buffer = new StringBuffer();
	        buffer.append("<form>Keine Abweichungen gefunden.<br/>Der Bestand passt zu den Umsätzen im Orderbuch.</form>");
	        libList = new FormTextPart(buffer.toString());
	      } else {
	        // Inkonsistenzen mit Buttons anzeigen
	        libList = createInconsistencyContainer(inconsistencies);
	      }
	    } catch (RemoteException | ApplicationException e) {
	      Logger.error("Fehler beim Laden der Inkonsistenzen", e);
	      StringBuffer buffer = new StringBuffer();
	      buffer.append("<form>Fehler beim Laden der Inkonsistenzen: " + StringEscapeUtils.escapeXml(e.getMessage()) + "</form>");
	      libList = new FormTextPart(buffer.toString());
	    }

	    return libList;
	  }
	  
	  /**
	   * Erstellt einen Container mit strukturierter Darstellung der Inkonsistenzen
	   */
	  private Part createInconsistencyContainer(List<InconsistencyData> inconsistencies) {
	    return new Part() {
	      @Override
	      public void paint(org.eclipse.swt.widgets.Composite parent) throws RemoteException {
	        // Verwendung von ScrolledContainer für scrollbare Darstellung
	        ScrolledContainer scrollContainer = new ScrolledContainer(parent);
	        Container container = scrollContainer;
	        container.addHeadline("Folgende Abweichungen wurden gefunden:");
	        
	        for (InconsistencyData inconsistency : inconsistencies) {
	          // Composite für jede Inkonsistenz
	          org.eclipse.swt.widgets.Composite rowComposite = new org.eclipse.swt.widgets.Composite(container.getComposite(), org.eclipse.swt.SWT.NONE);
	          org.eclipse.swt.layout.GridLayout gridLayout = new org.eclipse.swt.layout.GridLayout(2, false);
	          gridLayout.marginHeight = 5;
	          gridLayout.marginWidth = 5;
	          rowComposite.setLayout(gridLayout);
	          
	          // Linke Spalte: Info (70% der Breite)
	          org.eclipse.swt.widgets.Composite leftComposite = new org.eclipse.swt.widgets.Composite(rowComposite, org.eclipse.swt.SWT.NONE);
	          org.eclipse.swt.layout.GridData leftData = new org.eclipse.swt.layout.GridData(org.eclipse.swt.SWT.FILL, org.eclipse.swt.SWT.TOP, true, false);
	          leftData.widthHint = 550; // Etwas schmaler für mehr Button-Platz
	          leftComposite.setLayoutData(leftData);
	          leftComposite.setLayout(new org.eclipse.swt.layout.GridLayout(1, false));
	          
	          // Rechte Spalte: Button (30% der Breite)
	          org.eclipse.swt.widgets.Composite rightComposite = new org.eclipse.swt.widgets.Composite(rowComposite, org.eclipse.swt.SWT.NONE);
	          org.eclipse.swt.layout.GridData rightData = new org.eclipse.swt.layout.GridData(org.eclipse.swt.SWT.END, org.eclipse.swt.SWT.TOP, false, false);
	          rightData.widthHint = 280; // Breitere Button-Spalte für vollständige Anzeige
	          rightComposite.setLayoutData(rightData);
	          rightComposite.setLayout(new org.eclipse.swt.layout.GridLayout(1, false));
	          
	          // Info-Labels links
	          Container left = new SimpleContainer(leftComposite);
	          left.addText("Konto: " + inconsistency.getKontoName() + 
	                      " | WP: " + inconsistency.getWertpapiername() + 
	                      " (" + inconsistency.getWkn() + ")", false);
	          left.addText("ISIN: " + inconsistency.getIsin(), false);
	          left.addText("Bestand: " + inconsistency.getBestandAnzahl().toPlainString() + 
	                      " | Orderbuch: " + inconsistency.getOrderbuchAnzahl().toPlainString() + 
	                      " | Differenz: " + inconsistency.getDifferenz().toPlainString(), false);
	          
	          // Button rechts - immer an derselben horizontalen Position
	          Container right = new SimpleContainer(rightComposite);
	          String buttonText = inconsistency.needsPurchase() ? 
	            "Kauf hinzufügen (" + inconsistency.getRequiredAmount().toPlainString() + ")" :
	            "Verkauf hinzufügen (" + inconsistency.getRequiredAmount().toPlainString() + ")";
	            
	          Button fixButton = new Button(buttonText, new InconsistencyFixAction(), inconsistency);
	          fixButton.setEnabled(true);
	          right.addPart(fixButton);
	          
	          // Separator zwischen Inkonsistenzen
	          container.addSeparator();
	        }
	      }
	    };
	  }
	}
