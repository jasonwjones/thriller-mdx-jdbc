package com.jasonwjones.hyperion.thriller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Runs the cookbook in examples/ against a real Essbase server with Sample.Basic,
 * when given one (see {@link LiveServerTest}), and checks each query's output
 * against the copy recorded beside it and the cookbook page against both.
 *
 * To record the outputs again and rebuild the page, after changing an example
 * or against a server whose data differs:
 *
 * <pre>
 * mvn test -Dtest=ExamplesTest -Dthriller.examples.record=true
 * </pre>
 */
public class ExamplesTest {

	private static final Path EXAMPLES = Paths.get("examples");

	private static final boolean RECORD = Boolean.getBoolean("thriller.examples.record");

	private static final String URL = System.getenv("THRILLER_URL");

	private static final String USER = System.getenv("THRILLER_USER");

	private static final String PASSWORD = System.getenv("THRILLER_PASSWORD");

	private static final String INTRODUCTION = """
			# Thriller cookbook

			MDX queries against Sample.Basic, each with the result set Thriller returns for it. Every query is a
			file in this folder, so you can open it in a tool and run it.

			The outputs are recorded from Essbase 26.1 by `ExamplesTest`, which also checks them, and builds this
			page, whenever the tests run against a server.
			""";

	@BeforeAll
	public static void needsAServer() {
		assumeTrue(URL != null && USER != null && PASSWORD != null, "THRILLER_URL, THRILLER_USER and THRILLER_PASSWORD are not set");
	}

	@Test
	public void eachExampleGivesItsRecordedOutput() throws Exception {
		List<Path> examples = examples();
		assertTrue(examples.size() > 0, "No examples in " + EXAMPLES.toAbsolutePath());

		StringBuilder page = new StringBuilder(INTRODUCTION);
		try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
			for (Path example : examples) {
				String query = Files.readString(example, StandardCharsets.UTF_8);
				String output;
				try (ResultSet resultSet = connection.createStatement().executeQuery(query)) {
					output = table(resultSet);
				}
				Path recorded = example.resolveSibling(example.getFileName().toString().replace(".mdx", ".txt"));
				if (RECORD) {
					Files.writeString(recorded, output, StandardCharsets.UTF_8);
				} else {
					assertTrue(Files.exists(recorded), "No recorded output for " + example + "; record it with -Dthriller.examples.record=true");
					assertEquals(Files.readString(recorded, StandardCharsets.UTF_8), output, example.toString());
				}
				page.append(section(example.getFileName().toString(), query, output));
			}
		}

		Path readme = EXAMPLES.resolve("README.md");
		if (RECORD) {
			Files.writeString(readme, page.toString(), StandardCharsets.UTF_8);
		} else {
			assertEquals(Files.readString(readme, StandardCharsets.UTF_8), page.toString(), "examples/README.md is out of date");
		}
	}

	private static List<Path> examples() throws IOException {
		try (Stream<Path> files = Files.list(EXAMPLES)) {
			return files.filter(path -> path.toString().endsWith(".mdx")).sorted().toList();
		}
	}

	/**
	 * A result set as a text table, the way sqlline shows one.
	 */
	static String table(ResultSet resultSet) throws SQLException {
		ResultSetMetaData metaData = resultSet.getMetaData();
		int count = metaData.getColumnCount();
		List<String[]> rows = new ArrayList<>();
		String[] header = new String[count];
		for (int column = 1; column <= count; column++) {
			header[column - 1] = metaData.getColumnLabel(column);
		}
		rows.add(header);
		while (resultSet.next()) {
			String[] row = new String[count];
			for (int column = 1; column <= count; column++) {
				String value = resultSet.getString(column);
				row[column - 1] = value == null ? "null" : value;
			}
			rows.add(row);
		}

		int[] widths = new int[count];
		for (String[] row : rows) {
			for (int column = 0; column < count; column++) {
				widths[column] = Math.max(widths[column], row[column].length());
			}
		}
		StringBuilder border = new StringBuilder("+");
		for (int width : widths) {
			border.append("-".repeat(width + 2)).append('+');
		}
		StringBuilder table = new StringBuilder(border).append('\n');
		for (int index = 0; index < rows.size(); index++) {
			table.append('|');
			for (int column = 0; column < count; column++) {
				String value = rows.get(index)[column];
				table.append(' ').append(value).append(" ".repeat(widths[column] - value.length())).append(" |");
			}
			table.append('\n');
			if (index == 0) {
				table.append(border).append('\n');
			}
		}
		return table.append(border).append('\n').toString();
	}

	/**
	 * An example's part of the page: its title and description, from the comment
	 * at the top of its file, then the rest of the file and what it returns.
	 */
	static String section(String fileName, String text, String output) {
		int end = text.indexOf("*/");
		List<String> lines = text.substring(text.indexOf("/*") + 2, end).lines().map(String::strip).toList();
		String query = text.substring(end + 2).strip();

		StringBuilder section = new StringBuilder("\n");
		StringBuilder paragraph = new StringBuilder();
		for (String line : lines) {
			if (line.startsWith("# ")) {
				section.append("#").append(line).append(" ([").append(fileName).append("](").append(fileName).append("))\n\n");
			} else if (line.isEmpty()) {
				if (paragraph.length() > 0) {
					section.append(paragraph).append("\n\n");
					paragraph.setLength(0);
				}
			} else {
				paragraph.append(paragraph.length() > 0 ? " " : "").append(line);
			}
		}
		if (paragraph.length() > 0) {
			section.append(paragraph).append("\n\n");
		}
		return section.append("```\n").append(query).append("\n```\n\n```\n").append(output).append("```\n").toString();
	}

}
