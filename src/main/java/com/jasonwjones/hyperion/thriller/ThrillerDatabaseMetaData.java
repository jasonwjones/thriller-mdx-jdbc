package com.jasonwjones.hyperion.thriller;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jasonwjones.hyperion.thriller.table.ThrillerColumn;

/**
 * Describes Thriller and the server it is connected to.
 *
 * Tools browse a database through these methods as soon as they connect, so
 * the server is laid out the way they expect: each application is a schema,
 * each cube a table in it, and each dimension a column of that, with its
 * storage and member counts as the column's remarks. That puts Sample.Basic in
 * a tool's tree where FROM Sample.Basic says it is. Everything else a tool might
 * ask about, such as keys and procedures, answers with an empty result rather
 * than null.
 */
public class ThrillerDatabaseMetaData implements DatabaseMetaData {

	private static final Logger logger = LoggerFactory.getLogger(ThrillerDatabaseMetaData.class);

	/** The one table type, since a cube is the only thing to query */
	static final String TABLE_TYPE = "TABLE";

	private static final String[] TABLE_COLUMNS = { "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "TABLE_TYPE", "REMARKS", "TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "SELF_REFERENCING_COL_NAME", "REF_GENERATION" };

	private static final String[] COLUMN_COLUMNS = { "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "DATA_TYPE", "TYPE_NAME", "COLUMN_SIZE", "BUFFER_LENGTH", "DECIMAL_DIGITS", "NUM_PREC_RADIX", "NULLABLE", "REMARKS", "COLUMN_DEF", "SQL_DATA_TYPE", "SQL_DATETIME_SUB", "CHAR_OCTET_LENGTH", "ORDINAL_POSITION", "IS_NULLABLE", "SCOPE_CATALOG", "SCOPE_SCHEMA", "SCOPE_TABLE", "SOURCE_DATA_TYPE", "IS_AUTOINCREMENT", "IS_GENERATEDCOLUMN" };

	/** The columns of {@link #getColumns} that JDBC defines as numbers */
	private static final List<String> NUMERIC_COLUMN_COLUMNS = List.of("DATA_TYPE", "COLUMN_SIZE", "BUFFER_LENGTH", "DECIMAL_DIGITS", "NUM_PREC_RADIX", "NULLABLE", "SQL_DATA_TYPE", "SQL_DATETIME_SUB", "CHAR_OCTET_LENGTH", "ORDINAL_POSITION", "SOURCE_DATA_TYPE");

	/** The longest member name Essbase allows, in characters */
	private static final int MEMBER_NAME_LENGTH = 1024;

	private final ThrillerConnection connection;

	ThrillerDatabaseMetaData(ThrillerConnection connection) {
		this.connection = connection;
	}

	/**
	 * An empty result with the columns JDBC defines for a metadata query.
	 */
	private ResultSet empty(String... columnNames) throws SQLException {
		return result(List.of(), List.of(), columnNames);
	}

	/**
	 * A result with the columns JDBC defines for a metadata query, text except
	 * for the ones named as numeric.
	 */
	private ResultSet result(List<Object[]> rows, List<String> numeric, String... columnNames) throws SQLException {
		ThrillerColumn[] columns = new ThrillerColumn[columnNames.length];
		for (int index = 0; index < columnNames.length; index++) {
			columns[index] = new ThrillerColumn(numeric.contains(columnNames[index]) ? Types.INTEGER : Types.VARCHAR, 0);
			columns[index].setColumnName(columnNames[index]);
		}
		return new ObjectResultSet(rows, new ThrillerResultSetMetaData(columns));
	}

	/**
	 * Whether a name matches a JDBC search pattern, where % is any run of
	 * characters and _ any one character. A null pattern matches everything, as
	 * JDBC has it. Essbase names aren't case sensitive, so neither is this.
	 */
	static boolean matches(String pattern, String name) {
		if (pattern == null || pattern.equals("%")) {
			return true;
		}
		StringBuilder regex = new StringBuilder();
		for (char c : pattern.toCharArray()) {
			if (c == '%') {
				regex.append(".*");
			} else if (c == '_') {
				regex.append('.');
			} else {
				regex.append(Pattern.quote(String.valueOf(c)));
			}
		}
		return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(name).matches();
	}

	/**
	 * Nothing here belongs to a catalog, so only asking for everything, or for
	 * what has no catalog, finds anything.
	 */
	private static boolean noCatalog(String catalog) {
		return catalog == null || catalog.isEmpty();
	}

	private static List<String> sorted(List<String> names) {
		List<String> sorted = new ArrayList<>(names);
		sorted.sort(String.CASE_INSENSITIVE_ORDER);
		return sorted;
	}

	/**
	 * The applications a schema pattern matches, in order.
	 */
	private List<String> applications(CubeCatalog catalog, String schemaPattern) throws SQLException {
		List<String> applications = new ArrayList<>();
		for (String application : sorted(catalog.applications())) {
			if (matches(schemaPattern, application)) {
				applications.add(application);
			}
		}
		return applications;
	}

	/**
	 * The cubes in an application that a table pattern matches, in order. An
	 * application whose cubes can't be listed is left out rather than failing the
	 * whole tree.
	 */
	private List<String> cubes(CubeCatalog catalog, String application, String tableNamePattern) {
		List<String> cubes = new ArrayList<>();
		try {
			for (String cube : sorted(catalog.cubes(application))) {
				if (matches(tableNamePattern, cube)) {
					cubes.add(cube);
				}
			}
		} catch (ThrillerException e) {
			logger.warn("Leaving {} out of the metadata: {}", application, e.getMessage());
		}
		return cubes;
	}

	/**
	 * What a tool shows beside a dimension, such as "Dense, 19 members, 12 stored".
	 */
	static String remarks(CubeCatalog.Dimension dimension) {
		List<String> parts = new ArrayList<>();
		if (dimension.storageType() != null) {
			String storage = dimension.storageType();
			parts.add(storage.substring(0, 1).toUpperCase(Locale.ROOT) + storage.substring(1).toLowerCase(Locale.ROOT));
		}
		if (dimension.members() != null) {
			parts.add(dimension.members() + (dimension.members() == 1 ? " member" : " members"));
		}
		if (dimension.storedMembers() != null) {
			parts.add(dimension.storedMembers() + " stored");
		}
		return parts.isEmpty() ? null : String.join(", ", parts);
	}


	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (isWrapperFor(iface)) {
			return iface.cast(this);
		}
		throw new SQLException("Cannot cast to " + iface.getName());
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface.isInstance(this);
	}

	@Override
	public boolean allProceduresAreCallable() throws SQLException {
		return false;
	}

	@Override
	public boolean allTablesAreSelectable() throws SQLException {
		return false;
	}

	@Override
	public String getURL() throws SQLException {
		return connection.getUrl().getUrl();
	}

	@Override
	public String getUserName() throws SQLException {
		return connection.getUser();
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		return true;
	}

	@Override
	public boolean nullsAreSortedHigh() throws SQLException {
		return false;
	}

	@Override
	public boolean nullsAreSortedLow() throws SQLException {
		return false;
	}

	@Override
	public boolean nullsAreSortedAtStart() throws SQLException {
		return false;
	}

	@Override
	public boolean nullsAreSortedAtEnd() throws SQLException {
		return false;
	}

	@Override
	public String getDatabaseProductName() throws SQLException {
		return "Essbase";
	}

	@Override
	public String getDatabaseProductVersion() throws SQLException {
		return connection.getServerVersion();
	}

	@Override
	public String getDriverName() throws SQLException {
		return "Thriller";
	}

	@Override
	public String getDriverVersion() throws SQLException {
		return ThrillerDriver.VERSION;
	}

	@Override
	public int getDriverMajorVersion() {
		return ThrillerDriver.MAJOR_VERSION;
	}

	@Override
	public int getDriverMinorVersion() {
		return ThrillerDriver.MINOR_VERSION;
	}

	@Override
	public boolean usesLocalFiles() throws SQLException {
		return false;
	}

	@Override
	public boolean usesLocalFilePerTable() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsMixedCaseIdentifiers() throws SQLException {
		return false;
	}

	@Override
	public boolean storesUpperCaseIdentifiers() throws SQLException {
		return false;
	}

	@Override
	public boolean storesLowerCaseIdentifiers() throws SQLException {
		return false;
	}

	@Override
	public boolean storesMixedCaseIdentifiers() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
		return false;
	}

	@Override
	public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
		return false;
	}

	@Override
	public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
		return false;
	}

	@Override
	public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
		return false;
	}

	@Override
	/**
	 * MDX quotes names with brackets, which JDBC has no way to say. A space, the
	 * spec's answer for "not supported", is taken literally by tools that scan
	 * for the end of a statement - sqlline among them - and they then see an
	 * unclosed quote at every odd space. A double quote is safe: in MDX it only
	 * appears in pairs, around strings such as "Alias".
	 */
	public String getIdentifierQuoteString() throws SQLException {
		return "\"";
	}

	@Override
	public String getSQLKeywords() throws SQLException {
		return "";
	}

	@Override
	public String getNumericFunctions() throws SQLException {
		return "";
	}

	@Override
	public String getStringFunctions() throws SQLException {
		return "";
	}

	@Override
	public String getSystemFunctions() throws SQLException {
		return "";
	}

	@Override
	public String getTimeDateFunctions() throws SQLException {
		return "";
	}

	@Override
	public String getSearchStringEscape() throws SQLException {
		return "";
	}

	@Override
	public String getExtraNameCharacters() throws SQLException {
		return "";
	}

	@Override
	public boolean supportsAlterTableWithAddColumn() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsAlterTableWithDropColumn() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsColumnAliasing() throws SQLException {
		return false;
	}

	@Override
	public boolean nullPlusNonNullIsNull() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsConvert() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsConvert(int fromType, int toType) throws SQLException {
		return false;
	}

	@Override
	public boolean supportsTableCorrelationNames() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsDifferentTableCorrelationNames() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsExpressionsInOrderBy() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsOrderByUnrelated() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsGroupBy() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsGroupByUnrelated() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsGroupByBeyondSelect() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsLikeEscapeClause() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsMultipleResultSets() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsMultipleTransactions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsNonNullableColumns() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsMinimumSQLGrammar() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsCoreSQLGrammar() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsExtendedSQLGrammar() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsANSI92EntryLevelSQL() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsANSI92IntermediateSQL() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsANSI92FullSQL() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsIntegrityEnhancementFacility() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsOuterJoins() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsFullOuterJoins() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsLimitedOuterJoins() throws SQLException {
		return false;
	}

	@Override
	public String getSchemaTerm() throws SQLException {
		return "application";
	}

	@Override
	public String getProcedureTerm() throws SQLException {
		return "procedure";
	}

	@Override
	public String getCatalogTerm() throws SQLException {
		return "catalog";
	}

	@Override
	public boolean isCatalogAtStart() throws SQLException {
		return false;
	}

	@Override
	public String getCatalogSeparator() throws SQLException {
		return ".";
	}

	@Override
	public boolean supportsSchemasInDataManipulation() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsSchemasInProcedureCalls() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsSchemasInTableDefinitions() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsSchemasInIndexDefinitions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsCatalogsInDataManipulation() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsCatalogsInProcedureCalls() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsCatalogsInTableDefinitions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsPositionedDelete() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsPositionedUpdate() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsSelectForUpdate() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsStoredProcedures() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsSubqueriesInComparisons() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsSubqueriesInExists() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsSubqueriesInIns() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsSubqueriesInQuantifieds() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsCorrelatedSubqueries() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsUnion() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsUnionAll() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
		return false;
	}

	@Override
	public int getMaxBinaryLiteralLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxCharLiteralLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxColumnNameLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxColumnsInGroupBy() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxColumnsInIndex() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxColumnsInOrderBy() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxColumnsInSelect() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxColumnsInTable() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxConnections() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxCursorNameLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxIndexLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxSchemaNameLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxProcedureNameLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxCatalogNameLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxRowSize() throws SQLException {
		return 0;
	}

	@Override
	public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
		return false;
	}

	@Override
	public int getMaxStatementLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxStatements() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxTableNameLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxTablesInSelect() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxUserNameLength() throws SQLException {
		return 0;
	}

	@Override
	public int getDefaultTransactionIsolation() throws SQLException {
		return Connection.TRANSACTION_NONE;
	}

	@Override
	public boolean supportsTransactions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsTransactionIsolationLevel(int level) throws SQLException {
		return level == Connection.TRANSACTION_NONE;
	}

	@Override
	public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
		return false;
	}

	@Override
	public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
		return false;
	}

	@Override
	public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
		return false;
	}

	@Override
	public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern)
			throws SQLException {
		return empty("PROCEDURE_CAT", "PROCEDURE_SCHEM", "PROCEDURE_NAME", "RESERVED1", "RESERVED2", "RESERVED3", "REMARKS", "PROCEDURE_TYPE", "SPECIFIC_NAME");
	}

	@Override
	public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern,
			String columnNamePattern) throws SQLException {
		return empty("PROCEDURE_CAT", "PROCEDURE_SCHEM", "PROCEDURE_NAME", "COLUMN_NAME", "COLUMN_TYPE", "DATA_TYPE", "TYPE_NAME", "PRECISION", "LENGTH", "SCALE", "RADIX", "NULLABLE", "REMARKS", "COLUMN_DEF", "SQL_DATA_TYPE", "SQL_DATETIME_SUB", "CHAR_OCTET_LENGTH", "ORDINAL_POSITION", "IS_NULLABLE", "SPECIFIC_NAME");
	}

	@Override
	public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types)
			throws SQLException {
		CubeCatalog cubeCatalog = connection.getCubeCatalog();
		boolean wantsTables = types == null || List.of(types).stream().anyMatch(TABLE_TYPE::equalsIgnoreCase);
		if (cubeCatalog == null || !noCatalog(catalog) || !wantsTables) {
			return empty(TABLE_COLUMNS);
		}
		List<Object[]> rows = new ArrayList<>();
		for (String application : applications(cubeCatalog, schemaPattern)) {
			for (String cube : cubes(cubeCatalog, application, tableNamePattern)) {
				rows.add(new Object[] { null, application, cube, TABLE_TYPE, "Essbase cube", null, null, null, null, null });
			}
		}
		return result(rows, List.of(), TABLE_COLUMNS);
	}

	@Override
	public ResultSet getSchemas() throws SQLException {
		return getSchemas(null, null);
	}

	@Override
	public ResultSet getCatalogs() throws SQLException {
		return empty("TABLE_CAT");
	}

	@Override
	public ResultSet getTableTypes() throws SQLException {
		return result(Collections.singletonList(new Object[] { TABLE_TYPE }), List.of(), "TABLE_TYPE");
	}

	@Override
	public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern)
			throws SQLException {
		CubeCatalog cubeCatalog = connection.getCubeCatalog();
		if (cubeCatalog == null || !noCatalog(catalog)) {
			return result(List.of(), NUMERIC_COLUMN_COLUMNS, COLUMN_COLUMNS);
		}
		List<Object[]> rows = new ArrayList<>();
		for (String application : applications(cubeCatalog, schemaPattern)) {
			for (String cube : cubes(cubeCatalog, application, tableNamePattern)) {
				List<CubeCatalog.Dimension> dimensions;
				try {
					dimensions = cubeCatalog.dimensions(application, cube);
				} catch (ThrillerException e) {
					logger.warn("Leaving the dimensions of {}.{} out of the metadata: {}", application, cube, e.getMessage());
					continue;
				}
				for (int index = 0; index < dimensions.size(); index++) {
					CubeCatalog.Dimension dimension = dimensions.get(index);
					if (matches(columnNamePattern, dimension.name())) {
						rows.add(new Object[] { null, application, cube, dimension.name(), Types.VARCHAR, "VARCHAR", MEMBER_NAME_LENGTH, null, null, null,
								columnNoNulls, remarks(dimension), null, null, null, MEMBER_NAME_LENGTH, index + 1, "NO", null, null, null, null, "NO", "NO" });
					}
				}
			}
		}
		return result(rows, NUMERIC_COLUMN_COLUMNS, COLUMN_COLUMNS);
	}

	@Override
	public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern)
			throws SQLException {
		return empty("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "GRANTOR", "GRANTEE", "PRIVILEGE", "IS_GRANTABLE");
	}

	@Override
	public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern)
			throws SQLException {
		return empty("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "GRANTOR", "GRANTEE", "PRIVILEGE", "IS_GRANTABLE");
	}

	@Override
	public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable)
			throws SQLException {
		return empty("SCOPE", "COLUMN_NAME", "DATA_TYPE", "TYPE_NAME", "COLUMN_SIZE", "BUFFER_LENGTH", "DECIMAL_DIGITS", "PSEUDO_COLUMN");
	}

	@Override
	public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {
		return empty("SCOPE", "COLUMN_NAME", "DATA_TYPE", "TYPE_NAME", "COLUMN_SIZE", "BUFFER_LENGTH", "DECIMAL_DIGITS", "PSEUDO_COLUMN");
	}

	@Override
	public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
		return empty("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "KEY_SEQ", "PK_NAME");
	}

	@Override
	public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
		return empty("PKTABLE_CAT", "PKTABLE_SCHEM", "PKTABLE_NAME", "PKCOLUMN_NAME", "FKTABLE_CAT", "FKTABLE_SCHEM", "FKTABLE_NAME", "FKCOLUMN_NAME", "KEY_SEQ", "UPDATE_RULE", "DELETE_RULE", "FK_NAME", "PK_NAME", "DEFERRABILITY");
	}

	@Override
	public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
		return empty("PKTABLE_CAT", "PKTABLE_SCHEM", "PKTABLE_NAME", "PKCOLUMN_NAME", "FKTABLE_CAT", "FKTABLE_SCHEM", "FKTABLE_NAME", "FKCOLUMN_NAME", "KEY_SEQ", "UPDATE_RULE", "DELETE_RULE", "FK_NAME", "PK_NAME", "DEFERRABILITY");
	}

	@Override
	public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable,
			String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException {
		return empty("PKTABLE_CAT", "PKTABLE_SCHEM", "PKTABLE_NAME", "PKCOLUMN_NAME", "FKTABLE_CAT", "FKTABLE_SCHEM", "FKTABLE_NAME", "FKCOLUMN_NAME", "KEY_SEQ", "UPDATE_RULE", "DELETE_RULE", "FK_NAME", "PK_NAME", "DEFERRABILITY");
	}

	@Override
	public ResultSet getTypeInfo() throws SQLException {
		String[] columns = { "TYPE_NAME", "DATA_TYPE", "PRECISION", "LITERAL_PREFIX", "LITERAL_SUFFIX", "CREATE_PARAMS", "NULLABLE", "CASE_SENSITIVE", "SEARCHABLE", "UNSIGNED_ATTRIBUTE", "FIXED_PREC_SCALE", "AUTO_INCREMENT", "LOCAL_TYPE_NAME", "MINIMUM_SCALE", "MAXIMUM_SCALE", "SQL_DATA_TYPE", "SQL_DATETIME_SUB", "NUM_PREC_RADIX" };
		// The types a column hint can name, in the order JDBC asks for: by DATA_TYPE
		List<Object[]> rows = new ArrayList<>();
		rows.add(typeInfo("TINYINT", Types.TINYINT, 3, null, null, 10));
		rows.add(typeInfo("SMALLINT", Types.SMALLINT, 5, null, null, 10));
		rows.add(typeInfo("INTEGER", Types.INTEGER, 10, null, null, 10));
		rows.add(typeInfo("FLOAT", Types.FLOAT, 15, null, null, 2));
		rows.add(typeInfo("DOUBLE", Types.DOUBLE, 15, null, null, 2));
		rows.add(typeInfo("NUMERIC", Types.NUMERIC, 38, null, "precision,scale", 10));
		rows.add(typeInfo("DECIMAL", Types.DECIMAL, 38, null, "precision,scale", 10));
		rows.add(typeInfo("VARCHAR", Types.VARCHAR, MEMBER_NAME_LENGTH, "'", "length", null));
		rows.add(typeInfo("NVARCHAR", Types.NVARCHAR, MEMBER_NAME_LENGTH, "'", "length", null));
		rows.sort((a, b) -> Integer.compare((Integer) a[1], (Integer) b[1]));
		return result(rows, List.of("DATA_TYPE", "PRECISION", "NULLABLE", "SEARCHABLE", "MINIMUM_SCALE", "MAXIMUM_SCALE", "SQL_DATA_TYPE", "SQL_DATETIME_SUB", "NUM_PREC_RADIX"), columns);
	}

	private static Object[] typeInfo(String name, int type, int precision, String quote, String createParams, Integer radix) {
		boolean text = quote != null;
		int maximumScale = type == Types.DECIMAL || type == Types.NUMERIC ? 38 : 0;
		return new Object[] { name, type, precision, quote, quote, createParams, (int) typeNullable, text, (int) typePredBasic, false,
				false, false, name, 0, maximumScale, null, null, radix };
	}

	@Override
	public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate)
			throws SQLException {
		return empty("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "NON_UNIQUE", "INDEX_QUALIFIER", "INDEX_NAME", "TYPE", "ORDINAL_POSITION", "COLUMN_NAME", "ASC_OR_DESC", "CARDINALITY", "PAGES", "FILTER_CONDITION");
	}

	@Override
	public boolean supportsResultSetType(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException {
		return false;
	}

	@Override
	public boolean ownUpdatesAreVisible(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean ownDeletesAreVisible(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean ownInsertsAreVisible(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean othersUpdatesAreVisible(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean othersDeletesAreVisible(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean othersInsertsAreVisible(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean updatesAreDetected(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean deletesAreDetected(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean insertsAreDetected(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean supportsBatchUpdates() throws SQLException {
		return false;
	}

	@Override
	public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types)
			throws SQLException {
		return empty("TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "CLASS_NAME", "DATA_TYPE", "REMARKS", "BASE_TYPE");
	}

	@Override
	public Connection getConnection() throws SQLException {
		return connection;
	}

	@Override
	public boolean supportsSavepoints() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsNamedParameters() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsMultipleOpenResults() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsGetGeneratedKeys() throws SQLException {
		return false;
	}

	@Override
	public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException {
		return empty("TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "SUPERTYPE_CAT", "SUPERTYPE_SCHEM", "SUPERTYPE_NAME");
	}

	@Override
	public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
		return empty("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "SUPERTABLE_NAME");
	}

	@Override
	public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern,
			String attributeNamePattern) throws SQLException {
		return empty("TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "ATTR_NAME", "DATA_TYPE", "ATTR_TYPE_NAME", "ATTR_SIZE", "DECIMAL_DIGITS", "NUM_PREC_RADIX", "NULLABLE", "REMARKS", "ATTR_DEF", "SQL_DATA_TYPE", "SQL_DATETIME_SUB", "CHAR_OCTET_LENGTH", "ORDINAL_POSITION", "IS_NULLABLE", "SCOPE_CATALOG", "SCOPE_SCHEMA", "SCOPE_TABLE", "SOURCE_DATA_TYPE");
	}

	@Override
	public boolean supportsResultSetHoldability(int holdability) throws SQLException {
		return false;
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		return 0;
	}

	@Override
	public int getDatabaseMajorVersion() throws SQLException {
		return serverVersionPart(0);
	}

	@Override
	public int getDatabaseMinorVersion() throws SQLException {
		return serverVersionPart(1);
	}

	/**
	 * A part of the server's version, which Essbase reports as 26.1.0.0.0.
	 */
	private int serverVersionPart(int index) {
		String version = connection.getServerVersion();
		String[] parts = version == null ? new String[0] : version.split("\\.");
		try {
			return index < parts.length ? Integer.parseInt(parts[index].trim()) : 0;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	@Override
	public int getJDBCMajorVersion() throws SQLException {
		return 4;
	}

	@Override
	public int getJDBCMinorVersion() throws SQLException {
		return 3;
	}

	@Override
	public int getSQLStateType() throws SQLException {
		return 0;
	}

	@Override
	public boolean locatorsUpdateCopy() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsStatementPooling() throws SQLException {
		return false;
	}

	@Override
	public RowIdLifetime getRowIdLifetime() throws SQLException {
		return RowIdLifetime.ROWID_UNSUPPORTED;
	}

	@Override
	public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
		CubeCatalog cubeCatalog = connection.getCubeCatalog();
		if (cubeCatalog == null || !noCatalog(catalog)) {
			return empty("TABLE_SCHEM", "TABLE_CATALOG");
		}
		List<Object[]> rows = new ArrayList<>();
		for (String application : applications(cubeCatalog, schemaPattern)) {
			rows.add(new Object[] { application, null });
		}
		return result(rows, List.of(), "TABLE_SCHEM", "TABLE_CATALOG");
	}

	@Override
	public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
		return false;
	}

	@Override
	public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
		return false;
	}

	@Override
	public ResultSet getClientInfoProperties() throws SQLException {
		return empty("NAME", "MAX_LEN", "DEFAULT_VALUE", "DESCRIPTION");
	}

	@Override
	public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern)
			throws SQLException {
		String[] columns = { "FUNCTION_CAT", "FUNCTION_SCHEM", "FUNCTION_NAME", "REMARKS", "FUNCTION_TYPE", "SPECIFIC_NAME" };
		CubeCatalog cubeCatalog = connection.getCubeCatalog();
		if (cubeCatalog == null || !noCatalog(catalog)) {
			return empty(columns);
		}
		List<CubeCatalog.Function> functions;
		try {
			functions = cubeCatalog.functions();
		} catch (ThrillerException e) {
			logger.warn("Leaving the MDX functions out of the metadata: {}", e.getMessage());
			return empty(columns);
		}
		// The server's functions work in every cube, so each application lists
		// them all, which is where a tool looks for a schema's routines
		List<Object[]> rows = new ArrayList<>();
		for (String application : applications(cubeCatalog, schemaPattern)) {
			for (CubeCatalog.Function function : functions) {
				if (matches(functionNamePattern, function.name())) {
					rows.add(new Object[] { null, application, function.name(), remarks(function), functionResultUnknown, function.name() });
				}
			}
		}
		return result(rows, List.of("FUNCTION_TYPE"), columns);
	}

	/**
	 * What a tool shows beside a function: how to call it and what it does.
	 */
	static String remarks(CubeCatalog.Function function) {
		List<String> parts = new ArrayList<>();
		if (function.syntax() != null && !function.syntax().isBlank()) {
			parts.add(function.syntax().strip());
		}
		if (function.comment() != null && !function.comment().isBlank()) {
			parts.add(function.comment().strip());
		}
		return parts.isEmpty() ? function.group() : String.join(": ", parts);
	}

	@Override
	public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern,
			String columnNamePattern) throws SQLException {
		return empty("FUNCTION_CAT", "FUNCTION_SCHEM", "FUNCTION_NAME", "COLUMN_NAME", "COLUMN_TYPE", "DATA_TYPE", "TYPE_NAME", "PRECISION", "LENGTH", "SCALE", "RADIX", "NULLABLE", "REMARKS", "CHAR_OCTET_LENGTH", "ORDINAL_POSITION", "IS_NULLABLE", "SPECIFIC_NAME");
	}

	@Override
	public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern,
			String columnNamePattern) throws SQLException {
		return empty("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "DATA_TYPE", "COLUMN_SIZE", "DECIMAL_DIGITS", "NUM_PREC_RADIX", "COLUMN_USAGE", "REMARKS", "CHAR_OCTET_LENGTH", "IS_NULLABLE");
	}

	@Override
	public boolean generatedKeyAlwaysReturned() throws SQLException {
		return false;
	}

}
