package com.jasonwjones.hyperion.thriller.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jasonwjones.hyperion.thriller.ThrillerException;
import com.jasonwjones.hyperion.thriller.table.ThrillerColumn;

public class ColumnFactory {

	private static final Logger logger = LoggerFactory.getLogger(ColumnFactory.class);
	
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
	 * Recognized properties:
	 * 
	 * type, name, precision, scale
	 * 
	 * @param properties
	 * @return
	 */
	public ThrillerColumn[] parse(Properties properties) throws ThrillerException {
		
		Map<Integer, Map<String, String>> columnProps = new HashMap<Integer, Map<String, String>>();
		
		Pattern pattern = Pattern.compile("column\\.(\\d+)\\.(.+)");
		
		for (Object property : properties.keySet()) {
			String propertyName = (String) property;
			Matcher matcher = pattern.matcher(propertyName);

			if (matcher.find()) {
				try {
					Integer columnIndex = Integer.valueOf(matcher.group(1));
					String propName = matcher.group(2);
					String value = properties.getProperty(propertyName);
					if (!columnProps.containsKey(columnIndex)) {
						columnProps.put(columnIndex, new HashMap<String, String>());
					}
					columnProps.get(columnIndex).put(propName, value);
				} catch (NumberFormatException e) {
					// Apparently don't need to really worry about this happening since
					// the regex will 'filter' it out
					throw new ColumnFactoryParseException(propertyName, matcher.group(1), e);
				}	
				logger.info("Property {} = {} --> {}", propertyName, matcher.group(1), matcher.group(2));
			}
		}
		return createColumns(columnProps);
	}
	
	public ThrillerColumn[] createColumns(Map<Integer, Map<String, String>> columnProps) throws ThrillerException {
		Set<Integer> indices = columnProps.keySet();
		Integer min = min(indices);
		Integer max = max(indices);
		
		if (max == min && max == 0) {
			logger.info("Query hints not specified");
			throw new ColumnFactoryParseException("Query hints were not specified");
		} else {
			Set<Integer> missingIndices = missingIndices(indices, min, max);
			
			logger.info("Props: {}", columnProps);
			logger.info("Max/min: {}, {}", max, min);
			
			// There are no missing indices; we have a valid column set
			if (missingIndices.isEmpty()) {
				ThrillerColumn[] columns = new ThrillerColumn[max];
				for (int column = 1; column <= max; column++) {
					Map<String, String> props = columnProps.get(column);
					if (!props.containsKey("type")) {
						throw new ColumnFactoryParseException("No type specified for column " + column);
					}
					if (!props.containsKey("name")) {
						throw new ColumnFactoryParseException("No name specified for column " + column);
					}
					try {
						columns[column - 1] = columnFromProps(props);
					} catch (ColumnFactoryParseException e) {
						throw new ColumnFactoryParseException("Couldn't build column number " + column);
					}
				}
				return columns;
			}
			logger.info("Missing indices: {}", missingIndices);
			throw new ColumnFactoryParseException(missingIndices);
		} 
	}
	
	public static Integer min(Set<Integer> ints) {
		int min = 0;
		for (Integer value : ints) {
			min = Math.min(min, value); 
		}
		return min;
	}
	
	public static Integer max(Set<Integer> ints) {
		int max = 0;
		for (Integer value : ints) {
			max = Math.max(max, value);
		}
		return max;
	}
	
	public static Set<Integer> missingIndices(Set<Integer> indices, int min, int max) {
		Set<Integer> missingIndices = new TreeSet<Integer>();
		if (min != max && min != 0) {
			for (int value = min; value <= max; value++) {
				if (!indices.contains(value)) {
					missingIndices.add(value);
				}
			}
		}
		return missingIndices;
	}
	
	private ThrillerColumn columnFromProps(Map<String, String> props) throws ThrillerException {
		int type = columnType(props.get("type"));
		String name = props.get("name");
		
		int precision = 0;
		int scale = 0;
		
		try {
			precision = Integer.parseInt(props.get("precision"));
			scale = Integer.parseInt(props.get("scale"));
		} catch (NumberFormatException e) {
			logger.warn("Couldn't parse the precision/scale of column");
		}
		
		logger.warn("P/S {} {}", precision, scale);
		ThrillerColumn column = new ThrillerColumn(type, precision, scale);
		column.setColumnName(name);
		return column;
	}
	
	public int columnType(String type) throws ThrillerException {
		if (type != null) {
			Integer colType = columnTypeMap.get(type.toUpperCase());
			if (colType != null) {
				return colType;
			}
			throw ColumnFactoryParseException.unrecognizedColumnType(type);
		} else {
			throw new ColumnFactoryParseException("Type not specified");
		}
	}
		
}
