package de.open4me.depot.gui.dialogs;

import org.eclipse.swt.widgets.Composite;

/** Anbieterspezifischer Bereich im Dialog zur Auswahl der Kursquelle. */
public interface KursanbieterDialogErweiterung
{
	KursanbieterDialogErweiterung KEINE = new KursanbieterDialogErweiterung()
	{
		@Override public void paint(Composite parent) {}
		@Override public boolean isVisible() { return false; }
	};

	void paint(Composite parent) throws Exception;

	default boolean isVisible() { return true; }
}
