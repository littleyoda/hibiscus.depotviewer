package de.open4me.depot.tools.io.feldConverter.options;

import java.util.List;

public class FeldConverterAuswahl<T> extends FeldConverterOption<T> {

	private List<T> liste;

	public FeldConverterAuswahl(String id, String name, List<T> liste) {
		super(id, name);
		this.liste = liste;
		setAuswahl(liste.get(0));
	}

	public List<T> getListe() {
		return liste;
	}

	@Override
	public void setAuswahlByText(String out) {
		if (out == null) {
			return;
		}
		for (T x : getListe()) {
			if (x.toString().equals(out)) {
				setAuswahl(x);
				return;
			}
		}
	}

	
	
}
