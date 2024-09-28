-- dolphin
-- CREATE TABLE

CREATE SCHEMA IF NOT EXISTS tpcds;。
--创建表上索引
CREATE TABLE IF NOT EXISTS tpcds.warehouse_t24
(
    W_WAREHOUSE_SK            INTEGER               NOT NULL,
    W_WAREHOUSE_ID            CHAR(16)              NOT NULL,
    W_WAREHOUSE_NAME          VARCHAR(20)                   ,
    W_WAREHOUSE_SQ_FT         INTEGER                       ,
    W_STREET_NUMBER           CHAR(10)                      ,
    W_STREET_NAME             VARCHAR(60)                   ,
    W_STREET_TYPE             CHAR(15)                      ,
    W_SUITE_NUMBER            CHAR(10)                      ,
    W_CITY                    VARCHAR(60)                   ,
    W_COUNTY                  VARCHAR(30)                   ,
    W_STATE                   CHAR(2)                       ,
    W_ZIP                     CHAR(10)                      ,
    W_COUNTRY                 VARCHAR(20)                   ,
    W_GMT_OFFSET              DECIMAL(5,2)                  ,
    key (W_WAREHOUSE_SK)                                    ,
    index idx_ID using btree (W_WAREHOUSE_ID)
);。

--创建表上组合索引、表达式索引、函数索引
CREATE TABLE IF NOT EXISTS tpcds.warehouse_t25
(
    W_WAREHOUSE_SK            INTEGER               NOT NULL,
    W_WAREHOUSE_ID            CHAR(16)              NOT NULL,
    W_WAREHOUSE_NAME          VARCHAR(20)                   ,
    W_WAREHOUSE_SQ_FT         INTEGER                       ,
    W_STREET_NUMBER           CHAR(10)                      ,
    W_STREET_NAME             VARCHAR(60)                   ,
    W_STREET_TYPE             CHAR(15)                      ,
    W_SUITE_NUMBER            CHAR(10)                      ,
    W_CITY                    VARCHAR(60)                   ,
    W_COUNTY                  VARCHAR(30)                   ,
    W_STATE                   CHAR(2)                       ,
    W_ZIP                     CHAR(10)                      ,
    W_COUNTRY                 VARCHAR(20)                   ,
    W_GMT_OFFSET              DECIMAL(5,2)                  ,
    key using btree (W_WAREHOUSE_SK, W_WAREHOUSE_ID desc)   ,
    index idx_SQ_FT using btree ((abs(W_WAREHOUSE_SQ_FT)))  ,
    key idx_SK using btree ((abs(W_WAREHOUSE_SK)+1))
);。

--创建带INVISIBLE普通索引的表
CREATE TABLE IF NOT EXISTS tpcds.warehouse_t26
(
    W_WAREHOUSE_SK            INTEGER               NOT NULL,
    W_WAREHOUSE_ID            CHAR(16)              NOT NULL,
    W_WAREHOUSE_NAME          VARCHAR(20)                   ,
    W_WAREHOUSE_SQ_FT         INTEGER                       ,
    W_STREET_NUMBER           CHAR(10)                      ,
    W_STREET_NAME             VARCHAR(60)                   ,
    W_STREET_TYPE             CHAR(15)                      ,
    W_SUITE_NUMBER            CHAR(10)                      ,
    W_CITY                    VARCHAR(60)                   ,
    W_COUNTY                  VARCHAR(30)                   ,
    W_STATE                   CHAR(2)                       ,
    W_ZIP                     CHAR(10)                      ,
    W_COUNTRY                 VARCHAR(20)                   ,
    W_GMT_OFFSET              DECIMAL(5,2)                  ,
    index idx_ID using btree (W_WAREHOUSE_ID) INVISIBLE
);。

--包含index_option字段
create table IF NOT EXISTS test_option(a int, index idx_op using btree(a) comment 'idx comment');。

--创建表格时对列指定字符集。
--WARNING:  character set "test_charset" for type text is not supported yet. default value set
CREATE TABLE IF NOT EXISTS t_column_charset(c text CHARSET test_charset);。

--创建表格时对表格指定字符序。
--WARNING:  COLLATE for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_table_collate(c text) COLLATE test_collation;。

--创建表格时对表格指定字符集。
--WARNING:  CHARSET for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_table_charset(c text) CHARSET test_charset;。

--创建表格时对表格指定行记录格式。
--WARNING:  ROW_FORMAT for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_row_format(c text) ROW_FORMAT test_row_format;。

--创建表时对表指定在表空间变满时扩展表空间大小。
--WARNING:  AUTOEXTEND_SIZE for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_autoextend_size(c text) AUTOEXTEND_SIZE 4M;。

--创建表时对表指定表的平均行长度。
--WARNING:  AVG_ROW_LENGTH for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_avg_row_length(c text) AVG_ROW_LENGTH 10;。

--创建表时对表指定是否维护所有行的实时校验和。
--WARNING:  CHECKSUM for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_checksum(c text) CHECKSUM 0;。

--创建表时对表指定联合表的连接字符串。
--WARNING:  CONNECTION for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_connection(c text) CONNECTION 'connect_string';。

--创建表时对表指定表数据数据和索引的存储目录。
--WARNING:  DIRECTORY for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_data_directory(c text) DATA DIRECTORY 'data_directory';。
--WARNING:  DIRECTORY for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_index_directory(c text) INDEX DIRECTORY 'index_directory';。

--创建表时对表指定是否延迟表的索引更新直到表关闭。
--WARNING:  DELAY_KEY_WRITE for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_delay_key_write(c text) DELAY_KEY_WRITE 1;。

--创建表时对表指定表启用或禁用页面级数据加密。
--WARNING:  ENCRYPTION for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_encryption(c text) ENCRYPTION 'Y';。

--创建表时对表指定主存储引擎的表属性。
--WARNING:  ENGINE_ATTRIBUTE for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_engine_attribute(c text) ENGINE_ATTRIBUTE 'engine_attribute';。

--创建表时对表指定应将行插入到的表。
--WARNING:  INSERT_METHOD for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_insert_method(c text) INSERT_METHOD NO;。

--创建表时对表指定索引键块的字节大小。
--WARNING:  KEY_BLOCK_SIZE for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_key_block_size(c text) KEY_BLOCK_SIZE 10;。

--创建表时对表指定计划在表中存储的最大行数。
--WARNING:  MAX_ROWS for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_max_rows(c text) MAX_ROWS 20;。

--创建表时对表指定计划在表中存储的最小行数。
--WARNING:  MIN_ROWS for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_min_rows(c text) MIN_ROWS 5;。

--创建表时对表指定控制压缩索引的方式。
--WARNING:  PACK_KEYS for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_pack_keys(c text) PACK_KEYS DEFAULT;。
--WARNING:  PASSWORD for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_password(c text) PASSWORD 'password';。

--创建表时对表指定开启事务模式。
--WARNING:  START TRANSACTION for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_start_transaction(c text) START TRANSACTION;。

--创建表时对表指定辅助存储引擎的表属性。
--WARNING:  SECONDARY_ENGINE_ATTRIBUTE for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_secondary_engine_attribute(c text) SECONDARY_ENGINE_ATTRIBUTE 'secondary_engine_attribute';。

--创建表时对表指定是否自动重新计算表的持久统计信息。
--WARNING:  STATS_AUTO_RECALC for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_stats_auto_recalc(c text) STATS_AUTO_RECALC DEFAULT;。

--创建表时对表指定是否为表启用持久统计信息。
--WARNING:  STATS_PERSISTENT for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_stats_persistent(c text) STATS_PERSISTENT DEFAULT;。

--创建表时对表指定估计索引列的基数和其他统计信息时要采样的索引页数。
--WARNING:  STATS_SAMPLE_PAGES for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_stats_sample_pages(c text) STATS_SAMPLE_PAGES 1;。

--创建表时访问一组相同的表作为一个表。
--WARNING:  UNION for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_union(c text) UNION(a, b);。

--创建表时对表指定表存储在磁盘。
--WARNING:  Suffix ".ibd" of datafile path detected. The actual path will be renamed as "data_ibd"
CREATE TABLESPACE test ADD DATAFILE 'data.ibd';。
--WARNING:  TABLESPACE_OPTION for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_tablespace_storage_disk(c text) TABLESPACE test STORAGE DISK;。


--创建表时对表指定表存储在内存。
--WARNING:  Suffix ".ibd" of datafile path detected. The actual path will be renamed as "data_ibd"
--CREATE TABLESPACE test ADD DATAFILE 'data.ibd';
--WARNING:  TABLESPACE_OPTION for TABLE is not supported for current version. skipped
CREATE TABLE IF NOT EXISTS t_tablespace_storage_memory(c text) TABLESPACE test STORAGE MEMORY;。

--创建兼容MySQL全文索引语法的表 前提是兼容模式为B的数据库。
--NOTICE:  CREATE TABLE will create implicit sequence "test_id_seq" for serial column "test.id"
--NOTICE:  CREATE TABLE / PRIMARY KEY will create implicit index "test_pkey" for table "test"
CREATE TABLE IF NOT EXISTS test (
 id int unsigned auto_increment not null primary key,
 title varchar,
 boby text,
 name name,
 FULLTEXT (title, boby) WITH PARSER ngram
);。

--NOTICE:  table "articles" does not exist, skipping
drop table if exists articles CASCADE;
CREATE TABLE IF NOT EXISTS articles (
 ID int,
 title VARCHAR(100),
 FULLTEXT INDEX ngram_idx(title)WITH PARSER ngram
);。

SELECT 
    column_name,
    data_type,
    is_nullable,
    column_default
FROM 
    information_schema.columns
WHERE 
    table_name = 'articles';。

drop table if exists articles CASCADE;。


CREATE TABLE IF NOT EXISTS articles (
 ID int,
 title VARCHAR(100),
 FULLTEXT INDEX (title)WITH PARSER ngram
);。

SELECT 
    column_name,
    data_type,
    is_nullable,
    column_default
FROM 
    information_schema.columns
WHERE 
    table_name = 'articles';。

drop table if exists articles CASCADE;。

CREATE TABLE IF NOT EXISTS articles (
 ID int,
 title VARCHAR(100),
 FULLTEXT KEY keyngram_idx(title)WITH PARSER ngram
);。

SELECT 
    column_name,
    data_type,
    is_nullable,
    column_default
FROM 
    information_schema.columns
WHERE 
    table_name = 'articles';。

drop table if exists articles CASCADE;。


CREATE TABLE IF NOT EXISTS articles (
 ID int,
 title VARCHAR(100),
 FULLTEXT KEY (title)WITH PARSER ngram
);。

SELECT 
    column_name,
    data_type,
    is_nullable,
    column_default
FROM 
    information_schema.columns
WHERE 
    table_name = 'articles';。

drop table if exists articles CASCADE;。

create table IF NOT EXISTS table_ddl_0154(col1 int,col2 varchar(64), FULLTEXT idx_ddl_0154(col2));。

SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM
    information_schema.columns
WHERE
    table_name = 'table_ddl_0154';。

drop table if exists t_autoextend_size CASCADE;。
drop table if exists t_avg_row_length CASCADE;。
drop table if exists t_checksum CASCADE;。
drop table if exists t_column_charset CASCADE;。
drop table if exists t_connection CASCADE;。
drop table if exists t_data_directory CASCADE;。
drop table if exists t_delay_key_write CASCADE;。
drop table if exists t_encryption CASCADE;。
drop table if exists t_engine_attribute CASCADE;。
drop table if exists t_index_directory CASCADE;。
drop table if exists t_insert_method CASCADE;。
drop table if exists t_key_block_size CASCADE;。
drop table if exists t_max_rows CASCADE;。
drop table if exists t_min_rows CASCADE;。
drop table if exists t_pack_keys CASCADE;。
drop table if exists t_password CASCADE;。
drop table if exists t_secondary_engine_attribute CASCADE;。
drop table if exists t_start_transaction CASCADE;。
drop table if exists t_stats_auto_recalc CASCADE;。
drop table if exists t_stats_persistent CASCADE;。
drop table if exists t_stats_sample_pages CASCADE;。
drop table if exists t_table_charset CASCADE;。
drop table if exists t_table_collate CASCADE;。
drop table if exists t_union CASCADE;。
drop table if exists t_row_format CASCADE;。
drop table if exists test CASCADE;。
drop table if exists test_option CASCADE;。
drop table if exists t_tablespace_storage_disk cascade;。
drop table if exists t_tablespace_storage_memory cascade;。
drop TABLESPACE test;。
drop table if exists table_ddl_0154 CASCADE;。

DROP SCHEMA IF EXISTS tpcds CASCADE;。