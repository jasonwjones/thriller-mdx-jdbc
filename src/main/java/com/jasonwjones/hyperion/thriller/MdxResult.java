package com.jasonwjones.hyperion.thriller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final List<String> rowDimensions;

	private final List<String> columnDimensions;

	private final List<List<String>> headerRows;

	private final List<List<String>> dataRows;

	public MdxResult(List<String> rowDimensions, List<String> columnDimensions, List<List<String>> headerRows,
			List<List<String>> dataRows) {
		this.rowDimensions = Collections.unmodifiableList(rowDimensions);
		this.columnDimensions = Collections.unmodifiableList(columnDimensions);
		this.headerRows = Collections.unmodifiableList(headerRows);
		this.dataRows = Collections.unmodifiableList(dataRows);
	}

	/**
	 * Reads the JSON the REST API's MDX endpoint returns:
	 * {"metadata": {"row": [...], "column": [...], "page": [...]}, "data": [[...], ...]}
	 *
	 * @param json the response body
	 * @return the result
	 * @throws ThrillerException if the JSON can't be read
	 */
	public static MdxResult fromJson(String json) throws ThrillerException {
		JsonNode root;
		try {
			root = MAPPER.readTree(json);
		} catch (JsonProcessingException e) {
			throw new ThrillerException("Could not read the MDX result Essbase returned", e);
		}
		JsonNode metadata = root.path("metadata");
		List<String> rowDimensions = strings(metadata.path("row"));
		List<String> columnDimensions = strings(metadata.path("column"));

		List<List<String>> grid = new ArrayList<List<String>>();
		for (JsonNode row : root.path("data")) {
			grid.add(strings(row));
		}
		int headers = Math.min(columnDimensions.size(), grid.size());
		return new MdxResult(rowDimensions, columnDimensions, grid.subList(0, headers), grid.subList(headers, grid.size()));
	}

	private static List<String> strings(JsonNode array) {
		List<String> values = new ArrayList<String>();
		for (JsonNode value : array) {
			values.add(value.isNull() ? null : value.asText());
		}
		return values;
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
