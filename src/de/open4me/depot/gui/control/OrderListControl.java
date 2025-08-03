package de.open4me.depot.gui.control;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.action.OrderList;
import de.open4me.depot.gui.action.UmsatzEditorAction;
import de.open4me.depot.gui.menu.OrderListMenu;
import de.open4me.depot.gui.parts.PrintfColumn;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.open4me.depot.sql.SQLUtils;
import de.open4me.depot.sql.SQLQueries;
import de.open4me.depot.abruf.utils.Utils;
import de.willuhn.jameica.gui.AbstractControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Part;
import de.willuhn.jameica.gui.formatter.DateFormatter;
import de.willuhn.jameica.gui.formatter.TableFormatter;
import de.willuhn.jameica.gui.input.DateInput;
import de.willuhn.jameica.gui.input.SelectInput;
import de.willuhn.jameica.gui.input.TextInput;
import de.willuhn.jameica.gui.parts.TablePart;
import de.willuhn.jameica.gui.util.Color;
import de.willuhn.jameica.gui.util.Container;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.jameica.gui.util.ColumnLayout;
import de.willuhn.jameica.hbci.gui.ColorUtil;
import de.willuhn.logging.Logger;


public class OrderListControl extends AbstractControl
{

	private TablePart orderList;
	private TablePart umsatzList;
	private DateInput dateFrom;
	private DateInput dateTo;
	private SelectInput depotFilter;
	private SelectInput wknFilter;
	
	public OrderListControl(AbstractView view) {
		super(view);
	}


	public Part getOrderInfoTable() throws RemoteException
	{
		return getOrderInfoTable(false);
	}
	
	public Part getOrderInfoTable(boolean reload) throws RemoteException
	{
		if (orderList != null && !reload) {
			return orderList;
		}

		String query = buildQuery();
		List<GenericObjectSQL> list = SQLUtils.getResultSet(query, "depotviewer_umsaetze", "id");

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
	
	private String buildQuery() {
		StringBuilder query = new StringBuilder();
		query.append("select *, ")
			 .append("concat(kosten, ' ', kostenw) as joinkosten, ")
			 .append("konto.id as kontoid, ")
			 .append("depotviewer_umsaetze.id as umsatzid, ")
			 .append("concat(steuern, ' ', steuernw) as joinsteuern, ")
			 .append("concat(transaktionskosten, ' ', transaktionskostenw) as jointransaktionskosten ")
			 .append("from depotviewer_umsaetze ")
			 .append("left join depotviewer_wertpapier on depotviewer_umsaetze.wpid = depotviewer_wertpapier.id ")
			 .append("left join konto on konto.id = depotviewer_umsaetze.kontoid ");
			 
		boolean hasWhere = false;
		
		// Date filter
		try {
			if (dateFrom != null && dateFrom.getValue() != null) {
				query.append(hasWhere ? " and " : " where ");
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				query.append("buchungsdatum >= '").append(sdf.format((Date) dateFrom.getValue())).append("'");
				hasWhere = true;
			}
			if (dateTo != null && dateTo.getValue() != null) {
				query.append(hasWhere ? " and " : " where ");
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				query.append("buchungsdatum <= '").append(sdf.format((Date) dateTo.getValue())).append("'");
				hasWhere = true;
			}
		} catch (Exception e) {
			Logger.error("Error building date filter", e);
		}
		
		// Depot filter
		try {
			if (depotFilter != null && depotFilter.getValue() != null) {
				GenericObjectHashMap depot = (GenericObjectHashMap) depotFilter.getValue();
				query.append(hasWhere ? " and " : " where ");
				query.append("depotviewer_umsaetze.kontoid = ").append(depot.getAttribute("id"));
				hasWhere = true;
			}
		} catch (Exception e) {
			Logger.error("Error building depot filter", e);
		}
		
		// WKN filter
		try {
			if (wknFilter != null && wknFilter.getValue() != null) {
				GenericObjectSQL wp = (GenericObjectSQL) wknFilter.getValue();
				query.append(hasWhere ? " and " : " where ");
				query.append("depotviewer_wertpapier.id = ").append(wp.getID());
				hasWhere = true;
			}
		} catch (Exception e) {
			Logger.error("Error building WKN filter", e);
		}
		
		query.append(" order by buchungsdatum desc");
		return query.toString();
	}
	
	public Container getFilterContainer(Composite parent) throws RemoteException {
		Container container = new SimpleContainer(parent);
		
		// Date range filter in one row
		ColumnLayout dateColumns = new ColumnLayout(container.getComposite(), 2);
		Container dateLeft = new SimpleContainer(dateColumns.getComposite());
		Container dateRight = new SimpleContainer(dateColumns.getComposite());
		dateLeft.addLabelPair(Settings.i18n().tr("Von Datum"), getDateFrom());
		dateRight.addLabelPair(Settings.i18n().tr("Bis Datum"), getDateTo());
		
		// Depot filter
		container.addLabelPair(Settings.i18n().tr("Depot"), getDepotFilter());
		
		// WKN filter
		container.addLabelPair(Settings.i18n().tr("WKN"), getWknFilter());
		
		return container;
	}
	
	public DateInput getDateFrom() throws RemoteException {
		if (dateFrom != null)
			return dateFrom;
			
		dateFrom = new DateInput();
		dateFrom.addListener(new Listener() {
			public void handleEvent(Event event) {
				try {
					refreshTable();
				} catch (RemoteException e) {
					Logger.error("Error refreshing table", e);
				}
			}
		});
		return dateFrom;
	}
	
	public DateInput getDateTo() throws RemoteException {
		if (dateTo != null)
			return dateTo;
			
		dateTo = new DateInput();
		dateTo.addListener(new Listener() {
			public void handleEvent(Event event) {
				try {
					refreshTable();
				} catch (RemoteException e) {
					Logger.error("Error refreshing table", e);
				}
			}
		});
		return dateTo;
	}
	
	public SelectInput getDepotFilter() throws RemoteException {
		if (depotFilter != null)
			return depotFilter;
			
		try {
			List<GenericObjectHashMap> depots = Utils.getDepotKonten();
			depotFilter = new SelectInput(depots, null);
			depotFilter.setAttribute("bezeichnung");
			depotFilter.setPleaseChoose(Settings.i18n().tr("Alle Depots"));
			depotFilter.addListener(new Listener() {
				public void handleEvent(Event event) {
					try {
						refreshTable();
					} catch (RemoteException e) {
						Logger.error("Error refreshing table", e);
					}
				}
			});
		} catch (Exception e) {
			Logger.error("Error creating depot filter", e);
		}
		return depotFilter;
	}
	
	public SelectInput getWknFilter() throws RemoteException {
		if (wknFilter != null)
			return wknFilter;
			
		List<GenericObjectSQL> wertpapiere = SQLQueries.getOwnedWertpapiere();
		wknFilter = new SelectInput(wertpapiere, null);
		wknFilter.setAttribute("nicename");
		wknFilter.setPleaseChoose(Settings.i18n().tr("Alle Wertpapiere"));
		wknFilter.addListener(new Listener() {
			public void handleEvent(Event event) {
				try {
					refreshTable();
				} catch (RemoteException e) {
					Logger.error("Error refreshing table", e);
				}
			}
		});
		return wknFilter;
	}
	
	private void refreshTable() throws RemoteException {
		if (orderList != null) {
			// Clear the existing table
			orderList.removeAll();
			
			// Get fresh data with current filters
			String query = buildQuery();
			List<GenericObjectSQL> list = SQLUtils.getResultSet(query, "depotviewer_umsaetze", "id");
			
			// Update the table with new data
			for (GenericObjectSQL item : list) {
				orderList.addItem(item);
			}
		}
	}

}
