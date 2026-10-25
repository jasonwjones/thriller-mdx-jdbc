package com.jasonwjones.hyperion.thriller;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.jasonwjones.jolo.ResultSetTablePrinter;

public class ThrillerTestCase {

	private static Connection conn;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		String embeddedSampleBasic = "jdbc:essbase:thriller://epmvirt11124/Sample.Basic";
		Class.forName("com.jasonwjones.hyperion.thriller.ThrillerDriver");
		conn = DriverManager.getConnection(embeddedSampleBasic, "admin", "password");
	}
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	@Ignore
	public void test01() throws Exception {
		ResultSet resultSet = test("result-test-01.mdx");
		new ResultSetTablePrinter().printResultSet(resultSet);		
	}

	@Test
	//@Ignore
	public void testMajorMarkets() throws Exception {
		ResultSet resultSet = test("test-major-markets.mdx");
		new ResultSetTablePrinter(20).printResultSet(resultSet);		
	}
	
	@Test
	@Ignore
	public void testMoreDataThanResultColumns2() throws Exception {
		ResultSet resultSet = test("test-fetch-metadata-comment-after.mdx");
		new ResultSetTablePrinter().printResultSet(resultSet);		
	}
	
	@Test
	@Ignore
	public void testMoreDataThanResultColumns() throws Exception {
		ResultSet resultSet = test("test-more-data-than-defined-columns.mdx");
		new ResultSetTablePrinter().printResultSet(resultSet);		
	}

	@Test
	@Ignore
	public void testLessDataThanResultColumns() throws Exception {
		ResultSet resultSet = test("test-less-data-than-defined-columns.mdx");
		new ResultSetTablePrinter().printResultSet(resultSet);		
	}
	
	@Test
	@Ignore
	public void testFetchMetaData() throws Exception {
		ResultSet resultSet = test("test-fetch-metadata.mdx");
		new ResultSetTablePrinter().printResultSet(resultSet);		
	}

	@Test
	@Ignore
	public void testNoHints() throws Exception {
		ResultSet resultSet = test("test-no-hints.mdx");
		new ResultSetTablePrinter().printResultSet(resultSet);		
	}

	@Test
	@Ignore
	public void testNoCommentBlock() throws Exception {
		ResultSet resultSet = test("test-fetch-metadata-no-comment-block.mdx");
		new ResultSetTablePrinter().printResultSet(resultSet);		
	}

	
	public ResultSet test(String mdxFile) throws IOException, SQLException {
		File testQueryFile = new File("src/test/resources/" + mdxFile);
		String testQuery = FileUtils.readFileToString(testQueryFile, Charset.defaultCharset());
		
		Statement statement = conn.createStatement();
		//ResultSet resultSet = statement.executeQuery(testQuery);
		statement.execute(testQuery);
		System.err.println("After exec");
		//logger.error("After execute");
		return statement.getResultSet();
	}
	
}
