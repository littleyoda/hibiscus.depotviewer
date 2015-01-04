package de.open4me.depot.tools.io;

/**
 * Definitiert ein notwendiges Feld beim Import von Daten
 * @author sven
 *
 */
public class FeldDefinitionen implements Comparable {
	
	private String beschreibung;
	private	Class<?> feldtype;
	private String attr;
	private boolean required;
	
	public FeldDefinitionen(String b, Class<?> c, String attr, Boolean required) {
		beschreibung = b;
		feldtype = c;
		this.attr = attr;
		this.required = required;
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
	
	
	
}
