package de.open4me.depot.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.ThreadedRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;

import de.open4me.depot.DepotViewerPlugin;
import de.open4me.depot.abruf.utils.HtmlUtils;
import de.open4me.depot.abruf.utils.Utils;
import jsq.tools.HtmlUnitTools;

public class WertpapierSuche {
	/**
	 * bestandsList.addColumn(Settings.i18n().tr("Name"), "Name");
	* bestandsList.addColumn(Settings.i18n().tr("Typ"),"Typ");
	* bestandsList.addColumn(Settings.i18n().tr("ISIN"),"Isin");
	 * @throws IOException 
	 * @throws MalformedURLException 
	*/
	
	public static List<HashMap<String, String>>  search(String search) {
		List<HashMap<String, String>> x = new ArrayList<HashMap<String, String>>();
		try {
			x.addAll(getXetaListeResults(search));
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			x.addAll(getYahooSearchResults(search));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return x;
		
	}
	
	private static List<HashMap<String, String>> getYahooSearchResults(String search)
			throws IOException {
		List<HashMap<String, String>> x = new ArrayList<HashMap<String, String>>();
		WebClient webClient = new WebClient();
		HtmlUtils.setProxyCfg(webClient, "https://de.finance.yahoo.com/");
		webClient.getOptions().setTimeout(3000);
		webClient.setCssErrorHandler(new SilentCssErrorHandler());
		webClient.setRefreshHandler(new ThreadedRefreshHandler());
		webClient.getOptions().setJavaScriptEnabled(false);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(java.util.logging.Level.OFF);
		HtmlPage page = webClient.getPage("https://de.finance.yahoo.com/lookup/all?s=" + search +  "&t=A&m=ALL&r=");
		HtmlTable tab = (HtmlTable) HtmlUnitTools.getElementByPartContent(page, "Ticker", "table");
		if (tab == null) {
			webClient.close();
			return x;
		}
		x = HtmlUnitTools.analyse(tab);
		List<HashMap<String, String>> out = new ArrayList<HashMap<String, String>>();
		for (HashMap<String, String> e : x) {
			e.put("Source", "Yahoo Finance");
			e.remove("Letzter Kurs");
			e.remove("Ticker");
			e.remove("Börsenplatz");
			System.out.println(e.entrySet());
			if (!out.contains(e)) {
				out.add(e);
			}
		}
		webClient.close();
		return out;
	}

	public static boolean isXetaAvail() {
		File file = new File(Utils.getWorkingDir(DepotViewerPlugin.class) + File.separatorChar + "lists" + File.separatorChar + "xetra.csv");
		return file.exists();
	}
	
	private static List<HashMap<String, String>> getXetaListeResults(String search)
			throws IOException{
		search = search.toLowerCase();
		List<HashMap<String, String>> x = new ArrayList<HashMap<String, String>>();
		File file = new File(Utils.getWorkingDir(DepotViewerPlugin.class) + File.separatorChar + "lists" + File.separatorChar + "xetra.csv");
		if (file.exists()) {
			CSVFormat format = CSVFormat.RFC4180.withHeader().withDelimiter(';').withIgnoreEmptyLines(true);
			CSVParser parser;
			
			// erstmal ein Probedurchgang ohne Import
			InputStreamReader stream = new InputStreamReader(new FileInputStream(file), "UTF-8");
			parser = new CSVParser(stream, format);
			for(CSVRecord record : parser){
				if (!(record.get("WKN").toLowerCase().contains(search) 
						|| record.get("ISIN").toLowerCase().contains(search) 
						|| record.get("Instrument").toLowerCase().contains(search))) {
					continue;
				}
				HashMap<String, String> h = new HashMap<String, String>();
				h.put("Name", record.get("Instrument"));
				h.put("Isin", record.get("ISIN"));
				h.put("Typ", "");
				String wkn = record.get("WKN");
				while (wkn.length() > 0 && wkn.charAt(0) == '0') {
					wkn = wkn.substring(1);
				}
				h.put("Wkn", wkn);
				h.put("Source", "Xetra");
				x.add(h);
			}
			parser.close();
			
		}
		
		return x;
	}

}
