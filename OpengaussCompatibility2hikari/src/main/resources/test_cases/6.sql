--CREATE TABLE
--CREATE TABLESPACE

CREATE SCHEMA IF NOT EXISTS tpcds;。
--创建简单的表。
CREATE TABLE IF NOT EXISTS tpcds.warehouse_t1
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
    W_GMT_OFFSET              DECIMAL(5,2)
);。

CREATE TABLE IF NOT EXISTS tpcds.warehouse_t2
(
    W_WAREHOUSE_SK            INTEGER               NOT NULL,
    W_WAREHOUSE_ID            CHAR(16)              NOT NULL,
    W_WAREHOUSE_NAME          VARCHAR(20)                   ,
    W_WAREHOUSE_SQ_FT         INTEGER                       ,
    W_STREET_NUMBER           CHAR(10)                      ,
    W_STREET_NAME             VARCHAR(60),
    W_STREET_TYPE             CHAR(15)                      ,
    W_SUITE_NUMBER            CHAR(10)                      ,
    W_CITY                    VARCHAR(60)                   ,
    W_COUNTY                  VARCHAR(30)                   ,
    W_STATE                   CHAR(2)                       ,
    W_ZIP                     CHAR(10)                      ,
    W_COUNTRY                 VARCHAR(20)                   ,
    W_GMT_OFFSET              DECIMAL(5,2)
);。

--创建表，并指定W_STATE字段的缺省值为GA。
CREATE TABLE IF NOT EXISTS tpcds.warehouse_t3
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
    W_STATE                   CHAR(2)           DEFAULT 'GA',
    W_ZIP                     CHAR(10)                      ,
    W_COUNTRY                 VARCHAR(20)                   ,
    W_GMT_OFFSET              DECIMAL(5,2)
);。

--创建表，并在事务结束时检查W_WAREHOUSE_NAME字段是否有重复。
CREATE TABLE IF NOT EXISTS tpcds.warehouse_t4
(
    W_WAREHOUSE_SK            INTEGER                NOT NULL,
    W_WAREHOUSE_ID            CHAR(16)               NOT NULL,
    W_WAREHOUSE_NAME          VARCHAR(20)   UNIQUE DEFERRABLE,
    W_WAREHOUSE_SQ_FT         INTEGER                        ,
    W_STREET_NUMBER           CHAR(10)                       ,
    W_STREET_NAME             VARCHAR(60)                    ,
    W_STREET_TYPE             CHAR(15)                       ,
    W_SUITE_NUMBER            CHAR(10)                       ,
    W_CITY                    VARCHAR(60)                    ,
    W_COUNTY                  VARCHAR(30)                    ,
    W_STATE                   CHAR(2)                        ,
    W_ZIP                     CHAR(10)                       ,
    W_COUNTRY                 VARCHAR(20)                    ,
    W_GMT_OFFSET              DECIMAL(5,2)
);。

--创建一个带有70%填充因子的表。
CREATE TABLE IF NOT EXISTS tpcds.warehouse_t5
(
    W_WAREHOUSE_SK            INTEGER                NOT NULL,
    W_WAREHOUSE_ID            CHAR(16)               NOT NULL,
    W_WAREHOUSE_NAME          VARCHAR(20)                    ,
    W_WAREHOUSE_SQ_FT         INTEGER                        ,
    W_STREET_NUMBER           CHAR(10)                       ,
    W_STREET_NAME             VARCHAR(60)                    ,
    W_STREET_TYPE             CHAR(15)                       ,
    W_SUITE_NUMBER            CHAR(10)                       ,
    W_CITY                    VARCHAR(60)                    ,
    W_COUNTY                  VARCHAR(30)                    ,
    W_STATE                   CHAR(2)                        ,
    W_ZIP                     CHAR(10)                       ,
    W_COUNTRY                 VARCHAR(20)                    ,
    W_GMT_OFFSET              DECIMAL(5,2),
    UNIQUE(W_WAREHOUSE_NAME) WITH(fillfactor=70)
);。

--或者用下面的语法。
CREATE TABLE IF NOT EXISTS tpcds.warehouse_t6
(
    W_WAREHOUSE_SK            INTEGER                NOT NULL,
    W_WAREHOUSE_ID            CHAR(16)               NOT NULL,
    W_WAREHOUSE_NAME          VARCHAR(20)              UNIQUE,
    W_WAREHOUSE_SQ_FT         INTEGER                        ,
    W_STREET_NUMBER           CHAR(10)                       ,
    W_STREET_NAME             VARCHAR(60)                    ,
    W_STREET_TYPE             CHAR(15)                       ,
    W_SUITE_NUMBER            CHAR(10)                       ,
    W_CITY                    VARCHAR(60)                    ,
    W_COUNTY                  VARCHAR(30)                    ,
    W_STATE                   CHAR(2)                        ,
    W_ZIP                     CHAR(10)                       ,
    W_COUNTRY                 VARCHAR(20)                    ,
    W_GMT_OFFSET              DECIMAL(5,2)
) WITH(fillfactor=70);。

--创建表，并指定该表数据不写入预写日志。
CREATE UNLOGGED TABLE IF NOT EXISTS tpcds.warehouse_t7
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
    W_GMT_OFFSET              DECIMAL(5,2)
);。

--创建表临时表。
CREATE TEMPORARY TABLE IF NOT EXISTS warehouse_t24
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
    W_GMT_OFFSET              DECIMAL(5,2)
);。

--创建本地临时表，并指定提交事务时删除该临时表数据。
CREATE TEMPORARY TABLE IF NOT EXISTS warehouse_t25
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
    W_GMT_OFFSET              DECIMAL(5,2)
) ON COMMIT DELETE ROWS;。

--创建全局临时表，并指定会话结束时删除该临时表数据
CREATE GLOBAL TEMPORARY TABLE IF NOT EXISTS gtt1
(
    ID                        INTEGER               NOT NULL,
    NAME                      CHAR(16)              NOT NULL,
    ADDRESS                   VARCHAR(50)                   ,
    POSTCODE                  CHAR(6)
) ON COMMIT PRESERVE ROWS;。

--创建表时，不希望因为表已存在而报错。
CREATE TABLE IF NOT EXISTS tpcds.warehouse_t8
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
    W_GMT_OFFSET              DECIMAL(5,2)
);。

--创建普通表空间。
CREATE TABLESPACE DS_TABLESPACE1 RELATIVE LOCATION 'tablespace/tablespace_1';。
--创建表时，指定表空间。
CREATE TABLE IF NOT EXISTS tpcds.warehouse_t9
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
    W_GMT_OFFSET              DECIMAL(5,2)
) TABLESPACE DS_TABLESPACE1;。

--创建general文件系统的表空间
CREATE TABLESPACE ds_location1 RELATIVE LOCATION 'tablespace/tablespace_11' WITH (filesystem = 'general');。

--创建地址服务器为10.10.10.10:1111的表空间
CREATE TABLESPACE ds_location2 RELATIVE LOCATION 'tablespace/tablespace_2' WITH (address = '10.10.10.10:1111');。

--创建配置信息路径为'/data/omm/cfg/'的表空间
CREATE TABLESPACE ds_location3 RELATIVE LOCATION 'tablespace/tablespace_3' WITH (cfgpath = '/data/omm/cfg/');。

--创建存储信息路径为'/data/omm/store/'的表空间
CREATE TABLESPACE ds_location4 RELATIVE LOCATION 'tablespace/tablespace_4' WITH (cfgpath = '/data/omm/store/');。

--创建随机读取page的开销为1的表空间
CREATE TABLESPACE ds_location5 RELATIVE LOCATION 'tablespace/tablespace_5' WITH (random_page_cost = 1);。

--创建顺序读取page的开销为1的表空间
CREATE TABLESPACE ds_location6 RELATIVE LOCATION 'tablespace/tablespace_6' WITH (seq_page_cost = 1);。

--把表空间ds_location3重命名为ds_location3_rename。
ALTER TABLESPACE ds_location3 RENAME TO ds_location3_rename;。

--改变表空间ds_location2的所有者。
ALTER TABLESPACE ds_location2 OWNER TO proto_test;。

--删除表空间
DROP TABLESPACE ds_location1;。
DROP TABLESPACE ds_location2;。
DROP TABLESPACE ds_location3_rename;。
DROP TABLESPACE ds_location4;。
DROP TABLESPACE ds_location5;。
DROP TABLESPACE ds_location6;。

--创建表时，单独指定W_WAREHOUSE_NAME的索引表空间。
CREATE TABLE IF NOT EXISTS tpcds.warehouse_t10
(
    W_WAREHOUSE_SK            INTEGER               NOT NULL,
    W_WAREHOUSE_ID            CHAR(16)              NOT NULL,
    W_WAREHOUSE_NAME          VARCHAR(20)           UNIQUE USING INDEX TABLESPACE DS_TABLESPACE1,
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
    W_GMT_OFFSET              DECIMAL(5,2)
);。

--创建一个有主键约束的表。
CREATE TABLE IF NOT EXISTS tpcds.warehouse_t11
(
    W_WAREHOUSE_SK            INTEGER            PRIMARY KEY,
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
    W_GMT_OFFSET              DECIMAL(5,2)
);。

---或是用下面的语法，效果完全一样。
CREATE TABLE IF NOT EXISTS tpcds.warehouse_t12
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
    W_GMT_OFFSET              DECIMAL(5,2),
    PRIMARY KEY(W_WAREHOUSE_SK)
);。

--或是用下面的语法，指定约束的名称。
CREATE TABLE IF NOT EXISTS tpcds.warehouse_t13
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
    W_GMT_OFFSET              DECIMAL(5,2),
    CONSTRAINT W_CSTR_KEY1 PRIMARY KEY(W_WAREHOUSE_SK)
);。

--创建一个有复合主键约束的表。
CREATE TABLE IF NOT EXISTS tpcds.warehouse_t14
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
    W_GMT_OFFSET              DECIMAL(5,2),
    CONSTRAINT W_CSTR_KEY2 PRIMARY KEY(W_WAREHOUSE_SK, W_WAREHOUSE_ID)
);。

--创建列存表。
CREATE TABLE IF NOT EXISTS tpcds.warehouse_t15
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
    W_GMT_OFFSET              DECIMAL(5,2)
) WITH (ORIENTATION = COLUMN);。

--创建局部聚簇存储的列存表。
CREATE TABLE IF NOT EXISTS tpcds.warehouse_t16
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
    W_GMT_OFFSET              DECIMAL(5,2),
    PARTIAL CLUSTER KEY(W_WAREHOUSE_SK, W_WAREHOUSE_ID)
) WITH (ORIENTATION = COLUMN);。

--定义一个带压缩的列存表。
CREATE TABLE IF NOT EXISTS tpcds.warehouse_t17
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
    W_GMT_OFFSET              DECIMAL(5,2)
) WITH (ORIENTATION = COLUMN, COMPRESSION=HIGH);。


--定义一个检查列约束。
CREATE TABLE IF NOT EXISTS tpcds.warehouse_t19
(
    W_WAREHOUSE_SK            INTEGER               PRIMARY KEY CHECK (W_WAREHOUSE_SK > 0),
    W_WAREHOUSE_ID            CHAR(16)              NOT NULL,
    W_WAREHOUSE_NAME          VARCHAR(20)           CHECK (W_WAREHOUSE_NAME IS NOT NULL),
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
    W_GMT_OFFSET              DECIMAL(5,2)
);。

CREATE TABLE IF NOT EXISTS tpcds.warehouse_t20
(
    W_WAREHOUSE_SK            INTEGER               PRIMARY KEY,
    W_WAREHOUSE_ID            CHAR(16)              NOT NULL,
    W_WAREHOUSE_NAME          VARCHAR(20)           CHECK (W_WAREHOUSE_NAME IS NOT NULL),
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
    W_GMT_OFFSET              DECIMAL(5,2),
    CONSTRAINT W_CONSTR_KEY2 CHECK(W_WAREHOUSE_SK > 0 AND W_WAREHOUSE_NAME IS NOT NULL)
);。

--创建一个有外键约束的表。
CREATE TABLE IF NOT EXISTS tpcds.city_t23
(
    W_CITY            VARCHAR(60)                PRIMARY KEY,
    W_ADDRESS       TEXT
);。
CREATE TABLE IF NOT EXISTS tpcds.warehouse_t23
(
    W_WAREHOUSE_SK            INTEGER               NOT NULL,
    W_WAREHOUSE_ID            CHAR(16)              NOT NULL,
    W_WAREHOUSE_NAME          VARCHAR(20)                   ,
    W_WAREHOUSE_SQ_FT         INTEGER                       ,
    W_STREET_NUMBER           CHAR(10)                      ,
    W_STREET_NAME             VARCHAR(60)                   ,
    W_STREET_TYPE             CHAR(15)                      ,
    W_SUITE_NUMBER            CHAR(10)                      ,
    W_CITY                    VARCHAR(60)           REFERENCES tpcds.city_t23(W_CITY),
    W_COUNTY                  VARCHAR(30)                   ,
    W_STATE                   CHAR(2)                       ,
    W_ZIP                     CHAR(10)                      ,
    W_COUNTRY                 VARCHAR(20)                   ,
    W_GMT_OFFSET              DECIMAL(5,2)
);。

--或是用下面的语法，效果完全一样。
CREATE TABLE IF NOT EXISTS tpcds.warehouse_t23
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
    FOREIGN KEY(W_CITY) REFERENCES tpcds.city_t23(W_CITY)
);。

--或是用下面的语法，指定约束的名称。
CREATE TABLE IF NOT EXISTS tpcds.warehouse_t23
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
    CONSTRAINT W_FORE_KEY1 FOREIGN KEY(W_CITY) REFERENCES tpcds.city_t23(W_CITY)
);。

--向tpcds.warehouse_t19表中增加一个varchar列。
ALTER TABLE tpcds.warehouse_t19 ADD W_GOODS_CATEGORY varchar(30);。

--给tpcds.warehouse_t19表增加一个检查约束。
ALTER TABLE tpcds.warehouse_t19 ADD CONSTRAINT W_CONSTR_KEY4 CHECK (W_STATE IS NOT NULL);。

--在一个操作中改变两个现存字段的类型。
ALTER TABLE tpcds.warehouse_t19
    ALTER COLUMN W_GOODS_CATEGORY TYPE varchar(80),
    ALTER COLUMN W_STREET_NAME TYPE varchar(100);。

--此语句与上面语句等效。
ALTER TABLE tpcds.warehouse_t19 MODIFY (W_GOODS_CATEGORY varchar(30), W_STREET_NAME varchar(60));。

--给一个已存在字段添加非空约束。
ALTER TABLE tpcds.warehouse_t19 ALTER COLUMN W_GOODS_CATEGORY SET NOT NULL;。

--移除已存在字段的非空约束。
ALTER TABLE tpcds.warehouse_t19 ALTER COLUMN W_GOODS_CATEGORY DROP NOT NULL;。

--如果列存表中还未指定局部聚簇，向在一个列存表中添加局部聚簇列。
ALTER TABLE tpcds.warehouse_t17 ADD PARTIAL CLUSTER KEY(W_WAREHOUSE_SK);。

--查看约束的名称，并删除一个列存表中的局部聚簇列。
SELECT constraint_name
FROM information_schema.table_constraints
WHERE table_name = 'warehouse_t17' AND table_schema = 'tpcds';。

ALTER TABLE tpcds.warehouse_t17 DROP CONSTRAINT warehouse_t17_cluster;。

--将表移动到另一个表空间。
ALTER TABLE tpcds.warehouse_t19 SET TABLESPACE PG_DEFAULT;。
--创建模式joe。
CREATE SCHEMA IF NOT EXISTS joe;。

--将表移动到另一个模式中。
ALTER TABLE tpcds.warehouse_t19 SET SCHEMA joe;。

--重命名已存在的表。
ALTER TABLE joe.warehouse_t19 RENAME TO warehouse_t23;。

--从warehouse_t23表中删除一个字段。
ALTER TABLE joe.warehouse_t23 DROP COLUMN W_STREET_NAME;。

--创建带INVISIBLE唯一索引的表，需要在B兼容性数据库下
CREATE TABLE IF NOT EXISTS tpcds.warehouse_t26
(
    W_WAREHOUSE_SK            INTEGER                NOT NULL,
    W_WAREHOUSE_ID            CHAR(16)               NOT NULL,
    W_WAREHOUSE_NAME          VARCHAR(20)              UNIQUE,
    W_WAREHOUSE_SQ_FT         INTEGER                        ,
    W_STREET_NUMBER           CHAR(10)                       ,
    W_STREET_NAME             VARCHAR(60)                    ,
    W_STREET_TYPE             CHAR(15)                       ,
    W_SUITE_NUMBER            CHAR(10)                       ,
    W_CITY                    VARCHAR(60)                    ,
    W_COUNTY                  VARCHAR(30)                    ,
    W_STATE                   CHAR(2)                        ,
    W_ZIP                     CHAR(10)                       ,
    W_COUNTRY                 VARCHAR(20)                    ,
    W_GMT_OFFSET              DECIMAL(5,2)                   ,
    UNIQUE uni_t26 (W_WAREHOUSE_SK) INVISIBLE
) WITH(fillfactor=70);。

--删除表空间、模式joe和模式表warehouse。
DROP TABLE IF EXISTS tpcds.warehouse_t1 CASCADE;。
DROP TABLE IF EXISTS tpcds.warehouse_t2 CASCADE;。
DROP TABLE IF EXISTS tpcds.warehouse_t3 CASCADE;。
DROP TABLE IF EXISTS tpcds.warehouse_t4 CASCADE;。
DROP TABLE IF EXISTS tpcds.warehouse_t5 CASCADE;。
DROP TABLE IF EXISTS tpcds.warehouse_t6 CASCADE;。
DROP TABLE IF EXISTS tpcds.warehouse_t7 CASCADE;。
DROP TABLE IF EXISTS tpcds.warehouse_t8 CASCADE;。
DROP TABLE IF EXISTS tpcds.warehouse_t9 CASCADE;。
DROP TABLE IF EXISTS tpcds.warehouse_t10 CASCADE;。
DROP TABLE IF EXISTS tpcds.warehouse_t11 CASCADE;。
DROP TABLE IF EXISTS tpcds.warehouse_t12 CASCADE;。
DROP TABLE IF EXISTS tpcds.warehouse_t13 CASCADE;。
DROP TABLE IF EXISTS tpcds.warehouse_t14 CASCADE;。
DROP TABLE IF EXISTS tpcds.warehouse_t15 CASCADE;。
DROP TABLE IF EXISTS tpcds.warehouse_t16 CASCADE;。
DROP TABLE IF EXISTS tpcds.warehouse_t17 CASCADE;。
DROP TABLE IF EXISTS tpcds.warehouse_t18 CASCADE;。
DROP TABLE IF EXISTS tpcds.warehouse_t20 CASCADE;。
DROP TABLE IF EXISTS tpcds.warehouse_t21 CASCADE;。
DROP TABLE IF EXISTS tpcds.warehouse_t22 CASCADE;。
DROP TABLE IF EXISTS joe.warehouse_t23 CASCADE;。
DROP TABLE IF EXISTS tpcds.warehouse_t24 CASCADE;。
DROP TABLE IF EXISTS tpcds.warehouse_t25 CASCADE;。
DROP TABLE IF EXISTS tpcds.warehouse_t26 CASCADE;。
DROP TABLE IF EXISTS gtt1 CASCADE;。
DROP TABLESPACE DS_TABLESPACE1;。
DROP SCHEMA IF EXISTS joe CASCADE;。
DROP SCHEMA IF EXISTS tpcds CASCADE;。