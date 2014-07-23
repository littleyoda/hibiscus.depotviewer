package de.open4me.depot.gui.parts;

import de.willuhn.datasource.GenericObject;
import de.willuhn.logging.Logger;

/**
 * Eine Spalte, die mit HIlfe von String.format formatiert werden kann.
 * @author sven
 *
 */
public class PrintfColumn extends de.willuhn.jameica.gui.parts.Column {

	String[] parameters;
	String formatstring;
	
	/**
	 * 
	 * @param title Spaltenüberschrift
	 * @param sortbyattr Attribut, nach dem sortiert werden soll
	 * @param formatstring Format-String (see String.format)
	 * @param parameters Parameters für den Format-String
	 */
	public PrintfColumn(String title, String sortbyattr, String formatstring, String... parameters) {
		super(sortbyattr, title);
		this.parameters = parameters;
		this.formatstring = formatstring; 
	}
	
	/*
	 * (non-Javadoc)
	 * @see de.willuhn.jameica.gui.parts.Column#getFormattedValue(java.lang.Object, java.lang.Object)
	 */
	@Override
	public String getFormattedValue(Object value, Object context) {

		if (!(context instanceof GenericObject)) {
			Logger.error("kein GenericObject bei " + value + " für  " + context);
			return "";
		}
		GenericObject gobj = (GenericObject) context;
		String display = null;
		try
		{
			// Erstmal die Werte aus dem GenericObject auslesen
			Object[] values = new Object[parameters.length];
			for (int i = 0; i < parameters.length; i++) {
				values[i] = gobj.getAttribute(parameters[i]);
				// Ein Null Wert führt zu einer leeren Ausgabe
				if (values[i] == null) {
					return "";
				}
			}
			
			// Danach die Ausgabe formatieren
			display = String.format(formatstring, values);
		}
		catch (Exception e)
		{
			Logger.error("unable to format value " + value + " for bean " + context,e);
		}
		return display != null ? display : "";
	}


}
