package de.open4me.depot.abruf.utils;

import java.util.HashMap;
import java.util.List;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;

import de.willuhn.logging.Logger;

public class HtmlUtils {

	public static HtmlAnchor getLinksByLinkText(HtmlPage page, String search) {
		for (HtmlAnchor x : page.getAnchors()) {
			if (x.asText().contains(search)) {
				return x;
			}
		}
		return null;

	}

	public static void tabUntereinander2hash(HashMap<String, String> infos, HtmlTable tab, int idxname, int idxvalue) {
		for (HtmlTableRow row :tab.getRows()) {
			List<HtmlTableCell> cells = row.getCells();
			if (cells.size() < Math.max(idxname, idxvalue)) {
				Logger.info("Warnung. Ungültige Anzahl an Zellen: " + cells.size() + " " + row.asText());
				continue;
			}
			infos.put(cells.get(idxname).asText().toLowerCase(), cells.get(idxvalue).asText().trim());
		}
	}

	public static void tabNebeneinander2hash(HashMap<String, String> infos, HtmlTable tab) {
	List<HtmlTableRow> rows = tab.getRows();
	if (rows.size() < 2) {
		System.out.println("Warnung. Ungültige Anzahl an Zeilen: " + rows.toString());
		return;
	}
	List<HtmlTableCell> r1 = rows.get(0).getCells();
	for (int zeile = 1; zeile < rows.size(); zeile++) {
		List<HtmlTableCell> r2 = rows.get(zeile).getCells();
		if (r1.size() != r2.size()) {
			continue;
		}
		int missing=0;
		for (int i = 0; i < r1.size(); i++) {
			String header = r1.get(i).asText().toLowerCase();
			if ("".equals(header)) {
				header = "Missing" + missing;
				missing++;
			}
			infos.put(header, r2.get(i).asText().trim());
		}
	}
	}



}
