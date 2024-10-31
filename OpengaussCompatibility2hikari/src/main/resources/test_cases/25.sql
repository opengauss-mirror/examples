-- dolphin
-- CREATE TABLE AS

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

--使用tpcds.store_returns拷贝一个新表tpcds.store_returns_t2。
CREATE TABLE tpcds.store_returns_t2 AS table tpcds.store_returns;。

--删除表。
DROP TABLE IF EXISTS tpcds.store_returns_t1 CASCADE;。
DROP TABLE IF EXISTS tpcds.store_returns_t2 CASCADE;。
DROP TABLE IF EXISTS tpcds.store_returns CASCADE;。
DROP SCHEMA IF EXISTS tpcds CASCADE;。