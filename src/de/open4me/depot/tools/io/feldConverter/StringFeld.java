package de.open4me.depot.tools.io.feldConverter;

public class StringFeld extends FeldConverter<String> {
	
	public StringFeld() {
		super("Text");
	}

	@Override
	public Object convert(String roh) {
		return roh;
	}
}
