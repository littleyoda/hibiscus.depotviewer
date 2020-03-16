package de.open4me.depot.tools.io.feldConverter.options;

public abstract class FeldConverterOption<T> {

	private String name;
	private T auswahl;
	private String id;
	
	public String getName() {
		return name;
	}
	
	public  FeldConverterOption(String id, String name) {
		this.name = name;
		this.auswahl = null;
		this.id = id;
	}
	
	public void setAuswahl(T value) {
		auswahl = value;
	}

	abstract public void setAuswahlByText(String out);
	
	public T getAuswahl() {
		return auswahl;
	}

	public String getId() {
		return id;
	}
	
}
