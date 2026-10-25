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
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple ResultSet implementation that maps a List of Object arrays and given
 * metadata in to a ResultSet.
 * 
 * @author jasonwjones
 *
 */
public class ObjectResultSet implements ResultSet {

	private List<Object[]> data;

	private Map<String, Integer> columnLookups;

	private int position = -1;

	private ResultSetMetaData metaData;

	public ObjectResultSet(List<Object[]> data, ResultSetMetaData metaData) {
		this.data = data;
		this.metaData = metaData;
	}

	protected void buildLookups(ResultSetMetaData metaData) throws SQLException {
		columnLookups = new HashMap<String, Integer>();
		for (int columnIndex = 1; columnIndex <= metaData.getColumnCount(); columnIndex++) {
			columnLookups.put(metaData.getColumnName(columnIndex), columnIndex);
		}
	}

	protected Object column(int columnIndex) {
		return data.get(position)[columnIndex - 1];
	}
	
	protected Object column(String columnLabel) {
		return data.get(position)[columnLookups.get(columnLabel) - 1];
	}

	// TODO: use this on getString() et al?
	protected void checkColumn(int columnIndex) throws SQLException {
		if (columnIndex > metaData.getColumnCount()) {
			throw new SQLException("Invalid column requested: " + columnIndex);
		}
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return null;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}

	@Override
	public boolean next() throws SQLException {
		return ++position < data.size();
	}

	@Override
	public void close() throws SQLException {
		data = null;
	}

	@Override
	public boolean wasNull() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getString(int columnIndex) throws SQLException {
		Object object = data.get(position)[columnIndex - 1];
		if (object != null) {
			return object.toString();
		}
		return null;
	}

	@Override
	public boolean getBoolean(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public byte getByte(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public short getShort(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getInt(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getLong(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public float getFloat(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getDouble(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public byte[] getBytes(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Date getDate(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Time getTime(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Timestamp getTimestamp(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputStream getAsciiStream(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputStream getUnicodeStream(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputStream getBinaryStream(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getString(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean getBoolean(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public byte getByte(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public short getShort(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getInt(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getLong(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public float getFloat(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getDouble(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public byte[] getBytes(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Date getDate(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Time getTime(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Timestamp getTimestamp(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputStream getAsciiStream(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputStream getUnicodeStream(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputStream getBinaryStream(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearWarnings() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCursorName() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return this.metaData;
	}

	@Override
	public Object getObject(int columnIndex) throws SQLException {
		return column(columnIndex);
	}

	@Override
	public Object getObject(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int findColumn(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Reader getCharacterStream(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Reader getCharacterStream(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isBeforeFirst() throws SQLException {
		return position == -1;
	}

	@Override
	public boolean isAfterLast() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isFirst() throws SQLException {
		return position == 0;
	}

	@Override
	public boolean isLast() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void beforeFirst() throws SQLException {
		position = -1;
	}

	@Override
	public void afterLast() throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean first() throws SQLException {
		if (data.size() > 0) {
			position = 0;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean last() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getRow() throws SQLException {
		return position + 1;
	}

	@Override
	public boolean absolute(int row) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean relative(int rows) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean previous() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getHoldability() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isClosed() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getFetchDirection() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setFetchSize(int rows) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getFetchSize() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getType() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getConcurrency() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean rowUpdated() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean rowInserted() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean rowDeleted() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void insertRow() throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateRow() throws SQLException {
		// TODO Auto-generated method stub
	}

	@Override
	public void deleteRow() throws SQLException {
		// TODO Auto-generated method stub
	}

	@Override
	public void refreshRow() throws SQLException {
		// TODO Auto-generated method stub
	}

	@Override
	public void cancelRowUpdates() throws SQLException {
		// TODO Auto-generated method stub
	}

	@Override
	public void moveToInsertRow() throws SQLException {
		// TODO Auto-generated method stub
	}

	@Override
	public void moveToCurrentRow() throws SQLException {
		// TODO Auto-generated method stub
	}

	@Override
	public Statement getStatement() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/// GETS

	@Override
	public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Ref getRef(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Blob getBlob(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Clob getClob(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Array getArray(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Ref getRef(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Blob getBlob(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Clob getClob(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Array getArray(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getDate(int columnIndex, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getDate(String columnLabel, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Time getTime(int columnIndex, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Time getTime(String columnLabel, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URL getURL(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URL getURL(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RowId getRowId(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RowId getRowId(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NClob getNClob(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NClob getNClob(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SQLXML getSQLXML(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SQLXML getSQLXML(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNString(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNString(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Reader getNCharacterStream(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Reader getNCharacterStream(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/// UPDATES

	@Override
	public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateClob(int columnIndex, Reader reader) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateClob(String columnLabel, Reader reader) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateNClob(int columnIndex, Reader reader) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateNClob(String columnLabel, Reader reader) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateNull(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateBoolean(int columnIndex, boolean x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateByte(int columnIndex, byte x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateShort(int columnIndex, short x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateInt(int columnIndex, int x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateLong(int columnIndex, long x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateFloat(int columnIndex, float x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateDouble(int columnIndex, double x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateString(int columnIndex, String x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateBytes(int columnIndex, byte[] x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateDate(int columnIndex, Date x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateTime(int columnIndex, Time x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateObject(int columnIndex, Object x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateNull(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateBoolean(String columnLabel, boolean x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateByte(String columnLabel, byte x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateShort(String columnLabel, short x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateInt(String columnLabel, int x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateLong(String columnLabel, long x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateFloat(String columnLabel, float x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateDouble(String columnLabel, double x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateString(String columnLabel, String x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateBytes(String columnLabel, byte[] x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateDate(String columnLabel, Date x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateTime(String columnLabel, Time x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateObject(String columnLabel, Object x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateNString(int columnIndex, String nString) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateNString(String columnLabel, String nString) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateRowId(int columnIndex, RowId x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateRowId(String columnLabel, RowId x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateRef(int columnIndex, Ref x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateRef(String columnLabel, Ref x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateBlob(int columnIndex, Blob x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateBlob(String columnLabel, Blob x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateClob(int columnIndex, Clob x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateClob(String columnLabel, Clob x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateArray(int columnIndex, Array x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateArray(String columnLabel, Array x) throws SQLException {
		throw new UnsupportedOperationException();
	}

}
