package com.jasonwjones.hyperion.thriller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Turns a result into the table a query's settings ask for, before its
 * columns are worked out: the point of view's members as columns, one row for
 * each cell rather than for each row of the grid, and rows of nothing but
 * #Missing left out.
 */
final class Reshaping {

	/** The name of the one value column of a long result */
	static final String VALUE = "Value";

	private Reshaping() {}

	/**
	 * @param result the query's result
	 * @param settings the query's settings
	 * @return the result reshaped as the settings say
	 * @throws ThrillerException if the settings ask for the point of view's
	 *             members and the server didn't say what they are
	 */
	static MdxResult apply(MdxResult result, QuerySettings settings) throws ThrillerException {
		MdxResult reshaped = result;
		if (settings.povColumns()) {
			reshaped = withPovColumns(reshaped);
		}
		if (settings.shape() == QuerySettings.Shape.LONG) {
			reshaped = longShape(reshaped, settings.missing() == QuerySettings.Missing.SKIP);
		} else if (settings.missing() == QuerySettings.Missing.SKIP) {
			reshaped = withoutMissingRows(reshaped);
		}
		return reshaped;
	}

	/**
	 * Adds a member column for each page dimension, after the row members.
	 */
	static MdxResult withPovColumns(MdxResult result) throws ThrillerException {
		List<String> members = result.getPageMembers();
		if (members.contains(null)) {
			throw new ThrillerException("This server doesn't say which members make up a query's point of view, "
					+ "so pov = columns can't add them; Essbase 26.1 does");
		}
		int at = result.getRowDimensions().size();
		List<String> rowDimensions = new ArrayList<>(result.getRowDimensions());
		rowDimensions.addAll(result.getPageDimensions());
		List<List<String>> headerRows = new ArrayList<>();
		for (List<String> header : result.getHeaderRows()) {
			headerRows.add(inserted(header, at, Collections.nCopies(members.size(), "")));
		}
		List<List<String>> dataRows = new ArrayList<>();
		for (List<String> row : result.getDataRows()) {
			dataRows.add(inserted(row, at, members));
		}
		return new MdxResult(rowDimensions, result.getColumnDimensions(), headerRows, dataRows, List.of(), List.of());
	}

	private static List<String> inserted(List<String> row, int at, List<String> cells) {
		List<String> inserted = new ArrayList<>(row.subList(0, Math.min(at, row.size())));
		while (inserted.size() < at) {
			inserted.add(null);
		}
		inserted.addAll(cells);
		if (row.size() > at) {
			inserted.addAll(row.subList(at, row.size()));
		}
		return inserted;
	}

	/**
	 * One row for each value: the row's members, the members over the value's
	 * column, then the value. With nothing on the columns, the value has no
	 * members over it, and each value of a row gets a row of its own all the
	 * same.
	 */
	static MdxResult longShape(MdxResult result, boolean skipMissing) {
		int members = result.getRowDimensions().size();
		List<String> dimensions = new ArrayList<>(result.getRowDimensions());
		dimensions.addAll(result.getColumnDimensions());

		List<List<String>> rows = new ArrayList<>();
		for (List<String> row : result.getDataRows()) {
			for (int column = members; column < row.size(); column++) {
				String value = row.get(column);
				if (skipMissing && ResultSetBuilder.isMissing(value)) {
					continue;
				}
				List<String> cells = new ArrayList<>(row.subList(0, members));
				for (List<String> header : result.getHeaderRows()) {
					cells.add(column < header.size() ? header.get(column) : null);
				}
				cells.add(value);
				rows.add(cells);
			}
		}
		List<String> header = new ArrayList<>(Collections.nCopies(dimensions.size(), ""));
		header.add(VALUE);
		return new MdxResult(dimensions, List.of(VALUE), List.of(header), rows, List.of(), List.of());
	}

	/**
	 * The result without the rows whose values are all #Missing.
	 */
	static MdxResult withoutMissingRows(MdxResult result) {
		int members = result.getRowDimensions().size();
		List<List<String>> rows = new ArrayList<>();
		for (List<String> row : result.getDataRows()) {
			boolean anyValue = row.size() <= members;
			for (int column = members; column < row.size() && !anyValue; column++) {
				anyValue = !ResultSetBuilder.isMissing(row.get(column));
			}
			if (anyValue) {
				rows.add(row);
			}
		}
		return new MdxResult(result.getRowDimensions(), result.getColumnDimensions(), result.getHeaderRows(), rows,
				result.getPageDimensions(), result.getPageMembers());
	}

}
