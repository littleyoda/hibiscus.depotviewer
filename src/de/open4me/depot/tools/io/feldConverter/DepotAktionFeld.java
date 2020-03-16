package de.open4me.depot.tools.io.feldConverter;

import de.open4me.depot.datenobj.DepotAktion;

public class DepotAktionFeld extends FeldConverter<DepotAktion> {

	public DepotAktionFeld() {
		super("Aktion (Kauf/Verkauf/...)");
	}

	@Override
	public Object convert(String roh) throws Exception {
		DepotAktion da = DepotAktion.getByString(roh);
		if (da == null) {
			throw new IllegalArgumentException();
		}
		return da;
	}

}
