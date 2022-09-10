package de.open4me.depot.gui.dialogs;


import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

import org.apache.commons.io.IOUtils;
import org.eclipse.swt.widgets.Composite;

import de.open4me.depot.gui.parts.FormTextPartExt;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.dialogs.AbstractDialog;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.gui.util.Container;
import de.willuhn.jameica.services.BeanService;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.ApplicationException;

public class EinrichtungsAssistenten extends AbstractDialog
{
	Button weiterButton;
	Button vorButton;
	FormTextPartExt text;
	private String current = "Start";
	private String next = "";
	private Stack<String> prev = new Stack<String>();
	

	public EinrichtungsAssistenten(int position)
	{
		super(position);
		setTitle("Depot Viewer: Einrichtungsassistenten");
	}


	public String lade(String name) throws IOException {
		// Hilfe fuer das eingestellte Locale laden
		String path = "help/de_de/EinrichtungsAssistenten/EA" + name + ".txt";
		InputStream is  = Application.getClassLoader().getResourceAsStream(path);
		if (is == null) {
			return "Eintrag '" + name + "' nicht gefunden!";
		}
		String inhalt = IOUtils.toString(is, "ISO-8859-1");
		return inhalt;
	}


	protected boolean isModeless()
	{
		return true;
	}

	/**
	 * @see de.willuhn.jameica.gui.dialogs.AbstractDialog#paint(org.eclipse.swt.widgets.Composite)
	 */
	protected void paint(final Composite parent) throws Exception
	{
	    Container container = new Container(true) {

			@Override
			public Composite getComposite() {
				return parent;
			}
	    	
	    };
		text = new FormTextPartExt(lade(current)) {

			@Override
			public void hyperlinkPressed(String action) {
				if (action.startsWith("http") || action.startsWith("https") || action.startsWith("mailto")) {
					super.hyperlinkPressed(action);
					return;
				}
				if (action.contains("QUIT")) {
					close();
					return;
				}
				prev.add(current);
				current = action;
				aktuallisiere();
			}
			
		};
		container.addPart(text);


	    ButtonArea buttons = new ButtonArea();
		vorButton = new Button("Schritt zurück", new Action() {
			public void handleAction(Object context) throws ApplicationException
			{
				current = prev.pop();
				aktuallisiere();
			}},null ,false, "go-previous.png");
		weiterButton = new Button("Schritt weiter", new Action() {
			public void handleAction(Object context) throws ApplicationException
			{
				prev.add(current);
				current = next;
				aktuallisiere();
			}},null ,false, "go-next.png");
		buttons.addButton(vorButton);
		buttons.addButton(weiterButton);
		container.addButtonArea(buttons);
	    setSize(380,480); 
		aktuallisiere();
	}

	private void aktuallisiere() {
		String textForDialog = "";
		try {
			textForDialog = lade(current);
			text.setText(textForDialog);
			next = extractValue(textForDialog, "Next");
		} catch (IOException e) {
			text.setText("Leider ist ein Fehler aufgetreten");
		}
		vorButton.setEnabled(!prev.isEmpty());
		weiterButton.setEnabled(!next.isEmpty());
		String actionclass = extractValue(textForDialog, "Action");
		if (!actionclass.isEmpty()) {
			Class c;
			try {
				c = Application.getClassLoader().load(actionclass);
				BeanService beanService = Application.getBootLoader().getBootable(BeanService.class);
				Action action = (Action) beanService.get(c);
				action.handleAction(null);
			} catch (ClassNotFoundException | LinkageError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ApplicationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	protected String extractValue(String text, String var) {
		if (!text.contains(var + ": ")) {
			return "";
		}
		String actionclass = text.substring(text.indexOf(var + ": ") + var.length() + 2);
		return actionclass.substring(0, actionclass.indexOf(" "));
	}

	@Override
	protected Object getData() throws Exception {
		return null;
	}
}