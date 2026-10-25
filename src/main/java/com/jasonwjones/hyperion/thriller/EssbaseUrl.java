package com.jasonwjones.hyperion.thriller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EssbaseUrl {

	private final String username;
	private final String password;
	private final String hostname;
	private final int port;
	private final String app;
	private final String database;

	private final boolean embedded;
	private final Map<String, String> properties;
	
	EssbaseUrl(String username, String password, String hostname, int port, String app, String database, boolean embedded, Map<String, String> properties) {
		this.username = username;
		this.password = password;
		this.hostname = hostname;
		this.port = port;
		this.app = app;
		this.database = database;
		this.embedded = embedded;
		this.properties = Collections.unmodifiableMap(properties);
	}

	public static EssbaseUrl parse(final String url) {
		return parse(url, new Properties());
	}
	
	public static EssbaseUrl parse(final String url, Properties properties) {

		final String hostname;
		final int port;
		final boolean embedded;
		final String params;
		
		final Pattern p = Pattern
				.compile("^jdbc:(essbase|essbase:thriller)://((\\w+)(:(\\w*))?@)?([^/:;]+)(:(\\d+))?(/([^;]*))?(([;]+)(.+))?");
		final Matcher m = p.matcher(url);
	
		String username = properties.getProperty("user", "");
		String password = properties.getProperty("password", "");
		
		// For now, this driver always assumes it's being used in embedded mode
		if (m.find()) {
			System.out.println(m.group(1));
			if (m.group(1).equals("essbase")) {
				embedded = true;
			} else if (m.group(1).equals("essbase:thriller")) {
				embedded = true;
			} else {
				return null;
			}
			
			if (m.group(3) != null) {
				username = m.group(3);
			}
			
			if (m.group(5) != null) {
				password = m.group(5);
			}
			
			hostname = (m.group(6) == null ? "" : m.group(6));
						
			if (m.group(8) != null) {
				port = Integer.parseInt(m.group(8));
			} else {
				port = 1433;
			}

			String app = null;
			String db = null;
			
			String appDb = m.group(10);
			
			if (appDb.indexOf(".") != -1) {
				String[] appAndDb = appDb.split("\\.");
				app = appAndDb[0];
				db = appAndDb[1];
			// also allow Jason's more modern "slash" syntax for separating app/cube
			} else if (appDb.indexOf("/") != -1) {
				String[] appAndDb = appDb.split("\\/");
				app = appAndDb[0];
				db = appAndDb[1];				
			}
			
			params = (m.group(13)) == null ? "" : m.group(13);
			Map<String, String> props = parseProperties(params);
			
			return new EssbaseUrl(username, password, hostname, port, app, db, embedded, props);
		} else {
			return null;
		}
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getHostname() {
		return hostname;
	}

	public int getPort() {
		return port;
	}

	public String getApplication() {
		return app;
	}
	
	public String getDatabase() {
		return database;
	}

	public boolean isEmbedded() {
		return embedded;
	}
	
	public Map<String, String> getProperties() {
		return properties;
	}
	
	public static Map<String, String> parseProperties(String properties) {
		if (properties == null || properties.trim().length() == 0) return Collections.emptyMap();
		Map<String, String> props = new HashMap<String, String>();
		for (String propPair : properties.split(";")) {
			String[] keyAndValue = propPair.split("=");
			props.put(keyAndValue[0], keyAndValue[1]);
		}
		return props;
	}
	
	
}
