-- CREATE VIEW

-- 创建角色 role1，并设置密码
CREATE ROLE role1 IDENTIFIED BY 'role1@1234';。

-- 创建一个由角色 role1 授权的模式
CREATE SCHEMA IF NOT EXISTS AUTHORIZATION role1
    -- 在模式中创建 films 表，包含电影标题、发行日期和奖项
    CREATE TABLE films (
        title text,                -- 电影标题，文本类型
        release date,              -- 发行日期，日期类型
        awards text[]              -- 奖项，文本数组类型
    );。

-- 创建视图 winners，选择 films 表中有奖项的电影
CREATE VIEW winners AS
SELECT title, release FROM role1.films WHERE awards IS NOT NULL;。

-- 删除模式 role1 及其所有内容
DROP SCHEMA IF EXISTS role1 CASCADE;。

-- 删除用户 role1 及其所有内容
DROP USER role1 CASCADE;。

--创建字段spcname为pg_default组成的视图。
CREATE VIEW myView AS
    SELECT * FROM pg_tablespace WHERE spcname = 'pg_default';。

--查看视图。
SELECT * FROM myView ;。

--删除视图myView。
DROP VIEW myView;。

--创建基表，并插入数据。
CREATE TABLE IF NOT EXISTS base_tbl (a int PRIMARY KEY, b text DEFAULT 'Unspecified');。
INSERT INTO base_tbl values (1, 'insertTable');。

--创建视图
CREATE VIEW ro_view1 AS SELECT a, b FROM base_tbl;。

--视图插入、更新和删除数据
INSERT INTO ro_view1 values (2, 'insertView');。
UPDATE ro_view1 SET b = 'updateView' WHERE a = 1;。
DELETE FROM ro_view1 WHERE a= 2;。

--创建check option视图
CREATE VIEW ro_view2 AS SELECT a, b FROM base_tbl WHERE a > 10 WITH CHECK OPTION;。

--往基表插入视图不可见的数据
INSERT INTO base_tbl values (15, 'insertTable');。

--插入、更新视图不可见数据失败
--ERROR:  new row violates WITH CHECK OPTION for view "ro_view2"
--DETAIL:  Failing row contains (5, insertView).
--INSERT INTO ro_view2 values (5, 'insertView');。
--ERROR:  new row violates WITH CHECK OPTION for view "ro_view2"
--DETAIL:  Failing row contains (5, insertTable).
--UPDATE ro_view2 SET a = 5 WHERE a = 15;。

-- 创建用户架构 user1
CREATE SCHEMA IF NOT EXISTS user1;。

-- 创建客户表 user1.customer
CREATE TABLE IF NOT EXISTS user1.customer (
   c_customer_sk     INTEGER       NOT NULL,      -- 客户唯一标识符，整型，不能为空
   c_customer_id     CHAR(16)      NOT NULL,      -- 客户ID，字符型，长度为16，不能为空
   c_first_name      VARCHAR(50)   NOT NULL,      -- 客户名字，变长字符型，最大长度为50，不能为空
   c_last_name       VARCHAR(50)   NOT NULL,      -- 客户姓氏，变长字符型，最大长度为50，不能为空
   c_email_address   VARCHAR(100)  NOT NULL,      -- 客户电子邮件，变长字符型，最大长度为100，不能为空
   c_birth_date      DATE          NOT NULL,      -- 客户出生日期，日期型，不能为空
   c_create_date     DATE          NOT NULL,      -- 客户创建日期，日期型，不能为空
   c_address         VARCHAR(200)  NOT NULL,      -- 客户地址，变长字符型，最大长度为200，不能为空
   c_city            VARCHAR(100)  NOT NULL,      -- 客户城市，变长字符型，最大长度为100，不能为空
   c_state           CHAR(2)       NOT NULL,      -- 客户州，字符型，长度为2，不能为空
   c_zip             CHAR(10)      NOT NULL,      -- 客户邮政编码，字符型，长度为10，不能为空
   c_country         VARCHAR(100)  NOT NULL,      -- 客户国家，变长字符型，最大长度为100，不能为空
   c_gmt_offset      DECIMAL(5,2)  NOT NULL,      -- 客户GMT偏移，十进制型，不能为空
   c_active          BOOLEAN       NOT NULL,      -- 客户是否活跃，布尔型，不能为空
   PRIMARY KEY (c_customer_sk)            -- 设置主键为 c_customer_sk
);。

-- 创建视图 user1.customer_details_view_v1，查询所有客户，条件是 c_customer_sk 小于 150
CREATE VIEW user1.customer_details_view_v1 AS
    SELECT * FROM user1.customer
    WHERE c_customer_sk < 150;。

-- 将视图的名称从 customer_details_view_v1 修改为 customer_details_view_v2
ALTER VIEW user1.customer_details_view_v1 RENAME TO customer_details_view_v2;。

-- 将视图的架构修改为 public
ALTER VIEW user1.customer_details_view_v2 SET schema public;。

-- 删除视图 public.customer_details_view_v2
DROP VIEW public.customer_details_view_v2;。

DROP TABLE IF EXISTS base_tbl CASCADE;

-- 删除用户架构 user1 及其所有内容
DROP SCHEMA IF EXISTS user1 CASCADE;。
