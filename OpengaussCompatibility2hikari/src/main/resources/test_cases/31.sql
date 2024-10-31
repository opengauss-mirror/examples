--dolphin
--SELECT

--SOUNDS LIKE子句：同音字段查询
CREATE TABLE IF NOT EXISTS TEST(id int, name varchar);。
INSERT INTO TEST VALUES(1, 'too');。
SELECT * FROM TEST WHERE name SOUNDS LIKE 'two';。

--SELECT GROUP BY子句中使用ROLLUP
CREATE TABLESPACE t_tbspace ADD DATAFILE 'my_tablespace' ENGINE = test_engine;。
CREATE TABLE IF NOT EXISTS t_with_rollup(id int, name varchar(20), area varchar(50), count int);。
INSERT INTO t_with_rollup values(1, 'a', 'A', 10);。
INSERT INTO t_with_rollup values(2, 'b', 'B', 15);。
INSERT INTO t_with_rollup values(2, 'b', 'B', 20);。
INSERT INTO t_with_rollup values(3, 'c', 'C', 50);。
INSERT INTO t_with_rollup values(3, 'c', 'C', 15);。
SELECT name, sum(count) FROM t_with_rollup GROUP BY ROLLUP(name);。

SELECT name, sum(count) FROM t_with_rollup GROUP BY (name) WITH ROLLUP;。

create table if not exists join_1(col1 int4, col2 int8);。

create table if not exists join_2(col1 int4, col2 int8);。

insert into join_1 values(1, 2), (3, 3);。

insert into join_2 values(1, 1), (2, 3), (4, 4);。

--SELECT 语句中使用FROM DUAL
select 1 as col;。
select 1 as col FROM DUAL;。

--SELECT FROM PARTITION子句指定多个分区
create table if not exists multi_partition_select_test(C_INT INTEGER) partition by range(C_INT)
(
     partition test_part1 values less than (400),
     partition test_part2 values less than (700),
     partition test_part3 values less than (1000)
);。
insert into multi_partition_select_test values(111);。
insert into multi_partition_select_test values(555);。
insert into multi_partition_select_test values(888);。

select a.* from multi_partition_select_test partition (test_part1, test_part2) a;。

--UNION子句非相似数据类型按 TEXT 类型进行转换
-- 创建两个表并插入测试数据
CREATE TABLE IF NOT EXISTS tbl_date(col DATE);。
INSERT INTO tbl_date VALUES('2000-02-16');。
CREATE TABLE IF NOT EXISTS tbl_json(col JSON);。
INSERT INTO tbl_json VALUES('{"id":1,"dbname":"openGauss","language":"C++"}');。

-- UNION 查询，将会使用TEXT类型进行转换
SELECT * FROM tbl_date UNION SELECT * FROM tbl_json;。

--兼容MySQL兼容性全文索引语法查询
CREATE SCHEMA fulltext_test;。
set current_schema to 'fulltext_test';。
--NOTICE:  CREATE TABLE will create implicit sequence "test_id_seq" for serial column "test.id"
--NOTICE:  CREATE TABLE / PRIMARY KEY will create implicit index "test_pkey" for table "test"
CREATE TABLE IF NOT EXISTS test (
 id int unsigned auto_increment not null primary key,
 title varchar,
 boby text,
 name name,
 FULLTEXT (title, boby) WITH PARSER ngram
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

SELECT
    indexname,
    indexdef
FROM
    pg_indexes
WHERE
    tablename = 'test' AND
    indexname = 'test_to_tsvector_to_tsvector1_idx';。

DROP INDEX test_to_tsvector_to_tsvector1_idx;。
ALTER TABLE test ADD FULLTEXT INDEX test_index_1 (title, boby) WITH PARSER ngram;。
DROP INDEX test_index_1;。
CREATE FULLTEXT INDEX test_index_1 ON test (title, boby) WITH PARSER ngram;。
SELECT
    indexname,
    indexdef
FROM
    pg_indexes
WHERE
    tablename = 'test' AND
    indexname = 'test_index_1';。

INSERT INTO test(title, boby, name) VALUES('test', '&67575@gauss', 'opengauss');。
INSERT INTO test(title, boby, name) VALUES('test1', 'gauss', 'opengauss');。
INSERT INTO test(title, boby, name) VALUES('test2', 'gauss2', 'opengauss');。
INSERT INTO test(title, boby, name) VALUES('test3', 'test', 'opengauss');。
INSERT INTO test(title, boby, name) VALUES('gauss_123_@', 'test', 'opengauss');。
INSERT INTO test(title, boby, name) VALUES('', '', 'opengauss');。
INSERT INTO test(title, boby, name) VALUES(' ', ' ', ' ');。
SELECT * FROM TEST;。

SELECT * FROM TEST WHERE MATCH (title, boby) AGAINST ('test');。

SELECT * FROM TEST WHERE MATCH (title, boby) AGAINST ('gauss');。

DROP INDEX test_index_1;。
CREATE FULLTEXT INDEX test_index_1 ON test (boby) WITH PARSER ngram;。
SELECT
    indexname,
    indexdef
FROM
    pg_indexes
WHERE
    tablename = 'test' AND
    indexname = 'test_index_1';。

SELECT * FROM test WHERE MATCH (boby) AGAINST ('test');。

SELECT * FROM test WHERE MATCH (boby) AGAINST ('gauss');。

DROP INDEX test_index_1;。
CREATE FULLTEXT INDEX test_index_1 ON test (title, boby, name) WITH PARSER ngram;。
SELECT
    indexname,
    indexdef
FROM
    pg_indexes
WHERE
    tablename = 'test' AND
    indexname = 'test_index_1';。

SELECT * FROM test WHERE MATCH (title, boby, name) AGAINST ('test');。

SELECT * FROM test WHERE MATCH (title, boby, name) AGAINST ('gauss');。

SELECT * FROM test WHERE MATCH (title, boby, name) AGAINST ('opengauss');。

--NOTICE:  table "articles" does not exist, skipping
drop table if exists articles cascade;。
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

drop table if exists articles cascade;。
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

drop table if exists articles cascade ;。
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

drop table if exists articles cascade;。
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

create table if not exists table_ddl_0154(col1 int,col2 varchar(64), FULLTEXT idx_ddl_0154(col2));。
create table if not exists table_ddl_0085(
 id int(11) not null,
 username varchar(50) default null,
 sex varchar(5) default null,
 address varchar(100) default null,
 score_num int(11));。
create fulltext index idx_ddl_0085_02 on table_ddl_0085(username);。
insert into table_ddl_0085 values (1,'test','m','xi''an changanqu', 10001), (2,'tst','w','xi''an beilingqu', 10002),
(3,'es','w','xi''an yangtaqu', 10003),(4,'s','m','beijingchaoyangqu', 10004);。
SELECT * FROM table_ddl_0085 WHERE MATCH (username) AGAINST ('te' IN NATURAL LANGUAGE MODE);。
SELECT * FROM table_ddl_0085 WHERE MATCH (username) AGAINST ('ts' IN NATURAL LANGUAGE MODE WITH QUERY EXPANSION);。
SELECT * FROM table_ddl_0085 WHERE MATCH (username) AGAINST ('test' IN BOOLEAN MODE);。
SELECT * FROM table_ddl_0085 WHERE MATCH (username) AGAINST ('es' WITH QUERY EXPANSION);。
SELECT * FROM table_ddl_0085 WHERE MATCH (username) AGAINST ('s');。

insert into table_ddl_0085 select * from table_ddl_0085 where match (username) against ('te' IN NATURAL LANGUAGE MODE);。
select * from table_ddl_0085;。

create fulltext index idx_ddl_0085_03 on table_ddl_0085(username) with parser ngram visible;。
create fulltext index idx_ddl_0085_04 on table_ddl_0085(username) visible with parser ngram;。
create fulltext index idx_ddl_0085_05 on table_ddl_0085(username) visible;。
create fulltext index idx_ddl_0085_06 on table_ddl_0085(username) with parser ngram comment 'TEST FULLTEXT INDEX COMMENT';。
create fulltext index idx_ddl_0085_07 on table_ddl_0085(username) comment 'TEST FULLTEXT INDEX COMMENT' with parser ngram;。
create fulltext index idx_ddl_0085_08 on table_ddl_0085(username) comment 'TEST FULLTEXT INDEX COMMENT';。
--NOTICE:  drop cascades to 4 other objects
--DETAIL:  drop cascades to table test
drop schema fulltext_test cascade;。
reset current_schema;。

--关键字作为别名
CREATE TABLE IF NOT EXISTS sales (
    sale_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    sale_date DATE NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    month INT NOT NULL  -- 关键字 month
);。
--正确示例
SELECT sale_id, amount AS "month" FROM sales;。
SELECT sale_id, amount AS month FROM sales;。
SELECT sale_id, amount "month" FROM sales;。

--错误示例
--ERROR:  syntax error at or near "month"
--SELECT sale_id, amount month FROM sales;。
--SELECT sale_id, amount month FROM sales;。

DROP TABLE IF EXISTS join_1 CASCADE;。
DROP TABLE IF EXISTS join_2 CASCADE;。
DROP TABLE IF EXISTS multi_partition_select_test CASCADE;。
DROP TABLE IF EXISTS sales CASCADE;。
DROP TABLE IF EXISTS t_with_rollup CASCADE;。
DROP TABLE IF EXISTS tbl_date CASCADE;。
DROP TABLE IF EXISTS tbl_json CASCADE;。
DROP TABLE IF EXISTS test CASCADE;。

DROP TABLESPACE t_tbspace;。
