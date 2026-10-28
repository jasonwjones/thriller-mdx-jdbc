package com.jasonwjones.hyperion.thriller;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jasonwjones.hyperion.thriller.parser.QueryParser;
import com.jasonwjones.hyperion.thriller.table.ThrillerColumn;

/**
 * The queries tools and connection pools send to check that a connection still
 * works: a single literal, such as {@code SELECT 1}, or IntelliJ's
 * {@code select 'keep alive'}, perhaps with an alias, a dummy table such as
 * DUAL, or a comment. They aren't MDX and name no cube, so rather than fail,
 * Thriller checks the server still answers and returns the literal.
 */
final class ValidationQuery {

	private static final String LITERAL = "(\\d+|'(?:[^']|'')*')";

	private static final Pattern QUERY = Pattern.compile(
			"\\s*(?:SELECT\\s+" + LITERAL + "(?:\\s+(?:AS\\s+)?(\\w+))?(?:\\s+FROM\\s+(?:DUAL|DUMMY|SYSIBM\\.SYSDUMMY1))?"
					+ "|VALUES\\s*\\(?\\s*" + LITERAL + "\\s*\\)?)\\s*;?\\s*",
			Pattern.CASE_INSENSITIVE);

	private ValidationQuery() {}

	private static Matcher matcher(String sql) {
		return sql == null ? null : QUERY.matcher(QueryParser.withoutComments(sql));
	}

	/**
	 * @param sql a query
	 * @return whether it's one of the queries tools use to check a connection
	 */
	static boolean matches(String sql) {
		Matcher matcher = matcher(sql);
		return matcher != null && matcher.matches();
	}

	/**
	 * @param statement the statement that ran the query
	 * @param sql the query, which {@link #matches} accepts
	 * @return one row with one column, holding the query's literal
	 */
	static ResultSet result(Statement statement, String sql) throws SQLException {
		Matcher matcher = matcher(sql);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Not a validation query: " + sql);
		}
		String literal = matcher.group(1) != null ? matcher.group(1) : matcher.group(3);
		Object value;
		ThrillerColumn column;
		if (literal.startsWith("'")) {
			value = literal.substring(1, literal.length() - 1).replace("''", "'");
			column = new ThrillerColumn(Types.VARCHAR, Math.max(1, value.toString().length()));
		} else {
			value = Integer.valueOf(literal);
			column = new ThrillerColumn(Types.INTEGER, literal.length());
		}
		column.setColumnName(matcher.group(2) != null ? matcher.group(2) : literal);
		return new ObjectResultSet(statement, Collections.singletonList(new Object[] { value }),
				new ThrillerResultSetMetaData(new ThrillerColumn[] { column }));
	}

}
