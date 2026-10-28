package com.jasonwjones.hyperion.thriller;

import java.math.BigDecimal;
import java.sql.JDBCType;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import com.jasonwjones.hyperion.thriller.table.ThrillerColumn;

public class ThrillerResultSetMetaData implements ResultSetMetaData {

	private final ThrillerColumn[] columns;

	private final String application;

	private final String cube;

	public ThrillerResultSetMetaData(ThrillerColumn[] columns) {
		this(columns, "", "");
	}

	/**
	 * @param columns the columns
	 * @param application the application the result came from, reported as each
	 *            column's schema
	 * @param cube the cube it came from, reported as each column's table
	 */
	public ThrillerResultSetMetaData(ThrillerColumn[] columns, String application, String cube) {
		this.application = application;
		this.cube = cube;
		this.columns = columns;
	}

	protected ThrillerColumn column(int index) throws SQLException {
		if (index < 1 || index > columns.length) {
			throw new SQLException("Invalid column requested: " + index);
		}
		return this.columns[index - 1];
	}

	private boolean isNumeric(int column) throws SQLException {
		int type = column(column).getType();
		return type != Types.VARCHAR && type != Types.NVARCHAR;
	}

	public int getColumnCount() throws SQLException {
		return columns.length;
	}

	public String getColumnLabel(int column) throws SQLException {
		return column(column).getColumnName();
	}

	public String getColumnName(int column) throws SQLException {
		return column(column).getColumnName();
	}

	public int getColumnType(int column) throws SQLException {
		return column(column).getType();
	}

	public String getColumnTypeName(int column) throws SQLException {
		return JDBCType.valueOf(column(column).getType()).getName();
	}

	/**
	 * The class getObject() returns for the column, as ResultSetBuilder fills it.
	 */
	public String getColumnClassName(int column) throws SQLException {
		switch (column(column).getType()) {
		case Types.DECIMAL:
		case Types.NUMERIC:
			return BigDecimal.class.getName();
		case Types.DOUBLE:
		case Types.FLOAT:
			return Double.class.getName();
		case Types.INTEGER:
		case Types.SMALLINT:
		case Types.TINYINT:
			return Integer.class.getName();
		default:
			return String.class.getName();
		}
	}

	public int getPrecision(int column) throws SQLException {
		return column(column).getPrecision();
	}

	public int getScale(int column) throws SQLException {
		return column(column).getScale();
	}

	public int getColumnDisplaySize(int column) throws SQLException {
		return column(column).getPrecision();
	}

	/**
	 * Any cell can be #Missing, which comes back as null.
	 */
	public int isNullable(int column) throws SQLException {
		return ResultSetMetaData.columnNullable;
	}

	public boolean isSigned(int column) throws SQLException {
		return isNumeric(column);
	}

	public boolean isCaseSensitive(int column) throws SQLException {
		return !isNumeric(column);
	}

	public boolean isAutoIncrement(int column) throws SQLException {
		return false;
	}

	public boolean isSearchable(int column) throws SQLException {
		return false;
	}

	public boolean isCurrency(int column) throws SQLException {
		return false;
	}

	public boolean isReadOnly(int column) throws SQLException {
		return true;
	}

	public boolean isWritable(int column) throws SQLException {
		return false;
	}

	public boolean isDefinitelyWritable(int column) throws SQLException {
		return false;
	}

	public String getSchemaName(int column) throws SQLException {
		return application;
	}

	public String getTableName(int column) throws SQLException {
		return cube;
	}

	public String getCatalogName(int column) throws SQLException {
		return "";
	}

	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (isWrapperFor(iface)) {
			return (T) this;
		}
		throw new SQLException("Cannot cast to " + iface.getName());
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface.isAssignableFrom(getClass());
	}

}
