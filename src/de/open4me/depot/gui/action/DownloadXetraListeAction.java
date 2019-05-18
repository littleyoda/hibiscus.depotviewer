package de.open4me.depot.gui.action;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URL;

import de.open4me.depot.DepotViewerPlugin;
import de.open4me.depot.abruf.utils.Utils;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.jameica.services.TransportService;
import de.willuhn.jameica.system.Application;
import de.willuhn.jameica.system.BackgroundTask;
import de.willuhn.jameica.transport.Transport;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.ProgressMonitor;

public class DownloadXetraListeAction implements Action
{

	public void handleAction(Object context) throws ApplicationException
	{
		BackgroundTask task = new BackgroundTask() {
			public void run(ProgressMonitor monitor) throws ApplicationException {
				Logger.info("Download started");
				Application.getMessagingFactory().sendMessage(new StatusBarMessage(Application.getI18n().tr("Download gestartet!"),StatusBarMessage.TYPE_INFO));
				File dir = new File(Utils.getWorkingDir(DepotViewerPlugin.class) + File.separatorChar + "lists");
				dir.mkdirs();
				Transport t  = null;

				TransportService ts = Application.getBootLoader().getBootable(TransportService.class);
				try {
					t = ts.getTransport(new URL("https://www.xetra.com/resource/blob/1528/f3ef4eb78603279e35016190af20f9a1/data/t7-xetr-allTradableInstruments.csv"));
					if (!t.exists()) {
						Application.getMessagingFactory().sendMessage(new StatusBarMessage(Application.getI18n().tr("Datei nicht gefunden! Keine Internetverbindung?"),StatusBarMessage.TYPE_ERROR));
						return;
					}
					Application.getMessagingFactory().sendMessage(new StatusBarMessage(Application.getI18n().tr("Download im Hintergrund gestartet!"),StatusBarMessage.TYPE_INFO));
					File temp = new File(dir,"xetra.temp");
					t.get(new BufferedOutputStream(new FileOutputStream(temp)), monitor);

					// Datei für eine schnellere Suche vorbereiten
					File out = new File(dir,"xetra.csv");
					BufferedReader br = new BufferedReader(new FileReader(temp));
					BufferedWriter writer = new BufferedWriter(new FileWriter(out));
					String s = null;
					Boolean skipping = true; 
					while ((s = br.readLine()) != null) {
						if (skipping && !s.contains("WKN")) {
							continue;
						}
						skipping = false;
						s = limitrowcount(s, 9);
						writer.write(s + "\n");
					}
					br.close();
					writer.close();
					temp.delete();
					Logger.info("Finished");
					Application.getMessagingFactory().sendMessage(new StatusBarMessage(Application.getI18n().tr("Download komplett!"),StatusBarMessage.TYPE_SUCCESS));
				} catch (Exception e) {
					e.printStackTrace();
					throw new ApplicationException("Download oder Verarbeitung fehlgeschlagen.", e);

				}
			}
			public boolean isInterrupted()
			{
				return false;
			}
			public void interrupt()
			{
			}
		};

		Application.getController().start(task);


	}

	public String limitrowcount(String row, int rownr) {
		int pos = -1;
		for (int i = 0; i < rownr; i++) {
			pos = row.indexOf(';', pos + 1);
		}
		return row.substring(0, pos);
	}

}

