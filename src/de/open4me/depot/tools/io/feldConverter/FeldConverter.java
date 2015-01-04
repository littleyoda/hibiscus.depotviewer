package de.open4me.depot.tools.io.feldConverter;

public abstract class FeldConverter {

	private String beschreibung;

	public FeldConverter(String beschreibung) {
		this.beschreibung = beschreibung;
	}
	
	public abstract Object convert(String roh);
	
	@Override
	public String toString() {
		return beschreibung;
	}
}
