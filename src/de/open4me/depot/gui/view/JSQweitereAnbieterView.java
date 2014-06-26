package de.open4me.depot.gui.view;

import org.eclipse.swt.program.Program;

import de.open4me.depot.DepotViewerPlugin;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.input.TextInput;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.util.Container;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.util.ApplicationException;

public class JSQweitereAnbieterView extends AbstractView
{

	public void bind() throws Exception
	{
		GUI.getView().setTitle("Installation von weiteren Datenquellen");

		Container group = new SimpleContainer(getParent());
		group.addHeadline("Weitere Datenquellen");

		group.addText("Um weitere Datenquellen zu installieren, besuchen sie bitte die Webseite von Java Stock Quotes.\n" 
				+ "\n\nVon dieser Webseite laden sie bitte die zu dem gewünschten Anbieter gehörige Datei herunter und\n"
				+ "speichern diese in dem unten angegebenen Verzeichnis.\n\nAnschließend starten sie bitte das Progarmm neu.\n", true);
				
				
		group.addText("Webseite für weitere Datenquellen:", false);				
		TextInput web = new TextInput("http://rawgit.com/mikekorb/JavaStockQuotes/master/js/overview.html");
		web.setName("Verzeichnis");
		Button button = new Button("Webseite öffnen", new Action() {

			@Override
			public void handleAction(Object context)
					throws ApplicationException {
				Program.launch("http://rawgit.com/mikekorb/JavaStockQuotes/master/js/overview.html");
			}

		}
		,null,true,"dialog-information.png");
		web.paint(group.getComposite());
		button.paint(group.getComposite());
		
		
		
		group.addText("\n\nZielverzeichnis auf ihrem Rechner:", false);
		TextInput dir = new TextInput(DepotViewerPlugin.getJSDirectory());
		dir.setName("Verzeichnis");
		button = new Button("Verzeichnis öffnen", new Action() {

			@Override
			public void handleAction(Object context)
					throws ApplicationException {
				Program.launch(DepotViewerPlugin.getJSDirectory());
			}

		}
		,null,true,"dialog-information.png");
		dir.paint(group.getComposite());
		button.paint(group.getComposite());
		
	}
}