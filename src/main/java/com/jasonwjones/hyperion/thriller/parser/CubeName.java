package com.jasonwjones.hyperion.thriller.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The application and cube an MDX query's FROM clause names, as in
 * {@code FROM Sample.Basic} or {@code FROM [Sample].[Basic]}.
 */
public final class CubeName {

	private static final Pattern COMMENTS = Pattern.compile("/\\*.*?\\*/|//[^\\n]*|--[^\\n]*", Pattern.DOTALL);

	private static final String NAME = "(?:\\[([^\\]]+)\\]|([A-Za-z0-9_]+))";

	private static final Pattern FROM = Pattern.compile("\\bFROM\\s+" + NAME + "\\s*\\.\\s*" + NAME, Pattern.CASE_INSENSITIVE);

	private final String application;

	private final String cube;

	public CubeName(String application, String cube) {
		this.application = application;
		this.cube = cube;
	}

	/**
	 * Finds the first FROM clause that names an application and cube, outside
	 * of comments. A subselect's FROM ( ... ) is passed over for the one inside.
	 *
	 * @param query the MDX query
	 * @return the cube it reads from, or null if it doesn't name one
	 */
	public static CubeName fromQuery(String query) {
		Matcher matcher = FROM.matcher(COMMENTS.matcher(query).replaceAll(" "));
		if (matcher.find()) {
			return new CubeName(either(matcher.group(1), matcher.group(2)), either(matcher.group(3), matcher.group(4)));
		}
		return null;
	}

	private static String either(String bracketed, String plain) {
		return bracketed != null ? bracketed : plain;
	}

	public String getApplication() {
		return application;
	}

	public String getCube() {
		return cube;
	}

	@Override
	public String toString() {
		return application + "." + cube;
	}

}
