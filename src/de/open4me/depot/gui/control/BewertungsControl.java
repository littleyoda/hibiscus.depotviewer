package de.open4me.depot.gui.control;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.action.OrderList;
import de.open4me.depot.gui.parts.PrintfColumn;
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
		wertList.setRememberColWidths(true);
		wertList.setRememberOrder(true);
		wertList.addColumn(Settings.i18n().tr("Depot"), "bezeichnung");
		wertList.addColumn(Settings.i18n().tr("Name"),"wertpapiername"); 
		wertList.addColumn(Settings.i18n().tr("ISIN"),"isin");
		wertList.addColumn(Settings.i18n().tr("WKN"),"wkn");
		wertList.addColumn(new PrintfColumn(Settings.i18n().tr("Anzahl"), "anzahl",	"%.5f", "anzahl"));
		wertList.addColumn(new PrintfColumn(Settings.i18n().tr("Einstandspreis"), "einstand", "%.2f %s", "einstand", "währung"));
		wertList.addColumn(new PrintfColumn(Settings.i18n().tr("Verkaufserlöse"), "erloese", "%.2f %s", "erloese", "währung"));
		wertList.addColumn(new PrintfColumn(Settings.i18n().tr("Aktueller Wert"), "wert", "%.2f %s", "wert", "währung"));
		wertList.addColumn(new PrintfColumn(Settings.i18n().tr("Veränderung abs."), "abs", "%.2f %s", "abs", "währung"));
		wertList.addColumn(new PrintfColumn(Settings.i18n().tr("Veränderung %"), "absproz", "%.2f %%", "absproz"));
		wertList.addColumn(Settings.i18n().tr("Bewertungs-/Verkaufsdatum"),"datum", new DateFormatter(Settings.DATEFORMAT));
		return wertList;
	}
}
