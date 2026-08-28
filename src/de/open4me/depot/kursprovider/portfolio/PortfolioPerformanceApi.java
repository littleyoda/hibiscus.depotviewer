package de.open4me.depot.kursprovider.portfolio;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;

import com.fasterxml.jackson.databind.JsonNode;

import de.open4me.depot.kursprovider.KursAbrufResult;
import de.open4me.depot.kursprovider.KursAbrufResult.Kurs;
import de.willuhn.util.ApplicationException;

/** Suche und historische Kurse der Portfolio-Performance-Marktdaten-API. */
public class PortfolioPerformanceApi
{
	private static final String API = "https://api.portfolio-performance.info";
	private static final Set<String> SAMPLE_SYMBOLS = Set.of(
			"AMZN", "NVD.F", "MBG.DE", "DTG.DE", "IQQY.DE", "SXRS.DE",
			"EUNH.DE", "IQQN.DE", "X014.DE", "IQQE.DE");
	private static final long START = LocalDate.of(2000, 1, 1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();

	private final PortfolioPerformanceOAuthClient oauth;
	private final HttpClient http;

	public PortfolioPerformanceApi(PortfolioPerformanceOAuthClient oauth)
	{
		this.oauth = oauth;
		this.http = PortfolioPerformanceHttp.createClient();
	}

	public List<Market> searchByIsin(String isin) throws Exception
	{
		String normalized = normalizeIsin(isin);
		JsonNode response = getJson(API + "/v1/search?isin=" + enc(normalized), null);
		return mapMarkets(normalized, response);
	}

	static List<Market> mapMarkets(String isin, JsonNode response) throws ApplicationException
	{
		String normalized = normalizeIsin(isin);
		if (!response.isArray())
			throw new ApplicationException("Portfolio Performance lieferte keine gültige Wertpapiersuche.");

		List<Market> markets = new ArrayList<Market>();
		for (JsonNode instrument : response)
		{
			if (!normalized.equals(instrument.path("isin").asText("").trim().toUpperCase(Locale.ROOT))) continue;
			for (JsonNode market : instrument.path("markets"))
			{
				String symbol = market.path("symbol").asText("").trim();
				String exchange = market.path("exchange").asText("").trim().toUpperCase(Locale.ROOT);
				String currency = market.path("currency").asText("").trim().toUpperCase(Locale.ROOT);
				if (!symbol.isBlank() && !exchange.isBlank() && currency.matches("[A-Z]{3}"))
					markets.add(new Market(symbol, exchange, currency));
			}
		}
		if (markets.isEmpty())
			throw new ApplicationException("Portfolio Performance fand für diese ISIN keinen verwendbaren Börsenplatz.");
		return markets;
	}

	public KursAbrufResult fetch(Market market, BooleanSupplier cancelled) throws Exception
	{
		if (!market.isValid())
			throw new ApplicationException("Gespeicherte Portfolio-Performance-Einstellungen sind nicht mehr gültig.");
		boolean sample = SAMPLE_SYMBOLS.contains(market.symbol);
		String path = sample ? "/sample/v1/candle" : "/v1/candle";
		String url = API + path + "?symbol=" + enc(market.symbol) + "&from=" + START
				+ "&to=" + Instant.now().getEpochSecond();
		String token = sample ? null : oauth.getAccessToken(cancelled);
		try
		{
			return mapCandles(market.currency, getJson(url, token));
		}
		catch (UnauthorizedException e)
		{
			if (sample) throw e;
			return mapCandles(market.currency, getJson(url, oauth.refreshAfterUnauthorized()));
		}
	}

	static KursAbrufResult mapCandles(String currency, JsonNode response) throws ApplicationException
	{
		String normalizedCurrency = currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
		if (!normalizedCurrency.matches("[A-Z]{3}"))
			throw new ApplicationException("Portfolio Performance lieferte keine gültige Kurswährung.");
		if ("no_data".equals(response.path("s").asText()))
			throw new ApplicationException("Portfolio Performance lieferte keine Kursdaten.");

		JsonNode timestamps = response.get("t");
		JsonNode closes = response.get("c");
		if (timestamps == null || closes == null || !timestamps.isArray() || !closes.isArray()
				|| timestamps.size() != closes.size())
			throw new ApplicationException("Portfolio Performance lieferte unvollständige Kursdaten.");

		Map<LocalDate,TimedPrice> byDate = new TreeMap<LocalDate,TimedPrice>();
		for (int i = 0; i < timestamps.size(); i++)
		{
			JsonNode timestamp = timestamps.get(i);
			JsonNode close = closes.get(i);
			if (!timestamp.canConvertToLong() || close == null || !close.isNumber()) continue;
			BigDecimal price = close.decimalValue();
			if (price.signum() <= 0) continue;
			try
			{
				Instant instant = Instant.ofEpochSecond(timestamp.longValue());
				LocalDate date = instant.atZone(ZoneOffset.UTC).toLocalDate();
				TimedPrice candidate = new TimedPrice(instant, price);
				byDate.merge(date, candidate,
						(previous, current) -> previous.instant.isBefore(current.instant) ? current : previous);
			}
			catch (Exception ignored) {}
		}
		if (byDate.isEmpty())
			throw new ApplicationException("Portfolio Performance lieferte keine gültigen Schlusskurse.");

		List<Kurs> quotes = new ArrayList<Kurs>();
		for (Map.Entry<LocalDate,TimedPrice> entry : byDate.entrySet())
			quotes.add(new Kurs(entry.getKey(), entry.getValue().price, normalizedCurrency));
		return new KursAbrufResult(quotes, List.of());
	}

	private JsonNode getJson(String value, String token) throws Exception
	{
		URI uri = PortfolioPerformanceHttp.trustedUri(value);
		HttpRequest.Builder builder = HttpRequest.newBuilder(uri).timeout(PortfolioPerformanceHttp.TIMEOUT)
				.header("Accept", "application/json").header("User-Agent", "hibiscus.depotviewer/1.0").GET();
		if (token != null) builder.header("Authorization", "Bearer " + token);
		HttpResponse<String> response = http.send(builder.build(),
				HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (response.statusCode() == 401) throw new UnauthorizedException();
		if (response.statusCode() < 200 || response.statusCode() >= 300)
			throw new ApplicationException("Portfolio-Performance-Abruf fehlgeschlagen (HTTP " + response.statusCode() + ").");
		try { return PortfolioPerformanceHttp.JSON.readTree(response.body()); }
		catch (Exception e) { throw new ApplicationException("Portfolio Performance lieferte keine gültige JSON-Antwort.", e); }
	}

	private static String normalizeIsin(String isin) throws ApplicationException
	{
		String normalized = isin == null ? "" : isin.trim().toUpperCase(Locale.ROOT);
		if (!normalized.matches("[A-Z]{2}[A-Z0-9]{9}[0-9]"))
			throw new ApplicationException("Portfolio Performance benötigt eine gültige ISIN.");
		return normalized;
	}

	private static String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

	public static final class Market
	{
		private final String symbol;
		private final String exchange;
		private final String currency;

		public Market(String symbol, String exchange, String currency)
		{
			this.symbol = symbol == null ? "" : symbol.trim();
			this.exchange = exchange == null ? "" : exchange.trim().toUpperCase(Locale.ROOT);
			this.currency = currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
		}

		public String getSymbol() { return symbol; }
		public String getExchange() { return exchange; }
		public String getCurrency() { return currency; }
		public boolean isValid() { return !symbol.isBlank() && !exchange.isBlank() && currency.matches("[A-Z]{3}"); }
		@Override public String toString() { return symbol + " — " + exchange + " — " + currency; }
	}

	private static final class TimedPrice
	{
		final Instant instant;
		final BigDecimal price;
		TimedPrice(Instant instant, BigDecimal price) { this.instant = instant; this.price = price; }
	}

	private static final class UnauthorizedException extends Exception
	{
		private static final long serialVersionUID = 1L;
	}
}
