-- CREATE TABLE AS = SELECT INTO

CREATE SCHEMA IF NOT EXISTS tpcds;。
--创建一个表tpcds.store_returns表。
CREATE TABLE IF NOT EXISTS tpcds.store_returns
(
    W_WAREHOUSE_SK            INTEGER               NOT NULL,
    W_WAREHOUSE_ID            CHAR(16)              NOT NULL,
    sr_item_sk                VARCHAR(20)                   ,
    W_WAREHOUSE_SQ_FT         INTEGER
);。
--创建一个表tpcds.store_returns_t1并插入tpcds.store_returns表中sr_item_sk字段中大于16的数值。
CREATE TABLE tpcds.store_returns_t1 AS SELECT * FROM tpcds.store_returns WHERE sr_item_sk > '4795';。

--将tpcds.reason表中r_reason_sk小于5的值加入到新建表中。
CREATE TABLE tpcds.reason_t1 AS
SELECT * FROM reason WHERE r_reason_sk < 2 WITH NO DATA;。

SELECT * INTO tpcds.reason_t2 FROM reason WHERE r_reason_sk < 5;。

--删除tpcds.reason_t1表。
DROP TABLE IF EXISTS tpcds.reason_t1 CASCADE;。
--删除tpcds.reason_t2表。
DROP TABLE IF EXISTS tpcds.reason_t2 CASCADE;。

--使用tpcds.store_returns拷贝一个新表tpcds.store_returns_t2。
CREATE TABLE tpcds.store_returns_t2 AS table tpcds.store_returns;。

--B模式下
CREATE TABLE tpcds.store_returns_t3(newcol INTEGER) AS table tpcds.store_returns;。

--删除表。
DROP TABLE IF EXISTS tpcds.store_returns_t1 CASCADE;。
DROP TABLE IF EXISTS tpcds.store_returns_t2 CASCADE;。
DROP TABLE IF EXISTS tpcds.store_returns_t3 CASCADE;。
DROP TABLE IF EXISTS tpcds.store_returns CASCADE;。
DROP SCHEMA IF EXISTS tpcds CASCADE;。
