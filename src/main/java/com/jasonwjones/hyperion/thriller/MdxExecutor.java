package com.jasonwjones.hyperion.thriller;

/**
 * Runs an MDX query against a cube. Kept behind an interface so the result
 * handling can be tested without a server.
 */
public interface MdxExecutor {

	public MdxResult execute(String application, String cube, String query) throws ThrillerException;

}
