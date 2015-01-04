package de.open4me.depot.tools.io.feldConverter;

import java.math.BigDecimal;

public class BigDecimalDezimaltrennzeichenPunkt extends FeldConverter {
	
	public BigDecimalDezimaltrennzeichenPunkt() {
		super("Dezimaltrennzeichen: Punkt");
	}

	@Override
	public Object convert(String roh) {
		return new BigDecimal(roh);
	}
}
