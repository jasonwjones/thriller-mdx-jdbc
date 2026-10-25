package com.jasonwjones.hyperion.thriller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.saxifrages.hyperion.camshaft.CamshaftOutputDelegate;
import com.saxifrages.hyperion.camshaft.PrintOptions;

public class DummyMdxExecutor implements MdxExecutor {

	private static final Logger logger = LoggerFactory.getLogger(DummyMdxExecutor.class);
	
	public void execute(String query, CamshaftOutputDelegate outputDelegate, PrintOptions printOptions) {
		logger.info("Executing query");
	}

}
