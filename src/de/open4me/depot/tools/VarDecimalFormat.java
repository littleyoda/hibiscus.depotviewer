package de.open4me.depot.tools;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;

import de.willuhn.jameica.system.Application;

public class VarDecimalFormat extends DecimalFormat
{
	
	public static String nullen(int nachkommastellen) {
		String out = "";
		for (int i = 0;  i < nachkommastellen; i++) {
			out += "0";
		}
		return out;
	}
	int nachkommastellen;
	public VarDecimalFormat(int nachkommastellen)
	{
		super("###,###,##0." + nullen(nachkommastellen),new DecimalFormatSymbols(Application.getConfig().getLocale()));
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
