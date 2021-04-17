package de.open4me.depot.tools;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;

import org.apache.commons.lang3.StringUtils;

import de.willuhn.jameica.system.Application;

public class VarDecimalFormat extends DecimalFormat
{
	
	public static String nullen(int nachkommastellen) {
		return StringUtils.repeat('0', nachkommastellen);
	}
	
	int nachkommastellen;
	public VarDecimalFormat(int nachkommastellen)
	{
		this(nachkommastellen, 0);
	}
	
	/**
	 * Formatiert einen Wert mit x fixen Nachkommastellen, die Notfalls durch Nullen aufgefüllt werden.
	 * Sollte der Wert genauer sein und mehr Nachkommastellen enthalten, so werden bis zu {@code extranachkommastellen} viele zusätzlich angezeigt. 
	 * @param nachkommastellen
	 * @param extranachkommastellen
	 */
	public VarDecimalFormat(int nachkommastellen, int extranachkommastellen)
	{
		super("###,###,##0." + nullen(nachkommastellen) + StringUtils.repeat('#', extranachkommastellen),new DecimalFormatSymbols(Application.getConfig().getLocale()));
		this.nachkommastellen = nachkommastellen;
		setGroupingUsed(true);
	}

	/**
	 * Nachformatieren fuer "-0,00".
	 * @see java.text.DecimalFormat#format(double, java.lang.StringBuffer,
	 *      java.text.FieldPosition)
	 */
	public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition)
	{
		StringBuffer sb = super.format(number, result, fieldPosition);
		if (sb == null || sb.length() == 0)
			return sb;
		String s = sb.toString();
		if (("-0,"+ nullen(nachkommastellen)).equals(s))
		{
			sb.delete(0, sb.length());
			sb.append("0," + nullen(nachkommastellen));
		} 
		return sb;
	}
}
