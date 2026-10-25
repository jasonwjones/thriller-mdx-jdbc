package com.jasonwjones.hyperion.thriller.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.jasonwjones.hyperion.thriller.Fixtures;

public class CubeNameTest {

	@Test
	public void readsAPlainFromClause() {
		assertEquals("Sample.Basic", CubeName.fromQuery("SELECT {} ON AXIS(0) FROM Sample.Basic").toString());
	}

	@Test
	public void readsABracketedFromClause() {
		CubeName cube = CubeName.fromQuery(Fixtures.text("test-01.mdx"));
		assertEquals("Sample", cube.getApplication());
		assertEquals("Basic", cube.getCube());
	}

	@Test
	public void readsApplicationAndCubeInOnePairOfBrackets() {
		assertEquals("Sample.Basic", CubeName.fromQuery("SELECT {[Sales]} ON COLUMNS FROM [Sample.Basic]").toString());
		assertEquals("My App.My Cube", CubeName.fromQuery("SELECT {} ON 0 FROM [My App.My Cube]").toString());
	}

	@Test
	public void keepsSpacesInsideBrackets() {
		assertEquals("My App.My Cube", CubeName.fromQuery("SELECT {} ON 0 from [My App] . [My Cube] WHERE ([Sales])").toString());
	}

	@Test
	public void ignoresAFromInAComment() {
		String query = "/* copied from Other.Cube */ SELECT {} ON 0 FROM Sample.Basic";
		assertEquals("Sample.Basic", CubeName.fromQuery(query).toString());
	}

	@Test
	public void looksInsideASubselect() {
		String query = "SELECT {} ON 0 FROM (SELECT {[Jan]} ON 0 FROM [Sample].[Basic])";
		assertEquals("Sample.Basic", CubeName.fromQuery(query).toString());
	}

	@Test
	public void returnsNullWithoutAFromClause() {
		assertNull(CubeName.fromQuery("SELECT {[Jan]} ON COLUMNS"));
	}

}
