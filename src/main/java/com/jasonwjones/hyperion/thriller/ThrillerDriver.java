package com.jasonwjones.hyperion.thriller;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * 
 * @author jasonwjones
 *
 */
public class ThrillerDriver implements Driver {

	private static DriverPropertyInfo[] driverProperties;
	
	private static boolean hasBeenRegistered = false;
	
	public static final String START_URL = "jdbc:essbase:thriller:";
	
	static {
		try {
			java.sql.DriverManager.registerDriver(new ThrillerDriver());
		} catch (SQLException e) {
			throw new RuntimeException("Unable to register the Thriller driver: " + e.getMessage());
		}

		DriverPropertyInfo mode = new DriverPropertyInfo("mode", "embedded");
		mode.choices = new String[]{ "embedded", "server" };

		DriverPropertyInfo essHome = new DriverPropertyInfo("essHome", null);
		
		driverProperties = new DriverPropertyInfo[] {
			mode,
			essHome
		};		

	}
	
	public ThrillerDriver() {
		synchronized (this) {
			if (!hasBeenRegistered) {
				hasBeenRegistered = true;
			}
		}
	}
	
	public Connection connect(String url, Properties info) throws SQLException {
		// JDBC drivers are cycled through to find a winner. Return null if
		// we don't support it
		if (!acceptsURL(url)) {
			return null;
		}
		System.out.println("Accepts URL, parsing: " + url);
		return new ThrillerConnection(url, info);
	}

	public boolean acceptsURL(String url) throws SQLException {
        if (url != null) {
            if (url.startsWith(START_URL)) {
                return true;
            } 
        }
        return false;
	}

	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
		return driverProperties;
	}

	public int getMajorVersion() {
		return 1;
	}

	public int getMinorVersion() {
		return 0;
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
