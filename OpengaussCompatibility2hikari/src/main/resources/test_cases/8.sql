--CREATE SCHEMA

--创建一个角色role1。
CREATE ROLE role1 IDENTIFIED BY 'test@123';。

-- 为用户role1创建一个同名schema，子命令创建的表films和winners的拥有者为role1。
CREATE SCHEMA IF NOT EXISTS AUTHORIZATION role1
     CREATE TABLE films (title text, release date, awards text[])      
     CREATE VIEW winners AS         
     SELECT title, release FROM films WHERE awards IS NOT NULL;。
     
-- 创建一个schema ds，指定schema的默认字符集为utf8mb4，默认字符序为utf8mb4_bin。
--ERROR:  difference between the charset and the database encoding has not supported
CREATE SCHEMA IF NOT EXISTS ds CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;。

--删除schema。
DROP SCHEMA IF EXISTS role1 CASCADE;。
--删除用户。
DROP USER IF EXISTS role1 CASCADE;。
DROP SCHEMA IF EXISTS ds CASCADE;。
