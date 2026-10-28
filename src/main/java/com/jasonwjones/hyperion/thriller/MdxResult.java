package com.jasonwjones.hyperion.thriller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.appliedolap.essbase.EssMdxGrid;

/**
 * An MDX query's result as Essbase's REST API returns it. The grid starts with
 * a header row for each dimension on the columns, and each row after that is a
 * member for each dimension on the rows followed by the values. For example,
 * with Year on the columns and Market and Product on the rows:
 *
 * <pre>
 * ["",     "",       "Jan",    "Feb"   ]   header row (Year)
 * ["East", "100-10", "1812.0", "1754.0"]   data row
 * </pre>
 */
public final class MdxResult {

	private final List<String> rowDimensions;

	private final List<String> columnDimensions;

	private final List<List<String>> headerRows;

	private final List<List<String>> dataRows;

	private final List<String> pageDimensions;

	private final List<String> pageMembers;

	public MdxResult(List<String> rowDimensions, List<String> columnDimensions, List<List<String>> headerRows,
			List<List<String>> dataRows) {
		this(rowDimensions, columnDimensions, headerRows, dataRows, List.of(), List.of());
	}

	/**
	 * @param rowDimensions the dimensions on the rows
	 * @param columnDimensions the dimensions on the columns
	 * @param headerRows a row of members for each dimension on the columns
	 * @param dataRows the rows of members and values
	 * @param pageDimensions the dimensions on neither axis
	 * @param pageMembers the member of each, or nulls if the server didn't say
	 */
	public MdxResult(List<String> rowDimensions, List<String> columnDimensions, List<List<String>> headerRows,
			List<List<String>> dataRows, List<String> pageDimensions, List<String> pageMembers) {
		this.rowDimensions = Collections.unmodifiableList(rowDimensions);
		this.columnDimensions = Collections.unmodifiableList(columnDimensions);
		this.headerRows = Collections.unmodifiableList(headerRows);
		this.dataRows = Collections.unmodifiableList(dataRows);
		this.pageDimensions = Collections.unmodifiableList(pageDimensions);
		this.pageMembers = Collections.unmodifiableList(new ArrayList<>(pageMembers));
	}

	/**
	 * Reads the grid essbase-rest-client returns.
	 *
	 * When no rows survive the query, such as a NON EMPTY that drops them all,
	 * Essbase leaves out the empty cells that sit over the row members in each
	 * header row. They're put back here, so the headers line up with the data
	 * columns as they would with rows under them.
	 *
	 * @param grid the query's result
	 * @return the result
	 */
	public static MdxResult from(EssMdxGrid grid) {
		List<List<String>> rows = new ArrayList<>();
		for (int row = 0; row < grid.getRows(); row++) {
			List<String> cells = new ArrayList<>();
			for (int column = 0; column < grid.getColumns(); column++) {
				cells.add(grid.getCell(row, column));
			}
			// The grid pads a short row with nulls; the row Essbase sent ended sooner
			while (!cells.isEmpty() && cells.get(cells.size() - 1) == null) {
				cells.remove(cells.size() - 1);
			}
			rows.add(cells);
		}
		int headers = grid.getHeaderRows();
		List<List<String>> headerRows = new ArrayList<>();
		for (List<String> header : rows.subList(0, headers)) {
			headerRows.add(padded(header, grid.getRowDimensions().size()));
		}
		return new MdxResult(grid.getRowDimensions(), grid.getColumnDimensions(), headerRows, rows.subList(headers, rows.size()),
				grid.getPageDimensions(), grid.getPageMembers());
	}

	/**
	 * A header row with the empty cells over the row members, which a header
	 * row has unless Essbase left them out.
	 */
	private static List<String> padded(List<String> header, int rowDimensions) {
		if (rowDimensions == 0 || header.isEmpty() || header.get(0) == null || header.get(0).isEmpty()) {
			return header;
		}
		List<String> padded = new ArrayList<>(Collections.nCopies(rowDimensions, ""));
		padded.addAll(header);
		return padded;
	}

	/**
	 * @return the dimensions on the rows; each data row starts with a member of each
	 */
	public List<String> getRowDimensions() {
		return rowDimensions;
	}

	public List<String> getColumnDimensions() {
		return columnDimensions;
	}

	public List<List<String>> getHeaderRows() {
		return headerRows;
	}

	public List<List<String>> getDataRows() {
		return dataRows;
	}

	/**
	 * @return the dimensions on neither axis, which each contribute one member
	 *         to every cell
	 */
	public List<String> getPageDimensions() {
		return pageDimensions;
	}

	/**
	 * @return the member of each page dimension, or nulls if the server didn't
	 *         say which
	 */
	public List<String> getPageMembers() {
		return pageMembers;
	}

	/**
	 * @return the number of cells in the widest row
	 */
	public int getWidth() {
		int width = 0;
		for (List<String> row : headerRows) {
			width = Math.max(width, row.size());
		}
		for (List<String> row : dataRows) {
			width = Math.max(width, row.size());
		}
		return width;
	}

}
