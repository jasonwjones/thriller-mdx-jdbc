package com.jasonwjones.hyperion.thriller;

import java.net.http.HttpTimeoutException;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.HashMap;
import java.util.Map;

import com.appliedolap.essbase.EssApiException;
import com.appliedolap.essbase.EssCube;
import com.appliedolap.essbase.EssMdxGrid;
import com.appliedolap.essbase.EssMdxOptions;
import com.appliedolap.essbase.EssServer;

/**
 * Runs MDX over Essbase's REST API, through essbase-rest-client.
 */
public class RestMdxExecutor implements MdxExecutor {

	private final EssServer server;

	private final Map<String, EssCube> cubes = new HashMap<>();

	public RestMdxExecutor(EssServer server) {
		this.server = server;
	}

	@Override
	public MdxResult execute(String application, String cube, String query, EssMdxOptions options) throws SQLException {
		EssMdxGrid grid;
		try {
			grid = cube(application, cube).executeMdx(query, options);
		} catch (EssApiException e) {
			if (timedOut(e)) {
				String limit = options.getTimeout() == null ? "the client's timeout" : "its timeout of " + options.getTimeout().toSeconds() + " seconds";
				throw new SQLTimeoutException("The query on " + application + "." + cube + " took longer than " + limit, e);
			}
			throw new ThrillerException("Essbase could not run the query on " + application + "." + cube + ": " + e.getMessage(), e);
		}
		return MdxResult.from(grid);
	}

	/**
	 * Whether a failure was the query outlasting its timeout.
	 */
	static boolean timedOut(Throwable failure) {
		for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
			if (cause instanceof HttpTimeoutException) {
				return true;
			}
		}
		return false;
	}

	private synchronized EssCube cube(String application, String cube) throws ThrillerException {
		String key = application + "." + cube;
		EssCube essCube = cubes.get(key);
		if (essCube == null) {
			try {
				essCube = server.getApplication(application).getCube(cube);
			} catch (EssApiException e) {
				throw new ThrillerException("Could not open the cube " + key + ": " + e.getMessage(), e);
			}
			cubes.put(key, essCube);
		}
		return essCube;
	}

}
