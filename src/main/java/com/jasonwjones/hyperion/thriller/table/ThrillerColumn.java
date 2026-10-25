package com.jasonwjones.hyperion.thriller.table;

import java.util.HashMap;
import java.util.Map;

public class ThrillerColumn {

	private int type;

	private static final Map<String, Integer> columnTypeMap;
	
	static {
		columnTypeMap = new HashMap<String, Integer>();
		columnTypeMap.put("VARCHAR", java.sql.Types.VARCHAR);
		columnTypeMap.put("DECIMAL", java.sql.Types.DECIMAL);
		columnTypeMap.put("NVARCHAR", java.sql.Types.NVARCHAR);
		columnTypeMap.put("NUMERIC", java.sql.Types.NUMERIC);
		columnTypeMap.put("DOUBLE", java.sql.Types.DOUBLE);
		columnTypeMap.put("INTEGER", java.sql.Types.INTEGER);
		columnTypeMap.put("FLOAT", java.sql.Types.FLOAT);
		columnTypeMap.put("SMALLINT", java.sql.Types.SMALLINT);
		columnTypeMap.put("TINYINT", java.sql.Types.TINYINT);
	}
	
	/**
	 * Total number of digits
	 */
	private int precision;

	/** 
	 * Number of digits that are decimal
	 */
	private int scale;
	
	private String columnName;

	public ThrillerColumn(int type, int precision) {
		this(type, precision, 0);
	}

	public ThrillerColumn(int type, int precision, int scale) {
		this.type = type;
		this.precision = precision;
		this.scale = scale;
	}

	public int getType() {
		return type;
	}

	public int getPrecision() {
		return precision;
	}

	public void setPrecision(int precision) {
		this.precision = precision;
	}	
	
	public int getScale() {
		return scale;
	}
	
	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
	
	@Override
	public String toString() {
		return columnName + " (" + type + ")";
	}
	
}
