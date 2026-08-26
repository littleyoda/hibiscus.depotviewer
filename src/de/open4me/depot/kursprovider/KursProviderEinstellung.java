package de.open4me.depot.kursprovider;

/** Persistierbare, providerunabhängige Schlüssel/Wert-Einstellung. */
public final class KursProviderEinstellung
{
	private final String schluessel;
	private final String wert;

	public KursProviderEinstellung(String schluessel, String wert)
	{
		this.schluessel = schluessel;
		this.wert = wert;
	}

	public String getSchluessel() { return schluessel; }
	public String getWert() { return wert; }
}
