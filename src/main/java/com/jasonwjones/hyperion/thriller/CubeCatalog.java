package com.jasonwjones.hyperion.thriller;

import java.util.List;

/**
 * What a server has to browse: its applications, their cubes, and each cube's
 * dimensions. Kept behind an interface, like {@link MdxExecutor}, so the JDBC
 * metadata built from it can be tested without a server.
 */
public interface CubeCatalog {

	/**
	 * A dimension of a cube.
	 *
	 * @param name the dimension's name
	 * @param storageType DENSE or SPARSE, or null if the server didn't say
	 * @param members how many members it has, or null if the server didn't say
	 * @param storedMembers how many of them are stored, or null if the server
	 *            didn't say
	 */
	record Dimension(String name, String storageType, Integer members, Integer storedMembers) {}

	/**
	 * An MDX function the server understands.
	 *
	 * @param group the server's group for it, such as Set
	 * @param name its name, such as Children
	 * @param syntax how to call it, or null if the server didn't say
	 * @param comment what it does, or null if the server didn't say
	 */
	record Function(String group, String name, String syntax, String comment) {}

	/**
	 * @return the names of the applications the user can see
	 * @throws ThrillerException if the server can't be asked
	 */
	List<String> applications() throws ThrillerException;

	/**
	 * @param application an application's name
	 * @return the names of its cubes
	 * @throws ThrillerException if the server can't be asked
	 */
	List<String> cubes(String application) throws ThrillerException;

	/**
	 * @param application an application's name
	 * @param cube one of its cubes
	 * @return the cube's dimensions, in outline order
	 * @throws ThrillerException if the server can't be asked
	 */
	List<Dimension> dimensions(String application, String cube) throws ThrillerException;

	/**
	 * @return the MDX functions the server understands; none, unless the
	 *         catalog can ask
	 * @throws ThrillerException if the server can't be asked
	 */
	default List<Function> functions() throws ThrillerException {
		return List.of();
	}

}
