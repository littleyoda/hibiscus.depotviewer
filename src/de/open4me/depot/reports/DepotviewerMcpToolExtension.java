package de.open4me.depot.reports;

import java.lang.reflect.Method;

import de.willuhn.jameica.gui.extension.Extendable;
import de.willuhn.jameica.gui.extension.Extension;

public class DepotviewerMcpToolExtension implements Extension
{
    private static final String EXTENDABLE_ID = "hibiscus.ly.reports.mcp.tools";

    @Override
    public void extend(Extendable extendable)
    {
        if (extendable == null || !EXTENDABLE_ID.equals(extendable.getExtendableID()))
            return;

        try
        {
            Method register = extendable.getClass().getMethod("register", Object.class);
            register.invoke(extendable, new DepotviewerMcpToolProvider());
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("Depotviewer-MCP-Tools konnten nicht registriert werden", e);
        }
    }
}
