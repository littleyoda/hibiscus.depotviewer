package de.open4me.depot.kursprovider.portfolio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.open4me.depot.kursprovider.KursAbrufResult;
import de.open4me.depot.kursprovider.portfolio.PortfolioPerformanceApi.Market;

public class PortfolioPerformanceApiTest
{
	@Test
	public void mapsOnlyValidMarketsForExactIsin() throws Exception
	{
		JsonNode response = PortfolioPerformanceHttp.JSON.readTree("""
				[
				  {"isin":"OTHER","markets":[{"symbol":"NO","exchange":"XETR","currency":"EUR"}]},
				  {"isin":"DE0008404005","markets":[
				    {"symbol":"ALV.DE","exchange":"xetr","currency":"eur"},
				    {"symbol":"ALIZF","exchange":"OOTC","currency":"USD"},
				    {"symbol":"","exchange":"XFRA","currency":"EUR"},
				    {"symbol":"ALV.F","exchange":"XFRA","currency":"EU"}
				  ]}
				]
				""");

		List<Market> markets = PortfolioPerformanceApi.mapMarkets("de0008404005", response);

		assertEquals(2, markets.size());
		assertEquals("ALV.DE", markets.get(0).getSymbol());
		assertEquals("XETR", markets.get(0).getExchange());
		assertEquals("EUR", markets.get(0).getCurrency());
	}

	@Test
	public void mapsLastPositiveCloseOfEachUtcDay() throws Exception
	{
		JsonNode response = PortfolioPerformanceHttp.JSON.readTree("""
				{"s":"ok","t":[1787875200,1787918400,1787961600,1788048000],
				 "c":[300.10,301.20,-1,null]}
				""");

		KursAbrufResult result = PortfolioPerformanceApi.mapCandles("eur", response);

		assertEquals(1, result.getKurse().size());
		assertEquals(LocalDate.of(2026, 8, 28), result.getKurse().get(0).getDatum());
		assertTrue(new BigDecimal("301.20").compareTo(result.getKurse().get(0).getWert()) == 0);
		assertEquals("EUR", result.getKurse().get(0).getWaehrung());
		assertEquals(0, result.getEreignisse().size());
	}
}
