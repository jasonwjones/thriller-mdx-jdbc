package com.jasonwjones.hyperion.thriller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Runs against a real Essbase server with Sample.Basic loaded, when given one:
 *
 * <pre>
 * THRILLER_URL=jdbc:essbase:thriller:http://localhost:9000/essbase \
 * THRILLER_USER=admin THRILLER_PASSWORD=... mvn test
 * </pre>
 *
 * Skipped otherwise.
 */
public class LiveServerTest {

	private static final String URL = System.getenv("THRILLER_URL");

	private static final String USER = System.getenv("THRILLER_USER");

	private static final String PASSWORD = System.getenv("THRILLER_PASSWORD");

	@BeforeAll
	public static void needsAServer() {
		assumeTrue(URL != null && USER != null && PASSWORD != null, "THRILLER_URL, THRILLER_USER and THRILLER_PASSWORD are not set");
	}

	@Test
	public void connectsThroughDriverManager() throws SQLException {
		try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
			assertInstanceOf(ThrillerConnection.class, connection);
			assertEquals("Essbase", connection.getMetaData().getDatabaseProductName());
		}
	}

	@Test
	public void runsAHintedQuery() throws SQLException {
		try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
				ResultSet resultSet = connection.createStatement().executeQuery(Fixtures.text("test-01.mdx"))) {
			assertTrue(resultSet.next());
			assertEquals("Sales", resultSet.getString("MEASURE"));
			assertEquals(0, new BigDecimal("4860").compareTo(resultSet.getBigDecimal("JAN")));
		}
	}

	@Test
	public void runsADimensionPropertiesQuery() throws SQLException {
		try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
				ResultSet resultSet = connection.createStatement().executeQuery(Fixtures.text("test-fetch-metadata.mdx"))) {
			boolean found = false;
			while (resultSet.next()) {
				if (resultSet.getString("MEMBER_NAME").equals("100-10")) {
					assertEquals("Cola", resultSet.getString("ALIAS"));
					assertEquals(0, resultSet.getInt("LEVEL"));
					found = true;
				}
			}
			assertTrue(found, "No row for 100-10");
		}
	}

	@Test
	public void runsAQueryWithoutHints() throws SQLException {
		String query = "SELECT {[Jan]} ON COLUMNS, CROSSJOIN({[Nevada]}, [100].Children) ON ROWS "
				+ "FROM Sample.Basic WHERE ([Sales], [Actual])";
		try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
				ResultSet resultSet = connection.createStatement().executeQuery(query)) {
			assertEquals("Market", resultSet.getMetaData().getColumnName(1));
			while (resultSet.next()) {
				if (resultSet.getString("Product").equals("100-30")) {
					assertNull(resultSet.getObject("Jan"));
				}
			}
		}
	}

	@Test
	public void namesMembersByAlias() throws SQLException {
		String query = "SELECT {[Jan]} ON COLUMNS, {[100-10]} ON ROWS FROM Sample.Basic WHERE ([Sales], [Actual], [East])";
		try (Connection connection = DriverManager.getConnection(URL + ";members=alias", USER, PASSWORD)) {
			try (ResultSet resultSet = connection.createStatement().executeQuery(query)) {
				assertTrue(resultSet.next());
				assertEquals("Cola", resultSet.getString("Product"));
			}
			// Long Names has an alias for Jan but none for 100-10, which keeps its name
			try (ResultSet resultSet = connection.createStatement().executeQuery("SELECT /* -- alias.table = Long Names */" + query.substring(6))) {
				assertEquals("January", resultSet.getMetaData().getColumnName(2));
				assertTrue(resultSet.next());
				assertEquals("100-10", resultSet.getString("Product"));
			}
		}
	}

	@Test
	public void keepsTheColumnsWhenNoRowsSurvive() throws SQLException {
		String query = "SELECT {[Jan]} ON COLUMNS, NON EMPTY {[100-30]} ON ROWS FROM Sample.Basic WHERE ([Sales], [Actual], [Nevada])";
		try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
				ResultSet resultSet = connection.createStatement().executeQuery(query)) {
			assertEquals(2, resultSet.getMetaData().getColumnCount());
			assertEquals("Product", resultSet.getMetaData().getColumnName(1));
			assertEquals("Jan", resultSet.getMetaData().getColumnName(2));
			assertFalse(resultSet.next());
		}
	}

	@Test
	public void browsesSampleBasicAsATable() throws SQLException {
		try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
			DatabaseMetaData metaData = connection.getMetaData();
			ResultSet tables = metaData.getTables(null, "Sample", "Basic", null);
			assertTrue(tables.next(), "Sample.Basic is not listed");

			ResultSet columns = metaData.getColumns(null, "Sample", "Basic", "Year");
			assertTrue(columns.next());
			assertEquals(1, columns.getInt("ORDINAL_POSITION"));
			assertTrue(columns.getString("REMARKS").startsWith("Dense, "), columns.getString("REMARKS"));
		}
	}

	@Test
	public void passesTheChecksToolsMakeOnAConnection() throws SQLException {
		try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
				ResultSet resultSet = connection.createStatement().executeQuery("SELECT 1")) {
			assertTrue(connection.isValid(5));
			assertTrue(resultSet.next());
			assertEquals(1, resultSet.getInt(1));
		}
	}

	@Test
	public void fillsInParameters() throws SQLException {
		String query = "SELECT {[Jan]} ON COLUMNS, {?} ON ROWS FROM Sample.Basic WHERE ([Sales], [Actual], ?)";
		try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
				PreparedStatement statement = connection.prepareStatement(query)) {
			statement.setString(1, "100-10");
			statement.setString(2, "East");
			try (ResultSet resultSet = statement.executeQuery()) {
				assertTrue(resultSet.next());
				assertEquals("100-10", resultSet.getString(1));
				assertEquals(1812.0, resultSet.getDouble("Jan"));
			}
		}
	}

	@Test
	public void listsTheServersMdxFunctions() throws SQLException {
		try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
				ResultSet functions = connection.getMetaData().getFunctions(null, "Sample", "Children")) {
			assertTrue(functions.next(), "Children is not listed");
			assertEquals("Sample", functions.getString("FUNCTION_SCHEM"));
		}
	}

	@Test
	public void connectsThroughADataSource() throws SQLException {
		ThrillerDataSource dataSource = new ThrillerDataSource();
		dataSource.setUrl(URL);
		dataSource.setUser(USER);
		dataSource.setPassword(PASSWORD);
		dataSource.setProperty("members", "alias");
		try (Connection connection = dataSource.getConnection();
				ResultSet resultSet = connection.createStatement().executeQuery("SELECT {[Jan]} ON 0, {[100-10]} ON 1 FROM Sample.Basic")) {
			assertTrue(resultSet.next());
			assertEquals("Cola", resultSet.getString(1));
		}
	}

	@Test
	public void failsToConnectWithTheWrongPassword() {
		assertThrows(SQLException.class, () -> DriverManager.getConnection(URL, USER, PASSWORD + "-wrong"));
	}

}
