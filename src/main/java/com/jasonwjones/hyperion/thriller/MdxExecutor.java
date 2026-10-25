package com.jasonwjones.hyperion.thriller;

import com.saxifrages.hyperion.camshaft.CamshaftOutputDelegate;
import com.saxifrages.hyperion.camshaft.PrintOptions;

public interface MdxExecutor {

	public void execute(String query, CamshaftOutputDelegate delegate, PrintOptions printOptions) throws ThrillerException;
	
}
