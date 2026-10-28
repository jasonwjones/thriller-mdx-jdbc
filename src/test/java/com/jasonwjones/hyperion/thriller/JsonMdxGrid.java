package com.jasonwjones.hyperion.thriller;

import java.util.ArrayList;
import java.util.List;

import com.appliedolap.essbase.EssMdxGrid;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * An MDX result read from the JSON Essbase returned for it, the same way
 * essbase-rest-client reads it, so recorded responses can stand in for a
 * server: the grid is as wide as its widest row, and a short row reads null
 * past its end.
 */
final class JsonMdxGrid implements EssMdxGrid {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final List<List<String>> rows = new ArrayList<>();

	private final List<String> rowDimensions;

	private final List<String> columnDimensions;

	private final List<String> pageDimensions = new ArrayList<>();

	private final List<String> pageMembers = new ArrayList<>();

	private int columns;

	JsonMdxGrid(String json) throws JsonProcessingException {
		JsonNode root = MAPPER.readTree(json);
		JsonNode metadata = root.path("metadata");
		rowDimensions = strings(metadata.path("row"));
		columnDimensions = strings(metadata.path("column"));
		for (JsonNode page : metadata.path("page")) {
			// 21 sends a name, 26.1 an object with one
			pageDimensions.add(page.isTextual() ? page.asText() : page.path("name").asText());
			pageMembers.add(page.isTextual() ? null : page.path("pov").asText(null));
		}
		for (JsonNode row : root.path("data")) {
			List<String> cells = strings(row);
			rows.add(cells);
			columns = Math.max(columns, cells.size());
		}
	}

	private static List<String> strings(JsonNode array) {
		List<String> values = new ArrayList<>();
		for (JsonNode value : array) {
			values.add(value.isNull() ? null : value.asText());
		}
		return values;
	}

	@Override
	public int getRows() {
		return rows.size();
	}

	@Override
	public int getColumns() {
		return columns;
	}

	@Override
	public String getCell(int row, int col) {
		List<String> cells = rows.get(row);
		return col < cells.size() ? cells.get(col) : null;
	}

	@Override
	public List<String> getRowDimensions() {
		return rowDimensions;
	}

	@Override
	public List<String> getColumnDimensions() {
		return columnDimensions;
	}

	@Override
	public List<String> getPageDimensions() {
		return pageDimensions;
	}

	@Override
	public List<String> getPageMembers() {
		return pageMembers;
	}

}
