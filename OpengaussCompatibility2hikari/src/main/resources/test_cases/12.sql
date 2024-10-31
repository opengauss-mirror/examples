-- TRUNCATE

CREATE SCHEMA IF NOT EXISTS tpcds;。
-- 创建原因表 tpcds.reason
CREATE TABLE IF NOT EXISTS tpcds.reason (
  r_reason_sk integer PRIMARY KEY,
  r_reason_id character(16),
  r_reason_desc character(100)
);。

-- 向原因表插入示例数据
INSERT INTO tpcds.reason (r_reason_sk, r_reason_id, r_reason_desc) VALUES
(1, 'AAAAAAAABAAAAAAA', 'reason1'),
(2, 'AAAAAAAABAAAAAAA', 'reason2'),
(3, 'AAAAAAAACAAAAAAA', 'reason3'),
(4, 'AAAAAAAADAAAAAAA', 'reason4'),
(5, 'AAAAAAAAEAAAAAAA', 'reason5'),
(6, 'AAAAAAAAFAAAAAAA', 'reason6'),
(7, 'AAAAAAAAGAAAAAAA', 'reason7'),
(8, 'AAAAAAAAHAAAAAAA', 'reason8'),
(13, 'AAAAAAAAIAAAAAAA', 'reason9'),
(20, 'AAAAAAAAJAAAAAAA', 'reason10');。

-- 创建原因表的副本 tpcds.reason_t1
CREATE TABLE tpcds.reason_t1 AS TABLE tpcds.reason;。

-- 清空 tpcds.reason_t1 表中的所有数据
TRUNCATE TABLE tpcds.reason_t1;。

-- 查询 tpcds.reason_t1 表中的所有记录
SELECT * FROM tpcds.reason_t1;。

-- 查询原始原因表中的所有记录
SELECT * FROM tpcds.reason;。

-- 删除用户原因表的副本
DROP TABLE tpcds.reason_t1;。

-- 创建分区表 tpcds.reason_p
CREATE TABLE IF NOT EXISTS tpcds.reason_p (
  r_reason_sk integer,
  r_reason_id character(16),
  r_reason_desc character(100)
) PARTITION BY RANGE (r_reason_sk)
(
  partition p_05_before values less than (5),
  partition p_15 values less than (15),
  partition p_25 values less than (25),
  partition p_35 values less than (35),
  partition p_45_after values less than (MAXVALUE)
);。

-- 将原始原因表的数据插入到分区表中
INSERT INTO tpcds.reason_p SELECT * FROM tpcds.reason;。

-- 清空小于5的分区
ALTER TABLE tpcds.reason_p TRUNCATE PARTITION p_05_before;。

-- 清空指定的分区，可能是针对分区 p_15 的操作
ALTER TABLE tpcds.reason_p TRUNCATE PARTITION FOR (13);。

-- 清空整个分区表
TRUNCATE TABLE tpcds.reason_p;。

-- 删除分区表
DROP TABLE IF EXISTS tpcds.reason_p CASCADE;。

-- 删除用户架构及其所有内容
DROP SCHEMA IF EXISTS tpcds CASCADE;。
