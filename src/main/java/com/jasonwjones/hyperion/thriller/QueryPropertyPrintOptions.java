package com.jasonwjones.hyperion.thriller;

import java.util.Properties;

import com.saxifrages.hyperion.camshaft.PrintOptions;

public class QueryPropertyPrintOptions implements PrintOptions {

	private Properties properties;
		
	public static final String PRINT_TUPLE_SEPARATELY = "thriller.print-tuple-member-separately";
	
	public static final String HEADER_TUPLE_SEPARATOR = "thriller.header-tuple-separator";
	
	public static final String MAXIMUM_FRACTION_DIGITS = "thriller.maximum-fraction-digits";
	
	public static final String MINIMUM_FRACTION_DIGITS = "thriller.minimum-fraction-digits";
	
	public static final String DEFAULT_HEADER_TUPLE_SEPARATOR = ",";
	
	public static final String DEFAULT_MISSING_TEXT = "#Missing";
	
	public QueryPropertyPrintOptions(Properties properties) {
		this.properties = properties;
	}
	
	@Override
	public boolean isSuppressHeader() {
		return true;
	}

	@Override
	public String getNoAccessText() {
		return "#NoAccess";
	}

	@Override
	public String getMissingText() {
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getHeaderTupleSeparator() {
		return properties.getProperty(HEADER_TUPLE_SEPARATOR, DEFAULT_HEADER_TUPLE_SEPARATOR);
	}

	@Override
	public boolean isIndentHeader() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isLinePerHeaderTupleMember() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getMaximumFractionDigits() {
		return Integer.parseInt(properties.getProperty(MAXIMUM_FRACTION_DIGITS, "-1"));
	}

	@Override
	public boolean isPrintProperties() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isPrintPropertyNames() {
		return true;
	}

	@Override
	public boolean isPrintPropertiesSeparately() {
		return true;
	}

	@Override
	public boolean isPrintTupleMemberSeparately() {
		return Boolean.parseBoolean(properties.getProperty(PRINT_TUPLE_SEPARATELY, "false"));
	}

	@Override
	public int getMinimumFractionDigits() {
		return Integer.parseInt(properties.getProperty(MINIMUM_FRACTION_DIGITS, "0"));
	}

}
