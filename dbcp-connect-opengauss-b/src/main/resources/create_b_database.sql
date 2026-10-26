-- =====================================================================
-- 第 1 步：创建 B 兼容数据库 + 业务 schema + MySQL 协议 native 密码
-- 用法（在 postgres 默认库中执行，当前用户为超级用户，如 opengauss）：
--   gsql -d postgres -h <unix_socket目录> -p 5432 -f create_b_database.sql
-- 执行顺序：本脚本必须在 init.sql 之前执行（先建库，再在库里建表）。
--
-- 幂等说明：
--   - 首次在空环境执行：全部语句一次成功（CREATE DATABASE / schema / 授权）。
--   - 重复执行：仅 CREATE DATABASE 可能报 “already exists”（gsql 不支持 \gexec，
--     且 CREATE DATABASE 不能放入 DO 块）；CREATE SCHEMA 使用 IF NOT EXISTS，
--     重复执行不报错。重复建表由 init.sql 的 IF NOT EXISTS 与条件插入保证幂等。
-- =====================================================================

-- 1. 创建 B 兼容数据库（DBCOMPATIBILITY 'B'，提供 utf8mb4 / MySQL 语义）
CREATE DATABASE proto_test_db WITH DBCOMPATIBILITY = 'B';

-- 2. 连接进入 B 兼容库（后续 schema / 密码均在该库上下文执行）
\connect proto_test_db

-- 3. 设置 MySQL 协议 native 密码。执行前将 <DBCP_PASSWORD> 替换为实际密码，
--    并与 dbcp.properties 中的 password 保持一致。第三个参数是原 native 密码；
--    首次设置或重复使用同一密码时可保持如下写法，修改已有密码时应改为当前密码。
SELECT set_native_password('opengauss', '<DBCP_PASSWORD>', '<DBCP_PASSWORD>');

-- 4. 创建业务 schema（MySQL 协议下 schema 等价于 database，
--    jdbc:mysql://<host>:3308/mysql_test_db 中的库名即映射到该 schema）
CREATE SCHEMA IF NOT EXISTS mysql_test_db;

-- 5. 表权限：schema 与其中表对象默认属于当前超级用户 opengauss；
--    显式授权给 opengauss，保证 DBCP 连接（opengauss 账号）可读写
GRANT ALL ON SCHEMA mysql_test_db TO opengauss;
