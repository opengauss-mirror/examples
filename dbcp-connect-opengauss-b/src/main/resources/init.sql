-- =====================================================================
-- 第 2 步：在 B 兼容库 proto_test_db 中创建业务表并初始化数据
-- 用法（需先执行 create_b_database.sql，再连接进入 proto_test_db）：
--   gsql -d proto_test_db -h <unix_socket目录> -p 5432 -f init.sql
--
-- 幂等：建表使用 IF NOT EXISTS，初始数据按姓名去重，可重复执行。
-- =====================================================================

-- 1. 切换业务 schema（MySQL 协议下 schema 等价于 database，
--    与 jdbc:mysql://<host>:3308/mysql_test_db 的 URL 库名一致）
SET current_schema TO mysql_test_db;

-- 2. 创建业务表（与官网 Mybatis 示例保持一致，便于横向对比）
CREATE TABLE IF NOT EXISTS `user` (
    `id`   INT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL COMMENT '用户名',
    `age`  INT COMMENT '年龄'
) DEFAULT CHARSET=utf8mb4;

-- 3. 初始化数据（幂等：仅当姓名不存在时插入）
INSERT INTO `user` (`name`,`age`)
SELECT '张三', 18 WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `name` = '张三');
INSERT INTO `user` (`name`,`age`)
SELECT '李四', 19 WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `name` = '李四');
INSERT INTO `user` (`name`,`age`)
SELECT '王五', 20 WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `name` = '王五');
