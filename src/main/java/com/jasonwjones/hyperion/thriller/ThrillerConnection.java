package com.jasonwjones.hyperion.thriller;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jasonwjones.hyperion.thriller.camshaft.CamshaftMdxExecutor;

public class ThrillerConnection implements Connection {
	
	private static final Logger logger = LoggerFactory.getLogger(ThrillerConnection.class);
	
	private EssbaseUrl url;
	
	ThrillerConnection(String url, Properties props) {
		System.out.println("Thriller connecting to: " + url);
		this.url = EssbaseUrl.parse(url, props);
		logger.info("Parsed URL to {}", this.url);
	}
	
	public <T> T unwrap(Class<T> iface) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}

	public Statement createStatement() throws SQLException {
		logger.info("Creating new statement");
		CamshaftMdxExecutor executor = new CamshaftMdxExecutor(url.getHostname(), url.getApplication(), url.getDatabase(), url.getUsername(), url.getPassword());
		return new ThrillerStatement(executor);
	}

	/**
	 * This seems to be the method that Dodeca likes to call.
	 */
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException {
		logger.info("Creating new prepared statement ({}/{})", resultSetType, resultSetConcurrency);
		CamshaftMdxExecutor executor = new CamshaftMdxExecutor(url.getHostname(), url.getApplication(), url.getDatabase(), url.getUsername(), url.getPassword());
		return new ThrillerPreparedStatement(executor, sql);
	}
	
	/// NOT IMPLEMENTED
	
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		logger.info("Creating prepared statement");
		// TODO Auto-generated method stub
		return null;
	}

	public CallableStatement prepareCall(String sql) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public String nativeSQL(String sql) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setAutoCommit(boolean autoCommit) throws SQLException {
		// TODO Auto-generated method stub

	}

	public boolean getAutoCommit() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public void commit() throws SQLException {
		// TODO Auto-generated method stub

	}

	public void rollback() throws SQLException {
		// TODO Auto-generated method stub

	}

	public void close() throws SQLException {
		// TODO Auto-generated method stub

	}

	public boolean isClosed() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public DatabaseMetaData getMetaData() throws SQLException {
		return new ThrillerDatabaseMetaData();
	}

	public void setReadOnly(boolean readOnly) throws SQLException {
		// TODO Auto-generated method stub

	}

	public boolean isReadOnly() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public void setCatalog(String catalog) throws SQLException {
		// TODO Auto-generated method stub

	}

	public String getCatalog() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setTransactionIsolation(int level) throws SQLException {
		// TODO Auto-generated method stub

	}

	public int getTransactionIsolation() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public SQLWarning getWarnings() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public void clearWarnings() throws SQLException {
		// TODO Auto-generated method stub

	}

	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		logger.info("createStatement resultSetType/Concur");
		// TODO Auto-generated method stub
		return null;
	}

	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<String, Class<?>> getTypeMap() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void setHoldability(int holdability) throws SQLException {
		// TODO Auto-generated method stub

	}

	public int getHoldability() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public Savepoint setSavepoint() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public Savepoint setSavepoint(String name) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public void rollback(Savepoint savepoint) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		// TODO Auto-generated method stub

	}

	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public Clob createClob() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public Blob createBlob() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public NClob createNClob() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public SQLXML createSQLXML() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isValid(int timeout) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public void setClientInfo(String name, String value) throws SQLClientInfoException {
		// TODO Auto-generated method stub

	}

	public void setClientInfo(Properties properties) throws SQLClientInfoException {
		// TODO Auto-generated method stub

	}

	public String getClientInfo(String name) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public Properties getClientInfo() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setSchema(String schema) throws SQLException {
		// TODO Auto-generated method stub

	}

	public String getSchema() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public void abort(Executor executor) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
		// TODO Auto-generated method stub

	}

	public int getNetworkTimeout() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

}
