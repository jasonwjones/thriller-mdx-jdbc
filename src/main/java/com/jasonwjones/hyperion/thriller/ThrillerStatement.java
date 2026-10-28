package com.jasonwjones.hyperion.thriller;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jasonwjones.hyperion.thriller.parser.ColumnFactory;
import com.jasonwjones.hyperion.thriller.parser.CubeName;
import com.jasonwjones.hyperion.thriller.parser.ParsedQuery;
import com.jasonwjones.hyperion.thriller.parser.QueryParser;
import com.jasonwjones.hyperion.thriller.table.ThrillerColumn;

/**
 * Runs MDX and hands back a result set. Everything that would change data is
 * refused; there's nothing to update.
 */
public class ThrillerStatement implements Statement {

	private static final Logger logger = LoggerFactory.getLogger(ThrillerStatement.class);

	private final ThrillerConnection connection;

	private final QueryParser parser;

	private final ColumnFactory columnFactory;

	private ResultSet currentResultSet;

	private int maxRows;

	private int queryTimeout;

	private boolean closed;

	ThrillerStatement(ThrillerConnection connection) {
		this.connection = connection;
		this.parser = new QueryParser();
		this.columnFactory = new ColumnFactory();
	}

	public ResultSet executeQuery(String sql) throws SQLException {
		execute(sql);
		return currentResultSet;
	}

	public boolean execute(String sql) throws SQLException {
		checkOpen();
		logger.debug("Executing: {}", sql);
		if (ValidationQuery.matches(sql)) {
			connection.ping();
			closeResultSet();
			currentResultSet = ValidationQuery.result(this, sql);
			return true;
		}
		List<Integer> markers = ThrillerPreparedStatement.markers(sql);
		if (!markers.isEmpty()) {
			// Not MDX anywhere outside a string or a name, so a tool meant it as a parameter
			int line = (int) sql.substring(0, markers.get(0)).chars().filter(c -> c == '\n').count() + 1;
			throw new ThrillerException("The query has a ? on line " + line + ", which only a prepared statement fills in. "
					+ "A tool's console may have its own placeholders instead, such as IntelliJ's ${name}");
		}
		ParsedQuery query = parser.parse(sql);
		Properties hints = query.getProperties();
		CubeName cube = target(query.getQuery());

		QuerySettings settings = QuerySettings.of(connection.getSettings(), hints, queryTimeout);
		MdxResult result = connection.getExecutor().execute(cube.getApplication(), cube.getCube(),
				QueryParser.withoutComments(query.getQuery()), settings.options());
		result = Reshaping.apply(result, settings);
		ThrillerColumn[] columns = columnFactory.columns(hints, ResultSetBuilder.detectColumns(result, settings.names()));
		List<Object[]> rows = ResultSetBuilder.rows(columns, result, settings.missing());
		if (maxRows > 0 && rows.size() > maxRows) {
			rows = rows.subList(0, maxRows);
		}

		closeResultSet();
		currentResultSet = new ObjectResultSet(this, rows, new ThrillerResultSetMetaData(columns, cube.getApplication(), cube.getCube()));
		return true;
	}

	/**
	 * The cube the query's FROM clause names, or failing that the one in the
	 * connection URL.
	 */
	private CubeName target(String query) throws ThrillerException {
		CubeName named = CubeName.fromQuery(query);
		if (named != null) {
			return named;
		}
		String application = connection.getSettings().get("app");
		String cube = connection.getSettings().get("cube");
		if (application != null && cube != null) {
			return new CubeName(application, cube);
		}
		String shown = query.strip().replaceAll("\\s+", " ");
		throw new ThrillerException("The query doesn't say which cube to read: give it a FROM clause such as FROM Sample.Basic, "
				+ "or add ;app=...;cube=... to the connection URL. The query was: "
				+ (shown.length() > 200 ? shown.substring(0, 200) + "..." : shown));
	}

	private void checkOpen() throws SQLException {
		if (closed) {
			throw new SQLException("This statement has been closed");
		}
		if (connection.isClosed()) {
			throw new SQLException("This statement's connection has been closed");
		}
	}

	private void closeResultSet() throws SQLException {
		if (currentResultSet != null) {
			currentResultSet.close();
			currentResultSet = null;
		}
	}

	public ResultSet getResultSet() throws SQLException {
		return currentResultSet;
	}

	/**
	 * Always -1: a query's result is a result set, never a count of rows changed.
	 */
	public int getUpdateCount() throws SQLException {
		return -1;
	}

	public boolean getMoreResults() throws SQLException {
		closeResultSet();
		return false;
	}

	public boolean getMoreResults(int current) throws SQLException {
		return getMoreResults();
	}

	public Connection getConnection() throws SQLException {
		return connection;
	}

	public void close() throws SQLException {
		closeResultSet();
		closed = true;
	}

	public boolean isClosed() throws SQLException {
		return closed;
	}

	public int getMaxRows() throws SQLException {
		return maxRows;
	}

	public void setMaxRows(int max) throws SQLException {
		this.maxRows = max;
	}

	/**
	 * Accepted so tools that set it don't fail, but the REST call has its own
	 * timeouts and this doesn't change them.
	 */
	public int getQueryTimeout() throws SQLException {
		return queryTimeout;
	}

	public void setQueryTimeout(int seconds) throws SQLException {
		this.queryTimeout = seconds;
	}

	public int getResultSetType() throws SQLException {
		return ResultSet.TYPE_FORWARD_ONLY;
	}

	public int getResultSetConcurrency() throws SQLException {
		return ResultSet.CONCUR_READ_ONLY;
	}

	public int getResultSetHoldability() throws SQLException {
		return ResultSet.CLOSE_CURSORS_AT_COMMIT;
	}

	public int getFetchDirection() throws SQLException {
		return ResultSet.FETCH_FORWARD;
	}

	public void setFetchDirection(int direction) throws SQLException {
	}

	/**
	 * The whole result comes back in one response, so there is no fetch size to
	 * honor.
	 */
	public int getFetchSize() throws SQLException {
		return 0;
	}

	public void setFetchSize(int rows) throws SQLException {
	}

	public int getMaxFieldSize() throws SQLException {
		return 0;
	}

	public void setMaxFieldSize(int max) throws SQLException {
	}

	public void setEscapeProcessing(boolean enable) throws SQLException {
	}

	public SQLWarning getWarnings() throws SQLException {
		return null;
	}

	public void clearWarnings() throws SQLException {
	}

	public void setCursorName(String name) throws SQLException {
	}

	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
		return execute(sql);
	}

	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		return execute(sql);
	}

	public boolean execute(String sql, String[] columnNames) throws SQLException {
		return execute(sql);
	}

	public int executeUpdate(String sql) throws SQLException {
		throw new SQLFeatureNotSupportedException("Thriller only runs queries");
	}

	public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
		return executeUpdate(sql);
	}

	public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
		return executeUpdate(sql);
	}

	public int executeUpdate(String sql, String[] columnNames) throws SQLException {
		return executeUpdate(sql);
	}

	public void addBatch(String sql) throws SQLException {
		throw new SQLFeatureNotSupportedException("Thriller does not run batches");
	}

	public void clearBatch() throws SQLException {
	}

	public int[] executeBatch() throws SQLException {
		throw new SQLFeatureNotSupportedException("Thriller does not run batches");
	}

	public ResultSet getGeneratedKeys() throws SQLException {
		throw new SQLFeatureNotSupportedException("Thriller only runs queries");
	}

	public void cancel() throws SQLException {
		throw new SQLFeatureNotSupportedException("A running query can't be cancelled");
	}

	public void setPoolable(boolean poolable) throws SQLException {
	}

	public boolean isPoolable() throws SQLException {
		return false;
	}

	public void closeOnCompletion() throws SQLException {
	}

	public boolean isCloseOnCompletion() throws SQLException {
		return false;
	}

	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (isWrapperFor(iface)) {
			return (T) this;
		}
		throw new SQLException("Cannot cast to " + iface.getName());
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface.isAssignableFrom(getClass());
	}

}
