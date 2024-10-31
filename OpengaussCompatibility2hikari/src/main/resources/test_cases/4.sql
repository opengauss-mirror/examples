-- CREATE/ALTER/DROP ROLE

-- 创建角色 manager，并设置密码
CREATE ROLE manager IDENTIFIED BY 'manager@1234';。

-- 创建角色 miriam，允许登录并设置密码，有效期从 2020-01-01 到 2026-01-01
CREATE ROLE miriam WITH LOGIN PASSWORD 'Abcd@123' VALID BEGIN '2020-01-01' VALID UNTIL '2026-01-01';。

-- 修改角色 manager 的密码，替换为新的密码
ALTER ROLE manager IDENTIFIED BY 'Abcd@123' REPLACE 'manager@1234';。

-- 为角色 manager 添加 SYSADMIN 权限
ALTER ROLE manager SYSADMIN;。

-- 删除角色 manager
DROP ROLE manager;。

-- 删除角色 miriam
DROP ROLE miriam;。
