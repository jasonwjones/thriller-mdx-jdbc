package com.jasonwjones.hyperion.thriller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * How a server's applications, cubes and dimensions appear to a tool browsing
 * the connection: as schemas, tables and columns.
 */
public class MetadataTest {

	private static final List<CubeCatalog.Dimension> SAMPLE_BASIC = List.of(
			new CubeCatalog.Dimension("Year", "DENSE", 19, 12),
			new CubeCatalog.Dimension("Measures", "DENSE", 17, 11),
			new CubeCatalog.Dimension("Product", "SPARSE", 22, 19),
			new CubeCatalog.Dimension("Market", "SPARSE", 25, 25),
			new CubeCatalog.Dimension("Pkg Type", "SPARSE", 3, 0));

	/** Sample with two cubes, ASOsamp with one, and Broken, whose cubes can't be listed */
	private final CubeCatalog catalog = new CubeCatalog() {

		private final Map<String, List<String>> cubes = Map.of("Sample", List.of("Interntl", "Basic"), "ASOsamp", List.of("Basic"));

		@Override
		public List<String> applications() {
			return List.of("Sample", "ASOsamp", "Broken");
		}

		@Override
		public List<String> cubes(String application) throws ThrillerException {
			if (!cubes.containsKey(application)) {
				throw new ThrillerException("No access to " + application);
			}
			return cubes.get(application);
		}

		@Override
		public List<Dimension> dimensions(String application, String cube) {
			return SAMPLE_BASIC;
		}

		@Override
		public List<Function> functions() {
			return List.of(new Function("Set", "Children", "Children ( member )", "Returns the children of a member."),
					new Function("Set", "Descendants", "Descendants ( member )", null),
					new Function("Numeric Value", "Avg", null, null));
		}

	};

	private DatabaseMetaData metaData() throws SQLException {
		ThrillerUrl url = ThrillerUrl.parse("jdbc:essbase:thriller:http://localhost:9000/essbase");
		return new ThrillerConnection(url, "admin", "26.1.0", (application, cube, query, options) -> null, catalog).getMetaData();
	}

	private static List<String> column(ResultSet resultSet, String column) throws SQLException {
		List<String> values = new ArrayList<>();
		while (resultSet.next()) {
			values.add(resultSet.getString(column));
		}
		return values;
	}

	@Test
	public void listsApplicationsAsSchemas() throws SQLException {
		assertEquals(List.of("ASOsamp", "Broken", "Sample"), column(metaData().getSchemas(), "TABLE_SCHEM"));
		assertEquals(List.of("Sample"), column(metaData().getSchemas(null, "Sam%"), "TABLE_SCHEM"));
		assertEquals("application", metaData().getSchemaTerm());
	}

	@Test
	public void listsCubesAsTablesLeavingOutAnApplicationItCannotOpen() throws SQLException {
		ResultSet tables = metaData().getTables(null, null, "%", null);
		List<String> names = new ArrayList<>();
		while (tables.next()) {
			assertNull(tables.getString("TABLE_CAT"));
			assertEquals("TABLE", tables.getString("TABLE_TYPE"));
			names.add(tables.getString("TABLE_SCHEM") + "." + tables.getString("TABLE_NAME"));
		}
		assertEquals(List.of("ASOsamp.Basic", "Sample.Basic", "Sample.Interntl"), names);
	}

	@Test
	public void narrowsTablesByPatternAndType() throws SQLException {
		assertEquals(List.of("Basic", "Interntl"), column(metaData().getTables(null, "Sample", null, new String[] { "TABLE" }), "TABLE_NAME"));
		assertEquals(List.of("Basic", "Basic"), column(metaData().getTables(null, null, "B_sic", null), "TABLE_NAME"));
		assertEquals(List.of("Basic"), column(metaData().getTables(null, "sample", "basic", null), "TABLE_NAME"));
		assertFalse(metaData().getTables(null, null, "%", new String[] { "VIEW" }).next());
		assertFalse(metaData().getTables("Essbase", null, "%", null).next());
		assertEquals(List.of("TABLE"), column(metaData().getTableTypes(), "TABLE_TYPE"));
	}

	@Test
	public void listsDimensionsAsColumnsInOutlineOrder() throws SQLException {
		ResultSet columns = metaData().getColumns(null, "Sample", "Basic", "%");
		assertTrue(columns.next());
		assertEquals("Year", columns.getString("COLUMN_NAME"));
		assertEquals(Types.VARCHAR, columns.getInt("DATA_TYPE"));
		assertEquals("VARCHAR", columns.getString("TYPE_NAME"));
		assertEquals(1, columns.getInt("ORDINAL_POSITION"));
		assertEquals(DatabaseMetaData.columnNoNulls, columns.getInt("NULLABLE"));
		assertEquals("Dense, 19 members, 12 stored", columns.getString("REMARKS"));
		assertEquals(Types.INTEGER, columns.getMetaData().getColumnType(columns.findColumn("DATA_TYPE")));

		List<String> rest = column(columns, "COLUMN_NAME");
		assertEquals(List.of("Measures", "Product", "Market", "Pkg Type"), rest);
		assertEquals(List.of("Product", "Pkg Type"), column(metaData().getColumns(null, "Sample", "Basic", "P%"), "COLUMN_NAME"));
	}

	@Test
	public void describesADimensionByWhatTheServerSaid() {
		assertEquals("Sparse, 1 member, 0 stored", ThrillerDatabaseMetaData.remarks(new CubeCatalog.Dimension("Pkg Type", "SPARSE", 1, 0)));
		assertEquals("Dense", ThrillerDatabaseMetaData.remarks(new CubeCatalog.Dimension("Year", "DENSE", null, null)));
		assertNull(ThrillerDatabaseMetaData.remarks(new CubeCatalog.Dimension("Year", null, null, null)));
	}

	@Test
	public void matchesJdbcSearchPatterns() {
		assertTrue(ThrillerDatabaseMetaData.matches(null, "Basic"));
		assertTrue(ThrillerDatabaseMetaData.matches("%", "Basic"));
		assertTrue(ThrillerDatabaseMetaData.matches("Ba%", "Basic"));
		assertTrue(ThrillerDatabaseMetaData.matches("B_sic", "Basic"));
		assertTrue(ThrillerDatabaseMetaData.matches("BASIC", "Basic"));
		assertTrue(ThrillerDatabaseMetaData.matches("Pkg Type", "Pkg Type"));
		assertFalse(ThrillerDatabaseMetaData.matches("", "Basic"));
		assertFalse(ThrillerDatabaseMetaData.matches("Ba", "Basic"));
		assertFalse(ThrillerDatabaseMetaData.matches("B.sic", "Basic"));
	}

	@Test
	public void listsTheServersMdxFunctionsUnderEachApplication() throws SQLException {
		ResultSet functions = metaData().getFunctions(null, "Sample", "%");
		assertTrue(functions.next());
		assertEquals("Sample", functions.getString("FUNCTION_SCHEM"));
		assertEquals("Children", functions.getString("FUNCTION_NAME"));
		assertEquals("Children ( member ): Returns the children of a member.", functions.getString("REMARKS"));
		assertEquals(DatabaseMetaData.functionResultUnknown, functions.getInt("FUNCTION_TYPE"));

		assertEquals(List.of("Descendants"), column(metaData().getFunctions(null, "Sample", "Desc%"), "FUNCTION_NAME"));
		assertEquals(3 * 3, column(metaData().getFunctions(null, null, "%"), "FUNCTION_NAME").size());
		assertEquals("Numeric Value", ThrillerDatabaseMetaData.remarks(new CubeCatalog.Function("Numeric Value", "Avg", null, null)));
	}

}
