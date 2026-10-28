package com.jasonwjones.hyperion.thriller.parser;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a query's hints: lines starting with -- inside a block comment, as in
 *
 * <pre>
 * SELECT /&#42; -- members = alias &#42;/ {[Jan]} ON COLUMNS FROM Sample.Basic
 * </pre>
 *
 * A leading * on a line is skipped, so a Javadoc-style block works too.
 */
public class QueryParser {

	public static final String OPTION_PREFIX = "--";

	private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*(.+?)\\*/", Pattern.DOTALL);

	/**
	 * @param query the query as written
	 * @return the query and its hints
	 */
	public ParsedQuery parse(String query) {
		Properties properties = new Properties();
		Matcher matcher = BLOCK_COMMENT.matcher(query);
		while (matcher.find()) {
			for (String line : matcher.group(1).split("\n")) {
				String hint = line.trim();
				if (hint.startsWith("*")) {
					hint = hint.substring(1).trim();
				}
				if (hint.startsWith(OPTION_PREFIX) && hint.contains("=")) {
					String[] keyAndValue = hint.substring(OPTION_PREFIX.length()).split("=", 2);
					properties.setProperty(keyAndValue[0].trim(), keyAndValue[1].trim());
				}
			}
		}
		return new ParsedQuery(query, properties);
	}

	/**
	 * The query with each block comment blanked out, for sending to Essbase.
	 *
	 * Essbase stops reading a query at its first semicolon, even one inside a
	 * comment, and then complains that the comment never ends. The hints have
	 * already been read by then, so the comments can go. Each is replaced with
	 * spaces, keeping its line breaks, so that the line numbers in Essbase's
	 * errors still match the query as written. Comment markers inside a quoted
	 * string or a bracketed name are left alone.
	 *
	 * @param query the query as written
	 * @return the query without its block comments
	 */
	public static String withoutComments(String query) {
		StringBuilder mdx = new StringBuilder(query.length());
		int index = 0;
		while (index < query.length()) {
			char c = query.charAt(index);
			if (c == '/' && index + 1 < query.length() && query.charAt(index + 1) == '*') {
				int end = query.indexOf("*/", index + 2);
				end = end < 0 ? query.length() : end + 2;
				for (int blank = index; blank < end; blank++) {
					mdx.append(query.charAt(blank) == '\n' ? '\n' : ' ');
				}
				index = end;
			} else if (c == '"' || c == '\'' || c == '[') {
				int end = query.indexOf(c == '[' ? ']' : c, index + 1);
				end = end < 0 ? query.length() : end + 1;
				mdx.append(query, index, end);
				index = end;
			} else {
				mdx.append(c);
				index++;
			}
		}
		return mdx.toString();
	}

}
