package de.open4me.depot.kursprovider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Vom konkreten Kursanbieter unabhängiges Ergebnis eines Kursabrufs. */
public final class KursAbrufResult
{
	public static final class Kurs
	{
		private final LocalDate datum;
		private final BigDecimal wert;
		private final String waehrung;

		public Kurs(LocalDate datum, BigDecimal wert, String waehrung)
		{
			this.datum = datum;
			this.wert = wert;
			this.waehrung = waehrung;
		}

		public LocalDate getDatum() { return datum; }
		public BigDecimal getWert() { return wert; }
		public String getWaehrung() { return waehrung; }
	}

	public static final class Ereignis
	{
		private final LocalDate datum;
		private final String ratio;
		private final BigDecimal wert;
		private final String aktion;
		private final String waehrung;

		public Ereignis(LocalDate datum, String ratio, BigDecimal wert, String aktion, String waehrung)
		{
			this.datum = datum;
			this.ratio = ratio;
			this.wert = wert;
			this.aktion = aktion;
			this.waehrung = waehrung;
		}

		public LocalDate getDatum() { return datum; }
		public String getRatio() { return ratio; }
		public BigDecimal getWert() { return wert; }
		public String getAktion() { return aktion; }
		public String getWaehrung() { return waehrung; }
	}

	private final List<Kurs> kurse;
	private final List<Ereignis> ereignisse;

	public KursAbrufResult(List<Kurs> kurse, List<Ereignis> ereignisse)
	{
		this.kurse = Collections.unmodifiableList(new ArrayList<Kurs>(kurse));
		this.ereignisse = ereignisse == null ? null
				: Collections.unmodifiableList(new ArrayList<Ereignis>(ereignisse));
	}

	public List<Kurs> getKurse() { return kurse; }
	public List<Ereignis> getEreignisse() { return ereignisse; }
}
