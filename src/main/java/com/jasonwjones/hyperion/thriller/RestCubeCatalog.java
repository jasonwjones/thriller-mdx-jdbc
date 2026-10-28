package com.jasonwjones.hyperion.thriller;

import java.util.ArrayList;
import java.util.List;

import com.appliedolap.essbase.EssApiException;
import com.appliedolap.essbase.EssApplication;
import com.appliedolap.essbase.EssCube;
import com.appliedolap.essbase.EssDimension;
import com.appliedolap.essbase.EssMdxFunction;
import com.appliedolap.essbase.EssMdxFunctionGroup;
import com.appliedolap.essbase.EssServer;

/**
 * Browses a server over its REST API, through essbase-rest-client. Nothing but
 * the MDX functions is cached, so a tool that refreshes its tree sees cubes
 * added since; the functions belong to the server's version, which doesn't
 * change under a connection.
 */
public class RestCubeCatalog implements CubeCatalog {

	private final EssServer server;

	private volatile List<Function> functions;

	public RestCubeCatalog(EssServer server) {
		this.server = server;
	}

	@Override
	public List<String> applications() throws ThrillerException {
		try {
			List<String> names = new ArrayList<>();
			for (EssApplication application : server.getApplications()) {
				names.add(application.getName());
			}
			return names;
		} catch (EssApiException e) {
			throw new ThrillerException("Could not list the applications: " + e.getMessage(), e);
		}
	}

	@Override
	public List<String> cubes(String application) throws ThrillerException {
		try {
			List<String> names = new ArrayList<>();
			for (EssCube cube : server.getApplication(application).getCubes()) {
				names.add(cube.getName());
			}
			return names;
		} catch (EssApiException e) {
			throw new ThrillerException("Could not list the cubes in " + application + ": " + e.getMessage(), e);
		}
	}

	@Override
	public List<Function> functions() throws ThrillerException {
		List<Function> cached = functions;
		if (cached == null) {
			try {
				List<Function> listed = new ArrayList<>();
				for (EssMdxFunctionGroup group : server.getMdxFunctions()) {
					for (EssMdxFunction function : group.getFunctions()) {
						listed.add(new Function(group.getName(), function.getName(), function.getSyntax(), function.getComment()));
					}
				}
				cached = List.copyOf(listed);
				functions = cached;
			} catch (EssApiException e) {
				throw new ThrillerException("Could not list the MDX functions: " + e.getMessage(), e);
			}
		}
		return cached;
	}

	@Override
	public List<Dimension> dimensions(String application, String cube) throws ThrillerException {
		try {
			List<Dimension> dimensions = new ArrayList<>();
			for (EssDimension dimension : server.getApplication(application).getCube(cube).getDimensions()) {
				EssDimension.StorageType storage = dimension.getStorageType();
				dimensions.add(new Dimension(dimension.getName(), storage == null ? null : storage.name(),
						dimension.getMemberCount(), dimension.getStoredMemberCount()));
			}
			return dimensions;
		} catch (EssApiException e) {
			throw new ThrillerException("Could not list the dimensions of " + application + "." + cube + ": " + e.getMessage(), e);
		}
	}

}
