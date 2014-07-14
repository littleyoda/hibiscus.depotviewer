package de.open4me.depot.gui.control;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.action.OrderList;
import de.open4me.depot.gui.formater.BigDecimalFormater;
import de.open4me.depot.tools.WertBerechnung;
import de.willuhn.jameica.gui.AbstractControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Part;
import de.willuhn.jameica.gui.formatter.DateFormatter;
import de.willuhn.jameica.gui.parts.TablePart;

public class BewertungsControl extends AbstractControl {
	private TablePart wertList;

	public BewertungsControl(AbstractView view) {
		super(view);
	}


	public Part getOrderInfoTable() throws Exception
	{
		if (wertList != null) {
			return wertList;
		}


		wertList = new TablePart(WertBerechnung.getWertBerechnung(), new OrderList());
		wertList.addColumn(Settings.i18n().tr("Depot"), "bezeichnung");
		wertList.addColumn(Settings.i18n().tr("Name"),"wertpapiername"); 
		wertList.addColumn(Settings.i18n().tr("ISIN"),"isin");
		wertList.addColumn(Settings.i18n().tr("WKN"),"wkn");
		wertList.addColumn(Settings.i18n().tr("Anzahl"),"anzahl"); 
		wertList.addColumn(Settings.i18n().tr("Einstands-\npreis"),"einstand", new BigDecimalFormater(2)); 
		wertList.addColumn(Settings.i18n().tr("Verkauf-\nerlöse"),"erloese", new BigDecimalFormater(2)); 
		wertList.addColumn(Settings.i18n().tr("Aktueller\nWert"),"wert", new BigDecimalFormater(2)); 
		wertList.addColumn(Settings.i18n().tr("Veränderung\n(Abs.)"),"abs", new BigDecimalFormater(2)); 
		wertList.addColumn(Settings.i18n().tr("Veränderung\n(%)"),"absproz", new BigDecimalFormater(2)); 

		wertList.addColumn(Settings.i18n().tr("Bewertung-/\nVerkaufsdatum"),"datum", new DateFormatter(Settings.DATEFORMAT)); 
		return wertList;
	}




}
