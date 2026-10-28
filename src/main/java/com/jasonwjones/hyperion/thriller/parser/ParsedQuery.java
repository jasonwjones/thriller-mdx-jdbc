package com.jasonwjones.hyperion.thriller.parser;

import java.util.Properties;

/**
 * A query as written, and the hints read from its comments.
 */
public class ParsedQuery {

	private final String query;

	private final Properties properties;

	public ParsedQuery(String query, Properties properties) {
		this.query = query;
		this.properties = properties;
	}

	public Properties getProperties() {
		return properties;
	}

	public String getQuery() {
		return query;
	}

}
