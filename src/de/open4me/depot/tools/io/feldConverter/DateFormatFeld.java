package de.open4me.depot.tools.io.feldConverter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.open4me.depot.tools.io.feldConverter.options.FeldConverterText;

public class DateFormatFeld extends FeldConverter<Date> {

	private FeldConverterText opt = new FeldConverterText("dateformat", "Datumsformat");
	private SimpleDateFormat df;
	public DateFormatFeld() {
		super("Datumsformat (yyyy-MM-dd)");
		addOptions(opt);
		opt.setAuswahl("dd.MM.yyyy");
	}

	@Override
	public Object convert(String roh) throws Exception {
		if (df == null || !df.toPattern().equals(opt.getAuswahl())) {
			df = new SimpleDateFormat(opt.getAuswahl(), Locale.GERMAN);
			df.setLenient(false);
		}
		return df.parse(roh);
	}

}
