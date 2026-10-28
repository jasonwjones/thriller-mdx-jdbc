package com.jasonwjones.hyperion.thriller;

import java.sql.SQLException;

import com.appliedolap.essbase.EssMdxOptions;

/**
 * Runs an MDX query against a cube. Kept behind an interface so the result
 * handling can be tested without a server.
 */
public interface MdxExecutor {

	/**
	 * @param application the application
	 * @param cube the cube
	 * @param query the query
	 * @param options how the server should name the members
	 * @return the result
	 * @throws java.sql.SQLTimeoutException if the query outlasts the options'
	 *             timeout
	 * @throws SQLException if the query fails otherwise
	 */
	MdxResult execute(String application, String cube, String query, EssMdxOptions options) throws SQLException;

}
