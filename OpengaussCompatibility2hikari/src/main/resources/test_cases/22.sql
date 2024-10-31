-- dolphin
-- CREATE/ALTER DATABASE

-- 打开 dolphin.b_compatibility_mode 开关
set dolphin.b_compatibility_mode = on;。

-- 在dolphin版本中，若dolphin.b_compatibility_mode为on，CREATE DATABASE将被视作CREATE SCHEMA
create database if not exists test1;。
-- 查看所有schema
SELECT schema_name FROM information_schema.schemata;。
-- 查看所有数据库
SELECT datname FROM pg_database;。

-- 会失败当数据库编码方式和即将创建的schema不一致，数据库编码方式为SQL_ASCII
create database if not exists test2 charset 'utf8';。
-- ALTER失败
ALTER DATABASE test1 DEFAULT CHARACTER SET = 'utf8';。
drop database if exists test1;。
drop database if exists test2;。