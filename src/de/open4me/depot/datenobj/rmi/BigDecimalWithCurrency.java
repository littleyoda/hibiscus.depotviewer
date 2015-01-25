package de.open4me.depot.datenobj.rmi;

import java.math.BigDecimal;

public class BigDecimalWithCurrency {

	
	private BigDecimal zahl;
	private String waehrung;

	public BigDecimalWithCurrency(BigDecimal zahl, String waehrung) {
		this.zahl = zahl;
		this.waehrung = waehrung;
	}
	
	public String toString() {
		return zahl.toString() + " " + waehrung;
	}

	public BigDecimal getZahl() {
		return zahl;
	}
	
	public String getWaehrung() {
		return waehrung;
	}
}
