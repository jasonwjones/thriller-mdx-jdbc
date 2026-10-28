package com.jasonwjones.hyperion.thriller;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A Thriller JDBC URL: the driver prefix, then the Essbase server's address as
 * you would open it in a browser, then optional properties after semicolons.
 *
 * <pre>
 * jdbc:essbase:thriller:http://localhost:9000/essbase;app=Sample;cube=Basic
 * </pre>
 *
 * The application and cube are a default for queries whose FROM clause doesn't
 * name one, so they can be left off.
 */
public final class ThrillerUrl {

	public static final String PREFIX = "jdbc:essbase:thriller:";

	private final String url;

	private final String serverUrl;

	private final Map<String, String> properties;

	private ThrillerUrl(String url, String serverUrl, Map<String, String> properties) {
		this.url = url;
		this.serverUrl = serverUrl;
		this.properties = Collections.unmodifiableMap(properties);
	}

	public static boolean accepts(String url) {
		return url != null && url.startsWith(PREFIX);
	}

	public static ThrillerUrl parse(String url) throws ThrillerException {
		if (!accepts(url)) {
			throw new ThrillerException("Not a Thriller URL: " + url);
		}
		String[] parts = url.substring(PREFIX.length()).split(";");
		String serverUrl = parts[0].trim();
		if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
			throw new ThrillerException("A Thriller URL names the Essbase server's web address, as in " + PREFIX
					+ "http://localhost:9000/essbase, but this one has '" + serverUrl + "'");
		}
		while (serverUrl.endsWith("/")) {
			serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
		}

		Map<String, String> properties = new LinkedHashMap<>();
		for (int index = 1; index < parts.length; index++) {
			String part = parts[index].trim();
			if (part.isEmpty()) {
				continue;
			}
			int equals = part.indexOf('=');
			if (equals < 1) {
				throw new ThrillerException("Expected key=value after a ';' in " + url + ", but found '" + part + "'");
			}
			properties.put(part.substring(0, equals).trim(), part.substring(equals + 1).trim());
		}
		return new ThrillerUrl(url, serverUrl, properties);
	}

	/**
	 * @return the URL this was parsed from, as given
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @return the Essbase server's address, such as http://localhost:9000/essbase
	 */
	public String getServerUrl() {
		return serverUrl;
	}

	/**
	 * @return the default application, or null if the URL doesn't give one
	 */
	public String getApplication() {
		return properties.get("app");
	}

	/**
	 * @return the default cube, or null if the URL doesn't give one
	 */
	public String getCube() {
		return properties.get("cube");
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	@Override
	public String toString() {
		return PREFIX + serverUrl + properties;
	}

}
