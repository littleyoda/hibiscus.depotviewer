package de.open4me.depot.gui.action;

import de.open4me.depot.gui.dialogs.AboutDepotviewerDialog;
import de.willuhn.jameica.gui.Action;
import de.willuhn.util.ApplicationException;

public final class AboutDepotviewerAction implements Action
{
    @Override
    public void handleAction(Object context) throws ApplicationException
    {
        try
        {
            new AboutDepotviewerDialog().open();
        }
        catch (Exception e)
        {
            throw new ApplicationException("Der Dialog konnte nicht geoeffnet werden: " + e.getMessage(), e);
        }
    }
}
