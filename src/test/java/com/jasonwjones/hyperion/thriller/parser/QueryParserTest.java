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
	public void keepsEqualsSignsInAValue() {
		assertEquals("a=b", parser.parse("/* -- note = a=b */ SELECT Foo").getProperties().getProperty("note"));
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

	@Test
	public void blanksCommentsSoASemicolonInOneCannotEndTheQuery() {
		String query = "/* Top products; by sales */\nSELECT /* -- members = alias */ {[Sales]} ON 0 FROM Sample.Basic";
		String mdx = QueryParser.withoutComments(query);
		assertEquals(query.length(), mdx.length());
		assertEquals("SELECT {[Sales]} ON 0 FROM Sample.Basic", squeezed(mdx));
		assertTrue(mdx.startsWith(" ".repeat(28) + "\nSELECT "), mdx);
	}

	@Test
	public void keepsTheLineBreaksInAComment() {
		String mdx = QueryParser.withoutComments("/*\n a\n */SELECT");
		assertEquals(3, mdx.lines().count());
		assertTrue(mdx.endsWith("SELECT"), mdx);
		assertEquals("SELECT", squeezed(mdx));
	}

	@Test
	public void leavesCommentMarkersInStringsAndNamesAlone() {
		String query = "SELECT {[A/*B]} ON 0, Filter([Product].Members, [Product].CurrentMember.MEMBER_NAME <> \"/*x*/\") ON 1"
				+ " WITH MEMBER [Measures].[Y] AS '[Sales] /*2*/' FROM Sample.Basic";
		assertEquals(query, QueryParser.withoutComments(query));
	}

	@Test
	public void blanksAnUnfinishedCommentToTheEnd() {
		String mdx = QueryParser.withoutComments("SELECT /* oops");
		assertEquals("SELECT", squeezed(mdx));
		assertEquals(14, mdx.length());
	}

	private static String squeezed(String mdx) {
		return mdx.replaceAll("\\s+", " ").strip();
	}

}
