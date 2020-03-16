package de.open4me.depot.tools.io;

import java.math.BigDecimal;

import de.open4me.depot.datenobj.DepotAktion;
import de.open4me.depot.datenobj.rmi.BigDecimalWithCurrency;
import de.open4me.depot.tools.io.feldConverter.BigDecimalFeld;
import de.open4me.depot.tools.io.feldConverter.BigDecimalWithCurrencyFeld;
import de.open4me.depot.tools.io.feldConverter.DateFormatFeld;
import de.open4me.depot.tools.io.feldConverter.DepotAktionFeld;
import de.open4me.depot.tools.io.feldConverter.FeldConverter;
import de.open4me.depot.tools.io.feldConverter.StringFeld;
import de.open4me.depot.tools.io.feldConverter.WaehrungFeld;

import java.util.Currency;
import java.util.Date;

/**
 * Definitiert ein notwendiges Feld beim Import von Daten
 * @author sven
 *
 */
public class FeldDefinitionen implements Comparable<Object> {
	
	private String beschreibung;
	private	Class<?> feldtype;
	private String attr;
	private boolean required;
	private FeldConverter<?> converter;
	private String setSpalte;
	
	public FeldDefinitionen(String b, Class<?> c, String attr, Boolean required) {
		this.beschreibung = b;
		this.feldtype = c;
		this.attr = attr;
		this.required = required;
		setConverter();
	}
	
	@Override
	public String toString() {
		return beschreibung + " " + feldtype.getName() + " " + required; 
	}

	public String getBeschreibung() {
		return beschreibung;
	}

	public Class<?> getFeldtype() {
		return feldtype;
	}

	public String getAttr() {
		return attr;
	}

	public boolean isRequired() {
		return required;
	}

	@Override
	public int compareTo(Object o) {
		return beschreibung.compareTo(((FeldDefinitionen) o).getBeschreibung());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attr == null) ? 0 : attr.hashCode());
		result = prime * result
				+ ((beschreibung == null) ? 0 : beschreibung.hashCode());
		result = prime * result
				+ ((feldtype == null) ? 0 : feldtype.hashCode());
		result = prime * result + (required ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FeldDefinitionen other = (FeldDefinitionen) obj;
		if (attr == null) {
			if (other.attr != null)
				return false;
		} else if (!attr.equals(other.attr))
			return false;
		if (beschreibung == null) {
			if (other.beschreibung != null)
				return false;
		} else if (!beschreibung.equals(other.beschreibung))
			return false;
		if (feldtype == null) {
			if (other.feldtype != null)
				return false;
		} else if (!feldtype.equals(other.feldtype))
			return false;
		if (required != other.required)
			return false;
		return true;
	}
	
	public FeldConverter<?> getConverters() {
		return converter;
	}
	
	private void setConverter() {
		Class<?> feldtype = getFeldtype();
		if (feldtype.equals(BigDecimal.class)) {
				converter = new BigDecimalFeld();
		} else if (feldtype.equals(Date.class)) {
				converter = new DateFormatFeld();
		} else if (feldtype.equals(String.class)) {
			converter = new StringFeld();
		} else if (feldtype.equals(DepotAktion.class)) {
			converter = new DepotAktionFeld();
		} else if (feldtype.equals(Currency.class)) {
			converter = new WaehrungFeld();
		} else if (feldtype.equals(BigDecimalWithCurrency.class)) {
			converter = new BigDecimalWithCurrencyFeld();
		} else {
			throw new RuntimeException("Converter nicht gefunden");
		}
	}

	public void setSpalte(String value) {
		this.setSpalte = value;
	}

	public String getSpalte() {
		return setSpalte;
	}

	
}
