package com.jasonwjones.hyperion.thriller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Types;

import org.junit.jupiter.api.Test;

/**
 * Drives the JDBC classes the way a tool would, with an executor that answers
 * from recorded results instead of a server.
 */
public class JdbcTest {

	private static final String URL = "jdbc:essbase:thriller:http://localhost:9000/essbase";

	private String application;

	private String cube;

	private MdxResult next;

	private ThrillerConnection connect(String url) throws ThrillerException {
		return new ThrillerConnection(ThrillerUrl.parse(url), "admin", "26.1.0", (application, cube, query) -> {
			this.application = application;
			this.cube = cube;
			return next;
		});
	}

	@Test
	public void runsAHintedQuery() throws Exception {
		next = Fixtures.result("measures-by-month");
		try (Connection connection = connect(URL); Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(Fixtures.text("test-01.mdx"))) {
			assertEquals("Sample", application);
			assertEquals("Basic", cube);

			ResultSetMetaData metaData = resultSet.getMetaData();
			assertEquals(4, metaData.getColumnCount());
			assertEquals("JAN", metaData.getColumnLabel(2));
			assertEquals(Types.DECIMAL, metaData.getColumnType(2));
			assertEquals("DECIMAL", metaData.getColumnTypeName(2));
			assertEquals(BigDecimal.class.getName(), metaData.getColumnClassName(2));

			assertTrue(resultSet.next());
			assertEquals("Sales", resultSet.getString("MEASURE"));
			assertEquals(new BigDecimal("4860.0"), resultSet.getBigDecimal("jan"));
			assertEquals(4821, resultSet.getInt("FEB"));
			assertEquals(4904.0, resultSet.getDouble(4));
			assertSame(statement, resultSet.getStatement());

			int rows = 1;
			while (resultSet.next()) {
				rows++;
			}
			assertEquals(11, rows);
		}
	}

	@Test
	public void reportsMissingValuesAsNull() throws Exception {
		next = Fixtures.result("missing-values");
		try (Connection connection = connect(URL); Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT {[Jan]} ON COLUMNS, ... FROM Sample.Basic")) {
			resultSet.absolute(6);
			assertEquals("Nevada", resultSet.getString("Market"));
			assertEquals("100-30", resultSet.getString("Product"));
			assertEquals(0.0, resultSet.getDouble("Jan"));
			assertTrue(resultSet.wasNull());
			assertNull(resultSet.getObject("Jan"));
		}
	}

	@Test
	public void runsAPreparedStatement() throws Exception {
		next = Fixtures.result("dimension-properties");
		try (Connection connection = connect(URL);
				PreparedStatement statement = connection.prepareStatement(Fixtures.text("test-fetch-metadata.mdx"));
				ResultSet resultSet = statement.executeQuery()) {
			resultSet.absolute(3);
			assertEquals("100-10", resultSet.getString("MEMBER_NAME"));
			assertEquals("Cola", resultSet.getString("ALIAS"));
			assertEquals(0, resultSet.getInt("LEVEL"));
			assertFalse(resultSet.wasNull());
		}
	}

	@Test
	public void fallsBackOnTheCubeInTheUrl() throws Exception {
		next = Fixtures.result("missing-values");
		try (Connection connection = connect(URL + ";app=Demo;cube=Basic")) {
			connection.createStatement().executeQuery("SELECT {[Jan]} ON COLUMNS");
			assertEquals("Demo", application);
			assertEquals("Basic", cube);
		}
	}

	@Test
	public void needsACubeFromSomewhere() throws Exception {
		try (Connection connection = connect(URL)) {
			SQLException e = assertThrows(SQLException.class,
					() -> connection.createStatement().executeQuery("SELECT {[Jan]} ON COLUMNS"));
			assertTrue(e.getMessage().startsWith("The query doesn't say which cube to read"), e.getMessage());
		}
	}

	@Test
	public void limitsRowsToMaxRows() throws Exception {
		next = Fixtures.result("measures-by-month");
		try (Connection connection = connect(URL); Statement statement = connection.createStatement()) {
			statement.setMaxRows(2);
			ResultSet resultSet = statement.executeQuery(Fixtures.text("test-no-hints.mdx"));
			assertTrue(resultSet.next());
			assertTrue(resultSet.next());
			assertFalse(resultSet.next());
		}
	}

	@Test
	public void refusesUpdatesAndReportsNoUpdateCount() throws Exception {
		try (Connection connection = connect(URL); Statement statement = connection.createStatement()) {
			assertThrows(SQLFeatureNotSupportedException.class, () -> statement.executeUpdate("DELETE FROM Sample.Basic"));
			assertEquals(-1, statement.getUpdateCount());
			assertSame(connection, statement.getConnection());
		}
	}

	@Test
	public void refusesStatementsOnceClosed() throws Exception {
		Connection connection = connect(URL);
		connection.close();
		assertTrue(connection.isClosed());
		assertThrows(SQLException.class, connection::createStatement);
	}

	@Test
	public void describesItselfWithoutNulls() throws Exception {
		try (Connection connection = connect(URL)) {
			DatabaseMetaData metaData = connection.getMetaData();
			assertEquals("Essbase", metaData.getDatabaseProductName());
			assertEquals("26.1.0", metaData.getDatabaseProductVersion());
			assertEquals(URL, metaData.getURL());
			assertSame(connection, metaData.getConnection());

			ResultSet tables = metaData.getTables(null, null, "%", null);
			assertEquals(3, tables.findColumn("TABLE_NAME"));
			assertFalse(tables.next());
			assertFalse(metaData.getSchemas().next());
			assertFalse(metaData.getColumns(null, null, "%", "%").next());
		}
	}

}
