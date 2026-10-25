package com.jasonwjones.hyperion.thriller;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Connects to Essbase's REST API with URLs such as
 * {@code jdbc:essbase:thriller:http://localhost:9000/essbase;app=Sample;cube=Basic}.
 * See {@link ThrillerUrl}.
 *
 * @author jasonwjones
 *
 */
public class ThrillerDriver implements Driver {

	public static final String START_URL = ThrillerUrl.PREFIX;

	public static final int MAJOR_VERSION = 1;

	public static final int MINOR_VERSION = 0;

	static {
		try {
			java.sql.DriverManager.registerDriver(new ThrillerDriver());
		} catch (SQLException e) {
			throw new RuntimeException("Unable to register the Thriller driver: " + e.getMessage());
		}
	}

	public Connection connect(String url, Properties info) throws SQLException {
		// JDBC drivers are cycled through to find a winner. Return null if
		// we don't support it
		if (!acceptsURL(url)) {
			return null;
		}
		return new ThrillerConnection(ThrillerUrl.parse(url), info);
	}

	public boolean acceptsURL(String url) throws SQLException {
		return ThrillerUrl.accepts(url);
	}

	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
		DriverPropertyInfo user = new DriverPropertyInfo("user", info.getProperty("user"));
		user.required = true;
		DriverPropertyInfo password = new DriverPropertyInfo("password", null);
		password.required = true;
		DriverPropertyInfo app = new DriverPropertyInfo("app", null);
		app.description = "Application for queries whose FROM clause doesn't name one; set in the URL as ;app=...";
		DriverPropertyInfo cube = new DriverPropertyInfo("cube", null);
		cube.description = "Cube for queries whose FROM clause doesn't name one; set in the URL as ;cube=...";
		return new DriverPropertyInfo[] { user, password, app, cube };
	}

	public int getMajorVersion() {
		return MAJOR_VERSION;
	}

	public int getMinorVersion() {
		return MINOR_VERSION;
	}

	/**
	 * Hahaha, ROFL.
	 */
	public boolean jdbcCompliant() {
		return false;
	}

	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException("Getting parent logger not supported");
	}

}
