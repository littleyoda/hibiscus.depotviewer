package de.open4me.depot.kursprovider;

import de.open4me.depot.gui.dialogs.KursanbieterDialogErweiterung;
import de.willuhn.jameica.gui.Action;

/** Vertrag für eine direkt oder über eine Bibliothek angebundene Kursquelle. */
public interface KursProvider extends AutoCloseable
{
	String getId();
	String getName();
	String getWebsite();

	KursProviderErgebnis abrufen(KursProviderKontext context) throws Exception;

	default KursanbieterDialogErweiterung createDialogErweiterung(Action vorbereiten)
	{
		return KursanbieterDialogErweiterung.KEINE;
	}

	default KursanbieterDialogErweiterung createDialogErweiterung(Action vorbereiten, Runnable statusGeaendert)
	{
		return createDialogErweiterung(vorbereiten);
	}

	/** Gibt an, ob der Provider ohne weitere Verbindungsherstellung verwendet werden kann. */
	default boolean istAbrufbereit()
	{
		return true;
	}

	default boolean ersetztBestandBeimWechsel()
	{
		return false;
	}

	@Override
	default void close() {}
}
