package de.open4me.depot.kursprovider.scalable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.open4me.depot.kursprovider.KursAbrufResult;

public class ScalableMcpProviderTest
{
	@Test
	public void usesLastValidPointOfEachUtcDayAndClosingReferencePoint() throws Exception
	{
		JsonNode chart = ScalableHttp.JSON.readTree("""
				{
				  "isin": "DE0008404005",
				  "timeframe": "max",
				  "currency": "eur",
				  "closingReferencePoint": {
				    "midPrice": 303.50,
				    "timestampUtc": "2026-08-25T18:00:00Z"
				  },
				  "dataPoints": [
				    { "midPrice": 300.10, "timestampUtc": "2026-08-24T08:00:00Z" },
				    { "midPrice": 301.20, "timestampUtc": "2026-08-24T20:00:00Z" },
				    { "midPrice": -1, "timestampUtc": "2026-08-25T21:00:00Z" },
				    { "midPrice": null, "timestampUtc": "2026-08-26T10:00:00Z" }
				  ]
				}
				""");

		KursAbrufResult result = ScalableMcpProvider.mapChart("DE0008404005", chart);

		assertEquals(2, result.getKurse().size());
		assertEquals(LocalDate.of(2026, 8, 24), result.getKurse().get(0).getDatum());
		assertTrue(new BigDecimal("301.20").compareTo(result.getKurse().get(0).getWert()) == 0);
		assertEquals(LocalDate.of(2026, 8, 25), result.getKurse().get(1).getDatum());
		assertTrue(new BigDecimal("303.50").compareTo(result.getKurse().get(1).getWert()) == 0);
		assertEquals("EUR", result.getKurse().get(1).getWaehrung());
		assertEquals(0, result.getEreignisse().size());
	}
}
