# Thriller cookbook

MDX queries against Sample.Basic, each with the result set Thriller returns for it. Every query is a
file in this folder, so you can open it in a tool and run it.

The outputs are recorded from Essbase 26.1 by `ExamplesTest`, which also checks them, and builds this
page, whenever the tests run against a server.

## Sales by market and product ([01-sales-by-market-and-product.mdx](01-sales-by-market-and-product.mdx))

Two dimensions on the rows become two text columns, and each month on the columns becomes a numeric one. No hints needed: the columns are named for the dimensions and members they hold.

```
SELECT {[Jan], [Feb], [Mar]} ON COLUMNS,
CROSSJOIN({[East], [West]}, [Colas].Children) ON ROWS
FROM Sample.Basic
WHERE ([Sales], [Actual])
```

```
+--------+---------+--------+--------+--------+
| Market | Product | Jan    | Feb    | Mar    |
+--------+---------+--------+--------+--------+
| East   | 100-10  | 1812.0 | 1754.0 | 1805.0 |
| East   | 100-20  | 200.0  | 206.0  | 214.0  |
| East   | 100-30  | 93.0   | 101.0  | 107.0  |
| West   | 100-10  | 1174.0 | 1146.0 | 1173.0 |
| West   | 100-20  | 700.0  | 726.0  | 727.0  |
| West   | 100-30  | 465.0  | 426.0  | 413.0  |
+--------+---------+--------+--------+--------+
```

## Choosing column names and types ([02-typed-columns.mdx](02-typed-columns.mdx))

A calculated member's percentages come back with every digit Essbase has. Hints name the columns and make the percentage a DECIMAL rounded to one place. They sit just after SELECT, so tools that only send the statement still send them.

```
WITH MEMBER [Measures].[Margin Pct] AS '[Margin] / [Sales] * 100'
SELECT
/*
 -- column.1.name = REGION
 -- column.1.type = VARCHAR
 -- column.2.name = SALES
 -- column.2.type = INTEGER
 -- column.3.name = MARGIN
 -- column.3.type = INTEGER
 -- column.4.name = MARGIN_PCT
 -- column.4.type = DECIMAL
 -- column.4.precision = 4
 -- column.4.scale = 1
 */
{[Sales], [Margin], [Margin Pct]} ON COLUMNS,
[Market].Children ON ROWS
FROM Sample.Basic
WHERE ([Actual], [Year], [Product])
```

```
+---------+--------+--------+------------+
| REGION  | SALES  | MARGIN | MARGIN_PCT |
+---------+--------+--------+------------+
| East    | 87398  | 49471  | 56.6       |
| West    | 132931 | 70026  | 52.7       |
| South   | 50846  | 28896  | 56.8       |
| Central | 129680 | 73126  | 56.4       |
+---------+--------+--------+------------+
```

## Products by alias ([03-aliases.mdx](03-aliases.mdx))

Sample.Basic's products are named 100-10, 100-20 and so on, with aliases like Cola. Asking for aliases names them that way. Put members=alias in the connection URL to have it for every query instead.

```
SELECT
/*
 -- members = alias
 */
{[Qtr1], [Qtr2], [Qtr3], [Qtr4]} ON COLUMNS,
[Colas].Children ON ROWS
FROM Sample.Basic
WHERE ([Sales], [Actual], [Market])
```

```
+--------------------+---------+---------+---------+---------+
| Product            | Qtr1    | Qtr2    | Qtr3    | Qtr4    |
+--------------------+---------+---------+---------+---------+
| Cola               | 14585.0 | 16048.0 | 17298.0 | 14893.0 |
| Diet Cola          | 7276.0  | 7957.0  | 8057.0  | 7179.0  |
| Caffeine Free Cola | 3187.0  | 3182.0  | 3189.0  | 3283.0  |
+--------------------+---------+---------+---------+---------+
```

## Another alias table ([04-long-names.mdx](04-long-names.mdx))

Aliases come from the cube's default table unless a hint names another. Sample.Basic's Long Names table spells out the months, and has nothing for the markets, which keep their names.

```
SELECT
/*
 -- members = alias
 -- alias.table = Long Names
 */
{[Jan], [Feb], [Mar]} ON COLUMNS,
[East].Children ON ROWS
FROM Sample.Basic
WHERE ([Sales], [Actual], [Product])
```

```
+---------------+---------+----------+--------+
| Market        | January | February | March  |
+---------------+---------+----------+--------+
| New York      | 2479.0  | 2625.0   | 2601.0 |
| Massachusetts | 1251.0  | 1206.0   | 1203.0 |
| Florida       | 1321.0  | 1383.0   | 1428.0 |
| Connecticut   | 1197.0  | 1157.0   | 1118.0 |
| New Hampshire | 532.0   | 549.0    | 571.0  |
+---------------+---------+----------+--------+
```

## Top five products ([05-top-products.mdx](05-top-products.mdx))

TopCount ranks a set by a measure. Diet drinks are shared members under both their own family and Diet, so the level 0 products include them twice; the Filter drops the shared copies, whose unique names start with a bracket.

```
SELECT
/*
 -- members = alias
 */
{[Sales]} ON COLUMNS,
TopCount(
  Filter([Product].Levels(0).Members, LEFT([Product].CurrentMember.MEMBER_UNIQUE_NAME, 1) <> "["),
  5, [Sales]
) ON ROWS
FROM Sample.Basic
WHERE ([Actual], [Year], [Market])
```

```
+----------------+---------+
| Product        | Sales   |
+----------------+---------+
| Cola           | 62824.0 |
| Dark Cream     | 46956.0 |
| Old Fashioned  | 41537.0 |
| Diet Root Beer | 38240.0 |
| Diet Cream     | 36969.0 |
+----------------+---------+
```

## #Missing and NON EMPTY ([06-missing-and-non-empty.mdx](06-missing-and-non-empty.mdx))

Nevada sells no Caffeine Free Cola, so without NON EMPTY its row would be all #Missing, which Thriller hands back as nulls. NON EMPTY leaves such rows out. If every row drops out, the result still has its columns, just no rows.

```
SELECT {[Jan], [Feb]} ON COLUMNS,
NON EMPTY CROSSJOIN({[Nevada], [Utah]}, [Colas].Children) ON ROWS
FROM Sample.Basic
WHERE ([Sales], [Actual])
```

```
+--------+---------+-------+-------+
| Market | Product | Jan   | Feb   |
+--------+---------+-------+-------+
| Nevada | 100-10  | 76.0  | 74.0  |
| Nevada | 100-20  | 62.0  | 63.0  |
| Utah   | 100-10  | 130.0 | 130.0 |
| Utah   | 100-20  | 190.0 | 193.0 |
| Utah   | 100-30  | 170.0 | 165.0 |
+--------+---------+-------+-------+
```

## Member properties as columns ([07-member-properties.mdx](07-member-properties.mdx))

DIMENSION PROPERTIES adds a column for each property after the member. With nothing on the columns, every row is a member, which makes this a way to read an outline into a table.

```
SELECT
/*
 -- column.1.name = PRODUCT
 -- column.1.type = VARCHAR
 -- column.2.name = ALIAS
 -- column.2.type = VARCHAR
 -- column.3.name = LEVEL
 -- column.3.type = INTEGER
 */
{} ON COLUMNS,
[Product].Members DIMENSION PROPERTIES
  PROPERTY_EXPR([Product], MEMBER_ALIAS, CurrentAxisMember(), "Alias"),
  PROPERTY_EXPR([Product], LEVEL_NUMBER, CurrentAxisMember(), "Level")
ON ROWS
FROM Sample.Basic
```

```
+---------+--------------------+-------+
| PRODUCT | ALIAS              | LEVEL |
+---------+--------------------+-------+
| Product |                    | 2     |
| 100     | Colas              | 1     |
| 100-10  | Cola               | 0     |
| 100-20  | Diet Cola          | 0     |
| 100-30  | Caffeine Free Cola | 0     |
| 200     | Root Beer          | 1     |
| 200-10  | Old Fashioned      | 0     |
| 200-20  | Diet Root Beer     | 0     |
| 200-30  | Sasparilla         | 0     |
| 200-40  | Birch Beer         | 0     |
| 300     | Cream Soda         | 1     |
| 300-10  | Dark Cream         | 0     |
| 300-20  | Vanilla Cream      | 0     |
| 300-30  | Diet Cream         | 0     |
| 400     | Fruit Soda         | 1     |
| 400-10  | Grape              | 0     |
| 400-20  | Orange             | 0     |
| 400-30  | Strawberry         | 0     |
| Diet    | Diet Drinks        | 1     |
| 100-20  | Shared Diet Cola   | 0     |
| 200-20  | Diet Root Beer     | 0     |
| 300-30  | Diet Cream         | 0     |
+---------+--------------------+-------+
```

## Level 0 members ([08-level-0-products.mdx](08-level-0-products.mdx))

The leaves of a dimension, with nothing on the columns, make a one-column list. By alias, as on the MDX Examples page this cookbook borrows from.

```
SELECT
/*
 -- members = alias
 */
{} ON COLUMNS,
{[Product].Levels(0).Members} ON ROWS
FROM Sample.Basic
```

```
+--------------------+
| Product            |
+--------------------+
| Cola               |
| Diet Cola          |
| Caffeine Free Cola |
| Old Fashioned      |
| Diet Root Beer     |
| Sasparilla         |
| Birch Beer         |
| Dark Cream         |
| Vanilla Cream      |
| Diet Cream         |
| Grape              |
| Orange             |
| Strawberry         |
| Shared Diet Cola   |
| Diet Root Beer     |
| Diet Cream         |
+--------------------+
```

## Members with a UDA, sorted ([09-major-markets.mdx](09-major-markets.mdx))

Intersect keeps the level 0 markets that carry the Major Market UDA, and Order sorts them by name.

```
SELECT {} ON COLUMNS,
{Order(
  Intersect(Uda([Market], "Major Market"), [Market].Levels(0).Members),
  [Market].CurrentMember.MEMBER_NAME
)} ON ROWS
FROM Sample.Basic
```

```
+---------------+
| Market        |
+---------------+
| California    |
| Colorado      |
| Florida       |
| Illinois      |
| Massachusetts |
| New York      |
| Ohio          |
| Texas         |
+---------------+
```

## Products with an attribute ([10-caffeinated-products.mdx](10-caffeinated-products.mdx))

Attribute returns the members that carry an attribute member, here every caffeinated drink.

```
SELECT
/*
 -- members = alias
 */
{} ON COLUMNS,
{Attribute([Caffeinated_True])} ON ROWS
FROM Sample.Basic
```

```
+----------------+
| Product        |
+----------------+
| Cola           |
| Diet Cola      |
| Old Fashioned  |
| Diet Root Beer |
| Dark Cream     |
| Vanilla Cream  |
| Diet Cream     |
+----------------+
```

## A fact table ([11-fact-table.mdx](11-fact-table.mdx))

The long shape turns the grid into one row for each value, with a column for each dimension: what BI tools, dataframes and SQL want. The point of view's members become columns too, so every row says what it measures, the members are named by alias, and the column names are made into identifiers.

```
SELECT
/*
 -- shape = long
 -- pov = columns
 -- names = upper
 -- members = alias
 */
{[Jan], [Feb]} ON COLUMNS,
CROSSJOIN({[East], [West]}, {[Cola], [Root Beer]}) ON ROWS
FROM Sample.Basic
WHERE ([Sales], [Actual])
```

```
+--------+-----------+----------+----------+------+--------+
| MARKET | PRODUCT   | MEASURES | SCENARIO | YEAR | VALUE  |
+--------+-----------+----------+----------+------+--------+
| East   | Cola      | Sales    | Actual   | Jan  | 1812.0 |
| East   | Cola      | Sales    | Actual   | Feb  | 1754.0 |
| East   | Root Beer | Sales    | Actual   | Jan  | 1853.0 |
| East   | Root Beer | Sales    | Actual   | Feb  | 1966.0 |
| West   | Cola      | Sales    | Actual   | Jan  | 1174.0 |
| West   | Cola      | Sales    | Actual   | Feb  | 1146.0 |
| West   | Root Beer | Sales    | Actual   | Jan  | 2723.0 |
| West   | Root Beer | Sales    | Actual   | Feb  | 2759.0 |
+--------+-----------+----------+----------+------+--------+
```

## #Missing as zero ([12-missing-as-zero.mdx](12-missing-as-zero.mdx))

Some tools would rather have zeros than nulls. Nevada sells no Caffeine Free Cola, which comes back as 0 here, and as null without the hint.

```
SELECT
/*
 -- missing = zero
 */
{[Jan], [Feb]} ON COLUMNS,
{[Nevada], [Utah], [California]} ON ROWS
FROM Sample.Basic
WHERE ([Sales], [Actual], [100-30])
```

```
+------------+-------+-------+
| Market     | Jan   | Feb   |
+------------+-------+-------+
| Nevada     | 0.0   | 0.0   |
| Utah       | 170.0 | 165.0 |
| California | 145.0 | 132.0 |
+------------+-------+-------+
```
