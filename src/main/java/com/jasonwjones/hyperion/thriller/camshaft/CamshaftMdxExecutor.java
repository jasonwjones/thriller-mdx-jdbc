package com.jasonwjones.hyperion.thriller.camshaft;

import com.essbase.api.base.EssException;
import com.jasonwjones.hyperion.thriller.MdxExecutor;
import com.jasonwjones.hyperion.thriller.ThrillerException;
import com.saxifrages.hyperion.camshaft.Camshaft;
import com.saxifrages.hyperion.camshaft.CamshaftOutputDelegate;
import com.saxifrages.hyperion.camshaft.PrintOptions;

public class CamshaftMdxExecutor implements MdxExecutor {

	private String server;

	private String app;

	private String cube;

	private String username;

	private String password;

	public CamshaftMdxExecutor(String server, String app, String cube, String username, String password) {
		this.server = server;
		this.app = app;
		this.cube = cube;
		this.username = username;
		this.password = password;
	}

	public void execute(String query, CamshaftOutputDelegate delegate, PrintOptions printOptions) throws ThrillerException {
		try {
			System.err.println("Before Camshaft pull");
			Camshaft.pull(server, username, password, app, cube, query, printOptions, delegate);
		} catch (EssException e) {
			throw new ThrillerException("Error performing Essbase MDX query", e);
		}
		System.err.println("After Camshaft pull");
	}

}
