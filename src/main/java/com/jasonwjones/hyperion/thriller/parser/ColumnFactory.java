package com.jasonwjones.hyperion.thriller.parser;

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
 */
public class ColumnFactory {

	private static final Pattern COLUMN_PROPERTY = Pattern.compile("column\\.(\\d+)\\.(.+)");

	private Map<String, Integer> columnTypeMap;

	public ColumnFactory() {
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
		TreeMap<Integer, Map<String, String>> columnProps = new TreeMap<Integer, Map<String, String>>();
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
				columnProps.computeIfAbsent(columnIndex, index -> new HashMap<String, String>())
						.put(matcher.group(2), properties.getProperty(propertyName));
			}
		}
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

	private ThrillerColumn columnFromProps(int column, Map<String, String> props) throws ThrillerException {
		ThrillerColumn thrillerColumn = new ThrillerColumn(columnType(props.get("type")), number(column, props, "precision"),
				number(column, props, "scale"));
		thrillerColumn.setColumnName(props.get("name"));
		return thrillerColumn;
	}

	private int number(int column, Map<String, String> props, String key) throws ThrillerException {
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
