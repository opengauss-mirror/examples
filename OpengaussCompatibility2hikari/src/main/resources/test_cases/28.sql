-- dolphin
-- GRANT/REVOKE PROXY

--创建简单表
CREATE SCHEMA IF NOT EXISTS tst_schema1;。
SET SEARCH_PATH TO tst_schema1;。
CREATE TABLE IF NOT EXISTS tst_t1
(
    id int,
    name varchar(20)
);。
INSERT INTO tst_t1 values(20220101, 'proxy_example');。

--创建用户
REVOKE ALL PRIVILEGES FROM test_proxy_u1;。
DROP ROLE if EXISTS test_proxy_u1;。
CREATE ROLE test_proxy_u1 IDENTIFIED BY 'test_proxy_u1@123';。
DROP ROLE if EXISTS test_proxy_u2;。
CREATE ROLE test_proxy_u2 IDENTIFIED BY 'test_proxy_u2@123';。
DROP ROLE if EXISTS test_proxy_u3;。
CREATE ROLE test_proxy_u3 IDENTIFIED BY 'test_proxy_u3@123';。

--schema、表权限授予
GRANT ALL ON SCHEMA tst_schema1 TO test_proxy_u1;。
GRANT ALL ON SCHEMA tst_schema1 TO test_proxy_u2;。
GRANT ALL ON SCHEMA tst_schema1 TO test_proxy_u3;。
GRANT ALL ON tst_t1 to test_proxy_u1;。

--权限检测（无权限）
--ERROR:  permission denied for relation tst_t1
--DETAIL:  N/A
--SET ROLE test_proxy_u2 PASSWORD 'test_proxy_u2@123';。
SELECT * FROM tst_schema1.tst_t1;。

--权限检测（拥有代理者权限）
RESET ROLE;。
GRANT PROXY ON test_proxy_u1 TO test_proxy_u2;。
SET ROLE test_proxy_u2 PASSWORD 'test_proxy_u2@123';。
SELECT * FROM tst_schema1.tst_t1;。

 --权限检测（级联式检测usr_1->usr_2->usr_3)
RESET ROLE;。
GRANT PROXY ON test_proxy_u2 TO test_proxy_u3;。
SET ROLE test_proxy_u3 PASSWORD 'test_proxy_u3@123';。
SELECT * FROM tst_schema1.tst_t1;。

--对被代理者授予的权限检测（with grant option)
RESET ROLE;。
SET ROLE test_proxy_u2 PASSWORD 'test_proxy_u2@123';。
--ERROR:  must have admin option on role "test_proxy_u3"
--grant proxy on test_proxy_u1 to test_proxy_u3;。
RESET ROLE;。

SET ROLE test_proxy_u2 PASSWORD 'test_proxy_u2@123';。
--ERROR:  must have admin option on role "test_proxy_u3"
--grant proxy on test_proxy_u1 to test_proxy_u3;。
RESET ROLE;。
grant proxy on test_proxy_u1 to test_proxy_u2 with grant option;。
SET ROLE test_proxy_u2 PASSWORD 'test_proxy_u2@123';。
grant proxy on test_proxy_u1 to test_proxy_u3;。

--召回代理权限测试
revoke proxy on test_proxy_u1 from test_proxy_u3;。
revoke proxy on test_proxy_u1 from test_proxy_u2;。
SET ROLE test_proxy_u3 password 'test_proxy_u3@123';。
--ERROR:  permission denied for relation tst_t1
--DETAIL:  N/A
--SELECT * FROM tst_schema1.tst_t1;。
RESET ROLE;。

DROP SCHEMA IF EXISTS tst_schema1 CASCADE;。
DROP TABLE IF EXISTS tst_t1 CASCADE;。
DROP ROLE if EXISTS test_proxy_u1;。
DROP ROLE if EXISTS test_proxy_u2;。
DROP ROLE if EXISTS test_proxy_u3;。