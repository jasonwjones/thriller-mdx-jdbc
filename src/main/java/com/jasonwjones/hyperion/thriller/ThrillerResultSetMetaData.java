package com.jasonwjones.hyperion.thriller;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import com.jasonwjones.hyperion.thriller.table.ThrillerColumn;

public class ThrillerResultSetMetaData implements ResultSetMetaData {

	private ThrillerColumn[] columns;
	
	public ThrillerResultSetMetaData(ThrillerColumn[] columns) {
		this.columns = columns;
	}
	
	protected ThrillerColumn column(int index) {
		return this.columns[index - 1];
	}
	
	public int getPrecision(int column) throws SQLException {
		return column(column).getPrecision();
	}

	public int getScale(int column) throws SQLException {
		return column(column).getScale();
	}
	
	public <T> T unwrap(Class<T> iface) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public int getColumnCount() throws SQLException {
		return columns.length;
	}

	public boolean isAutoIncrement(int column) throws SQLException {
		return false;
	}

	/// NOT IMPLEMENTED
	
	public boolean isCaseSensitive(int column) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isSearchable(int column) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isCurrency(int column) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public int isNullable(int column) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean isSigned(int column) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public int getColumnDisplaySize(int column) throws SQLException {
		return column(column).getPrecision();
	}

	public String getColumnLabel(int column) throws SQLException {
		return column(column).getColumnName();
	}

	public String getColumnName(int column) throws SQLException {
		return column(column).getColumnName();
	}

	public String getSchemaName(int column) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public String getTableName(int column) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public String getCatalogName(int column) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public int getColumnType(int column) throws SQLException {
		return columns[column - 1].getType();
	}

	public String getColumnTypeName(int column) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isReadOnly(int column) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isWritable(int column) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isDefinitelyWritable(int column) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public String getColumnClassName(int column) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

}
