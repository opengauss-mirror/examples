--CREATE TABLE SUBPARTITION

--创建各种组合类型的二级分区表
CREATE TABLE IF NOT EXISTS list_list
(
    month_code VARCHAR2 ( 30 ) NOT NULL ,
    dept_code  VARCHAR2 ( 30 ) NOT NULL ,
    user_no    VARCHAR2 ( 30 ) NOT NULL ,
    sales_amt  int
)
PARTITION BY LIST (month_code) SUBPARTITION BY LIST (dept_code)
(
  PARTITION p_201901 VALUES ( '201902' )
  (
    SUBPARTITION p_201901_a VALUES ( '1' ),
    SUBPARTITION p_201901_b VALUES ( '2' )
  ),
  PARTITION p_201902 VALUES ( '201903' )
  (
    SUBPARTITION p_201902_a VALUES ( '1' ),
    SUBPARTITION p_201902_b VALUES ( '2' )
  )
);。
insert into list_list values('201902', '1', '1', 1);。
insert into list_list values('201902', '2', '1', 1);。
insert into list_list values('201902', '1', '1', 1);。
insert into list_list values('201903', '2', '1', 1);。
insert into list_list values('201903', '1', '1', 1);。
insert into list_list values('201903', '2', '1', 1);。
select * from list_list;。

drop table IF EXISTS list_list CASCADE;。
CREATE TABLE IF NOT EXISTS list_hash
(
    month_code VARCHAR2 ( 30 ) NOT NULL ,
    dept_code  VARCHAR2 ( 30 ) NOT NULL ,
    user_no    VARCHAR2 ( 30 ) NOT NULL ,
    sales_amt  int
)
PARTITION BY LIST (month_code) SUBPARTITION BY HASH (dept_code)
(
  PARTITION p_201901 VALUES ( '201902' )
  (
    SUBPARTITION p_201901_a,
    SUBPARTITION p_201901_b
  ),
  PARTITION p_201902 VALUES ( '201903' )
  (
    SUBPARTITION p_201902_a,
    SUBPARTITION p_201902_b
  )
);。
insert into list_hash values('201902', '1', '1', 1);。
insert into list_hash values('201902', '2', '1', 1);。
insert into list_hash values('201902', '3', '1', 1);。
insert into list_hash values('201903', '4', '1', 1);。
insert into list_hash values('201903', '5', '1', 1);。
insert into list_hash values('201903', '6', '1', 1);。
select * from list_hash;。

drop table IF EXISTS list_hash CASCADE;。
CREATE TABLE IF NOT EXISTS list_range
(
    month_code VARCHAR2 ( 30 ) NOT NULL ,
    dept_code  VARCHAR2 ( 30 ) NOT NULL ,
    user_no    VARCHAR2 ( 30 ) NOT NULL ,
    sales_amt  int
)
PARTITION BY LIST (month_code) SUBPARTITION BY RANGE (dept_code)
(
  PARTITION p_201901 VALUES ( '201902' )
  (
    SUBPARTITION p_201901_a values less than ('4'),
    SUBPARTITION p_201901_b values less than ('6')
  ),
  PARTITION p_201902 VALUES ( '201903' )
  (
    SUBPARTITION p_201902_a values less than ('3'),
    SUBPARTITION p_201902_b values less than ('6')
  )
);。
insert into list_range values('201902', '1', '1', 1);。
insert into list_range values('201902', '2', '1', 1);。
insert into list_range values('201902', '3', '1', 1);。
insert into list_range values('201903', '4', '1', 1);。
insert into list_range values('201903', '5', '1', 1);。
--ERROR:  inserted partition key does not map to any table partition
--insert into list_range values('201903', '6', '1', 1);。
select * from list_range;。

drop table IF EXISTS list_range CASCADE;。
CREATE TABLE IF NOT EXISTS range_list
(
    month_code VARCHAR2 ( 30 ) NOT NULL ,
    dept_code  VARCHAR2 ( 30 ) NOT NULL ,
    user_no    VARCHAR2 ( 30 ) NOT NULL ,
    sales_amt  int
)
PARTITION BY RANGE (month_code) SUBPARTITION BY LIST (dept_code)
(
  PARTITION p_201901 VALUES LESS THAN( '201903' )
  (
    SUBPARTITION p_201901_a values ('1'),
    SUBPARTITION p_201901_b values ('2')
  ),
  PARTITION p_201902 VALUES LESS THAN( '201904' )
  (
    SUBPARTITION p_201902_a values ('1'),
    SUBPARTITION p_201902_b values ('2')
  )
);。
insert into range_list values('201902', '1', '1', 1);。
insert into range_list values('201902', '2', '1', 1);。
insert into range_list values('201902', '1', '1', 1);。
insert into range_list values('201903', '2', '1', 1);。
insert into range_list values('201903', '1', '1', 1);。
insert into range_list values('201903', '2', '1', 1);。
select * from range_list;。

drop table IF EXISTS range_list CASCADE;。
CREATE TABLE IF NOT EXISTS range_hash
(
    month_code VARCHAR2 ( 30 ) NOT NULL ,
    dept_code  VARCHAR2 ( 30 ) NOT NULL ,
    user_no    VARCHAR2 ( 30 ) NOT NULL ,
    sales_amt  int
)
PARTITION BY RANGE (month_code) SUBPARTITION BY HASH (dept_code)
(
  PARTITION p_201901 VALUES LESS THAN( '201903' )
  (
    SUBPARTITION p_201901_a,
    SUBPARTITION p_201901_b
  ),
  PARTITION p_201902 VALUES LESS THAN( '201904' )
  (
    SUBPARTITION p_201902_a,
    SUBPARTITION p_201902_b
  )
);。
insert into range_hash values('201902', '1', '1', 1);。
insert into range_hash values('201902', '2', '1', 1);。
insert into range_hash values('201902', '1', '1', 1);。
insert into range_hash values('201903', '2', '1', 1);。
insert into range_hash values('201903', '1', '1', 1);。
insert into range_hash values('201903', '2', '1', 1);。
select * from range_hash;。

drop table IF EXISTS range_hash CASCADE;。
CREATE TABLE IF NOT EXISTS range_range
(
    month_code VARCHAR2 ( 30 ) NOT NULL ,
    dept_code  VARCHAR2 ( 30 ) NOT NULL ,
    user_no    VARCHAR2 ( 30 ) NOT NULL ,
    sales_amt  int
)
PARTITION BY RANGE (month_code) SUBPARTITION BY RANGE (dept_code)
(
  PARTITION p_201901 VALUES LESS THAN( '201903' )
  (
    SUBPARTITION p_201901_a VALUES LESS THAN( '2' ),
    SUBPARTITION p_201901_b VALUES LESS THAN( '3' )
  ),
  PARTITION p_201902 VALUES LESS THAN( '201904' )
  (
    SUBPARTITION p_201902_a VALUES LESS THAN( '2' ),
    SUBPARTITION p_201902_b VALUES LESS THAN( '3' )
  )
);。
insert into range_range values('201902', '1', '1', 1);。
insert into range_range values('201902', '2', '1', 1);。
insert into range_range values('201902', '1', '1', 1);。
insert into range_range values('201903', '2', '1', 1);。
insert into range_range values('201903', '1', '1', 1);。
insert into range_range values('201903', '2', '1', 1);。
select * from range_range;。

drop table IF EXISTS range_range CASCADE;。
CREATE TABLE IF NOT EXISTS hash_list
(
    month_code VARCHAR2 ( 30 ) NOT NULL ,
    dept_code  VARCHAR2 ( 30 ) NOT NULL ,
    user_no    VARCHAR2 ( 30 ) NOT NULL ,
    sales_amt  int
)
PARTITION BY hash (month_code) SUBPARTITION BY LIST (dept_code)
(
  PARTITION p_201901
  (
    SUBPARTITION p_201901_a VALUES ( '1' ),
    SUBPARTITION p_201901_b VALUES ( '2' )
  ),
  PARTITION p_201902
  (
    SUBPARTITION p_201902_a VALUES ( '1' ),
    SUBPARTITION p_201902_b VALUES ( '2' )
  )
);。
insert into hash_list values('201901', '1', '1', 1);。
insert into hash_list values('201901', '2', '1', 1);。
insert into hash_list values('201901', '1', '1', 1);。
insert into hash_list values('201903', '2', '1', 1);。
insert into hash_list values('201903', '1', '1', 1);。
insert into hash_list values('201903', '2', '1', 1);。
select * from hash_list;。

drop table IF EXISTS hash_list CASCADE;。
CREATE TABLE IF NOT EXISTS hash_hash
(
    month_code VARCHAR2 ( 30 ) NOT NULL ,
    dept_code  VARCHAR2 ( 30 ) NOT NULL ,
    user_no    VARCHAR2 ( 30 ) NOT NULL ,
    sales_amt  int
)
PARTITION BY hash (month_code) SUBPARTITION BY hash (dept_code)
(
  PARTITION p_201901
  (
    SUBPARTITION p_201901_a,
    SUBPARTITION p_201901_b
  ),
  PARTITION p_201902
  (
    SUBPARTITION p_201902_a,
    SUBPARTITION p_201902_b
  )
);。
insert into hash_hash values('201901', '1', '1', 1);。
insert into hash_hash values('201901', '2', '1', 1);。
insert into hash_hash values('201901', '1', '1', 1);。
insert into hash_hash values('201903', '2', '1', 1);。
insert into hash_hash values('201903', '1', '1', 1);。
insert into hash_hash values('201903', '2', '1', 1);。
select * from hash_hash;。

drop table IF EXISTS hash_hash CASCADE;。
CREATE TABLE IF NOT EXISTS hash_range
(
    month_code VARCHAR2 ( 30 ) NOT NULL ,
    dept_code  VARCHAR2 ( 30 ) NOT NULL ,
    user_no    VARCHAR2 ( 30 ) NOT NULL ,
    sales_amt  int
)
PARTITION BY hash (month_code) SUBPARTITION BY range (dept_code)
(
  PARTITION p_201901
  (
    SUBPARTITION p_201901_a VALUES LESS THAN ( '2' ),
    SUBPARTITION p_201901_b VALUES LESS THAN ( '3' )
  ),
  PARTITION p_201902
  (
    SUBPARTITION p_201902_a VALUES LESS THAN ( '2' ),
    SUBPARTITION p_201902_b VALUES LESS THAN ( '3' )
  )
);。
insert into hash_range values('201901', '1', '1', 1);。
insert into hash_range values('201901', '2', '1', 1);。
insert into hash_range values('201901', '1', '1', 1);。
insert into hash_range values('201903', '2', '1', 1);。
insert into hash_range values('201903', '1', '1', 1);。
insert into hash_range values('201903', '2', '1', 1);。
select * from hash_range;。

drop table if exists hash_range CASCADE;。

--对二级分区表进行DML指定分区操作
CREATE TABLE IF NOT EXISTS range_list
(
    month_code VARCHAR2 ( 30 ) NOT NULL ,
    dept_code  VARCHAR2 ( 30 ) NOT NULL ,
    user_no    VARCHAR2 ( 30 ) NOT NULL ,
    sales_amt  int
)
PARTITION BY RANGE (month_code) SUBPARTITION BY LIST (dept_code)
(
  PARTITION p_201901 VALUES LESS THAN( '201903' )
  (
    SUBPARTITION p_201901_a values ('1'),
    SUBPARTITION p_201901_b values ('2')
  ),
  PARTITION p_201902 VALUES LESS THAN( '201910' )
  (
    SUBPARTITION p_201902_a values ('1'),
    SUBPARTITION p_201902_b values ('2')
  )
);。
--指定一级分区插入数据
insert into range_list partition (p_201901) values('201902', '1', '1', 1);。
--实际分区和指定分区不一致，报错
--ERROR:  inserted partition key does not map to the table partition
--DETAIL:  N/A.
--insert into range_list partition (p_201902) values('201902', '1', '1', 1);。
--指定二级分区插入数据
insert into range_list subpartition (p_201901_a) values('201902', '1', '1', 1);。
--实际分区和指定分区不一致，报错
--ERROR:  inserted subpartition key does not map to the table subpartition
--DETAIL:  N/A.
--insert into range_list subpartition (p_201901_b) values('201902', '1', '1', 1);。
insert into range_list partition for ('201902') values('201902', '1', '1', 1);。
insert into range_list subpartition for ('201902','1') values('201902', '1', '1', 1);。

--指定分区查询数据
select * from range_list partition (p_201901);。

select * from range_list subpartition (p_201901_a);。

select * from range_list partition for ('201902');。

select * from range_list subpartition for ('201902','1');。

--指定分区更新数据
update range_list partition (p_201901) set user_no = '2';。
select * from range_list;。
update range_list subpartition (p_201901_a) set user_no = '3';。
select * from range_list;。
update range_list partition for ('201902') set user_no = '4';。
select * from range_list;。
update range_list subpartition for ('201902','2') set user_no = '5';。
select * from range_list;。

--指定分区删除数据
delete from range_list partition (p_201901);。
delete from range_list partition for ('201903');。
delete from range_list subpartition (p_201901_a);。
delete from range_list subpartition for ('201903','2');。
--参数sql_compatibility=B时，可指定多分区删除数据
delete from range_list as t partition (p_201901_a, p_201901);。

--指定分区insert数据
insert into range_list partition (p_201901)  values('201902', '1', '1', 1)  ON DUPLICATE KEY UPDATE sales_amt = 5;。
insert into range_list subpartition (p_201901_a)  values('201902', '1', '1', 1)  ON DUPLICATE KEY UPDATE sales_amt = 10;。
insert into range_list partition for ('201902')  values('201902', '1', '1', 1)  ON DUPLICATE KEY UPDATE sales_amt = 30;。
insert into range_list subpartition for ('201902','1')  values('201902', '1', '1', 1)  ON DUPLICATE KEY UPDATE sales_amt = 40;。
select * from range_list;。

--指定分区merge into数据
CREATE TABLE IF NOT EXISTS newrange_list
(
    month_code VARCHAR2 ( 30 ) NOT NULL ,
    dept_code  VARCHAR2 ( 30 ) NOT NULL ,
    user_no    VARCHAR2 ( 30 ) NOT NULL ,
    sales_amt  int
)
PARTITION BY RANGE (month_code) SUBPARTITION BY LIST (dept_code)
(
  PARTITION p_201901 VALUES LESS THAN( '201903' )
  (
    SUBPARTITION p_201901_a values ('1'),
    SUBPARTITION p_201901_b values ('2')
  ),
  PARTITION p_201902 VALUES LESS THAN( '201910' )
  (
    SUBPARTITION p_201902_a values ('1'),
    SUBPARTITION p_201902_b values ('2')
  )
);。
insert into newrange_list values('201902', '1', '1', 1);。
insert into newrange_list values('201903', '1', '1', 2);。

MERGE INTO range_list partition (p_201901) p
USING newrange_list partition (p_201901) np
ON p.month_code= np.month_code
WHEN MATCHED THEN
  UPDATE SET dept_code = np.dept_code, user_no = np.user_no, sales_amt = np.sales_amt
WHEN NOT MATCHED THEN
  INSERT VALUES (np.month_code, np.dept_code, np.user_no, np.sales_amt);。

select * from range_list;。

MERGE INTO range_list partition for ('201901') p
USING newrange_list partition for ('201901') np
ON p.month_code= np.month_code
WHEN MATCHED THEN
  UPDATE SET dept_code = np.dept_code, user_no = np.user_no, sales_amt = np.sales_amt
WHEN NOT MATCHED THEN
  INSERT VALUES (np.month_code, np.dept_code, np.user_no, np.sales_amt);。

select * from range_list;。

MERGE INTO range_list subpartition (p_201901_a) p
USING newrange_list subpartition (p_201901_a) np
ON p.month_code= np.month_code
WHEN MATCHED THEN
  UPDATE SET dept_code = np.dept_code, user_no = np.user_no, sales_amt = np.sales_amt
WHEN NOT MATCHED THEN
  INSERT VALUES (np.month_code, np.dept_code, np.user_no, np.sales_amt);。

select * from range_list;。

MERGE INTO range_list subpartition for ('201901', '1') p
USING newrange_list subpartition for ('201901', '1') np
ON p.month_code= np.month_code
WHEN MATCHED THEN
  UPDATE SET dept_code = np.dept_code, user_no = np.user_no, sales_amt = np.sales_amt
WHEN NOT MATCHED THEN
  INSERT VALUES (np.month_code, np.dept_code, np.user_no, np.sales_amt);。

select * from range_list;。

drop table if exists newrange_list cascade;。
drop table if exists range_list cascade;。

-- 对二级分区表进行truncate操作
CREATE TABLE IF NOT EXISTS list_list
(
    month_code VARCHAR2 ( 30 ) NOT NULL ,
    dept_code  VARCHAR2 ( 30 ) NOT NULL ,
    user_no    VARCHAR2 ( 30 ) NOT NULL ,
    sales_amt  int
)
PARTITION BY LIST (month_code) SUBPARTITION BY LIST (dept_code)
(
  PARTITION p_201901 VALUES ( '201902' )
  (
    SUBPARTITION p_201901_a VALUES ( '1' ),
    SUBPARTITION p_201901_b VALUES ( default )
  ),
  PARTITION p_201902 VALUES ( '201903' )
  (
    SUBPARTITION p_201902_a VALUES ( '1' ),
    SUBPARTITION p_201902_b VALUES ( '2' )
  )
);。
insert into list_list values('201902', '1', '1', 1);。
insert into list_list values('201902', '2', '1', 1);。
insert into list_list values('201902', '1', '1', 1);。
insert into list_list values('201903', '2', '1', 1);。
insert into list_list values('201903', '1', '1', 1);。
insert into list_list values('201903', '2', '1', 1);。
select * from list_list;。

select * from list_list partition (p_201901);。

alter table list_list truncate partition p_201901;。
select * from list_list partition (p_201901);。

select * from list_list partition (p_201902);。

alter table list_list truncate partition p_201902;。
select * from list_list partition (p_201902);。

select * from list_list;。

insert into list_list values('201902', '1', '1', 1);。
insert into list_list values('201902', '2', '1', 1);。
insert into list_list values('201902', '1', '1', 1);。
insert into list_list values('201903', '2', '1', 1);。
insert into list_list values('201903', '1', '1', 1);。
insert into list_list values('201903', '2', '1', 1);。
select * from list_list subpartition (p_201901_a);。

alter table list_list truncate subpartition p_201901_a;。
select * from list_list subpartition (p_201901_a);。

select * from list_list subpartition (p_201901_b);。

alter table list_list truncate subpartition p_201901_b;。
select * from list_list subpartition (p_201901_b);。

select * from list_list subpartition (p_201902_a);。

alter table list_list truncate subpartition p_201902_a;。
select * from list_list subpartition (p_201902_a);。

select * from list_list subpartition (p_201902_b);。

alter table list_list truncate subpartition p_201902_b;。
select * from list_list subpartition (p_201902_b);。

select * from list_list;。

drop table IF EXISTS list_list CASCADE;。

--对二级分区表进行split操作
CREATE TABLE IF NOT EXISTS list_list
(
    month_code VARCHAR2 ( 30 ) NOT NULL ,
    dept_code  VARCHAR2 ( 30 ) NOT NULL ,
    user_no    VARCHAR2 ( 30 ) NOT NULL ,
    sales_amt  int
)
PARTITION BY LIST (month_code) SUBPARTITION BY LIST (dept_code)
(
  PARTITION p_201901 VALUES ( '201902' )
  (
    SUBPARTITION p_201901_a VALUES ( '1' ),
    SUBPARTITION p_201901_b VALUES ( default )
  ),
  PARTITION p_201902 VALUES ( '201903' )
  (
    SUBPARTITION p_201902_a VALUES ( '1' ),
    SUBPARTITION p_201902_b VALUES ( default )
  )
);。
insert into list_list values('201902', '1', '1', 1);。
insert into list_list values('201902', '2', '1', 1);。
insert into list_list values('201902', '1', '1', 1);。
insert into list_list values('201903', '2', '1', 1);。
insert into list_list values('201903', '1', '1', 1);。
insert into list_list values('201903', '2', '1', 1);。
select * from list_list;。

select * from list_list subpartition (p_201901_a);。

select * from list_list subpartition (p_201901_b);。

alter table list_list split subpartition p_201901_b values (2) into
(
 subpartition p_201901_b,
 subpartition p_201901_c
);。
select * from list_list subpartition (p_201901_a);。
select * from list_list subpartition (p_201901_b);。
select * from list_list subpartition (p_201901_c);。
select * from list_list partition (p_201901);。
select * from list_list subpartition (p_201902_a);。
select * from list_list subpartition (p_201902_b);。

alter table list_list split subpartition p_201902_b values (3) into
(
 subpartition p_201902_b,
 subpartition p_201902_c
);。
select * from list_list subpartition (p_201902_a);。
select * from list_list subpartition (p_201902_b);。
select * from list_list subpartition (p_201902_c);。

drop table IF EXISTS list_list CASCADE;。
