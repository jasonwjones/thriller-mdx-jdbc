package com.jasonwjones.hyperion.thriller;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A simple ResultSet implementation that maps a List of Object arrays and given
 * metadata in to a ResultSet.
 *
 * @author jasonwjones
 *
 */
/**
 * A result held in memory, as a list of rows. Thriller's results arrive whole
 * from one REST call, so moving around them is cheap, but the result set is
 * still reported as forward-only, which is all most tools need.
 */
public class ObjectResultSet implements ResultSet {

	private final Statement statement;

	private List<Object[]> data;

	private final ResultSetMetaData metaData;

	private final Map<String, Integer> columnLookups = new HashMap<>();

	private int position = -1;

	private boolean wasNull;

	public ObjectResultSet(Statement statement, List<Object[]> data, ResultSetMetaData metaData) throws SQLException {
		this.statement = statement;
		this.data = data;
		this.metaData = metaData;
		// Walked backwards so that, of two columns with the same name, the first wins
		for (int columnIndex = metaData.getColumnCount(); columnIndex >= 1; columnIndex--) {
			columnLookups.put(metaData.getColumnLabel(columnIndex).toUpperCase(Locale.ROOT), columnIndex);
		}
	}

	public ObjectResultSet(List<Object[]> data, ResultSetMetaData metaData) throws SQLException {
		this(null, data, metaData);
	}

	/**
	 * The value in a column of the current row, noting for wasNull() whether it
	 * was null.
	 */
	protected Object value(int columnIndex) throws SQLException {
		checkOpen();
		if (position < 0 || position >= data.size()) {
			throw new SQLException("There is no current row");
		}
		if (columnIndex < 1 || columnIndex > metaData.getColumnCount()) {
			throw new SQLException("Invalid column requested: " + columnIndex);
		}
		Object[] row = data.get(position);
		Object value = columnIndex <= row.length ? row[columnIndex - 1] : null;
		wasNull = value == null;
		return value;
	}

	protected Number number(int columnIndex) throws SQLException {
		Object value = value(columnIndex);
		if (value == null || value instanceof Number) {
			return (Number) value;
		}
		try {
			return new BigDecimal(value.toString().trim());
		} catch (NumberFormatException e) {
			throw new SQLException("Column " + columnIndex + " holds '" + value + "', which is not a number");
		}
	}

	private void checkOpen() throws SQLException {
		if (data == null) {
			throw new SQLException("This result set has been closed");
		}
	}

	private boolean onRow() {
		return position >= 0 && position < data.size();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (isWrapperFor(iface)) {
			return iface.cast(this);
		}
		throw new SQLException("Cannot cast to " + iface.getName());
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface.isInstance(this);
	}

	@Override
	public boolean next() throws SQLException {
		checkOpen();
		if (position < data.size()) {
			position++;
		}
		return position < data.size();
	}

	@Override
	public void close() throws SQLException {
		data = null;
	}

	@Override
	public boolean wasNull() throws SQLException {
		return wasNull;
	}

	@Override
	public String getString(int columnIndex) throws SQLException {
		Object value = value(columnIndex);
		if (value instanceof BigDecimal) {
			return ((BigDecimal) value).toPlainString();
		}
		return value == null ? null : value.toString();
	}

	@Override
	public boolean getBoolean(int columnIndex) throws SQLException {
		Object value = value(columnIndex);
		if (value == null) {
			return false;
		}
		if (value instanceof Number) {
			return ((Number) value).doubleValue() != 0;
		}
		String text = value.toString().trim();
		return text.equalsIgnoreCase("true") || text.equals("1");
	}

	@Override
	public byte getByte(int columnIndex) throws SQLException {
		Number number = number(columnIndex);
		return number == null ? 0 : number.byteValue();
	}

	@Override
	public short getShort(int columnIndex) throws SQLException {
		Number number = number(columnIndex);
		return number == null ? 0 : number.shortValue();
	}

	@Override
	public int getInt(int columnIndex) throws SQLException {
		Number number = number(columnIndex);
		return number == null ? 0 : number.intValue();
	}

	@Override
	public long getLong(int columnIndex) throws SQLException {
		Number number = number(columnIndex);
		return number == null ? 0 : number.longValue();
	}

	@Override
	public float getFloat(int columnIndex) throws SQLException {
		Number number = number(columnIndex);
		return number == null ? 0 : number.floatValue();
	}

	@Override
	public double getDouble(int columnIndex) throws SQLException {
		Number number = number(columnIndex);
		return number == null ? 0 : number.doubleValue();
	}

	@Override
	public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
		BigDecimal value = getBigDecimal(columnIndex);
		return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
	}

	@Override
	public byte[] getBytes(int columnIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Date getDate(int columnIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Time getTime(int columnIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Timestamp getTimestamp(int columnIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public InputStream getAsciiStream(int columnIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public InputStream getUnicodeStream(int columnIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public InputStream getBinaryStream(int columnIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public String getString(String columnLabel) throws SQLException {
		return getString(findColumn(columnLabel));
	}

	@Override
	public boolean getBoolean(String columnLabel) throws SQLException {
		return getBoolean(findColumn(columnLabel));
	}

	@Override
	public byte getByte(String columnLabel) throws SQLException {
		return getByte(findColumn(columnLabel));
	}

	@Override
	public short getShort(String columnLabel) throws SQLException {
		return getShort(findColumn(columnLabel));
	}

	@Override
	public int getInt(String columnLabel) throws SQLException {
		return getInt(findColumn(columnLabel));
	}

	@Override
	public long getLong(String columnLabel) throws SQLException {
		return getLong(findColumn(columnLabel));
	}

	@Override
	public float getFloat(String columnLabel) throws SQLException {
		return getFloat(findColumn(columnLabel));
	}

	@Override
	public double getDouble(String columnLabel) throws SQLException {
		return getDouble(findColumn(columnLabel));
	}

	@Override
	public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
		return getBigDecimal(findColumn(columnLabel), scale);
	}

	@Override
	public byte[] getBytes(String columnLabel) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Date getDate(String columnLabel) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Time getTime(String columnLabel) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Timestamp getTimestamp(String columnLabel) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public InputStream getAsciiStream(String columnLabel) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public InputStream getUnicodeStream(String columnLabel) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public InputStream getBinaryStream(String columnLabel) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		return null;
	}

	@Override
	public void clearWarnings() throws SQLException {
	}

	@Override
	public String getCursorName() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return metaData;
	}

	@Override
	public Object getObject(int columnIndex) throws SQLException {
		return value(columnIndex);
	}

	@Override
	public Object getObject(String columnLabel) throws SQLException {
		return getObject(findColumn(columnLabel));
	}

	@Override
	public int findColumn(String columnLabel) throws SQLException {
		Integer columnIndex = columnLabel == null ? null : columnLookups.get(columnLabel.toUpperCase(Locale.ROOT));
		if (columnIndex == null) {
			throw new SQLException("No column named " + columnLabel);
		}
		return columnIndex;
	}

	@Override
	public Reader getCharacterStream(int columnIndex) throws SQLException {
		String value = getString(columnIndex);
		return value == null ? null : new StringReader(value);
	}

	@Override
	public Reader getCharacterStream(String columnLabel) throws SQLException {
		return getCharacterStream(findColumn(columnLabel));
	}

	@Override
	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		Number number = number(columnIndex);
		if (number == null || number instanceof BigDecimal) {
			return (BigDecimal) number;
		}
		if (number instanceof Double || number instanceof Float) {
			return BigDecimal.valueOf(number.doubleValue());
		}
		return BigDecimal.valueOf(number.longValue());
	}

	@Override
	public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
		return getBigDecimal(findColumn(columnLabel));
	}

	@Override
	public boolean isBeforeFirst() throws SQLException {
		checkOpen();
		return !data.isEmpty() && position < 0;
	}

	@Override
	public boolean isAfterLast() throws SQLException {
		checkOpen();
		return !data.isEmpty() && position >= data.size();
	}

	@Override
	public boolean isFirst() throws SQLException {
		checkOpen();
		return !data.isEmpty() && position == 0;
	}

	@Override
	public boolean isLast() throws SQLException {
		checkOpen();
		return !data.isEmpty() && position == data.size() - 1;
	}

	@Override
	public void beforeFirst() throws SQLException {
		checkOpen();
		position = -1;
	}

	@Override
	public void afterLast() throws SQLException {
		checkOpen();
		position = data.size();
	}

	@Override
	public boolean first() throws SQLException {
		return absolute(1);
	}

	@Override
	public boolean last() throws SQLException {
		return absolute(-1);
	}

	@Override
	public int getRow() throws SQLException {
		checkOpen();
		return onRow() ? position + 1 : 0;
	}

	@Override
	public boolean absolute(int row) throws SQLException {
		checkOpen();
		if (row > 0) {
			position = Math.min(row - 1, data.size());
		} else if (row < 0) {
			position = Math.max(data.size() + row, -1);
		} else {
			position = -1;
		}
		return onRow();
	}

	@Override
	public boolean relative(int rows) throws SQLException {
		checkOpen();
		position = Math.max(-1, Math.min(position + rows, data.size()));
		return onRow();
	}

	@Override
	public boolean previous() throws SQLException {
		return relative(-1);
	}

	@Override
	public int getHoldability() throws SQLException {
		return ResultSet.CLOSE_CURSORS_AT_COMMIT;
	}

	@Override
	public boolean isClosed() throws SQLException {
		return data == null;
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException {
	}

	@Override
	public int getFetchDirection() throws SQLException {
		return ResultSet.FETCH_FORWARD;
	}

	@Override
	public void setFetchSize(int rows) throws SQLException {
	}

	@Override
	public int getFetchSize() throws SQLException {
		return 0;
	}

	@Override
	public int getType() throws SQLException {
		return ResultSet.TYPE_FORWARD_ONLY;
	}

	@Override
	public int getConcurrency() throws SQLException {
		return ResultSet.CONCUR_READ_ONLY;
	}

	@Override
	public boolean rowUpdated() throws SQLException {
		return false;
	}

	@Override
	public boolean rowInserted() throws SQLException {
		return false;
	}

	@Override
	public boolean rowDeleted() throws SQLException {
		return false;
	}

	@Override
	public void insertRow() throws SQLException {

	}

	@Override
	public void updateRow() throws SQLException {
	}

	@Override
	public void deleteRow() throws SQLException {
	}

	@Override
	public void refreshRow() throws SQLException {
	}

	@Override
	public void cancelRowUpdates() throws SQLException {
	}

	@Override
	public void moveToInsertRow() throws SQLException {
	}

	@Override
	public void moveToCurrentRow() throws SQLException {
	}

	@Override
	public Statement getStatement() throws SQLException {
		return statement;
	}

	/// GETS

	@Override
	public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
		return getObject(columnIndex);
	}

	@Override
	public Ref getRef(int columnIndex) throws SQLException {
		return null;
	}

	@Override
	public Blob getBlob(int columnIndex) throws SQLException {
		return null;
	}

	@Override
	public Clob getClob(int columnIndex) throws SQLException {
		return null;
	}

	@Override
	public Array getArray(int columnIndex) throws SQLException {
		return null;
	}

	@Override
	public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
		return getObject(findColumn(columnLabel));
	}

	@Override
	public Ref getRef(String columnLabel) throws SQLException {
		return null;
	}

	@Override
	public Blob getBlob(String columnLabel) throws SQLException {
		return null;
	}

	@Override
	public Clob getClob(String columnLabel) throws SQLException {
		return null;
	}

	@Override
	public Array getArray(String columnLabel) throws SQLException {
		return null;
	}

	@Override
	public Date getDate(int columnIndex, Calendar cal) throws SQLException {
		return null;
	}

	@Override
	public Date getDate(String columnLabel, Calendar cal) throws SQLException {
		return null;
	}

	@Override
	public Time getTime(int columnIndex, Calendar cal) throws SQLException {
		return null;
	}

	@Override
	public Time getTime(String columnLabel, Calendar cal) throws SQLException {
		return null;
	}

	@Override
	public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
		return null;
	}

	@Override
	public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
		return null;
	}

	@Override
	public URL getURL(int columnIndex) throws SQLException {
		return null;
	}

	@Override
	public URL getURL(String columnLabel) throws SQLException {
		return null;
	}

	@Override
	public RowId getRowId(int columnIndex) throws SQLException {
		return null;
	}

	@Override
	public RowId getRowId(String columnLabel) throws SQLException {
		return null;
	}

	@Override
	public NClob getNClob(int columnIndex) throws SQLException {
		return null;
	}

	@Override
	public NClob getNClob(String columnLabel) throws SQLException {
		return null;
	}

	@Override
	public SQLXML getSQLXML(int columnIndex) throws SQLException {
		return null;
	}

	@Override
	public SQLXML getSQLXML(String columnLabel) throws SQLException {
		return null;
	}

	@Override
	public String getNString(int columnIndex) throws SQLException {
		return getString(columnIndex);
	}

	@Override
	public String getNString(String columnLabel) throws SQLException {
		return getString(findColumn(columnLabel));
	}

	@Override
	public Reader getNCharacterStream(int columnIndex) throws SQLException {
		return getCharacterStream(columnIndex);
	}

	@Override
	public Reader getNCharacterStream(String columnLabel) throws SQLException {
		return getCharacterStream(findColumn(columnLabel));
	}

	@Override
	public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
		if (type == null) {
			throw new SQLException("No type given for column " + columnIndex);
		}
		Object value = value(columnIndex);
		if (value == null || type.isInstance(value)) {
			return type.cast(value);
		}
		if (type == String.class) {
			return type.cast(getString(columnIndex));
		}
		if (type == BigDecimal.class) {
			return type.cast(getBigDecimal(columnIndex));
		}
		if (type == Double.class) {
			return type.cast(getDouble(columnIndex));
		}
		if (type == Float.class) {
			return type.cast(getFloat(columnIndex));
		}
		if (type == Long.class) {
			return type.cast(getLong(columnIndex));
		}
		if (type == Integer.class) {
			return type.cast(getInt(columnIndex));
		}
		if (type == Short.class) {
			return type.cast(getShort(columnIndex));
		}
		if (type == Boolean.class) {
			return type.cast(getBoolean(columnIndex));
		}
		throw new SQLException("Column " + columnIndex + " can't be read as " + type.getName());
	}

	@Override
	public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
		return getObject(findColumn(columnLabel), type);
	}

	/// UPDATES

	@Override
	public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateClob(int columnIndex, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateClob(String columnLabel, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNClob(int columnIndex, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNClob(String columnLabel, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNull(int columnIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBoolean(int columnIndex, boolean x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateByte(int columnIndex, byte x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateShort(int columnIndex, short x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateInt(int columnIndex, int x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateLong(int columnIndex, long x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateFloat(int columnIndex, float x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateDouble(int columnIndex, double x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateString(int columnIndex, String x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBytes(int columnIndex, byte[] x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateDate(int columnIndex, Date x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateTime(int columnIndex, Time x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateObject(int columnIndex, Object x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNull(String columnLabel) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBoolean(String columnLabel, boolean x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateByte(String columnLabel, byte x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateShort(String columnLabel, short x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateInt(String columnLabel, int x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateLong(String columnLabel, long x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateFloat(String columnLabel, float x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateDouble(String columnLabel, double x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateString(String columnLabel, String x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBytes(String columnLabel, byte[] x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateDate(String columnLabel, Date x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateTime(String columnLabel, Time x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateObject(String columnLabel, Object x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNString(int columnIndex, String nString) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNString(String columnLabel, String nString) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateRowId(int columnIndex, RowId x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateRowId(String columnLabel, RowId x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateRef(int columnIndex, Ref x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateRef(String columnLabel, Ref x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBlob(int columnIndex, Blob x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBlob(String columnLabel, Blob x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateClob(int columnIndex, Clob x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateClob(String columnLabel, Clob x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateArray(int columnIndex, Array x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateArray(String columnLabel, Array x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

}
