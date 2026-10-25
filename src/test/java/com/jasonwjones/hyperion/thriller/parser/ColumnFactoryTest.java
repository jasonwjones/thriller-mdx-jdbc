package com.jasonwjones.hyperion.thriller.parser;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.jasonwjones.hyperion.thriller.ThrillerException;

public class ColumnFactoryTest {

	private Properties props;
	
	private ColumnFactory factory;
	
	@Before
	public void setUp() throws Exception {
		props = new Properties();
		props.setProperty("column.1.name", "TIME");
		props.setProperty("column.1.type", "VARCHAR");
		props.setProperty("column.1.precision", "30");
		props.setProperty("column.1.scale", "0");
		factory = new ColumnFactory();
	}

	@Test
	public void testParse() throws ThrillerException {
		factory.parse(props);
	}

}
