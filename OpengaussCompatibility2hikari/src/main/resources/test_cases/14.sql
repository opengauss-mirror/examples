-- GRANT & REVOKE

-- 创建用户架构 user1
CREATE SCHEMA IF NOT EXISTS user1;。

-- 创建用户 joe，并设置密码
CREATE USER IF NOT EXISTS joe PASSWORD 'Abcd@123';。

-- 授予用户 joe 所有权限
GRANT ALL PRIVILEGES TO joe;。

-- 撤销用户 joe 的所有权限
REVOKE ALL PRIVILEGES FROM joe;。

-- 授予用户 joe 使用 schema 的权限
GRANT USAGE ON SCHEMA user1 TO joe;。

-- 创建原因表 user1.reason
CREATE TABLE IF NOT EXISTS user1.reason (
    r_reason_sk    INTEGER PRIMARY KEY,      -- 唯一标识符
    r_reason_id    CHARACTER(16),            -- 原因ID
    r_reason_desc  CHARACTER(100)            -- 原因描述
);。

-- 授予用户 joe 对 user1.reason 表的所有权限
GRANT ALL PRIVILEGES ON user1.reason TO joe;。

-- 授予用户 joe 对 user1.reason 表的部分权限
GRANT SELECT (r_reason_sk, r_reason_id, r_reason_desc),
       UPDATE (r_reason_desc) ON user1.reason TO joe;。

-- 授予用户 joe 对 user1.reason 表的选择权限，并允许其转授
GRANT SELECT (r_reason_sk, r_reason_id) ON user1.reason TO joe WITH GRANT OPTION;。

-- 关闭 dolphin.b_compatibility_mode 开关
set dolphin.b_compatibility_mode = off;。
CREATE DATABASE IF NOT EXISTS bmode_db;。
-- 授予用户 joe 对数据库 bmode_db 的创建和连接权限，并允许其转授
GRANT CREATE, CONNECT ON DATABASE bmode_db TO joe WITH GRANT OPTION;。

-- 创建角色 user1_manager，并设置密码
CREATE ROLE user1_manager PASSWORD 'Abcd@123';。

-- 授予角色 user1_manager 对 schema user1 的使用和创建权限
GRANT USAGE, CREATE ON SCHEMA user1 TO user1_manager;。

-- 创建表空间 user1_tbspc，并设置相对位置
CREATE TABLESPACE user1_tbspc RELATIVE LOCATION 'tablespace/tablespace_1';。

-- 授予用户 joe 对表空间 user1_tbspc 的所有权限
GRANT ALL ON TABLESPACE user1_tbspc TO joe;。

-- 创建角色 manager，并设置密码
CREATE ROLE manager PASSWORD 'Abcd@123';。

-- 将用户 joe 授予给角色 manager，并允许管理
GRANT joe TO manager WITH ADMIN OPTION;。

-- 创建角色 senior_manager，并设置密码
CREATE ROLE senior_manager PASSWORD 'Abcd@123';。

-- 将角色 manager 授予给角色 senior_manager
GRANT manager TO senior_manager;。

-- 撤销角色 manager 对用户 joe 的授予
REVOKE manager FROM joe;。

-- 撤销角色 senior_manager 对角色 manager 的授予
REVOKE senior_manager FROM manager;。

-- 删除用户 manager
DROP USER IF EXISTS manager;。
DROP USER IF EXISTS senior_manager;。

-- 删除表 user1.reason
DROP TABLE IF EXISTS user1.reason;。

-- 撤销角色 user1_manager 的权限
REVOKE ALL PRIVILEGES ON SCHEMA user1 FROM user1_manager;。
REVOKE USAGE, CREATE ON SCHEMA user1 FROM user1_manager;。

-- 删除角色 user1_manager
DROP ROLE user1_manager;。

-- 删除表空间 user1_tbspc
DROP TABLESPACE user1_tbspc;。

-- 删除用户架构 user1 及其所有内容
DROP SCHEMA IF EXISTS user1 CASCADE;。

-- 撤销用户 joe 在数据库 bmode_db 上的所有权限
REVOKE ALL PRIVILEGES ON DATABASE bmode_db FROM joe;。
REVOKE CREATE, CONNECT ON DATABASE bmode_db FROM joe;。

DROP DATABASE IF EXISTS bmode_db;。
-- 删除用户 joe
DROP USER IF EXISTS joe CASCADE;。