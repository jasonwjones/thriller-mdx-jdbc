package com.jasonwjones.hyperion.thriller.parser;

import java.util.Properties;

public class ParsedQuery {

	private String query;
	
	private Properties properties;

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
