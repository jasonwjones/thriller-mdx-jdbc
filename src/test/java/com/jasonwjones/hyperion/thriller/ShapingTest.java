package com.jasonwjones.hyperion.thriller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.jasonwjones.hyperion.thriller.parser.ColumnFactory;
import com.jasonwjones.hyperion.thriller.table.ThrillerColumn;

/**
 * The settings that reshape a result: long shape, point-of-view columns, names
 * as identifiers and what #Missing becomes.
 */
public class ShapingTest {

	private static QuerySettings settings(String... hints) throws ThrillerException {
		Properties properties = new Properties();
		for (int index = 0; index < hints.length; index += 2) {
			properties.setProperty(hints[index], hints[index + 1]);
		}
		return QuerySettings.of(Map.of(), properties, 0);
	}

	private static List<String> names(ThrillerColumn[] columns) {
		return Arrays.stream(columns).map(ThrillerColumn::getColumnName).toList();
	}

	@Test
	public void aLongResultHasARowForEachValue() throws ThrillerException {
		// Measures on the rows, Year and Product on the columns: 11 measures by 3 months
		MdxResult result = Reshaping.apply(Fixtures.result("measures-by-month"), settings("shape", "long"));
		ThrillerColumn[] columns = ResultSetBuilder.detectColumns(result);

		assertEquals(List.of("Measures", "Year", "Product", "Value"), names(columns));
		assertEquals(Types.DOUBLE, columns[3].getType());
		assertEquals(11 * 3, result.getDataRows().size());
		assertEquals(List.of("Sales", "Jan", "100-10", "4860.0"), result.getDataRows().get(0));
		assertEquals(List.of("Sales", "Feb", "100-10", "4821.0"), result.getDataRows().get(1));
	}

	@Test
	public void aLongResultCanLeaveOutMissingValues() throws ThrillerException {
		MdxResult all = Reshaping.apply(Fixtures.result("missing-values"), settings("shape", "long"));
		MdxResult present = Reshaping.apply(Fixtures.result("missing-values"), settings("shape", "long", "missing", "skip"));

		assertTrue(all.getDataRows().stream().anyMatch(row -> row.get(3).equals("#Missing")));
		assertTrue(present.getDataRows().stream().noneMatch(row -> row.get(3).equals("#Missing")));
		assertTrue(present.getDataRows().size() < all.getDataRows().size());
	}

	@Test
	public void addsThePointOfViewAsColumns() throws ThrillerException {
		MdxResult result = Reshaping.apply(Fixtures.result("missing-values"), settings("pov", "columns"));
		ThrillerColumn[] columns = ResultSetBuilder.detectColumns(result);

		assertEquals(List.of("Market", "Product", "Measures", "Scenario", "Jan"), names(columns));
		assertEquals(List.of("Utah", "100-10", "Sales", "Actual", "130.0"), result.getDataRows().get(0));
	}

	@Test
	public void putsThePointOfViewBeforeTheColumnMembersOfALongResult() throws ThrillerException {
		MdxResult result = Reshaping.apply(Fixtures.result("missing-values"), settings("pov", "columns", "shape", "long"));

		assertEquals(List.of("Market", "Product", "Measures", "Scenario", "Year", "Value"), names(ResultSetBuilder.detectColumns(result)));
		assertEquals(List.of("Utah", "100-10", "Sales", "Actual", "Jan", "130.0"), result.getDataRows().get(0));
	}

	@Test
	public void saysWhenTheServerDoesNotReportThePointOfView() {
		ThrillerException e = assertThrows(ThrillerException.class,
				() -> Reshaping.apply(Fixtures.result("essbase-21-two-row-dimensions"), settings("pov", "columns")));
		assertTrue(e.getMessage().contains("doesn't say which members"), e.getMessage());
	}

	@Test
	public void leavesOutRowsOfNothingButMissing() throws ThrillerException {
		MdxResult result = Reshaping.apply(Fixtures.result("missing-values"), settings("missing", "skip"));
		assertTrue(result.getDataRows().stream().noneMatch(row -> row.get(2).equals("#Missing")));
		long withValues = Fixtures.result("missing-values").getDataRows().stream().filter(row -> !row.get(2).equals("#Missing")).count();
		assertEquals(withValues, result.getDataRows().size());
	}

	@Test
	public void canMakeMissingZero() throws ThrillerException {
		MdxResult result = Fixtures.result("missing-values");
		ThrillerColumn[] columns = ResultSetBuilder.detectColumns(result);
		List<Object[]> rows = ResultSetBuilder.rows(columns, result, QuerySettings.Missing.ZERO);
		// Nevada's Caffeine Free Cola
		assertEquals(0.0, rows.get(5)[2]);

		ThrillerColumn decimal = new ThrillerColumn(Types.DECIMAL, 9, 2);
		assertEquals(new BigDecimal("0.00"), ResultSetBuilder.convert(decimal, 0, "#Missing", QuerySettings.Missing.ZERO));
		assertEquals("#Missing", ResultSetBuilder.convert(new ThrillerColumn(Types.VARCHAR, 8), 0, "#Missing", QuerySettings.Missing.ZERO));
		assertNull(ResultSetBuilder.convert(decimal, 0, "#Missing", QuerySettings.Missing.NULL));
	}

	@Test
	public void makesNamesIntoIdentifiers() throws ThrillerException {
		assertEquals("JAN_100_10", ResultSetBuilder.identifier("Jan 100-10", QuerySettings.Names.UPPER));
		assertEquals("pkg_type", ResultSetBuilder.identifier("Pkg Type", QuerySettings.Names.LOWER));
		assertEquals("_100_10", ResultSetBuilder.identifier("100-10", QuerySettings.Names.LOWER));
		assertEquals("Pkg Type", ResultSetBuilder.identifier("Pkg Type", QuerySettings.Names.AS_IS));

		MdxResult result = Fixtures.result("measures-by-month");
		assertEquals(List.of("MEASURES", "JAN_100_10", "FEB_100_10", "MAR_100_10"),
				names(ResultSetBuilder.detectColumns(result, settings("names", "upper").names())));
	}

	@Test
	public void refusesSettingsItDoesNotKnow() {
		ThrillerException e = assertThrows(ThrillerException.class, () -> settings("shape", "tall"));
		assertEquals("shape can be long, wide, not 'tall'", e.getMessage());
		assertThrows(ThrillerException.class, () -> settings("timeout", "soon"));
	}

	@Test
	public void takesTheTimeoutFromTheHintThenTheStatementThenTheConnection() throws ThrillerException {
		Map<String, String> connection = Map.of("timeout", "60");
		Properties none = new Properties();
		Properties hint = new Properties();
		hint.setProperty("timeout", "5");

		assertEquals(Duration.ofSeconds(5), QuerySettings.of(connection, hint, 30).options().getTimeout());
		assertEquals(Duration.ofSeconds(30), QuerySettings.of(connection, none, 30).options().getTimeout());
		assertEquals(Duration.ofSeconds(60), QuerySettings.of(connection, none, 0).options().getTimeout());
		assertNull(QuerySettings.of(Map.of(), none, 0).options().getTimeout());
	}

	@Test
	public void hintsCanAdjustJustSomeColumns() throws ThrillerException {
		MdxResult result = Fixtures.result("missing-values");
		ThrillerColumn[] detected = ResultSetBuilder.detectColumns(result);
		Properties hints = new Properties();
		hints.setProperty("column.2.name", "PRODUCT");
		hints.setProperty("column.3.scale", "1");

		ThrillerColumn[] columns = new ColumnFactory().columns(hints, detected);

		assertEquals(List.of("Market", "PRODUCT", "Jan"), names(columns));
		assertEquals(Types.VARCHAR, columns[1].getType());
		assertEquals(Types.DECIMAL, columns[2].getType(), "a scale makes a numeric column DECIMAL");
		assertEquals(1, columns[2].getScale());
	}

	@Test
	public void aHintCanAddAColumnPastTheDetectedOnes() throws ThrillerException {
		ThrillerColumn[] detected = ResultSetBuilder.detectColumns(Fixtures.result("missing-values"));
		Properties hints = new Properties();
		hints.setProperty("column.5.name", "NOTE");

		ThrillerColumn[] columns = new ColumnFactory().columns(hints, detected);

		assertArrayEquals(new String[] { "Market", "Product", "Jan", "COLUMN4", "NOTE" }, names(columns).toArray());
		assertEquals(Types.VARCHAR, columns[4].getType());
	}

	@Test
	public void recognisesAQueryThatTimedOut() {
		Exception timeout = new RuntimeException("mDXExecuteMDX failed", new java.io.IOException(new java.net.http.HttpTimeoutException("request timed out")));
		assertTrue(RestMdxExecutor.timedOut(timeout));
		assertTrue(!RestMdxExecutor.timedOut(new RuntimeException("Syntax error in input MDX query")));
	}

}
