package com.jasonwjones.hyperion.thriller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ThrillerUrlTest {

	@Test
	public void readsServerApplicationAndCube() throws ThrillerException {
		ThrillerUrl url = ThrillerUrl.parse("jdbc:essbase:thriller:http://localhost:9000/essbase;app=Sample;cube=Basic");
		assertEquals("http://localhost:9000/essbase", url.getServerUrl());
		assertEquals("Sample", url.getApplication());
		assertEquals("Basic", url.getCube());
	}

	@Test
	public void leavesApplicationAndCubeOptional() throws ThrillerException {
		ThrillerUrl url = ThrillerUrl.parse("jdbc:essbase:thriller:https://essbase.example.com/essbase/");
		assertEquals("https://essbase.example.com/essbase", url.getServerUrl());
		assertNull(url.getApplication());
		assertNull(url.getCube());
	}

	@Test
	public void acceptsOnlyThrillerUrls() {
		assertTrue(ThrillerUrl.accepts("jdbc:essbase:thriller:http://localhost:9000/essbase"));
		assertFalse(ThrillerUrl.accepts("jdbc:postgresql://localhost/db"));
		assertFalse(ThrillerUrl.accepts(null));
	}

	@Test
	public void explainsTheOldStyleOfUrl() {
		ThrillerException e = assertThrows(ThrillerException.class,
				() -> ThrillerUrl.parse("jdbc:essbase:thriller://epmvirt11124/Sample.Basic"));
		assertTrue(e.getMessage().contains("http://localhost:9000/essbase"), e.getMessage());
	}

	@Test
	public void rejectsAPropertyWithoutAValue() {
		assertThrows(ThrillerException.class, () -> ThrillerUrl.parse("jdbc:essbase:thriller:http://localhost:9000/essbase;Sample"));
	}

}
