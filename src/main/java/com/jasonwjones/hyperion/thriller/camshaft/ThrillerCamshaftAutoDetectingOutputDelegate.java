package com.jasonwjones.hyperion.thriller.camshaft;

import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jasonwjones.hyperion.thriller.ObjectResultSet;
import com.jasonwjones.hyperion.thriller.ThrillerResultSetMetaData;
import com.jasonwjones.hyperion.thriller.table.ThrillerColumn;
import com.saxifrages.hyperion.camshaft.CamshaftOutputDelegate;

public class ThrillerCamshaftAutoDetectingOutputDelegate implements CamshaftOutputDelegate {

	private static final Logger logger = LoggerFactory.getLogger(ThrillerCamshaftAutoDetectingOutputDelegate.class);

	private List<Object[]> rows;

	private List<ThrillerColumn> columnTypes;

	private Object[] currentRow;

	private List<String> firstRow;
	
	private int currentColumn;
	
	private boolean hasProcessedFirstRow = false;
	
	public ThrillerCamshaftAutoDetectingOutputDelegate() {
		this.columnTypes = new ArrayList<ThrillerColumn>();
		this.rows = new ArrayList<Object[]>();
		this.firstRow = new ArrayList<String>();
	}

	public Object[] convertRow(List<String> row) {
		Object[] convertedRow = new Object[columnTypes.size()];
		for (int rowIndex = 0; rowIndex < row.size(); rowIndex++) {
			convertedRow[rowIndex] = createCell(rowIndex, row.get(rowIndex));
		}
		logger.info("Converted row: {}", row);
		return convertedRow;
	}
	
	private Object createCell(int columnIndex, String value) {
		int type = columnTypes.get(columnIndex).getType();
		switch (type) {
		case Types.VARCHAR:
			return value;
		case Types.DECIMAL:
		case Types.DOUBLE:
			return Double.parseDouble(value);
		case Types.INTEGER:
			return Integer.parseInt(value);
		default:
			throw new RuntimeException("Not sure what type for " + value + " (type int was " + type + ")");
		}
	}
	
	public void startOutput() {
		logger.info("Starting output");
		firstRow = new ArrayList<String>();
	}

	public void startHeader() {
		logger.info("Starting header");
	}

	public void endHeader() {
		logger.info("Ending header");
		;
	}

	public void addRow() {
		if (hasProcessedFirstRow) {
			currentRow = new Object[columnTypes.size()];
		}
		currentColumn = 0;
	}

	public void addHeaderSpace() {
	}

	public void addHeaderTuple(String[] values) {
		logger.info("Header tuple:");
	}

	public void addTuple(String[] values) {
		logger.info("Got tuple: {}");
	}

	public void addValue(String value) {
		if (!hasProcessedFirstRow) {
			logger.info("On first row: {}", value);
			logger.info("Is number: {}", NumberUtils.isNumber(value));
			
			String columnName = "COLUMN" + currentColumn;
			
			if (NumberUtils.isNumber(value)) {
				ThrillerColumn column = new ThrillerColumn(java.sql.Types.DOUBLE, 20);
				column.setColumnName(columnName);
				columnTypes.add(column);
			} else {
				ThrillerColumn column = new ThrillerColumn(java.sql.Types.VARCHAR, 20);
				column.setColumnName(columnName);
				columnTypes.add(column);
			}
			firstRow.add(value);
		} else {
			switch (columnTypes.get(currentColumn).getType()) {
			case Types.VARCHAR:
				currentRow[currentColumn] = value;
				break;
			case Types.DECIMAL:
			case Types.DOUBLE:
				currentRow[currentColumn] = Double.parseDouble(value);
				break;
			case Types.INTEGER:
				currentRow[currentColumn] = Integer.parseInt(value);
				break;
			default:
				throw new RuntimeException("Not sure what type for " + value);
			}
		} 
		currentColumn++;
		logger.info("Value: {}", value);
	}

	public void addValue() {
		logger.info("Spacer value");
	}

	public void endRow() {
		if (!hasProcessedFirstRow) {
			hasProcessedFirstRow = true;
			rows.add(convertRow(firstRow));
		} else {
			rows.add(currentRow);
		}
	}

	public void endOutput() {
		logger.info("Done, have {} rows", rows.size());
	}

	public ResultSet getResultSet() {
		logger.info("Column types: {}", columnTypes);
		ThrillerColumn[] cols = columnTypes.toArray(new ThrillerColumn[0]);
		ThrillerResultSetMetaData md = new ThrillerResultSetMetaData(cols);
		return new ObjectResultSet(rows, md);
	}
	
}
