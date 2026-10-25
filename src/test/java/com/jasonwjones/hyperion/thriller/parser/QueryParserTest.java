package com.jasonwjones.hyperion.thriller.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.jasonwjones.hyperion.thriller.Fixtures;

public class QueryParserTest {

	private final QueryParser parser = new QueryParser();

	@Test
	public void readsHintsFromAJavadocStyleBlock() {
		Properties hints = parser.parse(Fixtures.text("test-major-markets.mdx")).getProperties();
		assertEquals("ENTITY", hints.getProperty("column.1.name"));
		assertEquals("DECIMAL", hints.getProperty("column.5.type"));
		assertEquals("2", hints.getProperty("column.5.scale"));
		assertEquals("true", hints.getProperty("thriller.print-tuple-member-separately"));
	}

	@Test
	public void readsHintsFromABlockInsideTheQuery() {
		Properties hints = parser.parse(Fixtures.text("test-fetch-metadata-comment-after.mdx")).getProperties();
		assertEquals("LEVEL", hints.getProperty("column.3.name"));
		assertEquals("INTEGER", hints.getProperty("column.3.type"));
	}

	@Test
	public void readsHintsWithoutLeadingAsterisks() {
		Properties hints = parser.parse(Fixtures.text("test-fetch-metadata-no-comment-block.mdx")).getProperties();
		assertEquals("MEMBER_NAME", hints.getProperty("column.1.name"));
	}

	@Test
	public void findsNoHintsInAPlainQuery() {
		assertTrue(parser.parse(Fixtures.text("test-no-hints.mdx")).getProperties().isEmpty());
	}

	@Test
	public void leavesTheQueryAsWritten() {
		String query = "/* this is comment \n-- foo=bar */ SELECT Foo";
		ParsedQuery parsed = parser.parse(query);
		assertEquals(query, parsed.getQuery());
		assertEquals("bar", parsed.getProperties().getProperty("foo"));
	}

}
