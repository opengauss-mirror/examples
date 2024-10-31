-- CREATE/ALTER/DROP USER

-- 创建用户 jim，并设置密码
CREATE USER IF NOT EXISTS jim PASSWORD 'jim@1234';。

-- 创建用户 kim，并设置密码
CREATE USER IF NOT EXISTS kim IDENTIFIED BY 'kim@1234';。

-- 创建用户 dim，允许创建数据库，并设置密码
CREATE USER IF NOT EXISTS dim CREATEDB PASSWORD 'dim@1234';。

-- 修改用户 jim 的密码为 'Abcd@123'，并替换旧密码
ALTER USER jim IDENTIFIED BY 'Abcd@123' REPLACE 'jim@1234';。

-- 授予用户 jim 创建角色的权限
ALTER USER jim CREATEROLE;。

-- 设置用户 jim 的选项 enable_seqscan 为开启状态
ALTER USER jim SET enable_seqscan TO on;。

-- 重置用户 jim 的选项 enable_seqscan 为默认状态
ALTER USER jim RESET enable_seqscan;。

-- 锁定用户 jim 的账户，禁止其登录
ALTER USER jim ACCOUNT LOCK;。

-- 删除用户 kim，使用 CASCADE 删除其所有依赖对象
DROP USER IF EXISTS kim CASCADE;。

-- 删除用户 jim，使用 CASCADE 删除其所有依赖对象
DROP USER IF EXISTS jim CASCADE;。

-- 删除用户 dim，使用 CASCADE 删除其所有依赖对象
DROP USER IF EXISTS dim CASCADE;。
