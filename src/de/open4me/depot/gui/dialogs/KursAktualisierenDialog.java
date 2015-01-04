package de.open4me.depot.gui.dialogs;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import jsq.config.Config;
import jsq.config.ConfigTuple;

import org.eclipse.swt.widgets.Composite;

import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.dialogs.AbstractDialog;
import de.willuhn.jameica.gui.input.SelectInput;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.gui.util.Container;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.jameica.system.OperationCanceledException;
import de.willuhn.util.ApplicationException;

public class KursAktualisierenDialog extends AbstractDialog
{	  
	private List<Config> list;
	private HashMap<Config, SelectInput> inputs = new HashMap<Config, SelectInput>();


	public KursAktualisierenDialog(int position, List<Config> list)
	{
		super(position);
		setTitle("Einstellungen");
		this.list = list;
	}

	/**
	 * @see de.willuhn.jameica.gui.dialogs.AbstractDialog#paint(org.eclipse.swt.widgets.Composite)
	 */
	protected void paint(Composite parent) throws Exception
	{
		Container group = new SimpleContainer(parent);
		group.addText("Notwendige Einstellungen:", false);
		for (Config cfg : list) {
			group.addInput(getSelectInput(cfg));
		}


		ButtonArea buttons = new ButtonArea();
		buttons.addButton(i18n.tr("Weiter"), new Action() {
			public void handleAction(Object context) throws ApplicationException
			{
				set();
				close();
			}

		},null,true,"ok.png");
		buttons.addButton(i18n.tr("Abbrechen"), new Action() {
			public void handleAction(Object context) throws ApplicationException
			{
				throw new OperationCanceledException("Abgebrochen");
			}

		},null,false,"process-stop.png");
		group.addButtonArea(buttons);
	}

	private SelectInput getSelectInput(Config cfg)
	{
		SelectInput encoding = new SelectInput(cfg.getOptions(), null);
		encoding.setName(cfg.getBeschreibung());
		encoding.setMandatory(true);
		inputs.put(cfg, encoding);
		return encoding;
	}

	@Override
	protected Object getData() throws Exception {
		return list;
	}

	private void set() {
			for (Entry<Config, SelectInput> i : inputs.entrySet()) {
				Config c = i.getKey();
				c.addSelectedOptions((ConfigTuple) i.getValue().getValue());
			}
	}

}
