package de.open4me.depot.kursprovider;

import java.util.function.BooleanSupplier;

import de.open4me.depot.sql.GenericObjectSQL;
import de.willuhn.util.ProgressMonitor;

/** Eingaben und UI-Kontext eines einzelnen Kursabrufs. */
public final class KursProviderKontext
{
	private final GenericObjectSQL wertpapier;
	private final String suchbegriff;
	private final boolean manuell;
	private final boolean vorbereiten;
	private final ProgressMonitor monitor;
	private final float fortschrittsschritt;
	private final BooleanSupplier abgebrochen;

	public KursProviderKontext(GenericObjectSQL wertpapier, String suchbegriff, boolean manuell,
			boolean vorbereiten, ProgressMonitor monitor, float fortschrittsschritt,
			BooleanSupplier abgebrochen)
	{
		this.wertpapier = wertpapier;
		this.suchbegriff = suchbegriff;
		this.manuell = manuell;
		this.vorbereiten = vorbereiten;
		this.monitor = monitor;
		this.fortschrittsschritt = fortschrittsschritt;
		this.abgebrochen = abgebrochen;
	}

	public GenericObjectSQL getWertpapier() { return wertpapier; }
	public String getSuchbegriff() { return suchbegriff; }
	public boolean isManuell() { return manuell; }
	public boolean isVorbereiten() { return vorbereiten; }
	public ProgressMonitor getMonitor() { return monitor; }
	public float getFortschrittsschritt() { return fortschrittsschritt; }
	public boolean isAbgebrochen() { return abgebrochen.getAsBoolean(); }
}
