package de.open4me.depot.gui.dialogs;


import java.util.List;

import org.eclipse.swt.widgets.Composite;

import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.dialogs.AbstractDialog;
import de.willuhn.jameica.gui.input.TextAreaInput;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.gui.parts.FormTextPart;
import de.willuhn.jameica.gui.util.Container;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.util.ApplicationException;

public class DebugDialogWithTextarea extends AbstractDialog
{

	private List<String> fehlerhafteOrder;

	/**
	 * ct
	 * @param position
	 * @param fehlerhafteOrder
	 */
	public DebugDialogWithTextarea(int position, List<String> fehlerhafteOrder)
	{
		super(position);
		this.setTitle(i18n.tr("Order"));
		setSize(600, 600);
		this.fehlerhafteOrder = fehlerhafteOrder;
	}

	/**
	 * @see de.willuhn.jameica.gui.dialogs.AbstractDialog#getData()
	 */
	public Object getData() throws Exception
	{
		return null;
	}

	/**
	 * @see de.willuhn.jameica.gui.dialogs.AbstractDialog#paint(org.eclipse.swt.widgets.Composite)
	 */
	protected void paint(Composite parent) throws Exception
	{

		StringBuffer sb = new StringBuffer();
		for (String x : fehlerhafteOrder) {
			sb.append(x);
			sb.append(System.lineSeparator());
			sb.append("------------------------------------------------------------------------------------------------" + System.lineSeparator());
			sb.append(System.lineSeparator());
		}

		Container container = new SimpleContainer(parent);
		 FormTextPart text = new FormTextPart();
		    text.setText("<form>" +
		       "<p>Die folgenden Umsätze konnten von der Software nicht analysiert werden.<br/>"
		     + "Um die Software zu verbessern, bitte ich sie, mir die Umsätze<br/>"
		     + "per E-Mail an depotviewer@open4me.de zuzusenden. (Verschlüsselung auf Anfrage)</p>"
		     + "<p>In den unten aufgeführten Umsätzen sind relevante Informationen<br/>"
		     + "(Aktienname, Kurs, Depotnummer, Auftragsnummer) bereits teilweise(!) anonymisiert worden.<br/>"
		     + "Je nach Fehler ist eine Anonymisierung jedoch nicht möglich.</p>"
		     + "</form>");

		    container.addPart(text);

		container.addHeadline(i18n.tr("Order"));
		TextAreaInput textarea = new TextAreaInput(sb.toString());
		container.addPart(textarea);

		ButtonArea buttons = new ButtonArea();
		buttons.addButton(i18n.tr("Schließen"),new Action() {
			public void handleAction(Object context) throws ApplicationException
			{
				close();
			}
		},null,true,"window-close.png");
		container.addButtonArea(buttons);

	}

}
