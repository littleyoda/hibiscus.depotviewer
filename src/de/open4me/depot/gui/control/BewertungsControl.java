package de.open4me.depot.gui.control;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.util.List;

import org.eclipse.swt.widgets.TableItem;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.action.OrderList;
import de.open4me.depot.gui.parts.PrintfColumn;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.open4me.depot.tools.WertBerechnung;
import de.willuhn.jameica.gui.AbstractControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Part;
import de.willuhn.jameica.gui.formatter.DateFormatter;
import de.willuhn.jameica.gui.formatter.TableFormatter;
import de.willuhn.jameica.gui.parts.TablePart;
import de.willuhn.jameica.gui.parts.table.Feature;
import de.willuhn.jameica.gui.parts.table.Feature.Context;
import de.willuhn.jameica.gui.parts.table.FeatureSummary;
import de.willuhn.jameica.gui.util.Color;
import de.willuhn.jameica.hbci.gui.ColorUtil;
import de.willuhn.logging.Logger;

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

		wertList = new TablePart(WertBerechnung.getWertBerechnung(), new OrderList()) {
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
				return String.format("Gesamtdepotwert: %,.2f EUR, Gewinn / Verlust: %,.2f EUR, %,.2f %%, Gesamtverkaufserlöse: %,.2f EUR",
					gewinnSumme.add(einstandSumme).doubleValue(),
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
}
