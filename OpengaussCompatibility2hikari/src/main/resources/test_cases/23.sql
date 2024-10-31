-- dolphin
-- CREATE/ALTER PROCEDURE

CREATE TABLE IF NOT EXISTS t1 (id INT PRIMARY KEY,a INT);。
INSERT INTO t1 (id, a) VALUES (1, 100);。
INSERT INTO t1 (id, a) VALUES (2, 200);。

--创建存储过程使用单条查询语句，显示为CREATE PROCEDURE
create procedure procxx() select a from t1;。
--调用时需要开启参数
set dolphin.sql_mode = 'block_return_multi_results';。
show dolphin.sql_mode;。
call procxx();。

create procedure proc1() select * from t1;。
--指定 NO SQL
ALTER PROCEDURE proc1() NO SQL;。
--指定 CONTAINS SQL
ALTER PROCEDURE proc1() CONTAINS SQL;。
--指定 LANGUAGE SQL
ALTER PROCEDURE proc1() CONTAINS SQL LANGUAGE SQL;。
--指定 MODIFIES SQL DATA
ALTER PROCEDURE proc1() CONTAINS SQL MODIFIES SQL DATA;。
--指定 SECURITY INVOKER
ALTER PROCEDURE proc1() SQL SECURITY INVOKER;。

DROP PROCEDURE IF EXISTS proc1;。
DROP PROCEDURE IF EXISTS procxx;。

DROP TABLE IF EXISTS t1 CASCADE;。