package com.jasonwjones.hyperion.thriller;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appliedolap.essbase.EssApiException;
import com.appliedolap.essbase.EssAuthentication;
import com.appliedolap.essbase.EssServer;
import com.appliedolap.essbase.impl.EssServerImpl;

/**
 * A login to an Essbase server's REST API. Read-only and without transactions:
 * the settings tools like to change are accepted and make no difference.
 */
public class ThrillerConnection implements Connection {

	/** The setting for how to log in: password, token or session */
	static final String AUTH = "auth";

	/** The setting for a WebLogic cookie to present with a session id, when there is one */
	static final String WEBLOGIC_COOKIE = "weblogic.cookie";

	private static final Logger logger = LoggerFactory.getLogger(ThrillerConnection.class);

	private final ThrillerUrl url;

	private final String user;

	private final Map<String, String> settings;

	/** The server, or null for a connection made for tests */
	private final EssServer server;

	private final MdxExecutor executor;

	private final CubeCatalog catalog;

	private final String serverVersion;

	private boolean closed;

	ThrillerConnection(ThrillerUrl url, Properties info) throws SQLException {
		this.url = url;
		this.user = info.getProperty("user");
		this.settings = settings(url, info);
		this.server = server(url, settings, user, info.getProperty("password"));
		try {
			// Asked for now, rather than on the first query, so that a wrong
			// address or password fails at connect time where tools expect it
			this.serverVersion = server.getAbout().getVersion();
		} catch (EssApiException e) {
			if (unreachable(e)) {
				throw new ThrillerException("Could not reach Essbase at " + url.getServerUrl() + ": " + e.getMessage(), e);
			}
			throw new ThrillerException("Could not log in to " + url.getServerUrl() + (user == null ? "" : " as " + user) + ": " + e.getMessage(), e);
		}
		this.executor = new RestMdxExecutor(server);
		this.catalog = new RestCubeCatalog(server);
		logger.info("Connected to Essbase {} at {}", serverVersion, url.getServerUrl());
	}

	/**
	 * A connection that runs its queries through the given executor, for tests.
	 */
	ThrillerConnection(ThrillerUrl url, String user, String serverVersion, MdxExecutor executor) {
		this(url, user, serverVersion, executor, null);
	}

	/**
	 * A connection that runs its queries through the given executor and browses
	 * the given catalog, for tests.
	 */
	ThrillerConnection(ThrillerUrl url, String user, String serverVersion, MdxExecutor executor, CubeCatalog catalog) {
		this.url = url;
		this.user = user;
		this.settings = settings(url, new Properties());
		this.server = null;
		this.serverVersion = serverVersion;
		this.executor = executor;
		this.catalog = catalog;
	}

	/**
	 * A server to log in to the way the auth setting says: with the user and
	 * password, the default; with the password as a bearer token; or with the
	 * password as the id of a session established elsewhere, such as by signing
	 * in through an identity provider, where Essbase has no password to check.
	 * Neither of the last two needs a user.
	 */
	static EssServer server(ThrillerUrl url, Map<String, String> settings, String user, String password) throws ThrillerException {
		String auth = settings.getOrDefault(AUTH, "password").trim().toLowerCase(Locale.ROOT);
		switch (auth) {
		case "password":
			if (user == null || user.isEmpty() || password == null) {
				throw new ThrillerException("Thriller needs a user and password to log in to " + url.getServerUrl());
			}
			return new EssServerImpl(url.getServerUrl(), user, password);
		case "token":
			if (password == null || password.isEmpty()) {
				throw new ThrillerException("With auth=token, the password is the bearer token, and there isn't one");
			}
			return new EssServerImpl(url.getServerUrl(), EssAuthentication.bearerToken(password));
		case "session":
			if (password == null || password.isEmpty()) {
				throw new ThrillerException("With auth=session, the password is the session id (JSESSIONID), and there isn't one");
			}
			return new EssServerImpl(url.getServerUrl(), EssAuthentication.sessionCookie(password, settings.get(WEBLOGIC_COOKIE)));
		default:
			throw new ThrillerException("auth can be password, token or session, not '" + auth + "'");
		}
	}

	/**
	 * The connection's settings, such as members=alias: its properties, which
	 * is where a tool's driver settings put them, then the URL's, which win.
	 * The user and password aren't settings.
	 */
	static Map<String, String> settings(ThrillerUrl url, Properties info) {
		Map<String, String> settings = new LinkedHashMap<>();
		for (String name : info.stringPropertyNames()) {
			if (!name.equals("user") && !name.equals("password")) {
				settings.put(name, info.getProperty(name));
			}
		}
		settings.putAll(url.getProperties());
		return Collections.unmodifiableMap(settings);
	}

	/**
	 * Asks the server something cheap, which tells whether it still answers and
	 * keeps the session from going idle.
	 *
	 * @throws ThrillerException if the server doesn't answer
	 */
	void ping() throws ThrillerException {
		if (server == null) {
			return;
		}
		try {
			server.getAbout();
		} catch (EssApiException e) {
			throw new ThrillerException("Essbase at " + url.getServerUrl() + " didn't answer: " + e.getMessage(), e);
		}
	}

	/**
	 * Whether a failure was the server not answering at all - the message is
	 * then just an exception's class name, so it helps to say what it means.
	 */
	private static boolean unreachable(Throwable failure) {
		String message = failure.getMessage();
		return message != null && (message.contains("ConnectException") || message.contains("UnknownHostException")
				|| message.contains("HttpTimeoutException"));
	}

	ThrillerUrl getUrl() {
		return url;
	}

	String getUser() {
		return user;
	}

	/**
	 * @return the connection's settings, from its properties and its URL
	 */
	Map<String, String> getSettings() {
		return settings;
	}

	String getServerVersion() {
		return serverVersion;
	}

	MdxExecutor getExecutor() {
		return executor;
	}

	/**
	 * @return what the server has to browse, or null if this connection has
	 *         nothing to browse
	 */
	CubeCatalog getCubeCatalog() {
		return catalog;
	}

	private void checkOpen() throws SQLException {
		if (closed) {
			throw new SQLException("This connection has been closed");
		}
	}

	public Statement createStatement() throws SQLException {
		checkOpen();
		return new ThrillerStatement(this);
	}

	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		return createStatement();
	}

	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		return createStatement();
	}

	public PreparedStatement prepareStatement(String sql) throws SQLException {
		checkOpen();
		return new ThrillerPreparedStatement(this, sql);
	}

	/**
	 * This seems to be the method that Dodeca likes to call.
	 */
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException {
		return prepareStatement(sql);
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		return prepareStatement(sql);
	}

	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
		return prepareStatement(sql);
	}

	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		return prepareStatement(sql);
	}

	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		return prepareStatement(sql);
	}

	public DatabaseMetaData getMetaData() throws SQLException {
		return new ThrillerDatabaseMetaData(this);
	}

	public void close() throws SQLException {
		closed = true;
	}

	public boolean isClosed() throws SQLException {
		return closed;
	}

	/**
	 * Whether the connection is open and the server still answers. Tools call
	 * this to check a connection before they use it.
	 */
	public boolean isValid(int timeout) throws SQLException {
		if (timeout < 0) {
			throw new SQLException("The timeout can't be negative: " + timeout);
		}
		if (closed) {
			return false;
		}
		try {
			ping();
			return true;
		} catch (ThrillerException e) {
			logger.debug("The connection is no longer valid: {}", e.getMessage());
			return false;
		}
	}

	public String nativeSQL(String sql) throws SQLException {
		return sql;
	}

	public boolean getAutoCommit() throws SQLException {
		return true;
	}

	public void setAutoCommit(boolean autoCommit) throws SQLException {
	}

	public void commit() throws SQLException {
	}

	public void rollback() throws SQLException {
	}

	public boolean isReadOnly() throws SQLException {
		return true;
	}

	public void setReadOnly(boolean readOnly) throws SQLException {
	}

	public int getTransactionIsolation() throws SQLException {
		return Connection.TRANSACTION_NONE;
	}

	public void setTransactionIsolation(int level) throws SQLException {
	}

	public String getCatalog() throws SQLException {
		return null;
	}

	public void setCatalog(String catalog) throws SQLException {
	}

	public String getSchema() throws SQLException {
		return null;
	}

	public void setSchema(String schema) throws SQLException {
	}

	public int getHoldability() throws SQLException {
		return ResultSet.CLOSE_CURSORS_AT_COMMIT;
	}

	public void setHoldability(int holdability) throws SQLException {
	}

	public SQLWarning getWarnings() throws SQLException {
		return null;
	}

	public void clearWarnings() throws SQLException {
	}

	public Map<String, Class<?>> getTypeMap() throws SQLException {
		return Collections.emptyMap();
	}

	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
	}

	public String getClientInfo(String name) throws SQLException {
		return null;
	}

	public Properties getClientInfo() throws SQLException {
		return new Properties();
	}

	public void setClientInfo(String name, String value) throws SQLClientInfoException {
	}

	public void setClientInfo(Properties properties) throws SQLClientInfoException {
	}

	public int getNetworkTimeout() throws SQLException {
		return 0;
	}

	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
	}

	public void abort(Executor executor) throws SQLException {
		close();
	}

	public CallableStatement prepareCall(String sql) throws SQLException {
		throw new SQLFeatureNotSupportedException("Thriller has no stored procedures");
	}

	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		return prepareCall(sql);
	}

	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		return prepareCall(sql);
	}

	public Savepoint setSavepoint() throws SQLException {
		throw new SQLFeatureNotSupportedException("Thriller has no transactions");
	}

	public Savepoint setSavepoint(String name) throws SQLException {
		return setSavepoint();
	}

	public void rollback(Savepoint savepoint) throws SQLException {
		throw new SQLFeatureNotSupportedException("Thriller has no transactions");
	}

	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		throw new SQLFeatureNotSupportedException("Thriller has no transactions");
	}

	public Clob createClob() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	public Blob createBlob() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	public NClob createNClob() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	public SQLXML createSQLXML() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
		throw new SQLFeatureNotSupportedException();
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
