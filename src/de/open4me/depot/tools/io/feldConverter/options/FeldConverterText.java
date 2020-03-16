package de.open4me.depot.tools.io.feldConverter.options;

public class FeldConverterText extends FeldConverterOption<String> {

	public FeldConverterText(String id, String name) {
		super(id, name);
	}

	@Override
	public void setAuswahlByText(String out) {
		setAuswahl(out);
	}
	
}
