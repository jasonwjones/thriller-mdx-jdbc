package com.jasonwjones.hyperion.thriller;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.appliedolap.essbase.EssApiException;
import com.appliedolap.essbase.EssCube;
import com.appliedolap.essbase.EssServer;
import com.appliedolap.essbase.impl.EssCubeImpl;

/**
 * Runs MDX over Essbase's REST API, through essbase-rest-client.
 *
 * It asks for the raw JSON rather than calling {@code executeMdx(String)},
 * whose grid keeps the cells but not the names of the row and column
 * dimensions - which Thriller needs to name columns and to tell header rows
 * from data.
 */
public class RestMdxExecutor implements MdxExecutor {

	private final EssServer server;

	private final Map<String, EssCube> cubes = new HashMap<String, EssCube>();

	public RestMdxExecutor(EssServer server) {
		this.server = server;
	}

	public MdxResult execute(String application, String cube, String query) throws ThrillerException {
		ByteArrayOutputStream json = new ByteArrayOutputStream();
		try {
			cube(application, cube).executeMdx(query, EssCube.MdxOutputType.JSON, new EssCubeImpl.MdxOptions(), json);
		} catch (EssApiException e) {
			throw new ThrillerException("Essbase could not run the query on " + application + "." + cube + ": " + e.getMessage(), e);
		}
		return MdxResult.fromJson(json.toString(StandardCharsets.UTF_8));
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
