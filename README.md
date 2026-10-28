# Thriller

A JDBC driver for Essbase that runs MDX and hands back an ordinary, typed result set, so a tool
that speaks JDBC can read a cube. It talks to Essbase over the REST API and needs no Oracle software.

```
sqlline> SELECT {[Jan], [Feb]} ON COLUMNS, CROSSJOIN({[East], [West]}, {[Cola], [Diet Cola]}) ON ROWS
. . . .> FROM [Sample].[Basic] WHERE ([Sales], [Actual]);
+--------+---------+--------+--------+
| Market | Product |  Jan   |  Feb   |
+--------+---------+--------+--------+
| East   | 100-10  | 1812.0 | 1754.0 |
| East   | 100-20  | 200.0  | 206.0  |
| West   | 100-10  | 1174.0 | 1146.0 |
| West   | 100-20  | 700.0  | 726.0  |
+--------+---------+--------+--------+
```

Thriller started in 2016 as an experiment on top of Oracle's Essbase Java API, by way of Camshaft,
and spent its working life giving Dodeca a way to treat MDX as a data source. Version 1.0 keeps the
idea and the query hints and swaps the plumbing for the REST API, through
[essbase-rest-client](https://github.com/appliedolap/essbase-rest-client). It has been tested against
Essbase 21.1 and 26.1.

The [cookbook](examples/README.md) has twelve queries against Sample.Basic with what each returns.

## Connecting

The URL is the driver prefix followed by the Essbase server's address, as you'd open it in a browser:

```
jdbc:essbase:thriller:http://localhost:9000/essbase
```

The user and password are ordinary JDBC connection properties, and the login is checked when the
connection opens. The driver class is `com.jasonwjones.hyperion.thriller.ThrillerDriver`, and it
registers itself. For code that configures a `javax.sql.DataSource` instead, such as Spring or a
connection pool, there's `com.jasonwjones.hyperion.thriller.ThrillerDataSource`.

Each query reads the cube its `FROM` clause names. For queries without one, give a default in the URL:

```
jdbc:essbase:thriller:http://localhost:9000/essbase;app=Sample;cube=Basic
```

Settings go after the address, each after a semicolon, or in the connection's properties, which is
where a tool's driver settings put them. The URL wins when both have one:

| Setting | Meaning |
| --- | --- |
| `app`, `cube` | The cube to read when a query has no `FROM` clause |
| `members` | How to name members: `name` (the default), `alias` or `unique` |
| `alias.table` | The alias table to use with `members=alias`, such as `Long Names`; the default table otherwise |
| `shape` | `wide` (the default) for the grid as Essbase lays it out, `long` for a row per value |
| `pov` | `columns` to add the point of view's members as columns; `none` by default |
| `names` | `as-is` (the default), or `upper` or `lower` to make column names identifiers, such as `JAN_COLA` |
| `missing` | What `#Missing` becomes: `null` (the default), `zero`, or `skip` to leave it out |
| `timeout` | Seconds to wait for a query; a statement's `setQueryTimeout` wins over it |
| `auth` | How to log in: `password` (the default), `token` or `session`; see [Logging in](#logging-in) |

All but `app`, `cube` and `auth` can also be set for one query, in a hint (see below), which wins over
the connection.

### Logging in

By default Thriller logs in with the user and password. For an Essbase behind an identity provider,
where users have no Essbase password, it can present something the provider issued instead, in the
password's place and with no user needed:

- `auth=token`: the password is a bearer token, if the server accepts them; an on-premises Essbase
  doesn't.
- `auth=session`: the password is the id of a session established elsewhere, the `JSESSIONID`
  cookie. Add `weblogic.cookie=...` if the server also issued a `_WL_AUTHCOOKIE_JSESSIONID`.

Tools and connection pools check a connection with a query such as `SELECT 1`, or IntelliJ's
`select 'keep alive'`. Thriller answers those itself, after checking that the server still does.

## Columns

Each row of the result is a member for each dimension on the rows, then the values. Without any
hints, Thriller names the columns from the result itself:

- one column per row dimension, named for the dimension and holding member names
- one per data column, named for the members above it (`Jan`, or `Jan 100-10` when two dimensions
  are on the columns), and numeric if every value in it is a number
- `COLUMN2`, `COLUMN3` and so on where there's nothing to name it from, such as dimension properties

`#Missing` comes back as null, unless `missing` says otherwise.

To choose the names and types yourself, declare them in a comment block. This is where the driver's
original idea lives:

```
/**
 * -- column.1.name = MEASURE
 * -- column.1.type = VARCHAR
 * -- column.2.name = JAN
 * -- column.2.type = DECIMAL
 * -- column.2.precision = 9
 * -- column.2.scale = 2
 */
SELECT {[Jan]} ON COLUMNS, {[Measures].Levels(0).members} ON ROWS FROM [Sample].[Basic]
```

Columns are matched to each row's cells by position. Hints that name and type every column, from the
first on, are the result's columns: extra cells are left off, and missing ones are null. Hints that
leave something out adjust the columns Thriller works out instead, so `-- column.4.scale = 1` just
rounds the fourth column, and `-- column.1.name = REGION` just renames the first. A scale or
precision on its own makes a numeric column `DECIMAL`. The types are `VARCHAR`, `NVARCHAR`, `DECIMAL`, `NUMERIC`, `DOUBLE`, `FLOAT`, `INTEGER`,
`SMALLINT` and `TINYINT`. A `DECIMAL` with a scale is rounded to it. The comment block can sit
anywhere in the query. Thriller blanks comments out before sending the query, since Essbase stops
reading at the first semicolon, even one inside a comment.

Some tools leave a comment off when it comes before the query, since it isn't part of the statement
they find to run. Putting the block just after `SELECT` keeps it inside the statement:

```
SELECT
/*
 -- column.1.name = MEASURE
 -- column.1.type = VARCHAR
 */
{[Jan]} ON COLUMNS, {[Measures].Levels(0).members} ON ROWS FROM [Sample].[Basic]
```

## Member names

Members come back by name unless asked otherwise. Set `members` in the URL for a whole connection, or
in a hint for one query; the hint wins:

```
SELECT
/*
 -- members = alias
 -- alias.table = Long Names
 */
{[Jan], [Feb]} ON COLUMNS, [Colas].Children ON ROWS FROM Sample.Basic
```

Aliases rename the members on both axes, so the columns above are `January` and `February`, and a
member without an alias in the table keeps its name.

## Shaping the result

A query's result is a grid: members down the side, members across the top, values in between. Tools
that think in tables, such as BI tools and dataframes, want one row per value instead, with a column
for each dimension. `shape = long` does that, `pov = columns` adds the point of view's members as
columns too, and `names = upper` makes the column names identifiers:

```
SELECT
/*
 -- shape = long
 -- pov = columns
 -- names = upper
 */
{[Jan], [Feb]} ON COLUMNS, CROSSJOIN({[East], [West]}, {[Cola]}) ON ROWS
FROM Sample.Basic WHERE ([Sales], [Actual])
```

```
+--------+---------+----------+----------+------+--------+
| MARKET | PRODUCT | MEASURES | SCENARIO | YEAR | VALUE  |
+--------+---------+----------+----------+------+--------+
| East   | 100-10  | Sales    | Actual   | Jan  | 1812.0 |
| East   | 100-10  | Sales    | Actual   | Feb  | 1754.0 |
...
```

`missing = skip` leaves out the rows of a long result whose value is `#Missing`, and the rows of a
wide one with nothing but. The point of view's members need a server that reports them, which 26.1
does and 21.1 doesn't.

## Browsing

Tools that show a database as a tree see the server laid out the way a `FROM` clause names it: each
application is a schema, each cube a table in it, and each dimension a column, with whether it's
dense or sparse and how many members it has as the column's comment. The server's MDX functions are
listed as each application's routines, with how to call them. Asking a tool to show a cube's data
won't work, though: it sends SQL, not MDX.

## Using it in a tool

Download `thriller-mdx-jdbc-<version>-all.jar` from the
[releases page](https://github.com/jasonwjones/thriller-mdx-jdbc/releases) or from
[Maven Central](https://central.sonatype.com/artifact/com.jasonwjones.hyperion.thriller/thriller-mdx-jdbc).
It's a single jar with everything Thriller needs; add it to the tool as a driver. With
[sqlline](https://github.com/julianhyde/sqlline):

```
java -cp sqlline-1.12.0-jar-with-dependencies.jar:thriller-mdx-jdbc-1.1.0-all.jar sqlline.SqlLine \
  -u "jdbc:essbase:thriller:http://localhost:9000/essbase" -n admin -p '...'
```

To build it yourself instead, `mvn package` leaves the same jar in `target/`.

### IntelliJ IDEA and DataGrip

1. In the Database tool window, add a **Driver**: add the jar under **Driver Files**, and choose
   `com.jasonwjones.hyperion.thriller.ThrillerDriver` as the class.
2. Add a **Data Source** using that driver, with the URL
   `jdbc:essbase:thriller:http://localhost:9000/essbase` and your Essbase user and password.
3. Run MDX in its query console.

With nothing selected, the console runs the statement under the cursor, and a comment block before
`SELECT` isn't part of it, so its hints never reach Thriller. Either select the whole query, comment
included, before running it, or put the block after `SELECT` as shown above.

The console reads queries as SQL, so it doesn't see a `?` in MDX as a parameter. Use IntelliJ's own
placeholders instead, which it asks for and fills in as text before sending the query, so write the
brackets around them yourself: `{[${product}]}`.

## Using it from code

Thriller is on Maven Central:

```xml
<dependency>
    <groupId>com.jasonwjones.hyperion.thriller</groupId>
    <artifactId>thriller-mdx-jdbc</artifactId>
    <version>1.1.0</version>
</dependency>
```

Then use JDBC as usual:

```java
try (Connection connection = DriverManager.getConnection(
        "jdbc:essbase:thriller:http://localhost:9000/essbase", "admin", password);
     ResultSet rows = connection.createStatement().executeQuery(
        "SELECT {[Jan]} ON COLUMNS, {[East], [West]} ON ROWS FROM Sample.Basic")) {
    while (rows.next()) {
        System.out.println(rows.getString("Market") + ": " + rows.getDouble("Jan"));
    }
}
```

A `PreparedStatement` fills in `?` with members and numbers. A string becomes a member name in
brackets, so `"East"` is `[East]`, and a number is itself:

```java
PreparedStatement statement = connection.prepareStatement(
        "SELECT {[Jan]} ON COLUMNS, {?} ON ROWS FROM Sample.Basic WHERE ([Sales], ?)");
statement.setString(1, "Cola");
statement.setString(2, "East");
```

A `?` inside a comment, a quoted string or a bracketed name is left alone. To pass a set or an
expression, put it in the query's text instead.

## What it doesn't do

- **Change anything.** It runs queries; updates and batches are refused.
- **Run SQL.** Every query is MDX, sent to Essbase as written apart from its comments. The one
  exception is a query that selects a single literal, such as `SELECT 1` or IntelliJ's
  `select 'keep alive'`, which tools and connection pools send to check a connection: Thriller checks
  that the server still answers and returns the literal.
- **Stream.** Each query is one REST call, and the whole result is held in memory.
- **Stop a query.** A timeout stops Thriller waiting, but Essbase carries on until the query is done.
- **Honor the old Camshaft hints.** `thriller.print-tuple-member-separately` is how the REST API
  always works now. `thriller.maximum-fraction-digits` is ignored; use a `DECIMAL` column's scale.
- **Keep its dependencies to itself.** The single jar bundles essbase-rest-client, Spring's web
  client and Jackson as they are. In an application with its own versions of those, use the plain
  jar from `target/` and let your build resolve them.

## Building and testing

Java 17 or newer.

```
mvn test
```

runs the tests against JSON recorded from Essbase 21.1 and 26.1 servers, with no server needed. To run them
against a live one with Sample.Basic loaded as well, which also checks the cookbook's outputs:

```
THRILLER_URL=jdbc:essbase:thriller:http://localhost:9000/essbase \
THRILLER_USER=admin THRILLER_PASSWORD=... mvn test
```

After changing an example, add `-Dtest=ExamplesTest -Dthriller.examples.record=true` to record its
output and rebuild the cookbook page.

## Releasing

Set the release version in `pom.xml`, commit, and push a matching tag:

```
git tag v1.1.0
git push origin v1.1.0
```

The release workflow checks the tag against the pom, builds and tests, publishes to Maven Central,
and publishes a GitHub release with the single jar attached. Then move the pom on to the next
`-SNAPSHOT` version. Publishing to Central needs the repository secrets listed at the top of
`.github/workflows/release.yml`.

## License

[Apache License 2.0](LICENSE).
