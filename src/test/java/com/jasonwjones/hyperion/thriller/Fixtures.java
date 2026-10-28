package com.jasonwjones.hyperion.thriller;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Test resources. The JSON under rest/ is what Essbase returned for the queries
 * of the same name against Sample.Basic: 26.1, except for the file whose name
 * starts essbase-21, which came from 21.1. A result is read from it as
 * essbase-rest-client would read the server's answer.
 */
public final class Fixtures {

	private Fixtures() {}

	public static String text(String path) {
		try (InputStream in = Fixtures.class.getResourceAsStream("/" + path)) {
			if (in == null) {
				throw new IllegalArgumentException("No test resource " + path);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static MdxResult result(String name) {
		try {
			return MdxResult.from(new JsonMdxGrid(text("rest/" + name + ".json")));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
