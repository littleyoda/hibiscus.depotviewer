package de.open4me.depot.kursprovider.portfolio;

import java.io.IOException;
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
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import org.eclipse.swt.program.Program;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import de.willuhn.jameica.gui.GUI;
import de.willuhn.logging.Logger;
import de.willuhn.jameica.system.OperationCanceledException;
import de.willuhn.util.ApplicationException;

/** OAuth-Authorization-Code-Flow mit PKCE für Portfolio Performance. */
public class PortfolioPerformanceOAuthClient
{
	static final String RESOURCE = "https://api.portfolio-performance.info";
	private static final URI AUTH_URL = URI.create("https://accounts.portfolio-performance.info/oidc/auth");
	private static final URI TOKEN_URL = URI.create("https://accounts.portfolio-performance.info/oidc/token");
	private static final URI REVOKE_URL = URI.create("https://accounts.portfolio-performance.info/oidc/token/revocation");
	private static final String CLIENT_ID = "d6d0voq1w081sxty0qq7a";
	private static final String SCOPES = "openid offline_access";
	private static final long EXPIRY_MARGIN_SECONDS = 30L;

	private final PortfolioPerformanceTokenStore store;
	private final HttpClient http;

	public PortfolioPerformanceOAuthClient(PortfolioPerformanceTokenStore store)
	{
		this.store = store;
		this.http = PortfolioPerformanceHttp.createClient();
	}

	public boolean hasSession() { return store.hasSession(); }

	public synchronized void reconnect(BooleanSupplier cancelled) throws Exception
	{
		connect(cancelled);
	}

	public synchronized String getAccessToken(BooleanSupplier cancelled) throws Exception
	{
		PortfolioPerformanceSession session = store.load();
		if (session == null || session.getRefreshToken() == null || session.getRefreshToken().isBlank())
		{
			connect(cancelled);
			session = store.load();
		}
		if (session.getAccessToken() != null
				&& session.getExpiresAtEpochSecond() > Instant.now().getEpochSecond() + EXPIRY_MARGIN_SECONDS)
			return session.getAccessToken();
		return refresh(session);
	}

	public synchronized String refreshAfterUnauthorized() throws Exception
	{
		PortfolioPerformanceSession session = store.load();
		if (session == null || session.getRefreshToken() == null || session.getRefreshToken().isBlank())
			throw new ApplicationException("Portfolio Performance ist nicht verbunden.");
		session.setAccessToken(null);
		session.setExpiresAtEpochSecond(0L);
		store.save(session);
		return refresh(session);
	}

	public synchronized void disconnect() throws Exception
	{
		PortfolioPerformanceSession session = store.load();
		try
		{
			if (session != null && session.getRefreshToken() != null && !session.getRefreshToken().isBlank())
				revoke(form(Map.of("client_id", CLIENT_ID, "token", session.getRefreshToken())));
		}
		catch (Exception e)
		{
			Logger.warn("Portfolio-Performance-Token konnte nicht widerrufen werden: " + e.getMessage());
		}
		finally
		{
			store.clear();
		}
	}

	private void revoke(String body) throws Exception
	{
		HttpRequest request = HttpRequest.newBuilder(PortfolioPerformanceHttp.trustedUri(REVOKE_URL.toString()))
				.timeout(PortfolioPerformanceHttp.TIMEOUT)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
		HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (response.statusCode() < 200 || response.statusCode() >= 300)
			throw new ApplicationException("Portfolio-Performance-Abmeldung fehlgeschlagen (HTTP "
					+ response.statusCode() + ").");
	}

	private void connect(BooleanSupplier cancelled) throws Exception
	{
		CallbackServer callback = CallbackServer.start();
		try
		{
			String verifier = randomUrlSafe(96).substring(0, 128);
			String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
					MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
			String state = randomUrlSafe(32);
			String authorizationUrl = AUTH_URL + "?" + form(Map.of(
					"response_type", "code",
					"prompt", "login consent",
					"code_challenge", challenge,
					"code_challenge_method", "S256",
					"client_id", CLIENT_ID,
					"redirect_uri", callback.redirectUri,
					"scope", SCOPES,
					"state", state));

			GUI.startSync(() -> {
				if (!Program.launch(authorizationUrl))
					Logger.error("Der Browser für die Portfolio-Performance-Anmeldung konnte nicht geöffnet werden.");
			});

			Map<String,String> values = callback.await(cancelled);
			if (values.containsKey("error"))
				throw new ApplicationException("Portfolio-Performance-Anmeldung abgelehnt: "
						+ values.getOrDefault("error_description", values.get("error")));
			if (!state.equals(values.get("state")))
				throw new ApplicationException("Portfolio-Performance-Anmeldung abgebrochen: OAuth-Status stimmt nicht überein.");
			String code = values.get("code");
			if (code == null || code.isBlank())
				throw new ApplicationException("Portfolio-Performance-Anmeldung lieferte keinen Autorisierungscode.");

			JsonNode token = postForm(TOKEN_URL, form(Map.of(
					"grant_type", "authorization_code",
					"client_id", CLIENT_ID,
					"code", code,
					"code_verifier", verifier,
					"redirect_uri", callback.redirectUri)));
			PortfolioPerformanceSession session = new PortfolioPerformanceSession();
			session.setRefreshToken(requiredText(token, "refresh_token"));
			session.setIdToken(token.path("id_token").asText(null));
			store.save(session);
		}
		finally
		{
			callback.close();
		}
	}

	private String refresh(PortfolioPerformanceSession session) throws Exception
	{
		JsonNode token;
		try
		{
			token = postForm(TOKEN_URL, form(Map.of(
					"grant_type", "refresh_token",
					"client_id", CLIENT_ID,
					"refresh_token", session.getRefreshToken(),
					"resource", RESOURCE)));
		}
		catch (Exception e)
		{
			throw new ApplicationException("Die Portfolio-Performance-Sitzung konnte nicht erneuert werden. Bitte neu verbinden.", e);
		}
		session.setAccessToken(requiredText(token, "access_token"));
		session.setExpiresAtEpochSecond(Instant.now().getEpochSecond() + Math.max(1L, token.path("expires_in").asLong(300L)));
		String refreshToken = token.path("refresh_token").asText(null);
		if (refreshToken != null && !refreshToken.isBlank()) session.setRefreshToken(refreshToken);
		String idToken = token.path("id_token").asText(null);
		if (idToken != null && !idToken.isBlank()) session.setIdToken(idToken);
		session.setScope(token.path("scope").asText(session.getScope()));
		store.save(session);
		return session.getAccessToken();
	}

	private JsonNode postForm(URI uri, String body) throws Exception
	{
		HttpRequest request = HttpRequest.newBuilder(PortfolioPerformanceHttp.trustedUri(uri.toString()))
				.timeout(PortfolioPerformanceHttp.TIMEOUT)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header("Accept", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
		HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		JsonNode json;
		try { json = PortfolioPerformanceHttp.JSON.readTree(response.body()); }
		catch (Exception e) { throw new ApplicationException("Portfolio Performance lieferte keine gültige JSON-Antwort.", e); }
		if (response.statusCode() < 200 || response.statusCode() >= 300)
			throw new ApplicationException("Portfolio-Performance-Tokenabruf fehlgeschlagen (HTTP " + response.statusCode()
					+ "): " + json.path("error_description").asText(json.path("error").asText("unbekannter Fehler")));
		return json;
	}

	private static String requiredText(JsonNode node, String field) throws ApplicationException
	{
		String value = node.path(field).asText(null);
		if (value == null || value.isBlank())
			throw new ApplicationException("Portfolio-Performance-Antwort enthält kein Feld '" + field + "'.");
		return value;
	}

	private static String randomUrlSafe(int bytes)
	{
		byte[] data = new byte[bytes];
		new SecureRandom().nextBytes(data);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
	}

	private static String form(Map<String,String> values)
	{
		return values.entrySet().stream().map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
				+ URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
	}

	private static final class CallbackServer implements AutoCloseable
	{
		private static final int[] PORTS = { 49968, 55968, 59968 };
		final HttpServer server;
		final String redirectUri;
		final CompletableFuture<Map<String,String>> result = new CompletableFuture<Map<String,String>>();
		private final java.util.concurrent.ExecutorService executor;

		private CallbackServer(HttpServer server, java.util.concurrent.ExecutorService executor)
		{
			this.server = server;
			this.executor = executor;
			this.redirectUri = "http://localhost:" + server.getAddress().getPort() + "/success";
		}

		static CallbackServer start() throws IOException
		{
			HttpServer server = null;
			for (int port : PORTS)
			{
				try { server = HttpServer.create(new InetSocketAddress("localhost", port), 0); break; }
				catch (IOException occupied) { Logger.info("Portfolio-Performance-Callback-Port belegt: " + port); }
			}
			if (server == null)
				throw new IOException("Keiner der Portfolio-Performance-Callback-Ports ist verfügbar.");
			java.util.concurrent.ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
				Thread thread = new Thread(r, "portfolio-performance-oauth-callback");
				thread.setDaemon(true);
				return thread;
			});
			CallbackServer callback = new CallbackServer(server, executor);
			server.createContext("/success", callback::handle);
			server.setExecutor(executor);
			server.start();
			return callback;
		}

		Map<String,String> await(BooleanSupplier cancelled) throws Exception
		{
			while (true)
			{
				if (cancelled != null && cancelled.getAsBoolean())
					throw new OperationCanceledException("Portfolio-Performance-Anmeldung abgebrochen");
				try { return result.get(500L, TimeUnit.MILLISECONDS); }
				catch (TimeoutException ignored) {}
				catch (ExecutionException e)
				{
					Throwable cause = e.getCause();
					if (cause instanceof Exception) throw (Exception) cause;
					throw e;
				}
			}
		}

		private void handle(HttpExchange exchange) throws IOException
		{
			Map<String,String> values = parseQuery(exchange.getRequestURI().getRawQuery());
			String message = values.containsKey("error") ? "Anmeldung abgebrochen."
					: "Login erfolgreich. Dieses Fenster kann geschlossen werden.";
			byte[] body = ("<!doctype html><html><head><meta charset=\"utf-8\"><title>Portfolio Performance</title></head>"
					+ "<body><h1>" + message + "</h1></body></html>").getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
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

		@Override public void close()
		{
			server.stop(0);
			executor.shutdownNow();
		}
	}
}
