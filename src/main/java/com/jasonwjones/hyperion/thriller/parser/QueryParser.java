package com.jasonwjones.hyperion.thriller.parser;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryParser {

	public static final String OPTION_PREFIX = "--";
	
	public ParsedQuery parse(String query) {
				
		Pattern commentPattern = Pattern.compile("\\/\\*(.+?)\\*\\/", Pattern.DOTALL);
		Matcher matcher = commentPattern.matcher(query);

		Properties properties = new Properties();
		
		while (matcher.find()) {	
			String tokenDefinition = matcher.group(1);
			for (String line : tokenDefinition.split("\n")) {
				String processedLine = line.trim();
				if (processedLine.startsWith("*")) {
					processedLine = processedLine.substring(1).trim();
				}
				if (processedLine.startsWith(OPTION_PREFIX)) {
					String optionText = processedLine.substring(OPTION_PREFIX.length());
					if (optionText.contains("=")) {
						String[] keyAndValue = optionText.split("=");
						properties.setProperty(keyAndValue[0].trim(), keyAndValue[1].trim());
					}
				}
			}
			
		}
		return new ParsedQuery(query, properties);
	}

}
