package de.open4me.depot.gui.control;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.List;

import org.eclipse.swt.widgets.TableItem;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.action.OrderList;
import de.open4me.depot.gui.action.UmsatzEditorAction;
import de.open4me.depot.gui.menu.OrderListMenu;
import de.open4me.depot.gui.parts.PrintfColumn;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.jameica.gui.AbstractControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Part;
import de.willuhn.jameica.gui.formatter.DateFormatter;
import de.willuhn.jameica.gui.formatter.TableFormatter;
import de.willuhn.jameica.gui.parts.TablePart;
import de.willuhn.jameica.gui.util.Color;
import de.willuhn.jameica.hbci.gui.ColorUtil;
import de.willuhn.logging.Logger;


public class OrderListControl extends AbstractControl
{

	private TablePart orderList;
	private TablePart umsatzList;
	public OrderListControl(AbstractView view) {
		super(view);
	}


	public Part getOrderInfoTable() throws RemoteException
	{
		if (orderList != null) {
			return orderList;
		}

		List<GenericObjectSQL> list = SQLUtils.getResultSet("select *, "
				+ "concat(kosten, ' ', kostenw) as joinkosten, "
				+ "konto.id as kontoid, "
				+ "depotviewer_umsaetze.id as umsatzid, "
				+ "concat(steuern, ' ', steuernw) as joinsteuern, "
				+ "concat(transaktionskosten, ' ', transaktionskostenw) as jointransaktionskosten "
				+ "from depotviewer_umsaetze "
				+ "	left join depotviewer_wertpapier on  depotviewer_umsaetze.wpid = depotviewer_wertpapier.id"
				+ "	left join konto on  konto.id = depotviewer_umsaetze.kontoid order by buchungsdatum desc"
				,
				"depotviewer_umsaetze", "id");

		orderList = new TablePart(list, new UmsatzEditorAction(false));
		orderList.setRememberColWidths(true);
		orderList.setRememberOrder(true);
		orderList.addColumn(Settings.i18n().tr("Depot"), "bezeichnung");
		orderList.addColumn(Settings.i18n().tr("WKN"),"wkn");
		orderList.addColumn(Settings.i18n().tr("Wertpapiername"),"wertpapiername");
		orderList.addColumn(new PrintfColumn(Settings.i18n().tr("Anzahl"), "anzahl",	"%,.5f", "anzahl"));
		orderList.addColumn(new PrintfColumn(Settings.i18n().tr("Kurs"), "kurs", "%,.6f %s", "kurs", "kursw"));
		orderList.addColumn(new PrintfColumn(Settings.i18n().tr("Kosten"), "kosten", "%,.2f %s", "kosten", "kostenw"));
		orderList.addColumn(new PrintfColumn(Settings.i18n().tr("Gebühren"), "transaktionskosten", "%,.2f %s", "transaktionskosten", "transaktionskostenw"));
		orderList.addColumn(new PrintfColumn(Settings.i18n().tr("Steuern"), "steuern", "%,.2f %s", "steuern", "steuernw"));
		orderList.addColumn(Settings.i18n().tr("Aktion"),"aktion");
		orderList.addColumn(Settings.i18n().tr("Datum"),"buchungsdatum", new DateFormatter(Settings.DATEFORMAT));
		orderList.addColumn(Settings.i18n().tr("Kommentar"), "kommentar");
		orderList.setContextMenu(new OrderListMenu(orderList));

		orderList.setFormatter(new TableFormatter()
	    {
			public void format(TableItem item) {
				GenericObjectSQL wp = (GenericObjectSQL) item.getData();
				final int absChangeColumn = 5;
				try {
					BigDecimal abs = (BigDecimal) wp.getAttribute("kosten");
					item.setForeground(absChangeColumn, ColorUtil.getColor(abs.doubleValue(), Color.ERROR,Color.SUCCESS,Color.FOREGROUND).getSWTColor());
				} catch (RemoteException e) {
					Logger.error("error while formatting abs value", e);
				}
			}
		});

		return orderList;
	}


}
