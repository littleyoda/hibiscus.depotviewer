package de.open4me.depot.tools;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.List;

import org.eclipse.swt.program.Program;

import jsq.config.Config;
import jsq.datastructes.Datacontainer;
import jsq.fetcher.history.BaseFetcher;
import de.open4me.depot.gui.dialogs.KursAktualisierenAnbieterAuswahlDialog;
import de.open4me.depot.gui.dialogs.KursAktualisierenDialog;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.util.ApplicationException;


public class UpdateStock {

	public static void update(GenericObjectSQL wertpapier) throws ApplicationException {
		try {
			String searchterm = null;
			if (!wertpapier.isEmpty("isin")) {
				searchterm = wertpapier.getAttribute("isin").toString();
			} else if (!wertpapier.isEmpty("wkn")) {
				searchterm = wertpapier.getAttribute("wkn").toString();
			}
			
			
			KursAktualisierenAnbieterAuswahlDialog dialog1 = new KursAktualisierenAnbieterAuswahlDialog(KursAktualisierenDialog.POSITION_CENTER);
			BaseFetcher base = (BaseFetcher) dialog1.open();
			Date d = new Date();
			base.prepare(searchterm, 2000, 1, 1, d.getYear() + 1900, d.getMonth() + 1, d.getDate());
			while (base.hasMoreConfig()) {
				List<Config> cfg = base.getConfigs();
				KursAktualisierenDialog dialog= new KursAktualisierenDialog(KursAktualisierenDialog.POSITION_CENTER, cfg);
				dialog.open();
				base.process(cfg);
			}
			Connection conn = SQLUtils.getConnection();
			PreparedStatement del = conn.prepareStatement("delete from depotviewer_kurse where wpid = ? ");
			del.setString(1, wertpapier.getID());
			del.executeUpdate();
			PreparedStatement insert = conn.prepareStatement("insert into depotviewer_kurse (wpid, kurs, kursw, kursdatum) values (?,?,?,?)");
			for (Datacontainer dc : base.getHistQuotes()) {
				insert.setString(1, wertpapier.getID());
				insert.setBigDecimal(2, (BigDecimal) dc.data.get("last")); 
				insert.setString(3, (String) dc.data.get("currency")); 
				insert.setDate(4,  new java.sql.Date(((Date) dc.data.get("date")).getTime()));
				insert.executeUpdate();
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new ApplicationException("Fehler beim Abruf der Kurse." , e);
		}


	}
}
