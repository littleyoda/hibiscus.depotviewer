package de.open4me.depot.reports;

import java.lang.reflect.Method;

import de.willuhn.jameica.gui.extension.Extendable;
import de.willuhn.jameica.gui.extension.Extension;

public class DepotviewerReportTemplateExtension implements Extension
{
    private static final String EXTENDABLE_ID = "hibiscus.ly.reports.template.context";

    @Override
    public void extend(Extendable extendable)
    {
        if (extendable == null || !EXTENDABLE_ID.equals(extendable.getExtendableID()))
            return;

        try
        {
            Method putIfAbsent = extendable.getClass().getMethod("putIfAbsent", String.class, Object.class);
            putIfAbsent.invoke(extendable, "depotviewer", new DepotviewerReportObjects());
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("Depotviewer-Report-Objekte konnten nicht registriert werden", e);
        }
    }
}
