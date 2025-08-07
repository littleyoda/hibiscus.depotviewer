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
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.hbci.gui.ColorUtil;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;


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
		orderList.setMulti(true); // macht erstmal nur zum löschen Sinn.

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
		
		// Date range and depot filter in one row
		ColumnLayout topColumns = new ColumnLayout(container.getComposite(), 3);
		Container dateLeft = new SimpleContainer(topColumns.getComposite());
		Container dateRight = new SimpleContainer(topColumns.getComposite());
		Container depotContainer = new SimpleContainer(topColumns.getComposite());
		dateLeft.addLabelPair(Settings.i18n().tr("von"), getDateFrom());
		dateRight.addLabelPair(Settings.i18n().tr("bis"), getDateTo());
		depotContainer.addLabelPair(Settings.i18n().tr("Depot"), getDepotFilter());
		
		// WKN filter and reset button in same row
		ColumnLayout wknLayout = new ColumnLayout(container.getComposite(), 2);
		Container wknContainer = new SimpleContainer(wknLayout.getComposite());
		Container buttonContainer = new SimpleContainer(wknLayout.getComposite());
		
		wknContainer.addLabelPair(Settings.i18n().tr("WKN"), getWknFilter());
		buttonContainer.addPart(getResetButton());
		
		// Add dispose listeners after controls are properly initialized
		setupDisposeListeners();
		
		return container;
	}
	
	public ButtonArea getButtonArea(Composite parent) throws RemoteException {
		ButtonArea buttons = new ButtonArea();
		buttons.addButton(Settings.i18n().tr("Filter zurücksetzen"), new Action() {
			public void handleAction(Object context) throws ApplicationException {
				try {
					handleReset();
				} catch (RemoteException e) {
					throw new ApplicationException("Fehler beim Zurücksetzen der Filter", e);
				}
			}
		}, null, false, "edit-undo.png");
		buttons.paint(parent);
		return buttons;
	}
	
	public Button getResetButton() throws RemoteException {
		Button resetButton = new Button(Settings.i18n().tr("Filter zurücksetzen"), new Action() {
			public void handleAction(Object context) throws ApplicationException {
				try {
					handleReset();
				} catch (RemoteException e) {
					throw new ApplicationException("Fehler beim Zurücksetzen der Filter", e);
				}
			}
		});
		return resetButton;
	}
	
	public DateInput getDateFrom() throws RemoteException {
		if (dateFrom != null)
			return dateFrom;
			
		dateFrom = new DateInput();
		// Load saved date from settings
		try {
			de.willuhn.jameica.system.Settings settings = new de.willuhn.jameica.system.Settings(OrderListControl.class);
			String savedDate = settings.getString("orderlist.dateFrom", null);
			if (savedDate != null && !savedDate.isEmpty()) {
				dateFrom.setValue(Settings.DATEFORMAT.parse(savedDate));
			}
		} catch (Exception e) {
			// Ignore parsing errors, use default empty value
		}
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
		// Load saved date from settings
		try {
			de.willuhn.jameica.system.Settings settings = new de.willuhn.jameica.system.Settings(OrderListControl.class);
			String savedDate = settings.getString("orderlist.dateTo", null);
			if (savedDate != null && !savedDate.isEmpty()) {
				dateTo.setValue(Settings.DATEFORMAT.parse(savedDate));
			}
		} catch (Exception e) {
			// Ignore parsing errors, use default empty value
		}
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
			// Load saved depot from settings
			try {
				de.willuhn.jameica.system.Settings settings = new de.willuhn.jameica.system.Settings(OrderListControl.class);
				String savedDepotId = settings.getString("orderlist.depot", null);
				if (savedDepotId != null && !savedDepotId.isEmpty()) {
					for (GenericObjectHashMap depot : depots) {
						if (savedDepotId.equals(String.valueOf(depot.getAttribute("id")))) {
							depotFilter.setValue(depot);
							break;
						}
					}
				}
			} catch (Exception e) {
				// Ignore errors, use default empty value
			}
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
		// Load saved WKN from settings
		try {
			de.willuhn.jameica.system.Settings settings = new de.willuhn.jameica.system.Settings(OrderListControl.class);
			String savedWknId = settings.getString("orderlist.wkn", null);
			if (savedWknId != null && !savedWknId.isEmpty()) {
				for (GenericObjectSQL wp : wertpapiere) {
					if (savedWknId.equals(wp.getID())) {
						wknFilter.setValue(wp);
						break;
					}
				}
			}
		} catch (Exception e) {
			// Ignore errors, use default empty value
		}
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
	
	private void setupDisposeListeners() {
		// Setup dispose listeners after controls are properly initialized
		if (dateFrom != null) {
			dateFrom.getControl().addDisposeListener(e -> {
				try {
					de.willuhn.jameica.system.Settings settings = new de.willuhn.jameica.system.Settings(OrderListControl.class);
					Date value = (Date) dateFrom.getValue();
					if (value != null) {
						settings.setAttribute("orderlist.dateFrom", Settings.DATEFORMAT.format(value));
					} else {
						settings.setAttribute("orderlist.dateFrom", "");
					}
				} catch (Exception ex) {
					// Ignore save errors
				}
			});
		}
		
		if (dateTo != null) {
			dateTo.getControl().addDisposeListener(e -> {
				try {
					de.willuhn.jameica.system.Settings settings = new de.willuhn.jameica.system.Settings(OrderListControl.class);
					Date value = (Date) dateTo.getValue();
					if (value != null) {
						settings.setAttribute("orderlist.dateTo", Settings.DATEFORMAT.format(value));
					} else {
						settings.setAttribute("orderlist.dateTo", "");
					}
				} catch (Exception ex) {
					// Ignore save errors
				}
			});
		}
		
		if (depotFilter != null) {
			depotFilter.getControl().addDisposeListener(e -> {
				try {
					de.willuhn.jameica.system.Settings settings = new de.willuhn.jameica.system.Settings(OrderListControl.class);
					GenericObjectHashMap depot = (GenericObjectHashMap) depotFilter.getValue();
					if (depot != null) {
						settings.setAttribute("orderlist.depot", String.valueOf(depot.getAttribute("id")));
					} else {
						settings.setAttribute("orderlist.depot", "");
					}
				} catch (Exception ex) {
					// Ignore save errors
				}
			});
		}
		
		if (wknFilter != null) {
			wknFilter.getControl().addDisposeListener(e -> {
				try {
					de.willuhn.jameica.system.Settings settings = new de.willuhn.jameica.system.Settings(OrderListControl.class);
					GenericObjectSQL wp = (GenericObjectSQL) wknFilter.getValue();
					if (wp != null) {
						settings.setAttribute("orderlist.wkn", wp.getID());
					} else {
						settings.setAttribute("orderlist.wkn", "");
					}
				} catch (Exception ex) {
					// Ignore save errors
				}
			});
		}
	}
	
	private void handleReset() throws RemoteException {
		try {
			// Alle Filter zurücksetzen
			if (dateFrom != null) {
				dateFrom.setValue(null);
			}
			if (dateTo != null) {
				dateTo.setValue(null);
			}
			if (depotFilter != null) {
				depotFilter.setValue(null);
			}
			if (wknFilter != null) {
				wknFilter.setValue(null);
			}
			// Tabelle aktualisieren
			refreshTable();
		} catch (Exception e) {
			Logger.error("Fehler beim Zurücksetzen der Filter", e);
			throw new RemoteException("Fehler beim Zurücksetzen der Filter", e);
		}
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
