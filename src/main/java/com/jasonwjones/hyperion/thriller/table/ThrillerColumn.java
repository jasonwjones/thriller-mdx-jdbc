package com.jasonwjones.hyperion.thriller.table;

import java.sql.Types;

public class ThrillerColumn {

	private int type;

	/**
	 * Total number of digits, or the width of a text column
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
		this.precision = precision > 0 ? precision : defaultPrecision(type);
		this.scale = scale;
	}

	/**
	 * A width for columns whose hints don't give one, since tools size their
	 * display from it.
	 */
	private static int defaultPrecision(int type) {
		switch (type) {
		case Types.DECIMAL:
		case Types.NUMERIC:
			return 38;
		case Types.DOUBLE:
		case Types.FLOAT:
			return 17;
		case Types.INTEGER:
			return 10;
		case Types.SMALLINT:
			return 5;
		case Types.TINYINT:
			return 3;
		default:
			return 255;
		}
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
