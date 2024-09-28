-- dolphin
-- CREATE INDEX

CREATE SCHEMA IF NOT EXISTS tpcds;。
--创建表tpcds.ship_mode_t1。
CREATE TABLE IF NOT EXISTS tpcds.ship_mode_t1
(
    SM_SHIP_MODE_SK           INTEGER               NOT NULL,
    SM_SHIP_MODE_ID           CHAR(16)              NOT NULL,
    SM_TYPE                   CHAR(30)                      ,
    SM_CODE                   CHAR(10)                      ,
    SM_CARRIER                CHAR(20)                      ,
    SM_CONTRACT               CHAR(20)
) 
;。

--在表tpcds.ship_mode_t1上的SM_SHIP_MODE_SK字段上创建普通的唯一索引。
CREATE UNIQUE INDEX IF NOT EXISTS ds_ship_mode_t1_index1 ON tpcds.ship_mode_t1(SM_SHIP_MODE_SK);。

--在表tpcds.ship_mode_t1上的SM_SHIP_MODE_SK字段上创建指定B-tree索引。
CREATE INDEX IF NOT EXISTS ds_ship_mode_t1_index4 ON tpcds.ship_mode_t1 USING btree(SM_SHIP_MODE_SK);。

--在表tpcds.ship_mode_t1上SM_CODE字段上创建表达式索引。
CREATE INDEX IF NOT EXISTS ds_ship_mode_t1_index2 ON tpcds.ship_mode_t1(SUBSTR(SM_CODE,1 ,4));。

--在表tpcds.ship_mode_t1上的SM_SHIP_MODE_SK字段上创建SM_SHIP_MODE_SK大于10的部分索引。
CREATE UNIQUE INDEX IF NOT EXISTS ds_ship_mode_t1_index3 ON tpcds.ship_mode_t1(SM_SHIP_MODE_SK) WHERE SM_SHIP_MODE_SK>10;。

--重命名一个现有的索引。
ALTER INDEX tpcds.ds_ship_mode_t1_index1 RENAME TO ds_ship_mode_t1_index5;。

--设置索引不可用。
ALTER INDEX tpcds.ds_ship_mode_t1_index2 UNUSABLE;。

--重建索引。
ALTER INDEX tpcds.ds_ship_mode_t1_index2 REBUILD;。

--删除一个现有的索引。
DROP INDEX IF EXISTS tpcds.ds_ship_mode_t1_index2;。

--删除表。
DROP TABLE IF EXISTS tpcds.ship_mode_t1 CASCADE;。

--创建表空间。
CREATE TABLESPACE example1 RELATIVE LOCATION 'tablespace1/tablespace_1';。
CREATE TABLESPACE example2 RELATIVE LOCATION 'tablespace2/tablespace_2';。
CREATE TABLESPACE example3 RELATIVE LOCATION 'tablespace3/tablespace_3';。
CREATE TABLESPACE example4 RELATIVE LOCATION 'tablespace4/tablespace_4';。

--创建表tpcds.customer_address_p1。
CREATE TABLE IF NOT EXISTS tpcds.customer_address_p1
(
    CA_ADDRESS_SK             INTEGER               NOT NULL,
    CA_ADDRESS_ID             CHAR(16)              NOT NULL,
    CA_STREET_NUMBER          CHAR(10)                      ,
    CA_STREET_NAME            VARCHAR(60)                   ,
    CA_STREET_TYPE            CHAR(15)                      ,
    CA_SUITE_NUMBER           CHAR(10)                      ,
    CA_CITY                   VARCHAR(60)                   ,
    CA_COUNTY                 VARCHAR(30)                   ,
    CA_STATE                  CHAR(2)                       ,
    CA_ZIP                    CHAR(10)                      ,
    CA_COUNTRY                VARCHAR(20)                   ,
    CA_GMT_OFFSET             DECIMAL(5,2)                  ,
    CA_LOCATION_TYPE          CHAR(20)
)
TABLESPACE example1
PARTITION BY RANGE(CA_ADDRESS_SK)
( 
   PARTITION p1 VALUES LESS THAN (3000),
   PARTITION p2 VALUES LESS THAN (5000) TABLESPACE example1,
   PARTITION p3 VALUES LESS THAN (MAXVALUE) TABLESPACE example2
)
ENABLE ROW MOVEMENT;。

--创建分区表索引ds_customer_address_p1_index1，不指定索引分区的名称。
CREATE INDEX IF NOT EXISTS ds_customer_address_p1_index1 ON tpcds.customer_address_p1(CA_ADDRESS_SK) LOCAL;。
--创建分区表索引ds_customer_address_p1_index2，并指定索引分区的名称。
CREATE INDEX IF NOT EXISTS ds_customer_address_p1_index2 ON tpcds.customer_address_p1(CA_ADDRESS_SK) LOCAL
(
    PARTITION CA_ADDRESS_SK_index1,
    PARTITION CA_ADDRESS_SK_index2 TABLESPACE example3,
    PARTITION CA_ADDRESS_SK_index3 TABLESPACE example4
) 
TABLESPACE example2;。

--创建GLOBAL分区索引
CREATE INDEX IF NOT EXISTS ds_customer_address_p1_index3 ON tpcds.customer_address_p1(CA_ADDRESS_ID) GLOBAL;。

--不指定关键字，默认创建GLOBAL分区索引
CREATE INDEX IF NOT EXISTS ds_customer_address_p1_index4 ON tpcds.customer_address_p1(CA_ADDRESS_ID);。

--修改分区表索引CA_ADDRESS_SK_index2的表空间为example1。
ALTER INDEX tpcds.ds_customer_address_p1_index2 MOVE PARTITION CA_ADDRESS_SK_index2 TABLESPACE example1;。

--修改分区表索引CA_ADDRESS_SK_index3的表空间为example2。
ALTER INDEX tpcds.ds_customer_address_p1_index2 MOVE PARTITION CA_ADDRESS_SK_index3 TABLESPACE example2;。

--重命名分区表索引。
ALTER INDEX tpcds.ds_customer_address_p1_index2 RENAME PARTITION CA_ADDRESS_SK_index1 TO CA_ADDRESS_SK_index4;。

--删除索引和分区表。
DROP INDEX IF EXISTS tpcds.ds_customer_address_p1_index1;。
DROP INDEX IF EXISTS tpcds.ds_customer_address_p1_index2;。
DROP TABLE IF EXISTS tpcds.customer_address_p1 CASCADE;。
--删除表空间。
DROP TABLESPACE example1;。
DROP TABLESPACE example2;。
DROP TABLESPACE example3;。
DROP TABLESPACE example4;。

--创建列存表以及列存表GIN索引。
create table if not exists cgin_create_test(a int, b text) with (orientation = column);。
create index if not exists cgin_test on cgin_create_test using gin(to_tsvector('ngram', b));。

--索引名重复的场景，打开dolphin.b_compatibility_mode后，重复索引名将自动替换成其他不重复的名字
set dolphin.b_compatibility_mode to on;。
create table if not exists t1(id int,index idx_id(id));。
create table if not exists t2(id int,index idx_id(id));。

-- 全文索引
CREATE SCHEMA IF NOT EXISTS fulltext_test;。
set current_schema to 'fulltext_test';。
CREATE TABLE IF NOT EXISTS test (
 id int unsigned auto_increment not null primary key,
 title varchar,
 boby text,
 name name
);。

SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM
    information_schema.columns
WHERE
    table_name = 'test';。

CREATE FULLTEXT INDEX test_index_1 ON test (title, boby) WITH PARSER ngram;。

SELECT
    indexname,
    indexdef
FROM
    pg_indexes
WHERE
    tablename = 'test' AND
    indexname = 'test_index_1';。

CREATE FULLTEXT INDEX test_index_2 ON test (title, boby, name);。

DROP TABLE IF EXISTS cgin_create_test CASCADE;。
DROP TABLE IF EXISTS t1 CASCADE;。
DROP TABLE IF EXISTS t2 CASCADE;。

DROP SCHEMA IF EXISTS fulltext_test CASCADE;。
DROP SCHEMA IF EXISTS tpcds CASCADE;。