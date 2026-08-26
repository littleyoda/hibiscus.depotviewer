package de.open4me.depot.kursprovider.scalable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.program.Program;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import de.willuhn.jameica.gui.GUI;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

/** OAuth-Authorization-Code-Flow mit PKCE für den Scalable-MCP-Server. */
public class ScalableOAuthClient
{
	private static final URI RESOURCE = URI.create("https://mcp.scalable.capital/mcp");
	private static final URI RESOURCE_METADATA = URI.create("https://mcp.scalable.capital/.well-known/oauth-protected-resource/mcp");
	private static final URI AUTH_METADATA = URI.create("https://mcp.scalable.capital/.well-known/oauth-authorization-server");
	private static final String SCOPES = "openid profile offline_access";
	private static final long EXPIRY_MARGIN_SECONDS = 60L;

	private final ScalableTokenStore store;
	private final HttpClient http;

	public ScalableOAuthClient(ScalableTokenStore store)
	{
		this.store = store;
		this.http = ScalableHttp.createClient();
	}

	public boolean hasSession()
	{
		return store.hasSession();
	}

	public void disconnect() throws ApplicationException
	{
		store.clear();
	}

	public synchronized String getAccessToken() throws Exception
	{
		ScalableOAuthData data = store.load();
		if (data == null)
			return connect();
		if (data.getAccessToken() != null
				&& data.getExpiresAtEpochSecond() > Instant.now().getEpochSecond() + EXPIRY_MARGIN_SECONDS)
			return data.getAccessToken();
		return refresh(data);
	}

	public synchronized String reconnect() throws Exception
	{
		return connect();
	}

	public synchronized String refreshAfterUnauthorized() throws Exception
	{
		ScalableOAuthData data = store.load();
		if (data == null)
			throw new ApplicationException("Scalable Capital ist nicht verbunden.");
		return refresh(data);
	}

	private String connect() throws Exception
	{
		Metadata metadata = discover();
		CallbackServer callback = CallbackServer.start();
		try
		{
			String clientId = registerClient(metadata.registrationEndpoint, callback.redirectUri);
			String verifier = randomUrlSafe(64);
			String challenge = Base64.getUrlEncoder().withoutPadding()
					.encodeToString(MessageDigest.getInstance("SHA-256")
							.digest(verifier.getBytes(StandardCharsets.US_ASCII)));
			String state = randomUrlSafe(32);

			String authorizationUrl = metadata.authorizationEndpoint
					+ "?response_type=code"
					+ "&client_id=" + enc(clientId)
					+ "&redirect_uri=" + enc(callback.redirectUri)
					+ "&state=" + enc(state)
					+ "&code_challenge=" + enc(challenge)
					+ "&code_challenge_method=S256"
					+ "&resource=" + enc(RESOURCE.toString())
					+ "&scope=" + enc(SCOPES)
					+ "&prompt=consent";

			GUI.startSync(() -> {
				if (!Program.launch(authorizationUrl))
					Logger.error("Der Browser für die Scalable-Anmeldung konnte nicht geöffnet werden.");
			});

			Map<String,String> callbackValues = callback.result.get(5, TimeUnit.MINUTES);
			if (!state.equals(callbackValues.get("state")))
				throw new ApplicationException("Scalable-Anmeldung abgebrochen: OAuth-Status stimmt nicht überein.");
			if (callbackValues.containsKey("error"))
				throw new ApplicationException("Scalable-Anmeldung abgelehnt: " + callbackValues.get("error"));
			String code = callbackValues.get("code");
			if (code == null || code.isBlank())
				throw new ApplicationException("Scalable-Anmeldung lieferte keinen Autorisierungscode.");

			String body = form(Map.of(
					"grant_type", "authorization_code",
					"client_id", clientId,
					"code", code,
					"code_verifier", verifier,
					"redirect_uri", callback.redirectUri,
					"resource", RESOURCE.toString()));
			JsonNode token = postForm(metadata.tokenEndpoint, body);
			ScalableOAuthData data = tokenData(token, null);
			data.setClientId(clientId);
			data.setRedirectUri(callback.redirectUri);
			store.save(data);
			return data.getAccessToken();
		}
		finally
		{
			callback.close();
		}
	}

	private String refresh(ScalableOAuthData current) throws Exception
	{
		if (current.getRefreshToken() == null || current.getRefreshToken().isBlank())
			throw new ApplicationException("Die Scalable-Sitzung kann nicht erneuert werden. Bitte neu verbinden.");
		Metadata metadata = discover();
		String body = form(Map.of(
				"grant_type", "refresh_token",
				"client_id", current.getClientId(),
				"refresh_token", current.getRefreshToken(),
				"resource", RESOURCE.toString()));
		try
		{
			ScalableOAuthData updated = tokenData(postForm(metadata.tokenEndpoint, body), current);
			updated.setClientId(current.getClientId());
			updated.setRedirectUri(current.getRedirectUri());
			store.save(updated);
			return updated.getAccessToken();
		}
		catch (Exception e)
		{
			throw new ApplicationException("Die Scalable-Sitzung ist abgelaufen. Bitte neu verbinden.", e);
		}
	}

	private Metadata discover() throws Exception
	{
		JsonNode resource = getJson(RESOURCE_METADATA);
		JsonNode servers = resource.path("authorization_servers");
		if (!servers.isArray() || servers.isEmpty())
			throw new ApplicationException("Scalable OAuth-Metadaten enthalten keinen Autorisierungsserver.");
		ScalableHttp.trustedUri(servers.get(0).asText());

		JsonNode auth = getJson(AUTH_METADATA);
		return new Metadata(
				ScalableHttp.trustedUri(requiredText(auth, "authorization_endpoint")).toString(),
				ScalableHttp.trustedUri(requiredText(auth, "token_endpoint")).toString(),
				ScalableHttp.trustedUri(requiredText(auth, "registration_endpoint")).toString());
	}

	private String registerClient(String endpoint, String redirectUri) throws Exception
	{
		ObjectNode body = ScalableHttp.JSON.createObjectNode();
		body.put("client_name", "hibiscus.depotviewer");
		ArrayNode redirects = body.putArray("redirect_uris");
		redirects.add(redirectUri);
		body.putArray("grant_types").add("authorization_code").add("refresh_token");
		body.putArray("response_types").add("code");
		body.put("token_endpoint_auth_method", "none");
		body.put("scope", SCOPES);

		HttpRequest request = HttpRequest.newBuilder(ScalableHttp.trustedUri(endpoint))
				.timeout(ScalableHttp.TIMEOUT)
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
				.build();
		HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		JsonNode json = parseSuccess(response, "Client-Registrierung");
		return requiredText(json, "client_id");
	}

	private JsonNode getJson(URI uri) throws Exception
	{
		HttpRequest request = HttpRequest.newBuilder(ScalableHttp.trustedUri(uri.toString()))
				.timeout(ScalableHttp.TIMEOUT).header("Accept", "application/json").GET().build();
		return parseSuccess(http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)), "OAuth-Erkennung");
	}

	private JsonNode postForm(String endpoint, String body) throws Exception
	{
		HttpRequest request = HttpRequest.newBuilder(ScalableHttp.trustedUri(endpoint))
				.timeout(ScalableHttp.TIMEOUT)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header("Accept", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
		return parseSuccess(http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)), "Token-Abruf");
	}

	private JsonNode parseSuccess(HttpResponse<String> response, String operation) throws Exception
	{
		JsonNode json;
		try { json = ScalableHttp.JSON.readTree(response.body()); }
		catch (Exception e) { throw new ApplicationException(operation + " lieferte keine gültige JSON-Antwort.", e); }
		if (response.statusCode() < 200 || response.statusCode() >= 300)
			throw new ApplicationException(operation + " fehlgeschlagen (HTTP " + response.statusCode() + "): "
					+ json.path("error_description").asText(json.path("error").asText("unbekannter Fehler")));
		return json;
	}

	private ScalableOAuthData tokenData(JsonNode token, ScalableOAuthData previous) throws Exception
	{
		ScalableOAuthData result = new ScalableOAuthData();
		result.setAccessToken(requiredText(token, "access_token"));
		result.setRefreshToken(token.path("refresh_token").asText(previous == null ? null : previous.getRefreshToken()));
		result.setTokenType(token.path("token_type").asText("Bearer"));
		result.setScope(token.path("scope").asText(SCOPES));
		long expiresIn = token.path("expires_in").asLong(300L);
		result.setExpiresAtEpochSecond(Instant.now().getEpochSecond() + Math.max(1L, expiresIn));
		return result;
	}

	private static String requiredText(JsonNode node, String field) throws ApplicationException
	{
		String value = node.path(field).asText(null);
		if (value == null || value.isBlank())
			throw new ApplicationException("Scalable-Antwort enthält kein Feld '" + field + "'.");
		return value;
	}

	private static String randomUrlSafe(int bytes)
	{
		byte[] data = new byte[bytes];
		new SecureRandom().nextBytes(data);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
	}

	private static String enc(String value)
	{
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private static String form(Map<String,String> values)
	{
		return values.entrySet().stream().map(e -> enc(e.getKey()) + "=" + enc(e.getValue()))
				.collect(java.util.stream.Collectors.joining("&"));
	}

	private static final class Metadata
	{
		final String authorizationEndpoint;
		final String tokenEndpoint;
		final String registrationEndpoint;
		Metadata(String authorizationEndpoint, String tokenEndpoint, String registrationEndpoint)
		{
			this.authorizationEndpoint = authorizationEndpoint;
			this.tokenEndpoint = tokenEndpoint;
			this.registrationEndpoint = registrationEndpoint;
		}
	}

	private static final class CallbackServer implements AutoCloseable
	{
		final HttpServer server;
		final String redirectUri;
		final CompletableFuture<Map<String,String>> result = new CompletableFuture<Map<String,String>>();

		private CallbackServer(HttpServer server)
		{
			this.server = server;
			this.redirectUri = "http://127.0.0.1:" + server.getAddress().getPort() + "/callback";
		}

		static CallbackServer start() throws IOException
		{
			InetAddress loopback = InetAddress.getByName("127.0.0.1");
			HttpServer server;
			try { server = HttpServer.create(new InetSocketAddress(loopback, 3030), 0); }
			catch (IOException occupied) { server = HttpServer.create(new InetSocketAddress(loopback, 0), 0); }
			CallbackServer callback = new CallbackServer(server);
			server.createContext("/callback", callback::handle);
			server.setExecutor(Executors.newSingleThreadExecutor(r -> {
				Thread thread = new Thread(r, "scalable-oauth-callback");
				thread.setDaemon(true);
				return thread;
			}));
			server.start();
			return callback;
		}

		private void handle(HttpExchange exchange) throws IOException
		{
			Map<String,String> values = parseQuery(exchange.getRequestURI().getRawQuery());
			String text = values.containsKey("error")
					? "Scalable-Anmeldung wurde abgebrochen. Dieses Fenster kann geschlossen werden."
					: "Scalable Capital wurde mit Depot-Viewer verbunden. Dieses Fenster kann geschlossen werden.";
			byte[] response = ("<!doctype html><html><head><meta charset=\"utf-8\"><title>Depot-Viewer</title></head>"
					+ "<body><p>" + text + "</p></body></html>").getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
			exchange.sendResponseHeaders(200, response.length);
			exchange.getResponseBody().write(response);
			exchange.close();
			result.complete(values);
		}

		private static Map<String,String> parseQuery(String query)
		{
			Map<String,String> values = new HashMap<String,String>();
			if (query == null) return values;
			for (String item : query.split("&"))
			{
				String[] pair = item.split("=", 2);
				values.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
						pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "");
			}
			return values;
		}

		@Override
		public void close()
		{
			server.stop(0);
		}
	}
}
