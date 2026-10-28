package com.jasonwjones.hyperion.thriller;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * A query with ? in place of members or numbers, filled in when it runs:
 *
 * <pre>
 * PreparedStatement statement = connection.prepareStatement(
 *     "SELECT {[Jan]} ON COLUMNS, {?} ON ROWS FROM Sample.Basic WHERE ([Sales], ?)");
 * statement.setString(1, "Cola");
 * statement.setString(2, "Actual");
 * </pre>
 *
 * A string becomes a member name in brackets, [Cola], with any ] in it
 * doubled, and a number becomes itself. A ? inside a comment, a quoted string
 * or a bracketed name is left alone.
 */
public class ThrillerPreparedStatement extends ThrillerStatement implements PreparedStatement {

	private final String query;

	/** Where each ? is in the query */
	private final List<Integer> markers;

	/** The MDX for each parameter, or null for one not yet set */
	private final String[] values;

	ThrillerPreparedStatement(ThrillerConnection connection, String sql) {
		super(connection);
		this.query = sql;
		this.markers = markers(sql);
		this.values = new String[markers.size()];
	}

	/**
	 * Finds each ? outside of comments, quoted strings and bracketed names.
	 */
	static List<Integer> markers(String query) {
		List<Integer> markers = new ArrayList<>();
		int index = 0;
		while (index < query.length()) {
			char c = query.charAt(index);
			char next = index + 1 < query.length() ? query.charAt(index + 1) : 0;
			int end;
			if (c == '/' && next == '*') {
				end = query.indexOf("*/", index + 2);
				end = end < 0 ? query.length() : end + 2;
			} else if ((c == '/' && next == '/') || (c == '-' && next == '-')) {
				end = query.indexOf('\n', index);
				end = end < 0 ? query.length() : end + 1;
			} else if (c == '"' || c == '\'' || c == '[') {
				end = query.indexOf(c == '[' ? ']' : c, index + 1);
				end = end < 0 ? query.length() : end + 1;
			} else {
				if (c == '?') {
					markers.add(index);
				}
				end = index + 1;
			}
			index = end;
		}
		return markers;
	}

	/**
	 * The query with each ? replaced by its parameter.
	 */
	String bound() throws SQLException {
		StringBuilder bound = new StringBuilder(query.length());
		int last = 0;
		for (int parameter = 0; parameter < markers.size(); parameter++) {
			if (values[parameter] == null) {
				throw new SQLException("Parameter " + (parameter + 1) + " has no value");
			}
			int marker = markers.get(parameter);
			bound.append(query, last, marker).append(values[parameter]);
			last = marker + 1;
		}
		return bound.append(query.substring(last)).toString();
	}

	private void set(int parameterIndex, String mdx) throws SQLException {
		checkIndex(parameterIndex);
		values[parameterIndex - 1] = mdx;
	}

	private void checkIndex(int parameterIndex) throws SQLException {
		if (parameterIndex < 1 || parameterIndex > markers.size()) {
			throw new SQLException("There's no parameter " + parameterIndex + ": the query has " + markers.size());
		}
	}

	/**
	 * A member name in brackets, as MDX writes one.
	 */
	static String member(String name) {
		return "[" + name.replace("]", "]]") + "]";
	}

	private static String number(Number number) {
		if (number instanceof BigDecimal) {
			return ((BigDecimal) number).toPlainString();
		}
		if (number instanceof Double || number instanceof Float) {
			return BigDecimal.valueOf(number.doubleValue()).toPlainString();
		}
		return number.toString();
	}

	private SQLException unsupported(int parameterIndex, String what) throws SQLException {
		checkIndex(parameterIndex);
		return new SQLFeatureNotSupportedException("Parameter " + parameterIndex + " can't be " + what
				+ ": a parameter is a member name, as a string, or a number");
	}

	@Override
	public ResultSet executeQuery() throws SQLException {
		return super.executeQuery(bound());
	}

	@Override
	public boolean execute() throws SQLException {
		return super.execute(bound());
	}

	@Override
	public int executeUpdate() throws SQLException {
		throw new SQLFeatureNotSupportedException("Thriller only runs queries");
	}

	@Override
	public void addBatch() throws SQLException {
		throw new SQLFeatureNotSupportedException("Thriller only runs queries, so it has no batches");
	}

	@Override
	public void clearParameters() throws SQLException {
		Arrays.fill(values, null);
	}

	@Override
	public void setString(int parameterIndex, String x) throws SQLException {
		if (x == null) {
			setNull(parameterIndex, Types.VARCHAR);
		}
		set(parameterIndex, member(x));
	}

	@Override
	public void setNString(int parameterIndex, String value) throws SQLException {
		setString(parameterIndex, value);
	}

	@Override
	public void setByte(int parameterIndex, byte x) throws SQLException {
		set(parameterIndex, Byte.toString(x));
	}

	@Override
	public void setShort(int parameterIndex, short x) throws SQLException {
		set(parameterIndex, Short.toString(x));
	}

	@Override
	public void setInt(int parameterIndex, int x) throws SQLException {
		set(parameterIndex, Integer.toString(x));
	}

	@Override
	public void setLong(int parameterIndex, long x) throws SQLException {
		set(parameterIndex, Long.toString(x));
	}

	@Override
	public void setFloat(int parameterIndex, float x) throws SQLException {
		set(parameterIndex, number(x));
	}

	@Override
	public void setDouble(int parameterIndex, double x) throws SQLException {
		set(parameterIndex, number(x));
	}

	@Override
	public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
		if (x == null) {
			setNull(parameterIndex, Types.DECIMAL);
		}
		set(parameterIndex, x.toPlainString());
	}

	@Override
	public void setObject(int parameterIndex, Object x) throws SQLException {
		if (x == null) {
			setNull(parameterIndex, Types.NULL);
		} else if (x instanceof Number) {
			set(parameterIndex, number((Number) x));
		} else if (x instanceof CharSequence || x instanceof Character) {
			setString(parameterIndex, x.toString());
		} else {
			throw unsupported(parameterIndex, "a " + x.getClass().getSimpleName());
		}
	}

	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
		setObject(parameterIndex, x);
	}

	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
		setObject(parameterIndex, x);
	}

	@Override
	public void setNull(int parameterIndex, int sqlType) throws SQLException {
		throw unsupported(parameterIndex, "null, which MDX has no way to write");
	}

	@Override
	public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
		setNull(parameterIndex, sqlType);
	}

	@Override
	public void setBoolean(int parameterIndex, boolean x) throws SQLException {
		throw unsupported(parameterIndex, "a boolean");
	}

	@Override
	public void setBytes(int parameterIndex, byte[] x) throws SQLException {
		throw unsupported(parameterIndex, "bytes");
	}

	@Override
	public void setDate(int parameterIndex, Date x) throws SQLException {
		throw unsupported(parameterIndex, "a date");
	}

	@Override
	public void setTime(int parameterIndex, Time x) throws SQLException {
		throw unsupported(parameterIndex, "a time");
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
		throw unsupported(parameterIndex, "a timestamp");
	}

	@Override
	public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
		setDate(parameterIndex, x);
	}

	@Override
	public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
		setTime(parameterIndex, x);
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
		setTimestamp(parameterIndex, x);
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
		throw unsupported(parameterIndex, "a stream");
	}

	@Override
	@Deprecated
	public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
		throw unsupported(parameterIndex, "a stream");
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
		throw unsupported(parameterIndex, "a stream");
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
		throw unsupported(parameterIndex, "a stream");
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
		throw unsupported(parameterIndex, "a stream");
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
		throw unsupported(parameterIndex, "a stream");
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
		throw unsupported(parameterIndex, "a stream");
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
		throw unsupported(parameterIndex, "a stream");
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
		throw unsupported(parameterIndex, "a stream");
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
		throw unsupported(parameterIndex, "a stream");
	}

	@Override
	public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
		throw unsupported(parameterIndex, "a stream");
	}

	@Override
	public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
		throw unsupported(parameterIndex, "a stream");
	}

	@Override
	public void setRef(int parameterIndex, Ref x) throws SQLException {
		throw unsupported(parameterIndex, "a REF");
	}

	@Override
	public void setBlob(int parameterIndex, Blob x) throws SQLException {
		throw unsupported(parameterIndex, "a BLOB");
	}

	@Override
	public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
		throw unsupported(parameterIndex, "a BLOB");
	}

	@Override
	public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
		throw unsupported(parameterIndex, "a BLOB");
	}

	@Override
	public void setClob(int parameterIndex, Clob x) throws SQLException {
		throw unsupported(parameterIndex, "a CLOB");
	}

	@Override
	public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
		throw unsupported(parameterIndex, "a CLOB");
	}

	@Override
	public void setClob(int parameterIndex, Reader reader) throws SQLException {
		throw unsupported(parameterIndex, "a CLOB");
	}

	@Override
	public void setNClob(int parameterIndex, NClob value) throws SQLException {
		throw unsupported(parameterIndex, "an NCLOB");
	}

	@Override
	public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
		throw unsupported(parameterIndex, "an NCLOB");
	}

	@Override
	public void setNClob(int parameterIndex, Reader reader) throws SQLException {
		throw unsupported(parameterIndex, "an NCLOB");
	}

	@Override
	public void setArray(int parameterIndex, Array x) throws SQLException {
		throw unsupported(parameterIndex, "an array");
	}

	@Override
	public void setURL(int parameterIndex, URL x) throws SQLException {
		throw unsupported(parameterIndex, "a URL");
	}

	@Override
	public void setRowId(int parameterIndex, RowId x) throws SQLException {
		throw unsupported(parameterIndex, "a ROWID");
	}

	@Override
	public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
		throw unsupported(parameterIndex, "XML");
	}

	/**
	 * Null, as JDBC allows: the columns aren't known until the query runs.
	 */
	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return null;
	}

	@Override
	public ParameterMetaData getParameterMetaData() throws SQLException {
		int count = markers.size();
		return new ParameterMetaData() {

			private void check(int param) throws SQLException {
				if (param < 1 || param > count) {
					throw new SQLException("There's no parameter " + param + ": the query has " + count);
				}
			}

			@Override
			public int getParameterCount() {
				return count;
			}

			@Override
			public int isNullable(int param) throws SQLException {
				check(param);
				return parameterNoNulls;
			}

			@Override
			public boolean isSigned(int param) throws SQLException {
				check(param);
				return false;
			}

			@Override
			public int getPrecision(int param) throws SQLException {
				check(param);
				return 0;
			}

			@Override
			public int getScale(int param) throws SQLException {
				check(param);
				return 0;
			}

			@Override
			public int getParameterType(int param) throws SQLException {
				check(param);
				return Types.VARCHAR;
			}

			@Override
			public String getParameterTypeName(int param) throws SQLException {
				check(param);
				return "VARCHAR";
			}

			@Override
			public String getParameterClassName(int param) throws SQLException {
				check(param);
				return String.class.getName();
			}

			@Override
			public int getParameterMode(int param) throws SQLException {
				check(param);
				return parameterModeIn;
			}

			@Override
			public <T> T unwrap(Class<T> iface) throws SQLException {
				throw new SQLException("Not a wrapper for " + iface.getName());
			}

			@Override
			public boolean isWrapperFor(Class<?> iface) {
				return false;
			}

		};
	}

}
