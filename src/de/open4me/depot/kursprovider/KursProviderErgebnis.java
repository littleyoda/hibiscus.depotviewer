package de.open4me.depot.kursprovider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Kursdaten und optional zu persistierende anbieterspezifische Einstellungen. */
public final class KursProviderErgebnis
{
	private final KursAbrufResult kurse;
	private final List<KursProviderEinstellung> konfiguration;

	public KursProviderErgebnis(KursAbrufResult kurse, List<KursProviderEinstellung> konfiguration)
	{
		this.kurse = kurse;
		this.konfiguration = Collections.unmodifiableList(new ArrayList<KursProviderEinstellung>(konfiguration));
	}

	public KursAbrufResult getKurse() { return kurse; }
	public List<KursProviderEinstellung> getKonfiguration() { return konfiguration; }
}
