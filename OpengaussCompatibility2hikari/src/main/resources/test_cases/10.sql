-- INSERT

CREATE SCHEMA IF NOT EXISTS tpcds;。
-- 创建原因表 tpcds.reason
CREATE TABLE IF NOT EXISTS tpcds.reason (
  r_reason_sk    INTEGER PRIMARY KEY,
  r_reason_id    CHARACTER(16),
  r_reason_desc  CHARACTER(100)
);。

-- 向原因表批量插入，确保 r_reason_sk 唯一
INSERT INTO tpcds.reason (r_reason_sk, r_reason_id, r_reason_desc)
SELECT
    generate_series(1, 1000000) AS r_reason_sk,
    'Reason' || generate_series(1, 1000000) AS r_reason_id,
    'Description for reason ' || generate_series(1, 1000000) AS r_reason_desc;。

-- 创建原因表 tpcds.reason_t2
CREATE TABLE IF NOT EXISTS tpcds.reason_t2 (
  r_reason_sk    INTEGER,
  r_reason_id    CHARACTER(16),
  r_reason_desc  CHARACTER(100)
);。

-- 向原因表插入单条记录
INSERT INTO tpcds.reason_t2 (r_reason_sk, r_reason_id, r_reason_desc) VALUES
(1, 'AAAAAAAABAAAAAAA', 'reason1');。

-- 向原因表插入另一条记录
INSERT INTO tpcds.reason_t2 VALUES
(2, 'AAAAAAAABAAAAAAA', 'reason2');。

-- 向原因表批量插入多条记录，确保没有重复的 r_reason_sk
INSERT INTO tpcds.reason_t2 VALUES
(3, 'AAAAAAAACAAAAAAA', 'reason3'),
(4, 'AAAAAAAADAAAAAAA', 'reason4'),
(5, 'AAAAAAAAEAAAAAAA', 'reason5');。

-- 插入数据到 tpcds.reason_t2，避免冲突
INSERT INTO tpcds.reason_t2 (r_reason_sk, r_reason_id, r_reason_desc)
SELECT 1, 'AAAAAAAABAAAAAAA', 'reason1'
WHERE NOT EXISTS (SELECT 1 FROM tpcds.reason_t2 WHERE r_reason_sk = 1);。

INSERT INTO tpcds.reason_t2 (r_reason_sk, r_reason_id, r_reason_desc)
SELECT 2, 'AAAAAAAABAAAAAAA', 'reason2'
WHERE NOT EXISTS (SELECT 1 FROM tpcds.reason_t2 WHERE r_reason_sk = 2);。

INSERT INTO tpcds.reason_t2 (r_reason_sk, r_reason_id, r_reason_desc)
SELECT 3, 'AAAAAAAACAAAAAAA', 'reason3'
WHERE NOT EXISTS (SELECT 1 FROM tpcds.reason_t2 WHERE r_reason_sk = 3);。

INSERT INTO tpcds.reason_t2 (r_reason_sk, r_reason_id, r_reason_desc)
SELECT 4, 'AAAAAAAADAAAAAAA', 'reason4'
WHERE NOT EXISTS (SELECT 1 FROM tpcds.reason_t2 WHERE r_reason_sk = 4);。

INSERT INTO tpcds.reason_t2 (r_reason_sk, r_reason_id, r_reason_desc)
SELECT 5, 'AAAAAAAAEAAAAAAA', 'reason5'
WHERE NOT EXISTS (SELECT 1 FROM tpcds.reason_t2 WHERE r_reason_sk = 5);。

-- 从 tpcds.reason 中选择数据插入 tpcds.reason_t2，避免冲突
INSERT INTO tpcds.reason_t2 (r_reason_sk, r_reason_id, r_reason_desc)
SELECT r_reason_sk, r_reason_id, r_reason_desc
FROM tpcds.reason
WHERE r_reason_sk < 5
AND NOT EXISTS (SELECT 1 FROM tpcds.reason_t2 WHERE tpcds.reason_t2.r_reason_sk = tpcds.reason.r_reason_sk);。

-- 创建唯一索引
CREATE UNIQUE INDEX IF NOT EXISTS reason_t2_u_index ON tpcds.reason_t2(r_reason_sk);。

-- 插入新数据，避免冲突
INSERT INTO tpcds.reason_t2 (r_reason_sk, r_reason_id, r_reason_desc)
SELECT 5, 'BBBBBBBBCAAAAAAA', 'reason5'
WHERE NOT EXISTS (SELECT 1 FROM tpcds.reason_t2 WHERE r_reason_sk = 5);。

INSERT INTO tpcds.reason_t2 (r_reason_sk, r_reason_id, r_reason_desc)
SELECT 6, 'AAAAAAAADAAAAAAA', 'reason6'
WHERE NOT EXISTS (SELECT 1 FROM tpcds.reason_t2 WHERE r_reason_sk = 6);。

SELECT * FROM tpcds.reason_t2;。

-- 从 tpcds.reason 表中插入数据到 reason_t2 表
INSERT INTO tpcds.reason_t2
SELECT * FROM tpcds.reason WHERE r_reason_sk > 7;。

-- 创建唯一索引，以确保 r_reason_sk 列的唯一性
CREATE UNIQUE INDEX IF NOT EXISTS reason_t2_u_index ON tpcds.reason_t2 (r_reason_sk);。

-- 向原因表插入新数据，避免主键冲突
INSERT INTO tpcds.reason_t2 VALUES
(1000011, 'BBBBBBBBCAAAAAAA', 'reason11'),
(1000012, 'AAAAAAAAFAAAAAAA', 'reason12');。

-- 删除用户架构及其所有内容
DROP SCHEMA IF EXISTS tpcds CASCADE;。
