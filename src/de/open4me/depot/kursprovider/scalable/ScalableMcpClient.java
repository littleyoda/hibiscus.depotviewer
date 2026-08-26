package de.open4me.depot.kursprovider.scalable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.willuhn.util.ApplicationException;

/** Minimaler Streamable-HTTP-MCP-Client für die read-only Scalable-Tools. */
public class ScalableMcpClient implements AutoCloseable
{
	private static final URI ENDPOINT = URI.create("https://mcp.scalable.capital/mcp");
	private static final String PROTOCOL_VERSION = "2025-11-25";

	private final ScalableOAuthClient oauth;
	private final HttpClient http;
	private final AtomicLong ids = new AtomicLong(1L);
	private String sessionId;
	private boolean initialized;

	public ScalableMcpClient(ScalableOAuthClient oauth)
	{
		this.oauth = oauth;
		this.http = ScalableHttp.createClient();
	}

	public synchronized void initialize() throws Exception
	{
		if (initialized) return;

		ObjectNode params = ScalableHttp.JSON.createObjectNode();
		params.put("protocolVersion", PROTOCOL_VERSION);
		params.set("capabilities", ScalableHttp.JSON.createObjectNode());
		ObjectNode clientInfo = params.putObject("clientInfo");
		clientInfo.put("name", "hibiscus.depotviewer");
		clientInfo.put("version", "1.1.1");

		JsonNode response = request("initialize", params, false);
		String negotiated = response.path("protocolVersion").asText();
		if (!PROTOCOL_VERSION.equals(negotiated))
			throw new ApplicationException("Nicht unterstützte MCP-Protokollversion: " + negotiated);

		notification("notifications/initialized", ScalableHttp.JSON.createObjectNode());
		initialized = true;
		verifyChartTool();
	}

	public synchronized JsonNode getSecurityChart(String isin) throws Exception
	{
		initialize();
		ObjectNode arguments = ScalableHttp.JSON.createObjectNode();
		arguments.put("isin", isin);
		arguments.put("timeframe", "max");
		ObjectNode params = ScalableHttp.JSON.createObjectNode();
		params.put("name", "get_security_chart");
		params.set("arguments", arguments);
		JsonNode result = request("tools/call", params, true);
		if (result.path("isError").asBoolean(false))
			throw new ApplicationException("Scalable MCP meldet einen Werkzeugfehler: " + toolText(result));

		JsonNode structured = result.get("structuredContent");
		if (structured == null || structured.isNull())
		{
			String text = toolText(result);
			try { structured = ScalableHttp.JSON.readTree(text); }
			catch (Exception e) { throw new ApplicationException("Scalable MCP lieferte keine strukturierten Kursdaten.", e); }
		}
		JsonNode error = structured.get("error");
		if (error != null && !error.isNull())
			throw new ApplicationException("Scalable MCP: " + error.path("message").asText(error.path("code").asText("Fehler")));
		return structured;
	}

	private void verifyChartTool() throws Exception
	{
		JsonNode result = request("tools/list", ScalableHttp.JSON.createObjectNode(), true);
		for (JsonNode tool : result.path("tools"))
		{
			if ("get_security_chart".equals(tool.path("name").asText()))
				return;
		}
		throw new ApplicationException("Der Scalable-MCP-Server bietet 'get_security_chart' nicht an.");
	}

	private JsonNode request(String method, JsonNode params, boolean withProtocol) throws Exception
	{
		long id = ids.getAndIncrement();
		ObjectNode message = ScalableHttp.JSON.createObjectNode();
		message.put("jsonrpc", "2.0");
		message.put("id", id);
		message.put("method", method);
		message.set("params", params);

		HttpResponse<String> response = send(message.toString(), withProtocol, oauth.getAccessToken());
		if (response.statusCode() == 401)
			response = send(message.toString(), withProtocol, oauth.refreshAfterUnauthorized());
		if (response.statusCode() < 200 || response.statusCode() >= 300)
			throw new ApplicationException("Scalable MCP fehlgeschlagen (HTTP " + response.statusCode() + ").");

		response.headers().firstValue("Mcp-Session-Id").ifPresent(value -> sessionId = value);
		JsonNode envelope = parseEnvelope(response, id);
		JsonNode error = envelope.get("error");
		if (error != null && !error.isNull())
			throw new ApplicationException("Scalable MCP: " + error.path("message").asText("JSON-RPC-Fehler"));
		JsonNode result = envelope.get("result");
		if (result == null)
			throw new ApplicationException("Scalable MCP lieferte kein JSON-RPC-Ergebnis.");
		return result;
	}

	private void notification(String method, JsonNode params) throws Exception
	{
		ObjectNode message = ScalableHttp.JSON.createObjectNode();
		message.put("jsonrpc", "2.0");
		message.put("method", method);
		message.set("params", params);
		HttpResponse<String> response = send(message.toString(), true, oauth.getAccessToken());
		if (response.statusCode() == 401)
			response = send(message.toString(), true, oauth.refreshAfterUnauthorized());
		if (response.statusCode() < 200 || response.statusCode() >= 300)
			throw new ApplicationException("Scalable MCP konnte nicht initialisiert werden (HTTP " + response.statusCode() + ").");
	}

	private HttpResponse<String> send(String body, boolean withProtocol, String token) throws IOException, InterruptedException
	{
		HttpRequest.Builder builder = HttpRequest.newBuilder(ENDPOINT)
				.timeout(ScalableHttp.TIMEOUT)
				.header("Authorization", "Bearer " + token)
				.header("Content-Type", "application/json")
				.header("Accept", "application/json, text/event-stream");
		if (withProtocol)
			builder.header("MCP-Protocol-Version", PROTOCOL_VERSION);
		if (sessionId != null)
			builder.header("Mcp-Session-Id", sessionId);
		return http.send(builder.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
				HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
	}

	private JsonNode parseEnvelope(HttpResponse<String> response, long expectedId) throws Exception
	{
		String contentType = response.headers().firstValue("Content-Type").orElse("").toLowerCase();
		if (!contentType.contains("text/event-stream"))
			return ScalableHttp.JSON.readTree(response.body());

		List<String> events = new ArrayList<String>();
		StringBuilder data = new StringBuilder();
		for (String line : response.body().split("\\R", -1))
		{
			if (line.isEmpty())
			{
				if (data.length() > 0) events.add(data.toString());
				data.setLength(0);
			}
			else if (line.startsWith("data:"))
			{
				if (data.length() > 0) data.append('\n');
				data.append(line.substring(5).stripLeading());
			}
		}
		if (data.length() > 0) events.add(data.toString());
		for (String event : events)
		{
			JsonNode node = ScalableHttp.JSON.readTree(event);
			if (node.path("id").asLong(Long.MIN_VALUE) == expectedId)
				return node;
		}
		throw new ApplicationException("Scalable MCP lieferte keine passende SSE-Antwort.");
	}

	private static String toolText(JsonNode result)
	{
		for (JsonNode content : result.path("content"))
		{
			if ("text".equals(content.path("type").asText()))
				return content.path("text").asText();
		}
		return "unbekannter Fehler";
	}

	@Override
	public synchronized void close()
	{
		if (sessionId == null) return;
		try
		{
			String token = oauth.getAccessToken();
			HttpRequest request = HttpRequest.newBuilder(ENDPOINT).timeout(ScalableHttp.TIMEOUT)
					.header("Authorization", "Bearer " + token)
					.header("MCP-Protocol-Version", PROTOCOL_VERSION)
					.header("Mcp-Session-Id", sessionId).DELETE().build();
			http.send(request, HttpResponse.BodyHandlers.discarding());
		}
		catch (Exception ignored) {}
		finally { sessionId = null; initialized = false; }
	}
}
