package de.open4me.depot.links;

public class Links {

	public static final Links einrichtung = new Links("http://github.com/littleyoda/hibiscus.depotviewer/blob/master/banken.md","Einrichtung von Konten");
	
	private String url;
	private String text;

	public Links(String url, String text) {
		this.url = url;
		this.text = text;
	}
	
	public String getHTML() {
		String out = "<a href=\"" + url + "\">" + text + "</a>";
		System.out.println(out);
		return out;
	}
}
