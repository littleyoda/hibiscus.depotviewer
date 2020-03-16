package de.open4me.depot.tools.io.feldConverter;

import java.util.Currency;

public class WaehrungFeld extends FeldConverter<Currency> {
	
	public WaehrungFeld() {
		super("Währung");
	}

	@Override
	public Object convert(String roh) {
		return Currency.getInstance(roh);
	}
}
