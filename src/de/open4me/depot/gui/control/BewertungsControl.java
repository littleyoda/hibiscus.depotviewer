package de.open4me.depot.gui.control;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableItem;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.action.OrderList;
import de.open4me.depot.gui.parts.PrintfColumn;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.open4me.depot.tools.Bestandsabfragen;
import de.open4me.depot.tools.WertBerechnung;
import de.willuhn.jameica.gui.AbstractControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Part;
import de.willuhn.jameica.gui.formatter.DateFormatter;
import de.willuhn.jameica.gui.formatter.TableFormatter;
import de.willuhn.jameica.gui.input.CheckboxInput;
import de.willuhn.jameica.gui.input.SelectInput;
import de.willuhn.jameica.gui.parts.TablePart;
import de.willuhn.jameica.gui.parts.table.Feature;
import de.willuhn.jameica.gui.parts.table.Feature.Context;
import de.willuhn.jameica.gui.parts.table.FeatureSummary;
import de.willuhn.jameica.gui.util.Color;
import de.willuhn.jameica.hbci.gui.ColorUtil;
import de.willuhn.logging.Logger;

public class BewertungsControl extends AbstractControl {
	private TablePart wertList;
	private SelectInput depotFilter;
	private SelectInput wertpapierFilter;
	private CheckboxInput nurBestandFilter;
	private List<GenericObjectHashMap> allData;

	public BewertungsControl(AbstractView view) {
		super(view);
	}

	public Part getOrderInfoTable() throws Exception
	{
		if (wertList != null) {
			return wertList;
		}

		// Alle Daten einmal laden
		allData = WertBerechnung.getWertBerechnung();
		
		wertList = new TablePart(getFilteredData(), new OrderList()) {
			/** Wenn die Tabelle eine Zusammenfassung hat, dann bestimme den Text. */
			@Override
			protected Context createFeatureEventContext(Feature.Event e, Object data) {
	    	Context ctx = super.createFeatureEventContext(e, data);
	      if (this.hasEvent(FeatureSummary.class,e))
	        ctx.addon.put(FeatureSummary.CTX_KEY_TEXT, totalWinLoss());
	      return ctx;
	    }

			/**
			Berechne den Gesamtdepotwert, den gesamten Gewinn / Verlust absolut und
			prozentual, und die Gesamtverkaufserlöse.
			*/
			@SuppressWarnings("unchecked")
			private String totalWinLoss() {
				BigDecimal einstandSumme = BigDecimal.ZERO;
				BigDecimal wertSumme = BigDecimal.ZERO;
				BigDecimal gewinnSumme = BigDecimal.ZERO;
				BigDecimal erloeseSumme = BigDecimal.ZERO;
				try {
					for (GenericObjectHashMap k: (List<GenericObjectHashMap>) getItems())
					{
						BigDecimal einstand = ((BigDecimal) k.getAttribute("einstand")).negate();
						BigDecimal wert = (BigDecimal) k.getAttribute("wert");
						BigDecimal erloese = (BigDecimal) k.getAttribute("erloese");
						BigDecimal abs = (BigDecimal) k.getAttribute("abs");

						// wenn Einstand und Wert vorhanden, dann noch nicht verkauft.
						if (wert != null) {
								einstandSumme = einstandSumme.add(einstand);
								gewinnSumme = gewinnSumme.add(abs);
								wertSumme = wertSumme.add(wert);
						}
						// wenn Einstand und Erlöse vorhanden, dann verkauft. Nur Erlöse summieren.
						if (erloese != null) {
								erloeseSumme = erloeseSumme.add(abs);
						}
					}
				} catch (Exception e) {
					Logger.error("Kann Gewinn / Verlust gesamt nicht berechnen",e);
				}

				// FIXME: assumes currency EUR for now, even though the values can be for multiple currencies
				BigDecimal gewinnPercent = BigDecimal.ZERO;
				if (einstandSumme.compareTo(BigDecimal.ZERO) != 0) {
					gewinnPercent = gewinnSumme.multiply(new BigDecimal("100.0")).divide(einstandSumme, 2, RoundingMode.HALF_UP);
				}
				return String.format("Kosten: %,.2f EUR, Wert: %,.2f EUR, Gewinn/Verlust: %,.2f EUR bzw. %,.2f %%, Erlöse: %,.2f EUR",
					//gewinnSumme.add(einstandSumme).doubleValue(),
					einstandSumme.doubleValue(),
					wertSumme.doubleValue(),
					gewinnSumme.doubleValue(),
						gewinnPercent.doubleValue(),
						erloeseSumme.doubleValue());
			}

		};

		wertList.setRememberColWidths(true);
		wertList.setRememberOrder(true);
		wertList.addColumn(Settings.i18n().tr("Depot"), "bezeichnung");
		wertList.addColumn(Settings.i18n().tr("Name"),"wertpapiername");
		wertList.addColumn(Settings.i18n().tr("ISIN"),"isin");
		wertList.addColumn(Settings.i18n().tr("WKN"),"wkn");
		wertList.addColumn(new PrintfColumn(Settings.i18n().tr("Anzahl"), "anzahl",	"%,.5f", "anzahl"));
		wertList.addColumn(new PrintfColumn(Settings.i18n().tr("Einstandspreis"), "einstand", "%,.2f %s", "einstand", "währung"));
		wertList.addColumn(new PrintfColumn(Settings.i18n().tr("Verkaufserlöse"), "erloese", "%,.2f %s", "erloese", "währung"));
		wertList.addColumn(new PrintfColumn(Settings.i18n().tr("Aktueller Wert"), "wert", "%,.2f %s", "wert", "währung"));
		wertList.addColumn(new PrintfColumn(Settings.i18n().tr("Gewinn / Verlust abs."), "abs", "%,.2f %s", "abs", "währung"));
		wertList.addColumn(new PrintfColumn(Settings.i18n().tr("Gewinn / Verlust %"), "absproz", "%,.2f %%", "absproz"));
		wertList.addColumn(Settings.i18n().tr("Bewertungs-/Verkaufsdatum"),"datum", new DateFormatter(Settings.DATEFORMAT));

		wertList.setFormatter(new TableFormatter() {
			public void format(TableItem item) {
				GenericObjectHashMap wp = (GenericObjectHashMap) item.getData();
				final int absChangeColumn = 8;
				try {
					BigDecimal abs = (BigDecimal) wp.getAttribute("abs");
					item.setForeground(absChangeColumn, ColorUtil.getColor(abs.doubleValue(), Color.ERROR,Color.SUCCESS,Color.FOREGROUND).getSWTColor());
					item.setForeground(absChangeColumn + 1, ColorUtil.getColor(abs.doubleValue(), Color.ERROR,Color.SUCCESS,Color.FOREGROUND).getSWTColor());
				} catch (RemoteException e) {
					Logger.error("error while formatting abs value", e);
				}
			}
		});

		return wertList;
	}

	/**
	 * Depot-Filter
	 */
	public SelectInput getDepotFilter() throws Exception {
		if (depotFilter != null)
			return depotFilter;

		List<GenericObjectSQL> depots = SQLUtils.getResultSet(
			"SELECT DISTINCT bezeichnung FROM depotviewer_umsaetze " +
			"LEFT JOIN konto ON konto.id = depotviewer_umsaetze.kontoid " +
			"WHERE bezeichnung IS NOT NULL ORDER BY bezeichnung", 
			"", "", "");
		
		List<String> depotNames = new ArrayList<String>();
		depotNames.add("Alle Depots");
		for (GenericObjectSQL depot : depots) {
			depotNames.add((String) depot.getAttribute("bezeichnung"));
		}

		depotFilter = new SelectInput(depotNames, "Alle Depots");
		depotFilter.setName(Settings.i18n().tr("Depot"));
		// Load saved depot from settings
		try {
			de.willuhn.jameica.system.Settings settings = new de.willuhn.jameica.system.Settings(BewertungsControl.class);
			String savedDepot = settings.getString("bewertung.depot", "Alle Depots");
			if (depotNames.contains(savedDepot)) {
				depotFilter.setValue(savedDepot);
			}
		} catch (Exception e) {
			// Ignore errors, use default
		}
		depotFilter.addListener(new Listener() {
			public void handleEvent(Event event) {
				try {
					refreshTable();
				} catch (Exception e) {
					Logger.error("Fehler beim Aktualisieren der Tabelle", e);
				}
			}
		});

		return depotFilter;
	}

	/**
	 * Wertpapier-Filter
	 */
	public SelectInput getWertpapierFilter() throws Exception {
		if (wertpapierFilter != null)
			return wertpapierFilter;

		List<GenericObjectSQL> wertpapiere = SQLUtils.getResultSet(
			"SELECT DISTINCT wertpapiername FROM depotviewer_wertpapier ORDER BY wertpapiername", 
			"", "", "");
		
		List<String> wpNames = new ArrayList<String>();
		wpNames.add("Alle Wertpapiere");
		for (GenericObjectSQL wp : wertpapiere) {
			wpNames.add((String) wp.getAttribute("wertpapiername"));
		}

		wertpapierFilter = new SelectInput(wpNames, "Alle Wertpapiere");
		wertpapierFilter.setName(Settings.i18n().tr("Wertpapier"));
		// Load saved wertpapier from settings
		try {
			de.willuhn.jameica.system.Settings settings = new de.willuhn.jameica.system.Settings(BewertungsControl.class);
			String savedWP = settings.getString("bewertung.wertpapier", "Alle Wertpapiere");
			if (wpNames.contains(savedWP)) {
				wertpapierFilter.setValue(savedWP);
			}
		} catch (Exception e) {
			// Ignore errors, use default
		}
		wertpapierFilter.addListener(new Listener() {
			public void handleEvent(Event event) {
				try {
					refreshTable();
				} catch (Exception e) {
					Logger.error("Fehler beim Aktualisieren der Tabelle", e);
				}
			}
		});

		return wertpapierFilter;
	}

	/**
	 * Nur-Bestand-Filter
	 */
	public CheckboxInput getNurBestandFilter() throws Exception {
		if (nurBestandFilter != null)
			return nurBestandFilter;

		nurBestandFilter = new CheckboxInput(false);
		nurBestandFilter.setName(Settings.i18n().tr("Nur Wertpapiere im Bestand anzeigen"));
		// Load saved checkbox from settings
		try {
			de.willuhn.jameica.system.Settings settings = new de.willuhn.jameica.system.Settings(BewertungsControl.class);
			boolean savedNurBestand = settings.getBoolean("bewertung.nurbestand", false);
			nurBestandFilter.setValue(savedNurBestand);
		} catch (Exception e) {
			// Ignore errors, use default
		}
		nurBestandFilter.addListener(new Listener() {
			public void handleEvent(Event event) {
				try {
					refreshTable();
				} catch (Exception e) {
					Logger.error("Fehler beim Aktualisieren der Tabelle", e);
				}
			}
		});

		return nurBestandFilter;
	}

	/**
	 * Setup dispose listeners for filter persistence
	 */
	public void setupDisposeListeners() {
		if (depotFilter != null) {
			depotFilter.getControl().addDisposeListener(e -> {
				try {
					de.willuhn.jameica.system.Settings settings = new de.willuhn.jameica.system.Settings(BewertungsControl.class);
					String value = (String) depotFilter.getValue();
					if (value != null) {
						settings.setAttribute("bewertung.depot", value);
					} else {
						settings.setAttribute("bewertung.depot", "Alle Depots");
					}
				} catch (Exception ex) {
					// Ignore save errors
				}
			});
		}
		
		if (wertpapierFilter != null) {
			wertpapierFilter.getControl().addDisposeListener(e -> {
				try {
					de.willuhn.jameica.system.Settings settings = new de.willuhn.jameica.system.Settings(BewertungsControl.class);
					String value = (String) wertpapierFilter.getValue();
					if (value != null) {
						settings.setAttribute("bewertung.wertpapier", value);
					} else {
						settings.setAttribute("bewertung.wertpapier", "Alle Wertpapiere");
					}
				} catch (Exception ex) {
					// Ignore save errors
				}
			});
		}
		
		if (nurBestandFilter != null) {
			nurBestandFilter.getControl().addDisposeListener(e -> {
				try {
					de.willuhn.jameica.system.Settings settings = new de.willuhn.jameica.system.Settings(BewertungsControl.class);
					Boolean value = (Boolean) nurBestandFilter.getValue();
					settings.setAttribute("bewertung.nurbestand", value != null ? value : false);
				} catch (Exception ex) {
					// Ignore save errors
				}
			});
		}
	}

	/**
	 * Filtert die Daten basierend auf den ausgewählten Filtern
	 */
	private List<GenericObjectHashMap> getFilteredData() throws Exception {
		if (allData == null)
			return new ArrayList<GenericObjectHashMap>();

		List<GenericObjectHashMap> filtered = new ArrayList<GenericObjectHashMap>();

		String selectedDepot = depotFilter != null ? (String) depotFilter.getValue() : "Alle Depots";
		String selectedWP = wertpapierFilter != null ? (String) wertpapierFilter.getValue() : "Alle Wertpapiere";
		Boolean nurBestand = nurBestandFilter != null ? (Boolean) nurBestandFilter.getValue() : false;

		for (GenericObjectHashMap item : allData) {
			try {
				// Depot-Filter
				if (!"Alle Depots".equals(selectedDepot)) {
					String depotName = (String) item.getAttribute("bezeichnung");
					if (depotName == null || !depotName.equals(selectedDepot)) {
						continue;
					}
				}

				// Wertpapier-Filter
				if (!"Alle Wertpapiere".equals(selectedWP)) {
					String wpName = (String) item.getAttribute("wertpapiername");
					if (wpName == null || !wpName.equals(selectedWP)) {
						continue;
					}
				}

				// Nur-im-Bestand-Filter
				if (nurBestand) {
					BigDecimal wert = (BigDecimal) item.getAttribute("wert");
					if (wert == null) {
						continue; // Verkaufte Positionen ausschließen
					}
				}

				filtered.add(item);
			} catch (RemoteException e) {
				Logger.error("Fehler beim Filtern der Daten", e);
			}
		}

		return filtered;
	}

	/**
	 * Aktualisiert die Tabelle mit gefilterten Daten
	 */
	private void refreshTable() throws Exception {
		if (wertList != null) {
			wertList.removeAll();
			List<GenericObjectHashMap> filteredData = getFilteredData();
			for (GenericObjectHashMap item : filteredData) {
				wertList.addItem(item);
			}
		}
	}
}
