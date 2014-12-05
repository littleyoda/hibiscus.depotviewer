package de.open4me.depot.gui.action;

import java.rmi.RemoteException;

import org.eclipse.swt.program.Program;

import de.open4me.depot.sql.GenericObjectSQL;
import de.willuhn.jameica.gui.Action;
import de.willuhn.util.ApplicationException;

public class BrowserOpenAction implements Action {

	private String url;
	public BrowserOpenAction(String url) {
		this.url = url;
	}

	@Override
	public void handleAction(Object context) throws ApplicationException {
		if (context == null || !(context instanceof GenericObjectSQL))
			return;

		GenericObjectSQL wp = (GenericObjectSQL) context;
		String currenturl = url;
		String st = "";
		try {
			if (wp.getAttribute("isin") != null && !((String) wp.getAttribute("isin")).isEmpty()) {
				st = (String) wp.getAttribute("isin");
				currenturl = currenturl.replace("{}", st);
				currenturl = currenturl.replace("{isin}", st);
			}
			if (wp.getAttribute("wkn") != null && !((String) wp.getAttribute("wkn")).isEmpty()) {
				st = (String) wp.getAttribute("wkn");
				currenturl = currenturl.replace("{}", st);
				currenturl = currenturl.replace("{wkn}", st);
			}
			st = (String) wp.getAttribute("name");
			if (st != null) {
			currenturl = currenturl.replace("{}", st);
			currenturl = currenturl.replace("{name}", st);
			}
			Program.launch(currenturl);
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new ApplicationException(e);
		}
	}

}
