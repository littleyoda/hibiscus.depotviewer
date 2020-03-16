package de.open4me.depot.tools.io.feldConverter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Currency;

import org.apache.commons.lang3.StringUtils;

import de.open4me.depot.datenobj.rmi.BigDecimalWithCurrency;
import de.open4me.depot.tools.io.feldConverter.options.FeldConverterAuswahl;

public class BigDecimalWithCurrencyFeld extends FeldConverter<BigDecimalWithCurrency> {

	private final String[] opts = new String[] {"Komma", "Punkt"};
	private String[] optsWaehrung = new String[] {"Zahl mit Währungszeichen (EUR, $, €)", "Betrag in EUR", "Betrag in USD", };
	private FeldConverterAuswahl<String> opt = new FeldConverterAuswahl<String>("separator", "Dezimaltrennzeichen", Arrays.asList(opts));
	private FeldConverterAuswahl<String> optWaehrung = new FeldConverterAuswahl<String>("format", "Zahlenformat", Arrays.asList(optsWaehrung));

	public BigDecimalWithCurrencyFeld() {
		super("Betrag");
		addOptions(opt);
		opt.setAuswahl(opt.getListe().get(0));
		addOptions(optWaehrung);
		optWaehrung.setAuswahl(optWaehrung.getListe().get(0));
	}

	@Override
	public Object convert(String roh) {
		roh = roh.trim();
		String auswahl = optWaehrung.getAuswahl();
		int spaceCount = StringUtils.countMatches(roh, " ");
		if (optsWaehrung[0].equals(auswahl)) {
			// Zahl mit Währung
			if (spaceCount != 1) {
				throw new RuntimeException("");
			}
			String[] splitted = roh.split(" ");
			return new BigDecimalWithCurrency(getZahl(splitted[0]), getWaehrung(splitted[1]));
		} else if (optsWaehrung[1].equals(auswahl) || optsWaehrung[2].equals(auswahl)) {
			if (spaceCount != 0) {
				throw new RuntimeException("");
			}
			String w = "";
			if (optsWaehrung[1].equals(auswahl)) {
				w = "EUR";

			} else {
				w = "USD";
			}
			return new BigDecimalWithCurrency(getZahl(roh), getWaehrung(w));
		} else {
			throw new RuntimeException("");
		}
	}

	private BigDecimal getZahl(String roh) {
		if (opts[0].equals(opt.getAuswahl())) {
			return new BigDecimal(roh.replace(".", "").replace(",","."));
		}
		return new BigDecimal(roh);
	}
	
	private Currency getWaehrung(String roh) {
		return Currency.getInstance(roh);
	}
}
