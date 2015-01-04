package de.open4me.depot.tools.io.feldConverter;

import java.math.BigDecimal;

public class BigDecimalDezimaltrennzeichenKomma extends FeldConverter {
	
	public BigDecimalDezimaltrennzeichenKomma() {
		super("Dezimaltrennzeichen: Komma");
	}

	@Override
	public Object convert(String roh) {
		return new BigDecimal(roh.replace(".", "").replace(",","."));
	}
}
