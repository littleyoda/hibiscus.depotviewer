package de.open4me.depot.gui.view;

import de.open4me.depot.gui.control.BestandsAbgleichControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.Part;
import de.willuhn.jameica.hbci.HBCI;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.I18N;

public class BestandsAbgleichView extends AbstractView
{
	  private final static I18N i18n = Application.getPluginLoader().getPlugin(HBCI.class).getResources().getI18N();

	  /**
	   * @see de.willuhn.jameica.gui.AbstractView#bind()
	   */
	  public void bind() throws Exception
	  {
			GUI.getView().setTitle(i18n.tr("Abgleich Bestand und Orderbuch"));
	    
			BestandsAbgleichControl control = new BestandsAbgleichControl(this);

			Part libs = control.getLibList();
			libs.paint(getParent());
	  }
	}
