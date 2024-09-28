-- CREATE/ALTER/DROP INDEX

CREATE SCHEMA IF NOT EXISTS tpcds;。

-- 创建表 tpcds.ship_mode_t1，包含运输方式的详细信息
CREATE TABLE IF NOT EXISTS tpcds.ship_mode_t1 (
    SM_SHIP_MODE_SK           INTEGER               NOT NULL,  -- 运输方式的唯一标识符
    SM_SHIP_MODE_ID           CHAR(16)              NOT NULL,  -- 运输方式的ID
    SM_TYPE                   CHAR(30),                      -- 运输方式类型
    SM_CODE                   CHAR(10),                      -- 运输方式代码
    SM_CARRIER                CHAR(20),                      -- 承运人
    SM_CONTRACT               CHAR(20)                       -- 合同信息
);。

-- 创建唯一索引，确保 SM_SHIP_MODE_SK 的唯一性
CREATE UNIQUE INDEX IF NOT EXISTS ds_ship_mode_t1_index1 ON tpcds.ship_mode_t1(SM_SHIP_MODE_SK);。

-- 创建普通索引，用于加速基于 SM_SHIP_MODE_SK 的查询
CREATE INDEX IF NOT EXISTS ds_ship_mode_t1_index4 ON tpcds.ship_mode_t1 USING btree(SM_SHIP_MODE_SK);。

-- 创建基于 SM_CODE 的前4个字符的索引
CREATE INDEX IF NOT EXISTS ds_ship_mode_t1_index2 ON tpcds.ship_mode_t1(SUBSTR(SM_CODE, 1, 4));。

-- 创建条件唯一索引，针对 SM_SHIP_MODE_SK 大于 10 的记录
CREATE UNIQUE INDEX IF NOT EXISTS ds_ship_mode_t1_index3 ON tpcds.ship_mode_t1(SM_SHIP_MODE_SK) WHERE SM_SHIP_MODE_SK > 10;。

-- 重命名索引 ds_ship_mode_t1_index1 为 ds_ship_mode_t1_index5
ALTER INDEX tpcds.ds_ship_mode_t1_index1 RENAME TO ds_ship_mode_t1_index5;。

-- 将索引 ds_ship_mode_t1_index2 标记为不可用
ALTER INDEX tpcds.ds_ship_mode_t1_index2 UNUSABLE;。

-- 重建不可用的索引
ALTER INDEX tpcds.ds_ship_mode_t1_index2 REBUILD;。

-- 删除索引 ds_ship_mode_t1_index2
DROP INDEX IF EXISTS tpcds.ds_ship_mode_t1_index2;。

-- 删除表 tpcds.ship_mode_t1
DROP TABLE IF EXISTS tpcds.ship_mode_t1 CASCADE;。

-- 创建表空间 example1，指定相对位置
CREATE TABLESPACE example1 RELATIVE LOCATION 'tablespace1/tablespace_1';。

-- 创建其他表空间
CREATE TABLESPACE example2 RELATIVE LOCATION 'tablespace2/tablespace_2';。
CREATE TABLESPACE example3 RELATIVE LOCATION 'tablespace3/tablespace_3';。
CREATE TABLESPACE example4 RELATIVE LOCATION 'tablespace4/tablespace_4';。

-- 创建表 tpcds.customer_address_p1，存储客户地址信息
CREATE TABLE IF NOT EXISTS tpcds.customer_address_p1 (
    CA_ADDRESS_SK             INTEGER               NOT NULL,  -- 地址的唯一标识符
    CA_ADDRESS_ID             CHAR(16)              NOT NULL,  -- 地址ID
    CA_STREET_NUMBER          CHAR(10),                      -- 街道号
    CA_STREET_NAME            VARCHAR(60),                   -- 街道名称
    CA_STREET_TYPE            CHAR(15),                      -- 街道类型
    CA_SUITE_NUMBER           CHAR(10),                      -- 套房号
    CA_CITY                   VARCHAR(60),                   -- 城市
    CA_COUNTY                 VARCHAR(30),                   -- 县
    CA_STATE                  CHAR(2),                       -- 州
    CA_ZIP                    CHAR(10),                      -- 邮政编码
    CA_COUNTRY                VARCHAR(20),                   -- 国家
    CA_GMT_OFFSET             DECIMAL(5,2),                  -- GMT时区偏移
    CA_LOCATION_TYPE          CHAR(20)                       -- 地址类型
)
TABLESPACE example1  -- 指定表空间为 example1
PARTITION BY RANGE(CA_ADDRESS_SK)  -- 按 CA_ADDRESS_SK 列进行范围分区
(
   PARTITION p1 VALUES LESS THAN (3000),  -- 第一个分区
   PARTITION p2 VALUES LESS THAN (5000) TABLESPACE example1,  -- 第二个分区
   PARTITION p3 VALUES LESS THAN (MAXVALUE) TABLESPACE example2  -- 第三个分区
)
ENABLE ROW MOVEMENT;。  -- 启用行移动

-- 创建局部索引，基于 CA_ADDRESS_SK 列
CREATE INDEX ds_customer_address_p1_index1 ON tpcds.customer_address_p1(CA_ADDRESS_SK) LOCAL;。

-- 创建局部索引，指定不同的分区和表空间
CREATE INDEX ds_customer_address_p1_index2 ON tpcds.customer_address_p1(CA_ADDRESS_SK) LOCAL
(
    PARTITION CA_ADDRESS_SK_index1,
    PARTITION CA_ADDRESS_SK_index2 TABLESPACE example3,
    PARTITION CA_ADDRESS_SK_index3 TABLESPACE example4
)
TABLESPACE example2;。

-- 创建全局索引，基于 CA_ADDRESS_ID 列
CREATE INDEX IF NOT EXISTS ds_customer_address_p1_index3 ON tpcds.customer_address_p1(CA_ADDRESS_ID) GLOBAL;。

-- 创建局部索引，基于 CA_ADDRESS_ID 列
CREATE INDEX IF NOT EXISTS ds_customer_address_p1_index4 ON tpcds.customer_address_p1(CA_ADDRESS_ID);。

-- 移动索引的分区到不同的表空间
ALTER INDEX tpcds.ds_customer_address_p1_index2 MOVE PARTITION CA_ADDRESS_SK_index2 TABLESPACE example1;。
ALTER INDEX tpcds.ds_customer_address_p1_index2 MOVE PARTITION CA_ADDRESS_SK_index3 TABLESPACE example2;。

-- 重命名分区
ALTER INDEX tpcds.ds_customer_address_p1_index2 RENAME PARTITION CA_ADDRESS_SK_index1 TO CA_ADDRESS_SK_index4;。

DROP TABLE IF EXISTS tpcds.customer_address_p1 CASCADE;。

-- 删除创建的表空间
DROP TABLESPACE example1;。
DROP TABLESPACE example2;。
DROP TABLESPACE example3;。
DROP TABLESPACE example4;。

-- 创建列式存储表 cgin_create_test
CREATE TABLE IF NOT EXISTS cgin_create_test (a INT, b TEXT) WITH (orientation = column);。

-- 创建 GIN 索引，基于 b 列的文本搜索向量
CREATE INDEX IF NOT EXISTS cgin_test ON cgin_create_test USING gin(to_tsvector('ngram', b));。

-- 删除表 cgin_create_test 及其相关联的对象
DROP TABLE IF EXISTS cgin_create_test CASCADE;。

DROP SCHEMA IF EXISTS tpcds CASCADE;。
