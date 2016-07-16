package de.open4me.depot.abruf.impl;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.ThreadedRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import de.open4me.depot.abruf.utils.HtmlUtils;
import de.willuhn.jameica.system.BackgroundTask;
import de.willuhn.logging.Logger;
import de.willuhn.util.ProgressMonitor;

public abstract class Runner implements BackgroundTask {

	public class ResultSets {

		public String command;
		public String[] parts;
		public String action;
		public Page page;
		public URL url;
		public Exception e;

		public String toString() {
			return command;
		}

	}

	private String password;
	private String username;
	private String[] codelines;
	private String logouturl;

	public Runner(String code, String username, String password, String logouturl)  {
		this(code.split("\n"), username, password, logouturl);


	}

	public Runner(String[] code, String username, String password, String logouturl)  {
		this.codelines = code;
		this.username = username;
		this.password = password;
		this.logouturl = logouturl;

	}

	@Override
	public void interrupt() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isInterrupted() {
		// TODO Auto-generated method stub
		return false;
	}

	public abstract void finish();

	static Pattern befehlMitKlammer = Pattern.compile("([a-zA-Z]*)" + Pattern.quote("(") + "(.*)" + Pattern.quote(")"));
	static Pattern textMitAnfuehrungstriche = Pattern.compile("\"(.*?)\".*");
	Pattern removeattribut = Pattern.compile("\"(.*)\" from (.*)");
	private ArrayList<ResultSets> results;
	private ArrayList<Page> downloads = new ArrayList<Page>();

	public void run(ProgressMonitor pm)  {
		results = new ArrayList<ResultSets>(); 
		final WebClient webClient = new WebClient();
		HtmlUtils.setProxyCfg(webClient, "https://www.willuhn.de");
		webClient.setCssErrorHandler(new SilentCssErrorHandler());
		webClient.setRefreshHandler(new ThreadedRefreshHandler());
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		HtmlPage page = null;
		ResultSets r = null;
		try {
			int nr = 0;
			for (String codeline : codelines) {
				if (pm != null) {
					pm.setPercentComplete(nr * (100/codelines.length) );
					pm.setStatusText(codeline);
				}
				nr++;
				if (codeline.trim().isEmpty() || codeline.startsWith("#")) {
					continue;
				}
				Logger.debug("Line " + nr + ": " + codeline);
				codeline = codeline.replace("${user}", username).replace("${pwd}", password);
				String[] parts = codeline.split(" ");
				r = new ResultSets();
				r.command = codeline;
				r.parts = parts;
				results.add(r);
				String rest = codeline.substring(parts[0].length() + 1);
				List<?> elements;
				switch (parts[0].toLowerCase()) {
				case "open":
					String openurl = extractTextAusAnf(parts[1]);
					r.action = "Open " + openurl;
					page = webClient.getPage(openurl);
					r.page = page.cloneNode(true);
					r.url = page.getUrl();
					break;

				case "set":
					Pattern setPat = Pattern.compile("(.*) to value (.*)");
					Matcher m = setPat.matcher(rest);
					if (!m.matches()) {
						throw new IllegalStateException("Befehl ist ungültig");
					}
					elements = getElements(page, m.group(1));
					if (elements.size() == 0) {
						throw new IllegalStateException("Element " + m.group(1) + " nicht gefunden");
					}
					for (Object x : elements) {
						if (!(x instanceof HtmlInput)) {
							throw new IllegalStateException("Element nicht vom Typ HtmlInput.\n" + x.getClass() + "\n" + x.toString() +"\n" + elements);
						}
						((HtmlInput) x).setValueAttribute(extractTextAusAnf(m.group(2)));
					}
					r.action = "Set " + elements +" to '" +  m.group(2) + "'";
					r.page = page.cloneNode(true);
					r.url = page.getUrl();
					break;
				case "click":
					elements = getElements(page, rest);
					if (elements.size() == 0) {
						throw new IllegalStateException("Line " + nr + ":" + "Kein Element gefunden!" + codeline);
					}
					Object x = elements.get(0);
					if (!(x instanceof HtmlElement)) {
						throw new IllegalStateException("Line " + nr + ":" + "Element nicht vom Typ HtmlInput");
					}
					HtmlElement e = ((HtmlElement) x);
					page = e.click();
					r.action = "Click on first of the following list " + elements;
					r.page = page.cloneNode(true);
					r.url = page.getUrl();
					break;
				case "download":
					elements = getElements(page, rest);
					if (elements.size() == 0) {
						throw new IllegalStateException("Line " + nr + ":" + "Kein Element gefunden!" + codeline);
					}
					Object dl = elements.get(0);
					if (!(dl instanceof HtmlElement)) {
						throw new IllegalStateException("Line " + nr + ":" + "Element nicht vom Typ HtmlInput");
					}
					Page p = ((HtmlElement) dl).click();
					r.action = "Click on first of the following list " + elements;
					r.page = p;
					r.url = p.getUrl();
					downloads.add(p);
					break;
				case "removeattribute":
					m = removeattribut.matcher(rest);
					if (!m.matches()) {
						throw new IllegalStateException("Line " + nr + ":" + "Befehl ist ungültig");
					}
					String attrName = m.group(1);
					String get = m.group(2);
					elements = getElements(page, get);
					for (Object o : elements) {
						if (!(o instanceof HtmlElement)) {
							throw new IllegalStateException("Element nicht vom Typ HtmlElement.\n");
						}
						((HtmlInput) o).removeAttribute(attrName);
					}
					break;
				case "assertexists":
					String fehlermeldung = extractTextAusAnf(rest);
					rest = rest.substring(fehlermeldung.length() + 2 + 1); // +2 wegen den fehlenden Anführungsstrichen in fehlermeldung
					if (rest.trim().isEmpty()) {
						throw new IllegalStateException("Line " + nr + ":" + "Zweiter Teil des Befehles nicht gefunden! " + codeline);
					}
					elements = getElements(page, rest);
					Logger.error("assertExists: " + fehlermeldung + " " + rest + " " + elements.toString());
					if (elements.size() == 0) {
						throw new IllegalStateException(fehlermeldung);
					}
					results.remove(r);
					break;
				default:
					throw new IllegalStateException("Line " + nr + ":" + "Unbekannter Befehl: " + codeline);
				}
			}
			if (pm != null) {
				pm.setStatus(ProgressMonitor.STATUS_DONE);
				pm.setStatusText("Finished");
				pm.setPercentComplete(100);
			}
		} catch (Exception e) {
			try {
				webClient.getPage(logouturl);
			} catch (FailingHttpStatusCodeException | IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
			Logger.error("", e);
			r.e = e;
			pm.setStatus(ProgressMonitor.STATUS_ERROR);
		}
		finish();

	}

	public ArrayList<ResultSets> getResults() {
		return results;
	}
	private static String extractTextAusAnf(String s) {
		Matcher m = textMitAnfuehrungstriche.matcher(s);
		if (!m.matches()) {
			throw new IllegalStateException("String nicht im Format '\"text\"' Text:" + s);
		}
		return m.group(1);

	}
	public static List<?> getElements(HtmlPage page, String string) {
		Matcher m = befehlMitKlammer.matcher(string);
		if (!m.matches()) {
			throw new IllegalStateException("Befehl ist ungültig. Klammerstrukt passt nicht. >" + string + "<");
		}
		switch (m.group(1).toLowerCase()) {
		case "getbyname":
			return page.getElementsByName(extractTextAusAnf(m.group(2)));
		case "getbyid":
			Object o = page.getElementById(extractTextAusAnf(m.group(2)));
			if (o == null) {
				return new ArrayList<Object>();
			}
			return Arrays.asList(new Object[] { o });
		case "getbyxpath":
			return page.getByXPath(extractTextAusAnf(m.group(2)));
		case "getbytext":
			String text = extractTextAusAnf(m.group(2));
			List<HtmlElement> elements = getAllHtmlElements(page);
			for (Iterator i = elements.listIterator(); i.hasNext();) {
				HtmlElement element = (HtmlElement) i.next();
				if (element.getChildElementCount() > 0 || !element.getTextContent().equals(text)) {
					i.remove();						
				}
			}
			return elements; 
		default:
			throw new IllegalStateException("Befehl " + m.group(1) + " ist unbekannt!");
		}
	}

	private static List<HtmlElement> getAllHtmlElements(HtmlPage page) {
		List<HtmlElement> out = new ArrayList<HtmlElement>();
		for (Object o : page.getByXPath("//*")) {
			if (!(o instanceof HtmlElement)) {
				continue;
			}
			out.add((HtmlElement) o);
		}
		return out;

	}

	public ArrayList<Page> getDownloads() {
		return downloads;
	}

	public void setDownloads(ArrayList<Page> downloads) {
		this.downloads = downloads;
	}

}
