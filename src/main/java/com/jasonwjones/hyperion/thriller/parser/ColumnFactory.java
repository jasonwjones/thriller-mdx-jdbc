package com.jasonwjones.hyperion.thriller.parser;

import java.sql.Types;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Locale;

import com.jasonwjones.hyperion.thriller.ThrillerException;
import com.jasonwjones.hyperion.thriller.table.ThrillerColumn;

/**
 * Builds the result columns a query's hints declare, as in
 *
 * <pre>
 * -- column.1.name = MEASURE
 * -- column.1.type = VARCHAR
 * -- column.2.name = JAN
 * -- column.2.type = DECIMAL
 * -- column.2.precision = 9
 * -- column.2.scale = 2
 * </pre>
 *
 * Hints that name and type every column, numbered from 1, are the result's
 * columns, and a cell beyond the last is left off. Hints that leave anything
 * out adjust the columns worked out from the result instead, so
 * {@code -- column.4.scale = 1} rounds the fourth column and leaves the rest
 * as they were.
 */
public class ColumnFactory {

	private static final Pattern COLUMN_PROPERTY = Pattern.compile("column\\.(\\d+)\\.(.+)");

	private Map<String, Integer> columnTypeMap;

	public ColumnFactory() {
		columnTypeMap = new HashMap<>();
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
	 * @param properties the query's hints
	 * @return whether they declare any columns
	 */
	public boolean hasColumns(Properties properties) {
		for (Object property : properties.keySet()) {
			if (COLUMN_PROPERTY.matcher((String) property).matches()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Recognized properties:
	 *
	 * type, name, precision, scale
	 *
	 * @param properties the query's hints
	 * @return the columns, in order
	 * @throws ThrillerException if no columns are declared, the numbering has a
	 *             gap, or a column lacks a name or a known type
	 */
	public ThrillerColumn[] parse(Properties properties) throws ThrillerException {
		TreeMap<Integer, Map<String, String>> columnProps = columnProperties(properties);
		if (columnProps.isEmpty()) {
			throw new ColumnFactoryParseException("Query hints were not specified");
		}

		int max = columnProps.lastKey();
		ThrillerColumn[] columns = new ThrillerColumn[max];
		for (int column = 1; column <= max; column++) {
			Map<String, String> props = columnProps.get(column);
			if (props == null) {
				throw new ColumnFactoryParseException("The hints declare columns up to " + max + " but none for column " + column);
			}
			if (!props.containsKey("type")) {
				throw new ColumnFactoryParseException("No type specified for column " + column);
			}
			if (!props.containsKey("name")) {
				throw new ColumnFactoryParseException("No name specified for column " + column);
			}
			columns[column - 1] = columnFromProps(column, props);
		}
		return columns;
	}

	/**
	 * The columns for a result: the hinted ones if the hints declare every
	 * column in full, otherwise the detected ones with the hints applied to
	 * them. A hinted column past the detected ones is added, as text.
	 *
	 * @param properties the query's hints
	 * @param detected the columns worked out from the result
	 * @return the columns, in order
	 * @throws ThrillerException if a hint has a value Thriller can't use
	 */
	public ThrillerColumn[] columns(Properties properties, ThrillerColumn[] detected) throws ThrillerException {
		TreeMap<Integer, Map<String, String>> columnProps = columnProperties(properties);
		if (columnProps.isEmpty()) {
			return detected;
		}
		if (complete(columnProps)) {
			return parse(properties);
		}
		ThrillerColumn[] columns = Arrays.copyOf(detected, Math.max(detected.length, columnProps.lastKey()));
		for (int index = 0; index < columns.length; index++) {
			if (columns[index] == null) {
				columns[index] = new ThrillerColumn(Types.VARCHAR, 0);
				columns[index].setColumnName("COLUMN" + (index + 1));
			}
			Map<String, String> props = columnProps.get(index + 1);
			if (props != null) {
				columns[index] = adjusted(index + 1, columns[index], props);
			}
		}
		return columns;
	}

	/**
	 * Whether hints declare every column from the first to the last, each with
	 * a name and a type.
	 */
	private static boolean complete(TreeMap<Integer, Map<String, String>> columnProps) {
		for (int column = 1; column <= columnProps.lastKey(); column++) {
			Map<String, String> props = columnProps.get(column);
			if (props == null || !props.containsKey("name") || !props.containsKey("type")) {
				return false;
			}
		}
		return true;
	}

	/**
	 * A detected column with its hints applied. A scale or precision without a
	 * type makes a numeric column DECIMAL, since only a DECIMAL has them.
	 */
	private ThrillerColumn adjusted(int column, ThrillerColumn detected, Map<String, String> props) throws ThrillerException {
		int type;
		if (props.containsKey("type")) {
			type = columnType(props.get("type"));
		} else if ((props.containsKey("scale") || props.containsKey("precision")) && isNumeric(detected.getType())) {
			type = Types.DECIMAL;
		} else {
			type = detected.getType();
		}
		boolean sameType = type == detected.getType();
		int precision = props.containsKey("precision") ? number(column, props, "precision") : sameType ? detected.getPrecision() : 0;
		int scale = props.containsKey("scale") ? number(column, props, "scale") : sameType ? detected.getScale() : 0;
		ThrillerColumn adjusted = new ThrillerColumn(type, precision, scale);
		adjusted.setColumnName(props.containsKey("name") ? props.get("name") : detected.getColumnName());
		return adjusted;
	}

	private static boolean isNumeric(int type) {
		switch (type) {
		case Types.DECIMAL:
		case Types.NUMERIC:
		case Types.DOUBLE:
		case Types.FLOAT:
		case Types.INTEGER:
		case Types.SMALLINT:
		case Types.TINYINT:
			return true;
		default:
			return false;
		}
	}

	private static TreeMap<Integer, Map<String, String>> columnProperties(Properties properties) throws ThrillerException {
		TreeMap<Integer, Map<String, String>> columnProps = new TreeMap<>();
		for (Object property : properties.keySet()) {
			String propertyName = (String) property;
			Matcher matcher = COLUMN_PROPERTY.matcher(propertyName);
			if (matcher.matches()) {
				Integer columnIndex;
				try {
					columnIndex = Integer.valueOf(matcher.group(1));
				} catch (NumberFormatException e) {
					throw new ColumnFactoryParseException(propertyName, matcher.group(1), e);
				}
				if (columnIndex < 1) {
					throw new ColumnFactoryParseException("Columns are numbered from 1, so " + propertyName + " names no column");
				}
				columnProps.computeIfAbsent(columnIndex, index -> new HashMap<>())
						.put(matcher.group(2), properties.getProperty(propertyName));
			}
		}
		return columnProps;
	}

	private ThrillerColumn columnFromProps(int column, Map<String, String> props) throws ThrillerException {
		ThrillerColumn thrillerColumn = new ThrillerColumn(columnType(props.get("type")), number(column, props, "precision"),
				number(column, props, "scale"));
		thrillerColumn.setColumnName(props.get("name"));
		return thrillerColumn;
	}

	private static int number(int column, Map<String, String> props, String key) throws ThrillerException {
		String value = props.get(key);
		if (value == null) {
			return 0;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new ColumnFactoryParseException("The " + key + " of column " + column + " should be a whole number, not '" + value + "'");
		}
	}

	public int columnType(String type) throws ThrillerException {
		if (type != null) {
			Integer colType = columnTypeMap.get(type.toUpperCase(Locale.ROOT));
			if (colType != null) {
				return colType;
			}
			throw ColumnFactoryParseException.unrecognizedColumnType(type);
		} else {
			throw new ColumnFactoryParseException("Type not specified");
		}
	}

}
