package com.jasonwjones.hyperion.thriller;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * Thriller as a {@link DataSource}, for frameworks and pools that configure one
 * by its properties rather than a driver by its URL. In Spring, for example:
 *
 * <pre>
 * &#64;Bean
 * DataSource essbase() {
 *     ThrillerDataSource dataSource = new ThrillerDataSource();
 *     dataSource.setUrl("jdbc:essbase:thriller:http://localhost:9000/essbase");
 *     dataSource.setUser("admin");
 *     dataSource.setPassword(password);
 *     dataSource.setProperty("members", "alias");
 *     return dataSource;
 * }
 * </pre>
 *
 * Each connection logs in afresh; put a pool in front for reuse.
 */
public class ThrillerDataSource implements DataSource {

	private String url;

	private String user;

	private String password;

	private final Properties properties = new Properties();

	private int loginTimeout;

	private PrintWriter logWriter;

	/**
	 * @return the Thriller URL connections are made to
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param url a Thriller URL, such as
	 *            jdbc:essbase:thriller:http://localhost:9000/essbase
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return the user connections log in as
	 */
	public String getUser() {
		return user;
	}

	/**
	 * @param user the user to log in as
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * @param password the password to log in with, or the token or session id
	 *            with auth=token or auth=session
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Sets a connection setting, such as members or auth, as the URL could.
	 *
	 * @param name the setting
	 * @param value its value
	 */
	public void setProperty(String name, String value) {
		properties.setProperty(name, value);
	}

	/**
	 * @param properties connection settings to add, such as members or auth
	 */
	public void setProperties(Properties properties) {
		this.properties.putAll(properties);
	}

	@Override
	public Connection getConnection() throws SQLException {
		return getConnection(user, password);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		if (url == null) {
			throw new ThrillerException("The data source has no URL to connect to");
		}
		Properties info = new Properties();
		info.putAll(properties);
		if (username != null) {
			info.setProperty("user", username);
		}
		if (password != null) {
			info.setProperty("password", password);
		}
		return new ThrillerConnection(ThrillerUrl.parse(url), info);
	}

	@Override
	public PrintWriter getLogWriter() {
		return logWriter;
	}

	@Override
	public void setLogWriter(PrintWriter out) {
		this.logWriter = out;
	}

	@Override
	public void setLoginTimeout(int seconds) {
		this.loginTimeout = seconds;
	}

	@Override
	public int getLoginTimeout() {
		return loginTimeout;
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException("Thriller logs through SLF4J");
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isInstance(this)) {
			return iface.cast(this);
		}
		throw new SQLException("Not a wrapper for " + iface.getName());
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) {
		return iface.isInstance(this);
	}

}
