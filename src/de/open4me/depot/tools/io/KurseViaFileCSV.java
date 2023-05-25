package de.open4me.depot.tools.io;

import java.util.ArrayList;
import java.util.List;

import de.open4me.depot.datenobj.rmi.BigDecimalWithCurrency;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.open4me.depot.tools.CSVImportHelper;
import de.willuhn.jameica.gui.GUI;
import jsq.config.Config;
import jsq.datastructes.Datacontainer;
import jsq.fetcher.history.BaseFetcher;

public class KurseViaFileCSV extends BaseFetcher {

	
	private int modus;

	public KurseViaFileCSV(int modus) {
		this.modus = modus;
	}
	
	@Override
	public String getName() {
		if (modus == 0) {
			return "CSV Import (via Datei)";
		} else {
			return "CSV Import (via Url)";
		}
	}

	@Override
	public String getURL() {
		return "";
	}

	@Override
	public void prepare(String search, int beginYear, int beginMon,
			int beginDay, int stopYear, int stopMon, int stopDay)
					throws Exception {
		super.prepare(search, beginYear, beginMon, beginDay, stopYear, stopMon, stopDay);
			
			// FeldDefinitionen anwenden 
			final ArrayList<FeldDefinitionen> fd = new ArrayList<FeldDefinitionen>();
			fd.add(new FeldDefinitionen("Datum", java.util.Date.class, "date", true));
			fd.add(new FeldDefinitionen("Kurs", BigDecimalWithCurrency.class, "tmp", true));

			final List<GenericObjectHashMap> daten = new ArrayList<GenericObjectHashMap>();
			final CSVImportHelper csv = new CSVImportHelper("kurse." + search, modus);
			GUI.getDisplay().syncExec(new Runnable() {
				public void run()
				{
					try {
						List<GenericObjectHashMap> l = csv.run(fd, false);
						if (l != null) {
							daten.addAll(l);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			if (daten.size() == 0) {
				return;
			}
			
			// Und die letzte Umwandlung
			List<Datacontainer> dc = new ArrayList<Datacontainer>();
			for (GenericObjectHashMap x : daten) {
				Datacontainer obj = new Datacontainer(x.getMap());
				BigDecimalWithCurrency tmp = (BigDecimalWithCurrency) obj.getMap().get("tmp");
				obj.getMap().put("last", tmp.getZahl());
				obj.getMap().put("currency", tmp.getWaehrung().toString());
				dc.add(obj);
			}
			setHistQuotes(dc);
			setHistEvents(new ArrayList<Datacontainer>());


	}

	@Override
	public void process(List<Config> options) {
	}





}
