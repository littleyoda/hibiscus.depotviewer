package de.open4me.depot.tools.io;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jsq.config.Config;
import jsq.datastructes.Datacontainer;
import jsq.fetcher.history.BaseFetcher;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.open4me.depot.tools.CSVImportHelper;
import de.willuhn.jameica.gui.GUI;

public class KurseViaCSV extends BaseFetcher {

	@Override
	public String getName() {
		return "CSV Import";
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
			fd.add(new FeldDefinitionen("Kurs", BigDecimal.class, "last", true));

			final List<GenericObjectHashMap> daten = new ArrayList<GenericObjectHashMap>();
			final CSVImportHelper csv = new CSVImportHelper("kurse." + search);
			GUI.getDisplay().syncExec(new Runnable() {
				public void run()
				{
					try {
						daten.addAll(csv.run(fd));
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
				Datacontainer obj = new Datacontainer((Map<String, Object>) x.getMap());
				obj.put("currency", "EUR");
				dc.add(obj);
			}
			setHistQuotes(dc);
			setHistEvents(new ArrayList<Datacontainer>());


	}

	@Override
	public void process(List<Config> options) {
	}





}
