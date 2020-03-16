package de.open4me.depot.datenobj.rmi;

import java.math.BigDecimal;
import java.util.Currency;

public class BigDecimalWithCurrency {

	
	private BigDecimal zahl;
	private Currency waehrung;

	public BigDecimalWithCurrency(BigDecimal zahl, Currency waehrung) {
		this.zahl = zahl;
		this.waehrung = waehrung;
	}
	
	public String toString() {
		return zahl.toString() + " " + waehrung;
	}

	public BigDecimal getZahl() {
		return zahl;
	}
	
	public Currency getWaehrung() {
		return waehrung;
	}
}
