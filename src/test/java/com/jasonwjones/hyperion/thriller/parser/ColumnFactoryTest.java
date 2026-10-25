package com.jasonwjones.hyperion.thriller.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Types;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.jasonwjones.hyperion.thriller.ThrillerException;
import com.jasonwjones.hyperion.thriller.table.ThrillerColumn;

public class ColumnFactoryTest {

	private final ColumnFactory factory = new ColumnFactory();

	@Test
	public void buildsColumnsInOrder() throws ThrillerException {
		Properties props = new Properties();
		props.setProperty("column.2.name", "AMOUNT");
		props.setProperty("column.2.type", "decimal");
		props.setProperty("column.2.precision", "9");
		props.setProperty("column.2.scale", "2");
		props.setProperty("column.1.name", "TIME");
		props.setProperty("column.1.type", "VARCHAR");
		props.setProperty("column.1.precision", "30");

		ThrillerColumn[] columns = factory.parse(props);
		assertEquals(2, columns.length);
		assertEquals("TIME", columns[0].getColumnName());
		assertEquals(Types.VARCHAR, columns[0].getType());
		assertEquals(30, columns[0].getPrecision());
		assertEquals(Types.DECIMAL, columns[1].getType());
		assertEquals(9, columns[1].getPrecision());
		assertEquals(2, columns[1].getScale());
	}

	@Test
	public void givesAColumnWithoutAPrecisionADefaultWidth() throws ThrillerException {
		Properties props = new Properties();
		props.setProperty("column.1.name", "LEVEL");
		props.setProperty("column.1.type", "INTEGER");
		assertEquals(10, factory.parse(props)[0].getPrecision());
	}

	@Test
	public void reportsAGapInTheNumbering() {
		Properties props = new Properties();
		props.setProperty("column.1.name", "A");
		props.setProperty("column.1.type", "VARCHAR");
		props.setProperty("column.3.name", "C");
		props.setProperty("column.3.type", "VARCHAR");
		ThrillerException e = assertThrows(ThrillerException.class, () -> factory.parse(props));
		assertEquals("The hints declare columns up to 3 but none for column 2", e.getMessage());
	}

	@Test
	public void reportsAnUnknownType() {
		Properties props = new Properties();
		props.setProperty("column.1.name", "WHEN");
		props.setProperty("column.1.type", "TIMESTAMP");
		assertThrows(ThrillerException.class, () -> factory.parse(props));
	}

	@Test
	public void tellsWhetherAnyColumnsAreDeclared() {
		Properties props = new Properties();
		props.setProperty("thriller.maximum-fraction-digits", "3");
		assertFalse(factory.hasColumns(props));
		props.setProperty("column.1.name", "A");
		assertTrue(factory.hasColumns(props));
	}

}
