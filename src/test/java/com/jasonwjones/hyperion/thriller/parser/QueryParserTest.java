package com.jasonwjones.hyperion.thriller.parser;

import java.io.File;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryParserTest {

	private static final Logger logger = LoggerFactory.getLogger(QueryParserTest.class);
	
	private QueryParser parser;
	
	private String queryWithHint;
	
	private String testQuery;
	
	@Before
	public void setUp() throws Exception {
		parser = new QueryParser();
		queryWithHint = "/* this is comment \n-- foo=bar */ SELECT Foo";
		
		File testQueryFile = new File("src/test/resources/test-fetch-metadata-comment-after.mdx");
		testQuery = FileUtils.readFileToString(testQueryFile, Charset.defaultCharset());

		//queryWithHint = "{{foo}} bar";
	}

	@Test
	public void test() {
		//System.out.println("Parsing: " + queryWithHint);
		System.out.println("Will test: " + testQuery);
		ParsedQuery parsed = parser.parse(testQuery);
		
		logger.info("Props: {}", parsed.getProperties());
	}

}
