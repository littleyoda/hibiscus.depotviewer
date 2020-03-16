package de.open4me.depot.tools.io.feldConverter;

import java.math.BigDecimal;
import java.util.Arrays;

import de.open4me.depot.tools.io.feldConverter.options.FeldConverterAuswahl;

public class BigDecimalFeld extends FeldConverter<BigDecimal> {
	
	private String[] opts = new String[] {"Komma", "Punkt"};
	private FeldConverterAuswahl<String> opt = new FeldConverterAuswahl<String>("separator", "Dezimaltrennzeichen", Arrays.asList(opts));
	
	public BigDecimalFeld() {
		super("Zahl");
		addOptions(opt);
		opt.setAuswahl(opts[0]);
	}

	@Override
	public Object convert(String roh) {
		if (opts[0].equals(opt.getAuswahl())) {
			return new BigDecimal(roh.replace(".", "").replace(",","."));
		}
		return new BigDecimal(roh);
	}
}
