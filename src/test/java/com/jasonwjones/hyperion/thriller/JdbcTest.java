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
import java.sql.DriverManager;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.appliedolap.essbase.EssCube;
import com.appliedolap.essbase.EssMdxOptions;

/**
 * Drives the JDBC classes the way a tool would, with an executor that answers
 * from recorded results instead of a server.
 */
public class JdbcTest {

	private static final String URL = "jdbc:essbase:thriller:http://localhost:9000/essbase";

	private String application;

	private String cube;

	private MdxResult next;

	private EssMdxOptions options;

	private String query;

	private ThrillerConnection connect(String url) throws ThrillerException {
		return new ThrillerConnection(ThrillerUrl.parse(url), "admin", "26.1.0", (application, cube, query, options) -> {
			this.application = application;
			this.cube = cube;
			this.options = options;
			this.query = query;
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
	public void aPlainQueryLeavesMemberNamingToTheServer() throws Exception {
		next = Fixtures.result("measures-by-month");
		try (Connection connection = connect(URL); Statement statement = connection.createStatement()) {
			statement.executeQuery(Fixtures.text("test-no-hints.mdx"));
			assertTrue(options.isEmpty());
		}
	}

	@Test
	public void namesMembersByAliasWhenTheUrlSaysTo() throws Exception {
		next = Fixtures.result("long-names");
		try (Connection connection = connect(URL + ";members=alias;alias.table=Long Names");
				ResultSet resultSet = connection.createStatement().executeQuery("SELECT {[Jan]} ON COLUMNS, {[100-10]} ON ROWS FROM Sample.Basic")) {
			assertEquals(EssCube.MdxIdentifierType.ALIAS, options.getMemberIdentifierType());
			assertEquals("Long Names", options.getAliasTableName());
			assertEquals("January", resultSet.getMetaData().getColumnLabel(2));
		}
	}

	@Test
	public void aHintOverridesTheUrlsMemberNaming() throws Exception {
		next = Fixtures.result("measures-by-month");
		String query = "SELECT /* -- members = name */ {[Jan]} ON COLUMNS FROM Sample.Basic";
		try (Connection connection = connect(URL + ";members=alias"); Statement statement = connection.createStatement()) {
			statement.executeQuery(query);
			assertEquals(EssCube.MdxIdentifierType.NAME, options.getMemberIdentifierType());
		}
	}

	@Test
	public void refusesMemberNamingItDoesNotKnow() throws Exception {
		try (Connection connection = connect(URL); Statement statement = connection.createStatement()) {
			SQLException e = assertThrows(SQLException.class,
					() -> statement.executeQuery("SELECT /* -- members = nicknames */ {[Jan]} ON COLUMNS FROM Sample.Basic"));
			assertEquals("members can be name, alias or unique, not 'nicknames'", e.getMessage());
		}
	}

	@Test
	public void answersTheQueriesToolsUseToCheckAConnection() throws Exception {
		try (Connection connection = connect(URL); Statement statement = connection.createStatement()) {
			for (String query : new String[] { "select 1", "SELECT 1 FROM DUAL", "VALUES 1", "values (1)", " SELECT 1; ", "/* ping */ SELECT 1" }) {
				ResultSet resultSet = statement.executeQuery(query);
				assertTrue(resultSet.next(), query);
				assertEquals(1, resultSet.getInt(1), query);
				assertFalse(resultSet.next(), query);
			}
			// IntelliJ's keep-alive query when it doesn't know the database
			ResultSet keepAlive = statement.executeQuery("select 'keep alive'");
			assertTrue(keepAlive.next());
			assertEquals("keep alive", keepAlive.getString(1));
			assertEquals(Types.VARCHAR, keepAlive.getMetaData().getColumnType(1));

			ResultSet aliased = statement.executeQuery("SELECT 'it''s alive' AS status FROM dual");
			assertTrue(aliased.next());
			assertEquals("it's alive", aliased.getString("STATUS"));
			assertNull(application, "a validation query shouldn't reach Essbase as MDX");
		}
	}

	@Test
	public void takesOnlyThoseQueriesForValidation() {
		assertFalse(ValidationQuery.matches("SELECT 1 ON COLUMNS FROM Sample.Basic"));
		assertFalse(ValidationQuery.matches("SELECT {[Jan]} ON 0 FROM Sample.Basic"));
		assertFalse(ValidationQuery.matches("SELECT 1 FROM Sample.Basic"));
		assertFalse(ValidationQuery.matches("SELECT 'a', 'b'"));
		assertFalse(ValidationQuery.matches(null));
	}

	@Test
	public void isValidUntilClosed() throws Exception {
		Connection connection = connect(URL);
		assertTrue(connection.isValid(5));
		assertThrows(SQLException.class, () -> connection.isValid(-1));
		connection.close();
		assertFalse(connection.isValid(5));
	}

	@Test
	public void takesSettingsFromConnectionPropertiesWithTheUrlWinning() throws Exception {
		Properties info = new Properties();
		info.setProperty("user", "admin");
		info.setProperty("password", "secret");
		info.setProperty("members", "alias");
		info.setProperty("alias.table", "Long Names");
		Map<String, String> settings = ThrillerConnection.settings(ThrillerUrl.parse(URL + ";members=name"), info);
		assertEquals("name", settings.get("members"));
		assertEquals("Long Names", settings.get("alias.table"));
		assertFalse(settings.containsKey("password"));
		assertFalse(settings.containsKey("user"));
	}

	@Test
	public void fillsInParametersAsMembersAndNumbers() throws Exception {
		next = Fixtures.result("measures-by-month");
		String mdx = "SELECT /* which product? */ {[Jan]} ON COLUMNS, {?} ON ROWS FROM Sample.Basic "
				+ "WHERE ([Sales], ?, [What?]) // why?\n";
		try (Connection connection = connect(URL); PreparedStatement statement = connection.prepareStatement(mdx)) {
			ParameterMetaData parameters = statement.getParameterMetaData();
			assertEquals(2, parameters.getParameterCount());

			statement.setString(1, "Diet [Lite]");
			statement.setBigDecimal(2, new BigDecimal("1.50"));
			statement.executeQuery();
			assertTrue(query.contains("{[Diet [Lite]]]} ON ROWS"), query);
			assertTrue(query.contains("WHERE ([Sales], 1.50, [What?])"), query);

			statement.setInt(2, 7);
			statement.executeQuery();
			assertTrue(query.contains("([Sales], 7, [What?])"), query);
		}
	}

	@Test
	public void saysWhenAPlainStatementHasAParameter() throws Exception {
		try (Connection connection = connect(URL); Statement statement = connection.createStatement()) {
			SQLException e = assertThrows(SQLException.class,
					() -> statement.executeQuery("SELECT {[Jan]} ON 0,\n {?} ON 1 FROM Sample.Basic"));
			assertTrue(e.getMessage().startsWith("The query has a ? on line 2, which only a prepared statement fills in"), e.getMessage());
			assertNull(application, "it shouldn't reach Essbase");
		}
	}

	@Test
	public void refusesParametersItCannotFillIn() throws Exception {
		try (Connection connection = connect(URL); PreparedStatement statement = connection.prepareStatement("SELECT {?} ON 0 FROM Sample.Basic")) {
			SQLException unset = assertThrows(SQLException.class, statement::executeQuery);
			assertEquals("Parameter 1 has no value", unset.getMessage());
			assertThrows(SQLException.class, () -> statement.setString(2, "East"));
			assertThrows(SQLFeatureNotSupportedException.class, () -> statement.setNull(1, Types.VARCHAR));
			assertThrows(SQLFeatureNotSupportedException.class, () -> statement.setBoolean(1, true));

			statement.setString(1, "East");
			statement.clearParameters();
			assertThrows(SQLException.class, statement::executeQuery);
		}
	}

	@Test
	public void saysWhichCubeAResultCameFrom() throws Exception {
		next = Fixtures.result("measures-by-month");
		try (Connection connection = connect(URL); ResultSet resultSet = connection.createStatement().executeQuery(Fixtures.text("test-no-hints.mdx"))) {
			assertEquals("Sample", resultSet.getMetaData().getSchemaName(1));
			assertEquals("Basic", resultSet.getMetaData().getTableName(1));
		}
	}

	@Test
	public void listsTheTypesAHintCanName() throws Exception {
		try (Connection connection = connect(URL); ResultSet types = connection.getMetaData().getTypeInfo()) {
			int last = Integer.MIN_VALUE;
			int count = 0;
			while (types.next()) {
				assertTrue(types.getInt("DATA_TYPE") >= last, "sorted by DATA_TYPE");
				last = types.getInt("DATA_TYPE");
				count++;
			}
			assertEquals(9, count);
		}
	}

	@Test
	public void logsInWithATokenOrASessionWithoutAUser() throws Exception {
		ThrillerUrl url = ThrillerUrl.parse(URL);
		ThrillerConnection.server(url, Map.of("auth", "token"), null, "eyJ0b2tlbiI6dHJ1ZX0");
		ThrillerConnection.server(url, Map.of("auth", "session", "weblogic.cookie", "wl"), null, "ABC123");

		SQLException noToken = assertThrows(SQLException.class, () -> ThrillerConnection.server(url, Map.of("auth", "token"), null, null));
		assertTrue(noToken.getMessage().contains("bearer token"), noToken.getMessage());
		SQLException noUser = assertThrows(SQLException.class, () -> ThrillerConnection.server(url, Map.of(), null, "secret"));
		assertTrue(noUser.getMessage().startsWith("Thriller needs a user and password"), noUser.getMessage());
		SQLException unknown = assertThrows(SQLException.class, () -> ThrillerConnection.server(url, Map.of("auth", "kerberos"), "admin", "x"));
		assertEquals("auth can be password, token or session, not 'kerberos'", unknown.getMessage());
	}

	@Test
	public void aDataSourceConnectsLikeTheDriver() throws Exception {
		ThrillerDataSource dataSource = new ThrillerDataSource();
		assertThrows(SQLException.class, dataSource::getConnection);

		dataSource.setUrl("jdbc:essbase:thriller:http://localhost:1/essbase");
		dataSource.setUser("admin");
		dataSource.setPassword("x");
		dataSource.setProperty("members", "alias");
		SQLException e = assertThrows(SQLException.class, dataSource::getConnection);
		assertTrue(e.getMessage().startsWith("Could not reach Essbase at http://localhost:1/essbase"), e.getMessage());
		assertTrue(dataSource.isWrapperFor(ThrillerDataSource.class));
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
	public void refusesToRunOnceTheConnectionIsClosed() throws Exception {
		Connection connection = connect(URL);
		Statement statement = connection.createStatement();
		connection.close();
		assertThrows(SQLException.class, () -> statement.executeQuery("SELECT {[Jan]} ON COLUMNS FROM Sample.Basic"));
	}

	@Test
	public void saysWhenTheServerCannotBeReached() {
		SQLException e = assertThrows(SQLException.class,
				() -> DriverManager.getConnection("jdbc:essbase:thriller:http://localhost:1/essbase", "admin", "x"));
		assertTrue(e.getMessage().startsWith("Could not reach Essbase at http://localhost:1/essbase"), e.getMessage());
	}

	@Test
	public void findsColumnsByNameWhateverTheLocale() throws Exception {
		Locale original = Locale.getDefault();
		Locale.setDefault(Locale.forLanguageTag("tr-TR"));
		try {
			next = Fixtures.result("measures-by-month");
			// Upper-cased in Turkish, "Description" becomes "DESCRİPTİON", with a dotted capital I
			String query = "/* -- column.1.name = Description\n -- column.1.type = VARCHAR */ SELECT {} ON 0 FROM Sample.Basic";
			try (Connection connection = connect(URL); ResultSet resultSet = connection.createStatement().executeQuery(query)) {
				assertTrue(resultSet.next());
				assertEquals("Sales", resultSet.getString("DESCRIPTION"));
			}
		} finally {
			Locale.setDefault(original);
		}
	}

	@Test
	public void describesItselfWithoutNulls() throws Exception {
		try (Connection connection = connect(URL)) {
			DatabaseMetaData metaData = connection.getMetaData();
			assertEquals("Essbase", metaData.getDatabaseProductName());
			assertEquals("26.1.0", metaData.getDatabaseProductVersion());
			assertEquals(26, metaData.getDatabaseMajorVersion());
			assertEquals(1, metaData.getDatabaseMinorVersion());
			assertEquals(4, metaData.getJDBCMajorVersion());
			assertEquals(3, metaData.getJDBCMinorVersion());
			assertTrue(metaData.getDriverVersion().matches("\\d+\\.\\d+\\.\\d+.*"), metaData.getDriverVersion());
			assertEquals(ThrillerDriver.MAJOR_VERSION, metaData.getDriverMajorVersion());
			assertEquals(URL, metaData.getURL());
			assertSame(connection, metaData.getConnection());
			assertEquals("\"", metaData.getIdentifierQuoteString());
			assertTrue(metaData.supportsTransactionIsolationLevel(Connection.TRANSACTION_NONE));

			ResultSet tables = metaData.getTables(null, null, "%", null);
			assertEquals(3, tables.findColumn("TABLE_NAME"));
			assertFalse(tables.next());
			assertFalse(metaData.getSchemas().next());
			assertFalse(metaData.getColumns(null, null, "%", "%").next());
		}
	}

}
