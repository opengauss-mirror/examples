--SELECT

CREATE SCHEMA IF NOT EXISTS tpcds;。
--先通过子查询得到一张临时表temp_t，然后查询表temp_t中的所有数据。
WITH temp_t(name,isdba) AS (SELECT usename,usesuper FROM pg_user) SELECT * FROM temp_t;。

--创建多级菜单表
CREATE TABLE IF NOT EXISTS exp_menu (
  id int,
  name text,
  parent_id int
);。

--插入数据
INSERT INTO exp_menu VALUES (1, 'grandpa', 0),(2, 'father', 1),(3, 'son', 2);。

--使用 WITH RECURSIVE 递归查询菜单关系
WITH RECURSIVE res AS (
    SELECT id, name, parent_id
    FROM exp_menu 
    WHERE id = 3
    UNION
    SELECT m.id,
           m.name || ' > ' || r.name,
           m.parent_id
    FROM res r INNER JOIN exp_menu m ON m.id = r.parent_id
)
select * from res;。

--查询tpcds.reason表的所有r_reason_sk记录，且去除重复。
SELECT DISTINCT(r_reason_sk) FROM reason;。

--LIMIT子句示例：获取表中一条记录。
SELECT * FROM reason LIMIT 1;。

--查询所有记录，且按字母升序排列。
SELECT r_reason_desc FROM reason ORDER BY r_reason_desc;。

--通过表别名，从pg_user和pg_user_status这两张表中获取数据。
SELECT a.usename,b.locktime FROM pg_user a,pg_user_status b WHERE a.usesysid=b.roloid;。

--FULL JOIN子句示例：将pg_user和pg_user_status这两张表的数据进行全连接显示，即数据的合集。
SELECT a.usename,b.locktime,a.usesuper FROM pg_user a FULL JOIN pg_user_status b on a.usesysid=b.roloid;。

--GROUP BY子句示例：根据查询条件过滤，并对结果进行分组。
SELECT r_reason_id, AVG(r_reason_sk) FROM reason GROUP BY r_reason_id HAVING AVG(r_reason_sk) > 25;。

--GROUP BY CUBE子句示例：根据查询条件过滤，并对结果进行分组汇总。
SELECT r_reason_id,AVG(r_reason_sk) FROM reason GROUP BY CUBE(r_reason_id,r_reason_sk);。

--GROUP BY GROUPING SETS子句示例:根据查询条件过滤，并对结果进行分组汇总。
SELECT r_reason_id,AVG(r_reason_sk) FROM reason GROUP BY GROUPING SETS((r_reason_id,r_reason_sk),r_reason_sk);。

--UNION子句示例：将表reason里r_reason_desc字段中的内容以W开头和以N开头的进行合并。
SELECT r_reason_sk, reason.r_reason_desc
    FROM reason
    WHERE reason.r_reason_desc LIKE 'W%'
UNION
SELECT r_reason_sk, reason.r_reason_desc
    FROM reason
    WHERE reason.r_reason_desc LIKE 'N%';。

--NLS_SORT子句示例：中文拼音排序。
SELECT * FROM reason ORDER BY NLSSORT( r_reason_desc, 'NLS_SORT = SCHINESE_PINYIN_M');。

--不区分大小写排序（可选，仅支持纯英文不区分大小写排序）:
SELECT * FROM reason ORDER BY NLSSORT( r_reason_desc, 'NLS_SORT = generic_m_ci');。

--创建分区表tpcds.reason_p
CREATE TABLE IF NOT EXISTS tpcds.reason_p
(
  r_reason_sk integer,
  r_reason_id character(16),
  r_reason_desc character(100)
)
PARTITION BY RANGE (r_reason_sk)
(
  partition P_05_BEFORE values less than (05),
  partition P_15 values less than (15),
  partition P_25 values less than (25),
  partition P_35 values less than (35),
  partition P_45_AFTER values less than (MAXVALUE)
)
;。

--插入数据。
INSERT INTO tpcds.reason_p values(3,'AAAAAAAABAAAAAAA','reason 1'),(10,'AAAAAAAABAAAAAAA','reason 2'),(4,'AAAAAAAABAAAAAAA','reason 3'),(10,'AAAAAAAABAAAAAAA','reason 4'),(10,'AAAAAAAABAAAAAAA','reason 5'),(20,'AAAAAAAACAAAAAAA','reason 6'),(30,'AAAAAAAACAAAAAAA','reason 7');。

--PARTITION子句示例：从tpcds.reason_p的表分区P_05_BEFORE中获取数据。
SELECT * FROM tpcds.reason_p PARTITION (P_05_BEFORE);。

--GROUP BY子句示例：按r_reason_id分组统计tpcds.reason_p表中的记录数。
SELECT COUNT(*),r_reason_id FROM tpcds.reason_p GROUP BY r_reason_id;。

--GROUP BY CUBE子句示例：根据查询条件过滤，并对查询结果分组汇总。
SELECT * FROM reason GROUP BY  CUBE (r_reason_id,r_reason_sk,r_reason_desc);。

--GROUP BY GROUPING SETS子句示例：根据查询条件过滤，并对查询结果分组汇总。
SELECT * FROM reason GROUP BY  GROUPING SETS ((r_reason_id,r_reason_sk),r_reason_desc);。

--HAVING子句示例：按r_reason_id分组统计tpcds.reason_p表中的记录，并只显示r_reason_id个数大于2的信息。
SELECT COUNT(*) c,r_reason_id FROM tpcds.reason_p GROUP BY r_reason_id HAVING c>2;。

--IN子句示例：按r_reason_id分组统计tpcds.reason_p表中的r_reason_id个数，并只显示r_reason_id值为 AAAAAAAABAAAAAAA或AAAAAAAADAAAAAAA的个数。
SELECT COUNT(*),r_reason_id FROM tpcds.reason_p GROUP BY r_reason_id HAVING r_reason_id IN('AAAAAAAABAAAAAAA','AAAAAAAADAAAAAAA'); 。

--INTERSECT子句示例：查询r_reason_id等于AAAAAAAABAAAAAAA，并且r_reason_sk小于5的信息。
SELECT * FROM tpcds.reason_p WHERE r_reason_id='AAAAAAAABAAAAAAA' INTERSECT SELECT * FROM tpcds.reason_p WHERE r_reason_sk<5;。
--EXCEPT子句示例：查询r_reason_id等于AAAAAAAABAAAAAAA，并且去除r_reason_sk小于4的信息。
SELECT * FROM tpcds.reason_p WHERE r_reason_id='AAAAAAAABAAAAAAA' EXCEPT SELECT * FROM tpcds.reason_p WHERE r_reason_sk<4;。

--通过在where子句中指定"(+)"来实现左连接。
select t1.sr_item_sk ,t2.c_customer_id from store_returns t1, customer t2 where t1.sr_customer_sk  = t2.c_customer_sk(+) 
order by 1 desc limit 1;。

--通过在where子句中指定"(+)"来实现右连接。
select t1.sr_item_sk ,t2.c_customer_id from store_returns t1, customer t2 where t1.sr_customer_sk(+)  = t2.c_customer_sk 
order by 1 desc limit 1;。

--通过在where子句中指定"(+)"来实现左连接，并且增加连接条件。
select t1.sr_item_sk ,t2.c_customer_id from store_returns t1, customer t2 where t1.sr_customer_sk  = t2.c_customer_sk(+) and t2.c_customer_sk(+) < 1 order by 1  limit 1;。

--不支持在where子句中指定"(+)"的同时使用内层嵌套AND/OR的表达式。
--ERROR:  Operator "(+)" can not be used in nesting expression.
--LINE 1: ...tomer_id from store_returns t1, customer t2 where not(t1.sr_...
--                                                             ^
--select t1.sr_item_sk ,t2.c_customer_id from store_returns t1, customer t2 where not(t1.sr_customer_sk  = t2.c_customer_sk(+) and t2.c_customer_sk(+) < 1);。

--where子句在不支持表达式宏指定"(+)"会报错。
--ERROR:  Operator "(+)" can only be used in common expression.
--select t1.sr_item_sk ,t2.c_customer_id from store_returns t1, customer t2 where (t1.sr_customer_sk  = t2.c_customer_sk(+))::bool;。

--where子句在表达式的两边都指定"(+)"会报错。
--ERROR:  Operator "(+)" can't be specified on more than one relation in one join condition
--HINT:  "t1", "t2"...are specified Operator "(+)" in one condition.
--select t1.sr_item_sk ,t2.c_customer_id from store_returns t1, customer t2 where t1.sr_customer_sk(+)  = t2.c_customer_sk(+);。

--删除表。
DROP TABLE IF EXISTS tpcds.reason_p CASCADE;。

--闪回查询示例
--创建表tpcds.time_table
create table if not exists tpcds.time_table(idx integer, snaptime timestamp, snapcsn bigint, timeDesc character(100));。
--向表tpcds.time_table中插入记录
INSERT INTO tpcds.time_table select 1, now(),int8in(xidout(next_csn)), 'time1' from gs_get_next_xid_csn();。
INSERT INTO tpcds.time_table select 2, now(),int8in(xidout(next_csn)), 'time2' from gs_get_next_xid_csn();。
INSERT INTO tpcds.time_table select 3, now(),int8in(xidout(next_csn)), 'time3' from gs_get_next_xid_csn();。
INSERT INTO tpcds.time_table select 4, now(),int8in(xidout(next_csn)), 'time4' from gs_get_next_xid_csn();。
select * from tpcds.time_table;。

delete tpcds.time_table;。
drop table if exists exp_menu cascade;。

DROP SCHEMA IF EXISTS tpcds CASCADE;。