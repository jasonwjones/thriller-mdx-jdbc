package com.jasonwjones.hyperion.thriller.camshaft;

import static org.junit.Assert.fail;

import java.sql.SQLException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.jasonwjones.jolo.ResultSetTablePrinter;

public class ThrillerCamshaftAutoDetectingOutputDelegateTest {

	private ThrillerCamshaftAutoDetectingOutputDelegate delegate;
	
	@Before
	public void setUp() throws Exception {
		this.delegate = new ThrillerCamshaftAutoDetectingOutputDelegate();
	}

	@Test
	@Ignore
	public void testConvertRow() {
		fail("Not yet implemented");
	}

	@Test
	public void testEndOutput() throws SQLException {
		delegate.startOutput();
		delegate.startHeader();
		delegate.endHeader();
		
		delegate.addRow();
		delegate.addValue("Sales");
		delegate.addValue("100");
		delegate.addValue("200");
		delegate.endRow();
		
		delegate.addRow();
		delegate.addValue("COGS");
		delegate.addValue("80");
		delegate.addValue("70");
		delegate.endRow();		
		
		delegate.endOutput();
		
		new ResultSetTablePrinter().printResultSet(delegate.getResultSet());		
	}

}
