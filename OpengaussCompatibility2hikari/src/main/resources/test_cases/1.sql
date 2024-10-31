-- CREATE/ALTER/DROP DATABASE

set dolphin.b_compatibility_mode=off;。

-- 创建用户 jim，设置密码
CREATE USER IF NOT EXISTS jim PASSWORD 'jim@1234';。

-- 创建用户 tom，设置密码
CREATE USER IF NOT EXISTS tom PASSWORD 'tom@1234';。

-- if not exists && if exists
create database if not exists test1;。
drop database if exists test1;。

-- 创建数据库 music，字符编码为 GBK，使用 template0 作为模板
CREATE DATABASE IF NOT EXISTS music ENCODING 'GBK' TEMPLATE = template0;。

-- 创建数据库 music2，所有者为 jim
CREATE DATABASE IF NOT EXISTS music2 OWNER jim;。

-- 创建数据库 music3，所有者为 jim，使用 template0 作为模板
CREATE DATABASE IF NOT EXISTS music3 OWNER jim TEMPLATE template0;。

-- 设置数据库 music 的连接限制为 10
ALTER DATABASE music CONNECTION LIMIT = 10;。

-- 重命名数据库 music 为 music4
ALTER DATABASE music RENAME TO music4;。

-- 修改数据库 music2 的所有者为 tom
ALTER DATABASE music2 OWNER TO tom;。

-- 设置数据库 music3 的默认表空间为 PG_DEFAULT
ALTER DATABASE music3 SET TABLESPACE PG_DEFAULT;。

-- 禁用数据库 music3 的索引扫描
ALTER DATABASE music3 SET enable_indexscan TO off;。

-- 重置数据库 music3 的索引扫描设置为默认值
ALTER DATABASE music3 RESET enable_indexscan;。

-- 删除数据库 music2
DROP DATABASE IF EXISTS music2;。

-- 删除数据库 music3
DROP DATABASE IF EXISTS music3;。

-- 删除数据库 music4
DROP DATABASE IF EXISTS music4;。

-- 删除用户 jim
DROP USER IF EXISTS jim CASCADE;。

-- 删除用户 tom
DROP USER IF EXISTS tom CASCADE;。

-- 创建兼容性数据库 td_compatible_db，兼容性设置为 'C'
CREATE DATABASE IF NOT EXISTS td_compatible_db DBCOMPATIBILITY 'C';。

-- 创建兼容性数据库 ora_compatible_db，兼容性设置为 'A'
CREATE DATABASE IF NOT EXISTS ora_compatible_db DBCOMPATIBILITY 'A';。

-- 删除数据库 td_compatible_db
DROP DATABASE IF EXISTS td_compatible_db;。

-- 删除数据库 ora_compatible_db
DROP DATABASE IF EXISTS ora_compatible_db;。
