package com.jasonwjones.hyperion.thriller.camshaft;

import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jasonwjones.hyperion.thriller.ObjectResultSet;
import com.jasonwjones.hyperion.thriller.ThrillerResultSetMetaData;
import com.jasonwjones.hyperion.thriller.table.ThrillerColumn;
import com.saxifrages.hyperion.camshaft.CamshaftOutputDelegate;

public class ThrillerCamshaftOutputDelegate implements CamshaftOutputDelegate {

	private static final Logger logger = LoggerFactory.getLogger(ThrillerCamshaftOutputDelegate.class);

	private List<Object[]> rows;

	private ThrillerColumn[] columnTypes;

	private Object[] currentRow;

	private int currentColumn;

	private boolean hasWarnedExcessDataColumns = false;

	public static final String MISSING_TEXT = "#Missing";

	public ThrillerCamshaftOutputDelegate(ThrillerColumn[] columnTypes) {
		this.columnTypes = columnTypes;
		rows = new ArrayList<Object[]>();
	}

	public ThrillerCamshaftOutputDelegate() {
		rows = new ArrayList<Object[]>();
	}

	public void startOutput() {
		logger.info("Starting output");
	}

	public void startHeader() {
		logger.info("Starting header");
	}

	public void endHeader() {
		logger.info("Ending header");
	}

	public void addRow() {
		currentRow = new Object[columnTypes.length];
		currentColumn = 0;
	}

	public void addHeaderSpace() {
		logger.info("Spacer");
	}

	public void addHeaderTuple(String[] values) {
		logger.info("Header tuple:");
	}

	public void addTuple(String[] values) {
		logger.info("Got tuple: {}");
	}

	/**
	 * Processes a value (as opposed to a tuple member string) from the MDX
	 * result set, converting it to a data type aas needed based on the mapped
	 * column type.
	 */
	public void addValue(String value) {
		if (currentColumn < columnTypes.length) {
			switch (columnTypes[currentColumn].getType()) {
			case Types.VARCHAR:
				currentRow[currentColumn] = value;
				break;
			case Types.DECIMAL:
				if (value.equals(MISSING_TEXT) || value.isEmpty()) {
					currentRow[currentColumn] = null;
				} else {
					currentRow[currentColumn] = Double.parseDouble(value);
				}
				break;
			case Types.INTEGER:
				if (value.equals(MISSING_TEXT)) {
					currentRow[currentColumn] = null;
				} else {
					currentRow[currentColumn] = Integer.parseInt(value);
				}
				break;
			default:
				throw new RuntimeException("Not sure what type for " + value);
			}
		} else {
			if (!hasWarnedExcessDataColumns) {
				logger.warn("There are more columns of data coming back than have been defined, they will be omitted");
				hasWarnedExcessDataColumns = true;
			}
		}
		currentColumn++;
	}

	public void addValue() {
		logger.info("Spacer value");
	}

	public void endRow() {
		rows.add(currentRow);
	}

	public void endOutput() {
		logger.info("Done, have {} rows", rows.size());
	}

	public ResultSet getResultSet() {
		return new ObjectResultSet(rows, new ThrillerResultSetMetaData(columnTypes));
	}

}
