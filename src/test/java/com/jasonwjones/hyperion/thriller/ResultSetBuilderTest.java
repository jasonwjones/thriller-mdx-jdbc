package com.jasonwjones.hyperion.thriller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.jasonwjones.hyperion.thriller.table.ThrillerColumn;

public class ResultSetBuilderTest {

	@Test
	public void namesColumnsFromRowDimensionsAndHeaders() throws ThrillerException {
		ThrillerColumn[] columns = ResultSetBuilder.detectColumns(Fixtures.result("measures-by-month"));
		assertArrayEquals(new String[] { "Measures", "Jan 100-10", "Feb 100-10", "Mar 100-10" }, names(columns));
		assertEquals(Types.VARCHAR, columns[0].getType());
		assertEquals(Types.DOUBLE, columns[1].getType());
	}

	@Test
	public void detectsNumericColumnsPastMissingValues() throws ThrillerException {
		ThrillerColumn[] columns = ResultSetBuilder.detectColumns(Fixtures.result("missing-values"));
		assertArrayEquals(new String[] { "Market", "Product", "Jan" }, names(columns));
		assertEquals(Types.DOUBLE, columns[2].getType());

		List<Object[]> rows = ResultSetBuilder.rows(columns, Fixtures.result("missing-values"));
		assertEquals(130.0, rows.get(0)[2]);
		assertNull(rows.get(5)[2]);
	}

	@Test
	public void numbersUnnamedColumnsAndKeepsTextAsText() throws ThrillerException {
		ThrillerColumn[] columns = ResultSetBuilder.detectColumns(Fixtures.result("dimension-properties"));
		assertArrayEquals(new String[] { "Product", "COLUMN2", "COLUMN3" }, names(columns));
		assertEquals(Types.VARCHAR, columns[1].getType());
		assertEquals(Types.DOUBLE, columns[2].getType());
	}

	@Test
	public void makesRepeatedNamesUnique() {
		MdxResult result = new MdxResult(Arrays.asList("Market"), Arrays.asList("Year"),
				Arrays.asList(Arrays.asList("", "Jan", "Jan")), Arrays.asList(Arrays.asList("East", "1", "2")));
		assertArrayEquals(new String[] { "Market", "Jan", "Jan_2" }, names(ResultSetBuilder.detectColumns(result)));
	}

	@Test
	public void convertsCellsToHintedTypes() throws ThrillerException {
		ThrillerColumn[] columns = {
				column("MEASURE", Types.VARCHAR, 0), column("JAN", Types.DECIMAL, 0),
				column("FEB", Types.DECIMAL, 2), column("MAR", Types.INTEGER, 0) };
		List<Object[]> rows = ResultSetBuilder.rows(columns, Fixtures.result("measures-by-month"));
		Object[] marginPercent = rows.get(8);
		assertEquals("Margin %", marginPercent[0]);
		assertEquals(new BigDecimal("61.15226337448559"), marginPercent[1]);
		assertEquals(new BigDecimal("60.88"), marginPercent[2]);
		assertEquals(61, marginPercent[3]);
	}

	@Test
	public void leavesOffCellsBeyondTheDefinedColumns() throws ThrillerException {
		ThrillerColumn[] columns = { column("MEASURE", Types.VARCHAR, 0), column("JAN", Types.DECIMAL, 0) };
		Object[] sales = ResultSetBuilder.rows(columns, Fixtures.result("measures-by-month")).get(0);
		assertArrayEquals(new Object[] { "Sales", new BigDecimal("4860.0") }, sales);
	}

	@Test
	public void padsShortRowsWithNulls() throws ThrillerException {
		ThrillerColumn[] columns = { column("MEASURE", Types.VARCHAR, 0), column("JAN", Types.DECIMAL, 0),
				column("FEB", Types.DECIMAL, 0) };
		MdxResult result = new MdxResult(Arrays.asList("Measures"), Arrays.asList("Year"),
				Collections.<List<String>>emptyList(), Arrays.asList(Arrays.asList("Sales", "4860.0")));
		assertArrayEquals(new Object[] { "Sales", new BigDecimal("4860.0"), null }, ResultSetBuilder.rows(columns, result).get(0));
	}

	@Test
	public void reportsTextInANumericColumn() throws ThrillerException {
		ThrillerColumn[] columns = { column("MEASURE", Types.INTEGER, 0) };
		ThrillerException e = assertThrows(ThrillerException.class,
				() -> ResultSetBuilder.rows(columns, Fixtures.result("measures-by-month")));
		assertEquals("Column 1 (MEASURE) is numeric, but a cell holds 'Sales'", e.getMessage());
	}

	private static ThrillerColumn column(String name, int type, int scale) {
		ThrillerColumn column = new ThrillerColumn(type, 0, scale);
		column.setColumnName(name);
		return column;
	}

	private static String[] names(ThrillerColumn[] columns) {
		return Arrays.stream(columns).map(ThrillerColumn::getColumnName).toArray(String[]::new);
	}

}
