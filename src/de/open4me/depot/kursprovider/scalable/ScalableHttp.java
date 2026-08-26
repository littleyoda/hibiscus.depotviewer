package de.open4me.depot.kursprovider.scalable;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.willuhn.jameica.system.Application;

final class ScalableHttp
{
	static final ObjectMapper JSON = new ObjectMapper();
	static final Duration TIMEOUT = Duration.ofSeconds(30);

	private ScalableHttp() {}

	static HttpClient createClient()
	{
		HttpClient.Builder builder = HttpClient.newBuilder()
				.connectTimeout(TIMEOUT)
				.followRedirects(HttpClient.Redirect.NORMAL);

		if (Application.getConfig().getUseSystemProxy())
		{
			ProxySelector selector = ProxySelector.getDefault();
			if (selector != null)
				builder.proxy(selector);
		}
		else
		{
			String host = Application.getConfig().getHttpsProxyHost();
			int port = Application.getConfig().getHttpsProxyPort();
			if (host != null && !host.isBlank() && port > 0)
				builder.proxy(ProxySelector.of(new InetSocketAddress(host, port)));
		}
		return builder.build();
	}

	static URI trustedUri(String value) throws Exception
	{
		URI uri = new URI(value);
		if (!"https".equalsIgnoreCase(uri.getScheme()) || !"mcp.scalable.capital".equalsIgnoreCase(uri.getHost()))
			throw new Exception("Nicht vertrauenswürdiger Scalable-Endpunkt: " + uri);
		return uri;
	}
}
