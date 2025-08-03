package de.open4me.depot.gui.action;

import de.open4me.depot.gui.view.UmsatzEditorView;
import de.open4me.depot.tools.InconsistencyData;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.util.ApplicationException;

/**
 * Action zum Öffnen des UmsatzEditors mit vorausgefüllten Daten aus einer Inkonsistenz
 */
public class InconsistencyFixAction implements Action {

    @Override
    public void handleAction(Object context) throws ApplicationException {
        if (!(context instanceof InconsistencyData)) {
            throw new ApplicationException("Ungültiger Kontext für InconsistencyFixAction");
        }
        
        InconsistencyData inconsistency = (InconsistencyData) context;
        
        // Öffne UmsatzEditorView mit der Inkonsistenz als Kontext
        GUI.startView(UmsatzEditorView.class, inconsistency);
    }
}