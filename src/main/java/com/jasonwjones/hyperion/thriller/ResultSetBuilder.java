package com.jasonwjones.hyperion.thriller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jasonwjones.hyperion.thriller.table.ThrillerColumn;

/**
 * Turns an MDX result into rows of typed values. Columns either come from the
 * query's hints, matched to each row's cells by position, or are worked out
 * from the result: a column per row dimension holding member names, then one
 * per data column, numeric if every value in it is a number.
 */
public final class ResultSetBuilder {

	private static final Logger logger = LoggerFactory.getLogger(ResultSetBuilder.class);

	public static final String MISSING_TEXT = "#Missing";

	private ResultSetBuilder() {}

	/**
	 * Names and types the columns of a result that came without hints.
	 *
	 * @param result the MDX result
	 * @return a column for each cell of the widest row
	 */
	public static ThrillerColumn[] detectColumns(MdxResult result) {
		int width = result.getWidth();
		int memberColumns = Math.min(result.getRowDimensions().size(), width);
		ThrillerColumn[] columns = new ThrillerColumn[width];
		Set<String> names = new HashSet<String>();

		for (int index = 0; index < width; index++) {
			String name;
			ThrillerColumn column;
			if (index < memberColumns) {
				name = result.getRowDimensions().get(index);
				column = new ThrillerColumn(Types.VARCHAR, longest(result, index));
			} else {
				name = header(result, index);
				column = isNumeric(result, index) ? new ThrillerColumn(Types.DOUBLE, 17) : new ThrillerColumn(Types.VARCHAR, longest(result, index));
			}
			column.setColumnName(unique(name == null || name.isEmpty() ? "COLUMN" + (index + 1) : name, names));
			columns[index] = column;
		}
		return columns;
	}

	/**
	 * Converts each data row's cells to the given columns. A row with more cells
	 * than columns has the extras left off; one with fewer is padded with nulls.
	 *
	 * @param columns the columns, in order
	 * @param result the MDX result
	 * @return a row of values for each data row
	 * @throws ThrillerException if a cell can't be converted to its column's type
	 */
	public static List<Object[]> rows(ThrillerColumn[] columns, MdxResult result) throws ThrillerException {
		List<Object[]> rows = new ArrayList<Object[]>();
		boolean warned = false;
		for (List<String> cells : result.getDataRows()) {
			if (cells.size() > columns.length && !warned) {
				logger.warn("Rows have {} cells but {} columns are defined, so the extra cells are left off", cells.size(), columns.length);
				warned = true;
			}
			Object[] row = new Object[columns.length];
			for (int index = 0; index < columns.length && index < cells.size(); index++) {
				row[index] = convert(columns[index], index, cells.get(index));
			}
			rows.add(row);
		}
		return rows;
	}

	/**
	 * Converts a cell to its column's type. Empty and #Missing cells are null in
	 * any column that isn't text.
	 */
	static Object convert(ThrillerColumn column, int index, String cell) throws ThrillerException {
		int type = column.getType();
		if (type == Types.VARCHAR || type == Types.NVARCHAR) {
			return cell;
		}
		if (isMissing(cell)) {
			return null;
		}
		try {
			switch (type) {
			case Types.DECIMAL:
			case Types.NUMERIC:
				BigDecimal decimal = new BigDecimal(cell);
				return column.getScale() > 0 ? decimal.setScale(column.getScale(), RoundingMode.HALF_UP) : decimal;
			case Types.DOUBLE:
			case Types.FLOAT:
				return Double.valueOf(cell);
			case Types.INTEGER:
			case Types.SMALLINT:
			case Types.TINYINT:
				return new BigDecimal(cell).setScale(0, RoundingMode.HALF_UP).intValueExact();
			default:
				throw new ThrillerException("Column " + (index + 1) + " (" + column.getColumnName() + ") has a type Thriller can't fill: " + type);
			}
		} catch (NumberFormatException | ArithmeticException e) {
			throw new ThrillerException("Column " + (index + 1) + " (" + column.getColumnName() + ") is numeric, but a cell holds '" + cell + "'", e);
		}
	}

	static boolean isMissing(String cell) {
		return cell == null || cell.isEmpty() || cell.equalsIgnoreCase(MISSING_TEXT);
	}

	private static boolean isNumeric(MdxResult result, int index) {
		for (List<String> row : result.getDataRows()) {
			if (index < row.size() && !isMissing(row.get(index))) {
				try {
					Double.parseDouble(row.get(index));
				} catch (NumberFormatException e) {
					return false;
				}
			}
		}
		return true;
	}

	private static int longest(MdxResult result, int index) {
		int longest = 1;
		for (List<String> row : result.getDataRows()) {
			if (index < row.size() && row.get(index) != null) {
				longest = Math.max(longest, row.get(index).length());
			}
		}
		return longest;
	}

	/**
	 * The member names above a data column, top to bottom, such as "Jan Cola".
	 */
	private static String header(MdxResult result, int index) {
		StringBuilder header = new StringBuilder();
		for (List<String> row : result.getHeaderRows()) {
			if (index < row.size() && row.get(index) != null && !row.get(index).isEmpty()) {
				if (header.length() > 0) {
					header.append(' ');
				}
				header.append(row.get(index));
			}
		}
		return header.toString();
	}

	private static String unique(String name, Set<String> names) {
		String candidate = name;
		for (int suffix = 2; !names.add(candidate.toUpperCase()); suffix++) {
			candidate = name + "_" + suffix;
		}
		return candidate;
	}

}
