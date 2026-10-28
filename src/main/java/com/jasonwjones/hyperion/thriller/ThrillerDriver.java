package com.jasonwjones.hyperion.thriller;

import java.io.IOException;
import java.io.InputStream;
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

	/** The version of Thriller, such as 1.1.0, as the build recorded it */
	public static final String VERSION = version();

	public static final int MAJOR_VERSION = versionPart(0);

	public static final int MINOR_VERSION = versionPart(1);

	static {
		try {
			java.sql.DriverManager.registerDriver(new ThrillerDriver());
		} catch (SQLException e) {
			throw new RuntimeException("Unable to register the Thriller driver: " + e.getMessage());
		}
	}

	private static String version() {
		Properties properties = new Properties();
		try (InputStream in = ThrillerDriver.class.getResourceAsStream("thriller.properties")) {
			if (in != null) {
				properties.load(in);
			}
		} catch (IOException e) {
			// Reported as 0.0 below
		}
		String version = properties.getProperty("version", "0.0");
		return version.startsWith("$") ? "0.0" : version;
	}

	private static int versionPart(int index) {
		String[] parts = VERSION.split("[.-]");
		try {
			return index < parts.length ? Integer.parseInt(parts[index]) : 0;
		} catch (NumberFormatException e) {
			return 0;
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
		DriverPropertyInfo app = new DriverPropertyInfo("app", info.getProperty("app"));
		app.description = "Application for queries whose FROM clause doesn't name one; also ;app=... in the URL";
		DriverPropertyInfo cube = new DriverPropertyInfo("cube", info.getProperty("cube"));
		cube.description = "Cube for queries whose FROM clause doesn't name one; also ;cube=... in the URL";
		return new DriverPropertyInfo[] { user, password, app, cube,
				setting(info, QuerySettings.MEMBERS, "How to name members", "name", "alias", "unique"),
				setting(info, QuerySettings.ALIAS_TABLE, "The alias table for members=alias, such as Long Names"),
				setting(info, QuerySettings.SHAPE, "wide for the grid as Essbase lays it out, long for a row per value", "wide", "long"),
				setting(info, QuerySettings.POV, "columns to add the point of view's members as columns", "none", "columns"),
				setting(info, QuerySettings.NAMES, "Column names as the members have them, or as identifiers", "as-is", "upper", "lower"),
				setting(info, QuerySettings.MISSING, "What #Missing becomes; skip leaves rows of it out", "null", "zero", "skip"),
				setting(info, QuerySettings.TIMEOUT, "Seconds to wait for a query before giving up"),
				setting(info, ThrillerConnection.AUTH, "password, token for a bearer token as the password, or session for a session id as the password",
						"password", "token", "session") };
	}

	private static DriverPropertyInfo setting(Properties info, String name, String description, String... choices) {
		DriverPropertyInfo setting = new DriverPropertyInfo(name, info.getProperty(name));
		setting.description = description + "; also ;" + name + "=... in the URL";
		if (choices.length > 0) {
			setting.choices = choices;
		}
		return setting;
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
