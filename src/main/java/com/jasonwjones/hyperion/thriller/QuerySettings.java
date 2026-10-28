package com.jasonwjones.hyperion.thriller;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import com.appliedolap.essbase.EssCube;
import com.appliedolap.essbase.EssMdxOptions;

/**
 * How to run one query and shape its result, from the connection's settings
 * and the query's hints, which win:
 *
 * <pre>
 * jdbc:essbase:thriller:http://localhost:9000/essbase;members=alias;shape=long
 *
 * SELECT
 * /&#42;
 *  -- members = alias
 *  -- alias.table = Long Names
 *  -- shape = long
 *  -- pov = columns
 *  -- names = upper
 *  -- missing = zero
 *  -- timeout = 30
 *  &#42;/
 * ...
 * </pre>
 *
 * See the README for what each setting does.
 */
final class QuerySettings {

	static final String MEMBERS = "members";

	static final String ALIAS_TABLE = "alias.table";

	static final String SHAPE = "shape";

	static final String POV = "pov";

	static final String NAMES = "names";

	static final String MISSING = "missing";

	static final String TIMEOUT = "timeout";

	/** The grid as Essbase lays it out, or one row for each cell */
	enum Shape {
		WIDE, LONG
	}

	/** Column names as the members have them, or made into SQL-style identifiers */
	enum Names {
		AS_IS, UPPER, LOWER
	}

	/** What a #Missing value becomes */
	enum Missing {
		NULL, ZERO, SKIP
	}

	private final EssMdxOptions options;

	private final Shape shape;

	private final boolean povColumns;

	private final Names names;

	private final Missing missing;

	private QuerySettings(EssMdxOptions options, Shape shape, boolean povColumns, Names names, Missing missing) {
		this.options = options;
		this.shape = shape;
		this.povColumns = povColumns;
		this.names = names;
		this.missing = missing;
	}

	/**
	 * @param settings the connection's settings
	 * @param hints the query's hints
	 * @param statementTimeout the statement's query timeout in seconds, or 0 for
	 *            none; a timeout hint wins over it, and it wins over the
	 *            connection's
	 * @return the settings for the query
	 * @throws ThrillerException if a setting has a value Thriller doesn't know
	 */
	static QuerySettings of(Map<String, String> settings, Properties hints, int statementTimeout) throws ThrillerException {
		EssMdxOptions options = EssMdxOptions.defaults();
		String members = setting(MEMBERS, settings, hints);
		if (members != null) {
			options.memberIdentifierType(identifierType(members));
		}
		String aliasTable = setting(ALIAS_TABLE, settings, hints);
		if (aliasTable != null) {
			options.aliasTableName(aliasTable);
		}
		Integer timeout = timeout(settings, hints, statementTimeout);
		if (timeout != null) {
			options.timeout(Duration.ofSeconds(timeout));
		}
		return new QuerySettings(options,
				choice(SHAPE, settings, hints, Shape.WIDE, Map.of("wide", Shape.WIDE, "long", Shape.LONG)),
				choice(POV, settings, hints, false, Map.of("none", false, "columns", true)),
				choice(NAMES, settings, hints, Names.AS_IS, Map.of("as-is", Names.AS_IS, "upper", Names.UPPER, "lower", Names.LOWER)),
				choice(MISSING, settings, hints, Missing.NULL, Map.of("null", Missing.NULL, "zero", Missing.ZERO, "0", Missing.ZERO, "skip", Missing.SKIP)));
	}

	private static String setting(String key, Map<String, String> settings, Properties hints) {
		String value = hints.getProperty(key, settings.get(key));
		return value == null || value.isBlank() ? null : value.trim();
	}

	private static <T> T choice(String key, Map<String, String> settings, Properties hints, T otherwise, Map<String, T> choices)
			throws ThrillerException {
		String value = setting(key, settings, hints);
		if (value == null) {
			return otherwise;
		}
		T choice = choices.get(value.toLowerCase(Locale.ROOT));
		if (choice == null) {
			throw new ThrillerException(key + " can be " + String.join(", ", choices.keySet().stream().sorted().toList()) + ", not '" + value + "'");
		}
		return choice;
	}

	private static Integer timeout(Map<String, String> settings, Properties hints, int statementTimeout) throws ThrillerException {
		String hint = hints.getProperty(TIMEOUT);
		if (hint == null && statementTimeout > 0) {
			return statementTimeout;
		}
		String value = setting(TIMEOUT, settings, hints);
		if (value == null) {
			return null;
		}
		try {
			int seconds = Integer.parseInt(value);
			return seconds > 0 ? seconds : null;
		} catch (NumberFormatException e) {
			throw new ThrillerException(TIMEOUT + " is a number of seconds, not '" + value + "'");
		}
	}

	private static EssCube.MdxIdentifierType identifierType(String members) throws ThrillerException {
		switch (members.toLowerCase(Locale.ROOT)) {
		case "name":
		case "names":
			return EssCube.MdxIdentifierType.NAME;
		case "alias":
		case "aliases":
			return EssCube.MdxIdentifierType.ALIAS;
		case "unique":
		case "unique_name":
			return EssCube.MdxIdentifierType.UNIQUE_NAME;
		default:
			throw new ThrillerException("members can be name, alias or unique, not '" + members + "'");
		}
	}

	EssMdxOptions options() {
		return options;
	}

	Shape shape() {
		return shape;
	}

	boolean povColumns() {
		return povColumns;
	}

	Names names() {
		return names;
	}

	Missing missing() {
		return missing;
	}

}
