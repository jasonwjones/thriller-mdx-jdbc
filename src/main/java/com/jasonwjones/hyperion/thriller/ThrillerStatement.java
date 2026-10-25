package com.jasonwjones.hyperion.thriller;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jasonwjones.hyperion.thriller.camshaft.ThrillerCamshaftOutputDelegate;
import com.jasonwjones.hyperion.thriller.parser.ColumnFactory;
import com.jasonwjones.hyperion.thriller.parser.ParsedQuery;
import com.jasonwjones.hyperion.thriller.parser.QueryParser;
import com.jasonwjones.hyperion.thriller.table.ThrillerColumn;
import com.saxifrages.hyperion.camshaft.PrintOptions;

public class ThrillerStatement implements Statement {

	private static final Logger logger = LoggerFactory.getLogger(ThrillerStatement.class);
	
	private MdxExecutor executor;
	
	private QueryParser parser;
	
	private ColumnFactory columnFactory;
	
	
	private ResultSet currentResultSet;
	
	ThrillerStatement(MdxExecutor executor) {
		this.executor = executor;
		this.parser = new QueryParser();
		this.columnFactory = new ColumnFactory();
	}
	
	public ResultSet executeQuery(String sql) throws SQLException {
		//throw new ThrillerException("Called executeQuery() with " + sql);
		execute(sql);
		return currentResultSet;
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

	/// NOT IMPLEMENTED
	
	public int executeUpdate(String sql) throws SQLException {
		throw new SQLException("Updates are not supported by this module");
	}

	public void close() throws SQLException {
		this.currentResultSet = null;
	}

	public int getMaxFieldSize() throws SQLException { 
		// TODO Auto-generated method stub
		return 0;
	}

	public void setMaxFieldSize(int max) throws SQLException {
		// TODO Auto-generated method stub
	}

	public int getMaxRows() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setMaxRows(int max) throws SQLException {
		// TODO Auto-generated method stub
	}

	public void setEscapeProcessing(boolean enable) throws SQLException {
		// TODO Auto-generated method stub
	}

	public int getQueryTimeout() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setQueryTimeout(int seconds) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void cancel() throws SQLException {
		// TODO Auto-generated method stub

	}

	public SQLWarning getWarnings() throws SQLException {
		logger.info("Asked for warnings");
		// TODO Auto-generated method stub
		return null;
	}

	public void clearWarnings() throws SQLException {
		// TODO Auto-generated method stub

	}

	public void setCursorName(String name) throws SQLException {
		// TODO Auto-generated method stub

	}

	public boolean execute(String sql) throws SQLException {
		try {
			logger.info("Called execute(): {}", sql);
			ParsedQuery query = parser.parse(sql);

			Properties queryProperties = query.getProperties();
			String mdxQuery = query.getQuery();

			PrintOptions printOptions = new QueryPropertyPrintOptions(queryProperties);
			
			ThrillerColumn[] cols = columnFactory.parse(queryProperties);
			ThrillerCamshaftOutputDelegate outputDelegate = new ThrillerCamshaftOutputDelegate(cols);
			// ThrillerCamshaftAutoDetectingOutputDelegate outputDelegate = new
			// ThrillerCamshaftAutoDetectingOutputDelegate();
			logger.warn("Before executor");
			executor.execute(mdxQuery, outputDelegate, printOptions);

			this.currentResultSet = outputDelegate.getResultSet();
			return true;
		} catch (Exception e) {
			logger.warn("Throwing ex");
			throw new ThrillerException("Error executing MDX query", e);
		}

	}

	public ResultSet getResultSet() throws SQLException {
		logger.info("Getting current result set");
		return currentResultSet;
	}

	public int getUpdateCount() throws SQLException {
		throw new SQLException("Asked for updateCount()");
		//return 0;
	}

	public boolean getMoreResults() throws SQLException {
		return false;
	}

	public void setFetchDirection(int direction) throws SQLException {
		// TODO Auto-generated method stub
	}

	public int getFetchDirection() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setFetchSize(int rows) throws SQLException {
		// TODO Auto-generated method stub
	}

	public int getFetchSize() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getResultSetConcurrency() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getResultSetType() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void addBatch(String sql) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void clearBatch() throws SQLException {
		// TODO Auto-generated method stub

	}

	public int[] executeBatch() throws SQLException {
		logger.info("Executing batch");
		// TODO Auto-generated method stub
		return null;
	}

	public Connection getConnection() throws SQLException {
		logger.info("Retrieving connection");
		// TODO Auto-generated method stub
		return null;
	}

	public boolean getMoreResults(int current) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public ResultSet getGeneratedKeys() throws SQLException {
		logger.info("Asked for generatedKeys()");
		// TODO Auto-generated method stub
		return null;
	}

	public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public int executeUpdate(String sql, String[] columnNames) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
		logger.info("Called execute (2)");
		// TODO Auto-generated method stub
		return false;
	}

	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean execute(String sql, String[] columnNames) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public int getResultSetHoldability() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean isClosed() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public void setPoolable(boolean poolable) throws SQLException {
		// TODO Auto-generated method stub

	}

	public boolean isPoolable() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public void closeOnCompletion() throws SQLException {
		// TODO Auto-generated method stub

	}

	public boolean isCloseOnCompletion() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

}
