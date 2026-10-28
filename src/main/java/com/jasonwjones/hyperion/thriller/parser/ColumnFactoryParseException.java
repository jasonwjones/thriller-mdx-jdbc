package com.jasonwjones.hyperion.thriller.parser;

import java.util.Set;

import com.jasonwjones.hyperion.thriller.ThrillerException;

@SuppressWarnings("serial")
public class ColumnFactoryParseException extends ThrillerException {;

	private static final String message = "Could not parse column index value '%s' from line '%s'";

	private static final String unrecColumnType = "Unrecognized column type '%s' in column definition";

	public ColumnFactoryParseException(String badLine, String badIndex, Throwable parseException) {
		super(String.format(message, badLine, badIndex), parseException);
	}

	public ColumnFactoryParseException(Set<Integer> missingIndices) {
		super("missing indices");
	}

	public ColumnFactoryParseException(String message) {
		super(message);
	}

	public static ColumnFactoryParseException unrecognizedColumnType(String columnType) {
		return new ColumnFactoryParseException(String.format(unrecColumnType, columnType));
	}

}
