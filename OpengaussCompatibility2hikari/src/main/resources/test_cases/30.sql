-- dolphin
-- CREATE TABLE PARTITION

--rebuild,remove,check,repair,optimize语法示例
--创建分区表test_part
CREATE TABLE IF NOT EXISTS test_part
(
    a int primary key not null default 5,
    b int,
    c int,
    d int
)
PARTITION BY RANGE(a)
(
    PARTITION p0 VALUES LESS THAN (100000),
    PARTITION p1 VALUES LESS THAN (200000),
    PARTITION p2 VALUES LESS THAN (300000)
);。
create unique index if not exists idx_c on test_part (c);。
create index idx_b on test_part using btree(b) local;。
alter table test_part add constraint uidx_d unique(d);。
alter table test_part add constraint uidx_c unique using index idx_c;。
--向分区表插入数据
insert into test_part (with RECURSIVE t_r(i,j,k,m) as(values(0,1,2,3) union all select i+1,j+2,k+3,m+4 from t_r where i < 250000) select * from t_r);。
--检查分区表系统信息
select relname from pg_partition where (parentid in (select oid from pg_class where relname = 'test_part')) and parttype = 'p' and oid != relfilenode order by relname;。
--通过索引从分区表select数据
explain select * from test_part where ((99990 < c and c < 100000) or (219990 < c and c < 220000));。
select * from test_part where ((99990 < c and c < 100000) or (219990 < c and c < 220000));。
select * from test_part where ((99990 < d and d < 100000) or (219990 < d and d < 220000));。
select * from test_part where ((99990 < b and b < 100000) or (219990 < b and b < 220000));。

--测试rebuild分区表语法
ALTER TABLE test_part REBUILD PARTITION p0, p1;。
--检查分区表系统信息和真实数据
select relname from pg_partition where (parentid in (select oid from pg_class where relname = 'test_part')) and parttype = 'p' and oid != relfilenode order by relname;。
explain select * from test_part where ((99990 < c and c < 100000) or (219990 < c and c < 220000));。
select * from test_part where ((99990 < c and c < 100000) or (219990 < c and c < 220000));。
select * from test_part where ((99990 < d and d < 100000) or (219990 < d and d < 220000));。
select * from test_part where ((99990 < b and b < 100000) or (219990 < b and b < 220000));。

--测试rebuild partition all分区表语法
ALTER TABLE test_part REBUILD PARTITION all;。
--检查分区表系统信息和真实数据
select relname from pg_partition where (parentid in (select oid from pg_class where relname = 'test_part')) and parttype = 'p' and oid != relfilenode order by relname;。
explain select * from test_part where ((99990 < c and c < 100000) or (219990 < c and c < 220000));。
select * from test_part where ((99990 < c and c < 100000) or (219990 < c and c < 220000));。
select * from test_part where ((99990 < d and d < 100000) or (219990 < d and d < 220000));。
select * from test_part where ((99990 < b and b < 100000) or (219990 < b and b < 220000));。

--测试 repair check optimize 分区表语法
ALTER TABLE test_part repair PARTITION p0,p1;。
ALTER TABLE test_part check PARTITION p0,p1;。
ALTER TABLE test_part optimize PARTITION p0,p1;。
ALTER TABLE test_part repair PARTITION all;。
ALTER TABLE test_part check PARTITION all;。
ALTER TABLE test_part optimize PARTITION all;。

--测试 remove partitioning 语法
select relname, boundaries from pg_partition where parentid in (select parentid from pg_partition where relname = 'test_part') order by relname;。
select parttype,relname from pg_class where relname = 'test_part' and relfilenode != oid;。
ALTER TABLE test_part remove PARTITIONING;。
--检查分区表移除分区信息后的系统信息和真实数据
explain select * from test_part where ((99990 < c and c < 100000) or (219990 < c and c < 220000));。
select * from test_part where ((99990 < c and c < 100000) or (219990 < c and c < 220000));。
select relname, boundaries from pg_partition where parentid in (select parentid from pg_partition where relname = 'test_part') order by relname;。
select parttype,relname from pg_class where relname = 'test_part' and relfilenode != oid;。

--truncate,analyze,exchange语法示例
CREATE TABLE IF NOT EXISTS test_part1
(
    a int,
    b int
)
PARTITION BY RANGE(a)
(
    PARTITION p0 VALUES LESS THAN (100),
    PARTITION p1 VALUES LESS THAN (200),
    PARTITION p2 VALUES LESS THAN (300)
);。
create table if not exists test_no_part1(a int, b int);。
insert into test_part1 values(99,1),(199,1),(299,1);。
select * from test_part1;。
--truncate partition语法
ALTER TABLE test_part1 truncate PARTITION p0, p1;。
select * from test_part1;。
insert into test_part1 (with RECURSIVE t_r(i,j) as(values(0,1) union all select i+1,j+2 from t_r where i < 20) select * from t_r);。
select * from test_part1;。
ALTER TABLE test_part1 truncate PARTITION all;。
select * from test_part1;。
--exchange partition语法
insert into test_part1 values(99,1),(199,1),(299,1);。
alter table test_part1 exchange partition p2 with table test_no_part1 without validation;。
select * from test_part1;。
select * from test_no_part1;。
alter table test_part1 exchange partition p2 with table test_no_part1 without validation;。
select * from test_part1;。
select * from test_no_part1;。
--analyze partition语法
alter table test_part1 analyze partition p0,p1;。
alter table test_part1 analyze partition all;。

--add, drop语法示例
CREATE TABLE IF NOT EXISTS test_part2
(
    a int,
    b int
)
PARTITION BY RANGE(a)
(
    PARTITION p0 VALUES LESS THAN (100),
    PARTITION p1 VALUES LESS THAN (200),
    PARTITION p2 VALUES LESS THAN (300),
    PARTITION p3 VALUES LESS THAN (400)
);。

CREATE TABLE IF NOT EXISTS test_subpart2
(
    a int,
    b int
)
PARTITION BY RANGE(a) SUBPARTITION BY RANGE(b)
(
    PARTITION p0 VALUES LESS THAN (100)
    (
        SUBPARTITION p0_0 VALUES LESS THAN (100),
        SUBPARTITION p0_1 VALUES LESS THAN (200),
        SUBPARTITION p0_2 VALUES LESS THAN (300)
    ),
    PARTITION p1 VALUES LESS THAN (200)
    (
        SUBPARTITION p1_0 VALUES LESS THAN (100),
        SUBPARTITION p1_1 VALUES LESS THAN (200),
        SUBPARTITION p1_2 VALUES LESS THAN (300)
    ),
    PARTITION p2 VALUES LESS THAN (300)
    (
        SUBPARTITION p2_0 VALUES LESS THAN (100),
        SUBPARTITION p2_1 VALUES LESS THAN (200),
        SUBPARTITION p2_2 VALUES LESS THAN (300)
    ),
    PARTITION p3 VALUES LESS THAN (400)
    (
        SUBPARTITION p3_0 VALUES LESS THAN (100),
        SUBPARTITION p3_1 VALUES LESS THAN (200),
        SUBPARTITION p3_2 VALUES LESS THAN (300)
    )
);。
--test b_compatibility drop and add partition syntax
select relname, boundaries from pg_partition where parentid in (select parentid from pg_partition where relname = 'test_part2');。
ALTER TABLE test_part2 DROP PARTITION p3;。
select relname, boundaries from pg_partition where parentid in (select parentid from pg_partition where relname = 'test_part2');。
ALTER TABLE test_part2 add PARTITION (PARTITION p3 VALUES LESS THAN (400),PARTITION p4 VALUES LESS THAN (500),PARTITION p5 VALUES LESS THAN (600));。
select relname, boundaries from pg_partition where parentid in (select parentid from pg_partition where relname = 'test_part2');。
ALTER TABLE test_part2 add PARTITION (PARTITION p6 VALUES LESS THAN (700),PARTITION p7 VALUES LESS THAN (800));。
ALTER TABLE test_part2 DROP PARTITION p4,p5,p6;。
select relname, boundaries from pg_partition where parentid in (select parentid from pg_partition where relname = 'test_part2');。
--ERROR:  upper boundary of adding partition MUST overtop last existing partition
--ALTER TABLE test_part2 add PARTITION (PARTITION p4 VALUES LESS THAN (500));。
select relname, boundaries from pg_partition where parentid in (select oid from pg_partition where parentid in (select parentid from pg_partition where relname = 'test_subpart2'));。
ALTER TABLE test_subpart2 DROP SUBPARTITION p0_0;。
ALTER TABLE test_subpart2 DROP SUBPARTITION p0_2, p1_0, p1_2;。
select relname, boundaries from pg_partition where parentid in (select oid from pg_partition where parentid in (select parentid from pg_partition where relname = 'test_subpart2'));。

--reorganize分区语法示例
CREATE TABLE IF NOT EXISTS test_range_subpart
(
    a INT4 PRIMARY KEY,
    b INT4
)
PARTITION BY RANGE (a) SUBPARTITION BY HASH (b)
(
    PARTITION p1 VALUES LESS THAN (200)
    (
        SUBPARTITION s11,
        SUBPARTITION s12,
        SUBPARTITION s13,
        SUBPARTITION s14
    ),
    PARTITION p2 VALUES LESS THAN (500)
    (
        SUBPARTITION s21,
        SUBPARTITION s22
    ),
    PARTITION p3 VALUES LESS THAN (800),
    PARTITION p4 VALUES LESS THAN (1200)
    (
        SUBPARTITION s41
    )
);。
insert into test_range_subpart values(199,1),(499,1),(799,1),(1199,1);。
--test test_range_subpart
alter table test_range_subpart reorganize partition p1,p2 into (partition m1 values less than(100),partition m2 values less than(500)(subpartition m21,subpartition m22));。
select pg_get_tabledef('test_range_subpart');。
select * from test_range_subpart subpartition(m22);。
select * from test_range_subpart subpartition(m21);。
select * from test_range_subpart partition(m1);。
explain select /*+ indexscan(test_range_subpart test_range_subpart_pkey) */ * from test_range_subpart where a > 0;。
select * from test_range_subpart;。

-- 分区表建索引，在create table 中index默认为local,不支持指定global/local
CREATE TABLE IF NOT EXISTS test_partition_btree
(
    f1  INTEGER,
    f2  INTEGER,
    f3  INTEGER,
    key part_btree_idx using btree(f1)
)
PARTITION BY RANGE(f1)
(
        PARTITION P1 VALUES LESS THAN(2450815),
        PARTITION P2 VALUES LESS THAN(2451179),
        PARTITION P3 VALUES LESS THAN(2451544),
        PARTITION P4 VALUES LESS THAN(MAXVALUE)
);。

-- 分区表建组合索引
CREATE TABLE IF NOT EXISTS test_partition_index
(
    f1  INTEGER,
    f2  INTEGER,
    f3  INTEGER,
    key part_btree_idx2 using btree(f1 desc, f2 asc)
)
PARTITION BY RANGE(f1)
(
        PARTITION P1 VALUES LESS THAN(2450815),
        PARTITION P2 VALUES LESS THAN(2451179),
        PARTITION P3 VALUES LESS THAN(2451544),
        PARTITION P4 VALUES LESS THAN(MAXVALUE)
);。

-- 分区表列存创建索引
CREATE TABLE IF NOT EXISTS test_partition_column
(
    f1  INTEGER,
    f2  INTEGER,
    f3  INTEGER,
    key part_column(f1)
) with (ORIENTATION = COLUMN)
PARTITION BY RANGE(f1)
(
        PARTITION P1 VALUES LESS THAN(2450815),
        PARTITION P2 VALUES LESS THAN(2451179),
        PARTITION P3 VALUES LESS THAN(2451544),
        PARTITION P4 VALUES LESS THAN(MAXVALUE)
);。

-- 分区表创建表达式索引
CREATE TABLE IF NOT EXISTS test_partition_expr
(
    f1  INTEGER,
    f2  INTEGER,
    f3  INTEGER,
    key part_expr_idx using btree((abs(f1)+1))
)
PARTITION BY RANGE(f1)
(
        PARTITION P1 VALUES LESS THAN(2450815),
        PARTITION P2 VALUES LESS THAN(2451179),
        PARTITION P3 VALUES LESS THAN(2451544),
        PARTITION P4 VALUES LESS THAN(MAXVALUE)
);。

--创建分区键为表达式分区的分区表。
create table if not exists testrangepart(a int, b int) partition by range(abs(a*2))
(
    partition p0 values less than(100),
    partition p1 values less than(200)
);。
select partkeyexpr from pg_partition where (parttype = 'r') and (parentid in (select oid from pg_class where relname = 'testrangepart'));。

insert into testrangepart values(-51,1),(49,2);。
--ERROR:  inserted partition key does not map to any table partition
--insert into testrangepart values(-101,1);。

select * from testrangepart partition(p0);。
select * from testrangepart partition(p1);。
select * from testrangepart where a = -51;。

DROP TABLE IF EXISTS test_part CASCADE;。
DROP TABLE IF EXISTS test_part1 CASCADE;。
DROP TABLE IF EXISTS test_no_part1 CASCADE;。
DROP TABLE IF EXISTS test_part2 CASCADE;。
DROP TABLE IF EXISTS test_subpart2 CASCADE;。
DROP TABLE IF EXISTS test_range_subpart CASCADE;。
DROP TABLE IF EXISTS test_partition_btree CASCADE;。
DROP TABLE IF EXISTS test_partition_index CASCADE;。
DROP TABLE IF EXISTS test_partition_column CASCADE;。
DROP TABLE IF EXISTS test_partition_expr CASCADE;。
DROP TABLE IF EXISTS testrangepart CASCADE;。