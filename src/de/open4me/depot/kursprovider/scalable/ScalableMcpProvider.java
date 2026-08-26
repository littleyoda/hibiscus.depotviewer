package de.open4me.depot.kursprovider.scalable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;

import de.open4me.depot.kursprovider.KursAbrufResult;
import de.open4me.depot.kursprovider.KursAbrufResult.Kurs;
import de.willuhn.util.ApplicationException;

/** Fachlicher Scalable-Anbieter auf Basis des read-only MCP-Chart-Tools. */
public class ScalableMcpProvider implements AutoCloseable
{
	private final ScalableTokenStore tokenStore;
	private final ScalableOAuthClient oauth;
	private ScalableMcpClient mcp;

	public ScalableMcpProvider()
	{
		this.tokenStore = new ScalableTokenStore();
		this.oauth = new ScalableOAuthClient(tokenStore);
	}

	public boolean isConnected()
	{
		return oauth.hasSession();
	}

	public void connect() throws Exception
	{
		oauth.reconnect();
		resetClient();
	}

	public void disconnect() throws ApplicationException
	{
		resetClient();
		oauth.disconnect();
	}

	public KursAbrufResult fetch(String isin) throws Exception
	{
		String normalized = isin == null ? "" : isin.trim().toUpperCase(Locale.ROOT);
		if (!normalized.matches("[A-Z]{2}[A-Z0-9]{9}[0-9]"))
			throw new ApplicationException("Scalable Capital benötigt eine gültige ISIN.");
		if (mcp == null)
			mcp = new ScalableMcpClient(oauth);
		return mapChart(normalized, mcp.getSecurityChart(normalized));
	}

	static KursAbrufResult mapChart(String expectedIsin, JsonNode chart) throws ApplicationException
	{
		String actualIsin = chart.path("isin").asText("").trim().toUpperCase(Locale.ROOT);
		if (!expectedIsin.equals(actualIsin))
			throw new ApplicationException("Scalable MCP lieferte Kurse für eine andere ISIN.");
		if (!"max".equals(chart.path("timeframe").asText()))
			throw new ApplicationException("Scalable MCP lieferte nicht den angeforderten Zeitraum 'max'.");

		String currency = chart.path("currency").asText("").trim().toUpperCase(Locale.ROOT);
		if (!currency.matches("[A-Z]{3}"))
			throw new ApplicationException("Scalable MCP lieferte keine gültige Kurswährung.");

		Map<LocalDate,TimedPrice> byDate = new TreeMap<LocalDate,TimedPrice>();
		addPoint(byDate, chart.get("closingReferencePoint"));
		for (JsonNode point : chart.path("dataPoints"))
			addPoint(byDate, point);
		if (byDate.isEmpty())
			throw new ApplicationException("Scalable MCP lieferte keine gültigen Chartpunkte.");

		List<Kurs> quotes = new ArrayList<Kurs>();
		for (Map.Entry<LocalDate,TimedPrice> entry : byDate.entrySet())
			quotes.add(new Kurs(entry.getKey(), entry.getValue().price, currency));
		return new KursAbrufResult(quotes, List.of());
	}

	private static void addPoint(Map<LocalDate,TimedPrice> byDate, JsonNode point)
	{
		if (point == null || point.isNull() || !point.path("midPrice").isNumber()) return;
		BigDecimal price = point.path("midPrice").decimalValue();
		if (price.signum() <= 0) return;
		String timestamp = point.path("timestampUtc").asText(null);
		if (timestamp == null) return;
		try
		{
			Instant instant = Instant.parse(timestamp);
			LocalDate date = instant.atZone(ZoneOffset.UTC).toLocalDate();
			TimedPrice candidate = new TimedPrice(instant, price);
			byDate.merge(date, candidate,
					(previous, current) -> previous.instant.isBefore(current.instant) ? current : previous);
		}
		catch (Exception ignored) {}
	}

	private void resetClient()
	{
		if (mcp != null) mcp.close();
		mcp = null;
	}

	@Override
	public void close()
	{
		resetClient();
	}

	private static final class TimedPrice
	{
		final Instant instant;
		final BigDecimal price;
		TimedPrice(Instant instant, BigDecimal price) { this.instant = instant; this.price = price; }
	}
}
