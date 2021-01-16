package de.open4me.depot.tools;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.datenobj.DepotAktion;
import de.open4me.depot.datenobj.rmi.Umsatz;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.util.ApplicationException;

public class UmsatzeAusBestandsAenderung {

	private List<GenericObjectSQL> lastBestand;
	private Konto konto;

	public UmsatzeAusBestandsAenderung(Konto konto) throws RemoteException {
		this.konto = konto;
		lastBestand = SQLUtils.getResultSet("select * from depotviewer_bestand where kontoid = " + konto.getID(),
				"depotviewer_bestand", "id");
	}

	public void erzeugeUmsaetze() throws ApplicationException {
		erzeugeUmsaetzeFuerBestandsdifferenz(konto, lastBestand);
	}
	
	/**
	 * Ermittelt die Bestandsänderung zwischen dem vorherigen und dem aktuellen Bestand und 
	 * erzeugt hieraus Kauf und Verkauf Umsätze
	 * 
	 * @param konto Konto
	 * @param lastBestand alter Bestand
	 * @throws ApplicationException Fehler
	 */
	private void erzeugeUmsaetzeFuerBestandsdifferenz(Konto konto, List<GenericObjectSQL> lastBestand) throws ApplicationException {
		try {
			List<GenericObjectSQL> currentBestand = SQLUtils.getResultSet("select * from depotviewer_bestand where kontoid = " + konto.getID(),
					"depotviewer_bestand", "id");

			// Liste mit allen Wertpapier-ID erstellen
			ArrayList<Integer> wpids = new ArrayList<Integer>();
			for (GenericObjectSQL x : lastBestand) {
				if (!wpids.contains(x.getAttribute("wpid"))) {
					wpids.add((Integer) x.getAttribute("wpid"));
				}
			}
			for (GenericObjectSQL x : currentBestand) {
				if (!wpids.contains(x.getAttribute("wpid"))) {
					wpids.add((Integer) x.getAttribute("wpid"));
				}
			}

			// For jede Wertpaier-ID die Differenz bestimmen
			for (Integer wpid : wpids) {
				// Bestandsdaten zusammensuchen
				GenericObjectSQL lastdata = null;
				BigDecimal last = new BigDecimal("0");
				BigDecimal current = new BigDecimal("0");
				GenericObjectSQL currentdata = null;
				for (GenericObjectSQL x : lastBestand) {
					if (wpid.equals((Integer) x.getAttribute("wpid"))) {
						lastdata = x;
						last = (BigDecimal) x.getAttribute("anzahl");
					}
				}
				for (GenericObjectSQL x : currentBestand) {
					if (wpid.equals((Integer) x.getAttribute("wpid"))) {
						currentdata = x;
						current = (BigDecimal) x.getAttribute("anzahl");
					}
				}

				// Differenz zwischen beiden Beständen bilden
				BigDecimal diff = current.subtract(last);
				if (diff.compareTo(BigDecimal.ZERO) == 0) {
					continue;
				}

				// In Abhängigkeit davon, ob es ein Kauf oder Verkauf war, die Referenzdaten passen setzen 
				boolean isKauf = (diff.compareTo(BigDecimal.ZERO) > 0);
				GenericObjectSQL ref;
				if (isKauf) {
					ref = currentdata;
				} else {
					ref = lastdata;
				}

				// Umsatz hinzufügen
				Umsatz u = Utils.addUmsatz(konto.getID(), 
						"" + wpid,
						(isKauf) ? DepotAktion.KAUF.internal() : DepotAktion.VERKAUF.internal(),
								"",
								diff.abs().doubleValue(),
								((BigDecimal) ref.getAttribute("kurs")).doubleValue(),
								(String) ref.getAttribute("kursw"),
								(isKauf) ? ((BigDecimal) ref.getAttribute("wert")).negate().doubleValue() : ((BigDecimal) ref.getAttribute("wert")).doubleValue(),
										(String) ref.getAttribute("kursw"),
										(Date) ref.getAttribute("datum"),
										null, "aus Bestandsänderungen generierte Schätzung"
										,0.0d, "EUR", 0.0d, "EUR");
				UmsatzHelper.storeUmsatzInHibiscus(u);
			}
		} catch (Exception e) {
			throw new ApplicationException(e);
		}

	}
	
}
