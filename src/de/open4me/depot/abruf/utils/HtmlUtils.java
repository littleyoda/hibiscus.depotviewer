package de.open4me.depot.abruf.utils;

import java.net.InetSocketAddress;

import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;

import org.htmlunit.ProxyConfig;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlTable;
import org.htmlunit.html.HtmlTableCell;
import org.htmlunit.html.HtmlTableRow;

import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;

public class HtmlUtils {


	public static void setProxyCfg(WebClient webClient, String url)  {
		boolean useSystem = Application.getConfig().getUseSystemProxy();

		ProxyConfig pc = null;
		if (useSystem) {
			try {
				List<Proxy> proxies = ProxySelector.getDefault().select(new URI(url));
				Logger.info("Using system proxy settings: " + proxies);
				for (Proxy p : proxies) {
					if (p.type() == Proxy.Type.HTTP && p.address() instanceof InetSocketAddress) {
						pc = new ProxyConfig();
						InetSocketAddress addr = (InetSocketAddress) p.address();
						pc.setProxyHost(addr.getHostString());
						pc.setProxyPort(addr.getPort());
						webClient.getOptions().setProxyConfig(pc);
						Logger.info("Setting Proxy to " + pc);
						return;
					}
				}
				Logger.error("No default Proxy found");
			} catch (URISyntaxException e) {
				Logger.error("No default Proxy found", e);
			}
		} else {
			String host = Application.getConfig().getHttpsProxyHost();
			int port = Application.getConfig().getHttpsProxyPort();
			if (host != null && host.length() > 0 && port > 0) {
				pc = new ProxyConfig();
				pc.setProxyHost(host);
				pc.setProxyPort(port);
				webClient.getOptions().setProxyConfig(pc);
				Logger.info("Setting Proxy to " + pc);
				return;
			}
		}
		Logger.info("Keine gültige Proxy-Einstellunge gefunden. (" + useSystem + ")");
	}

	public static HtmlAnchor getLinksByLinkText(HtmlPage page, String search) {
		for (HtmlAnchor x : page.getAnchors()) {
			if (x.asNormalizedText().contains(search)) {
				return x;
			}
		}
		return null;

	}

	public static void tabUntereinander2hash(HashMap<String, String> infos, HtmlTable tab, int idxname, int idxvalue) {
		for (HtmlTableRow row :tab.getRows()) {
			List<HtmlTableCell> cells = row.getCells();
			if (cells.size() < Math.max(idxname, idxvalue)) {
				Logger.info("Warnung. Ungültige Anzahl an Zellen: " + cells.size() + " " + row.asNormalizedText());
				continue;
			}
			infos.put(cells.get(idxname).asNormalizedText().toLowerCase(), cells.get(idxvalue).asNormalizedText().trim());
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
				String header = r1.get(i).asNormalizedText().toLowerCase();
				if ("".equals(header)) {
					header = "Missing" + missing;
					missing++;
				}
				infos.put(header, r2.get(i).asNormalizedText().trim());
			}
		}
	}



}
