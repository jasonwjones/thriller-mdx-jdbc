package com.jasonwjones.hyperion.thriller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
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
	public void failsToConnectWithTheWrongPassword() {
		assertThrows(SQLException.class, () -> DriverManager.getConnection(URL, USER, PASSWORD + "-wrong"));
	}

}
