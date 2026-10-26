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

## Connecting

The URL is the driver prefix followed by the Essbase server's address, as you'd open it in a browser:

```
jdbc:essbase:thriller:http://localhost:9000/essbase
```

The user and password are ordinary JDBC connection properties, and the login is checked when the
connection opens. The driver class is `com.jasonwjones.hyperion.thriller.ThrillerDriver`, and it
registers itself.

Each query reads the cube its `FROM` clause names. For queries without one, give a default in the URL:

```
jdbc:essbase:thriller:http://localhost:9000/essbase;app=Sample;cube=Basic
```

## Columns

Each row of the result is a member for each dimension on the rows, then the values. Without any
hints, Thriller names the columns from the result itself:

- one column per row dimension, named for the dimension and holding member names
- one per data column, named for the members above it (`Jan`, or `Jan 100-10` when two dimensions
  are on the columns), and numeric if every value in it is a number
- `COLUMN2`, `COLUMN3` and so on where there's nothing to name it from, such as dimension properties

`#Missing` comes back as null.

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

Columns are matched to each row's cells by position. Extra cells are left off, and missing ones are
null. The types are `VARCHAR`, `NVARCHAR`, `DECIMAL`, `NUMERIC`, `DOUBLE`, `FLOAT`, `INTEGER`,
`SMALLINT` and `TINYINT`. A `DECIMAL` with a scale is rounded to it. The comment block can sit
anywhere in the query, and Essbase ignores it.

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

## Using it in a tool

Download `thriller-mdx-jdbc-<version>-all.jar` from the
[releases page](https://github.com/jasonwjones/thriller-mdx-jdbc/releases). It's a single jar with
everything Thriller needs; add it to the tool as a driver. With
[sqlline](https://github.com/julianhyde/sqlline):

```
java -cp sqlline-1.12.0-jar-with-dependencies.jar:thriller-mdx-jdbc-1.0.0-all.jar sqlline.SqlLine \
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

## Using it from code

Put the jar on the classpath and use JDBC as usual:

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

## What it doesn't do

- **Change anything.** It runs queries; updates and batches are refused.
- **List tables.** Essbase has none, so the metadata methods tools browse on connect return empty
  results.
- **Stream.** Each query is one REST call, and the whole result is held in memory.
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
against a live one with Sample.Basic loaded as well:

```
THRILLER_URL=jdbc:essbase:thriller:http://localhost:9000/essbase \
THRILLER_USER=admin THRILLER_PASSWORD=... mvn test
```

## Releasing

Set the release version in `pom.xml`, commit, and push a matching tag:

```
git tag v1.0.0
git push origin v1.0.0
```

The release workflow checks the tag against the pom, builds and tests, and publishes a GitHub
release with the single jar attached. Then move the pom on to the next `-SNAPSHOT` version.

## License

[Apache License 2.0](LICENSE).
