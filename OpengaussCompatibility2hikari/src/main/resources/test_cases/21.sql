--dolphin
-- CREATE/ALTER FUNCTION

--指定 CONTAINS SQL
CREATE FUNCTION func_test1 (s CHAR(20)) RETURNS int
CONTAINS SQL AS $$ select 1 $$ ;。

--指定 DETERMINISTIC
CREATE FUNCTION func_test2 (s int) RETURNS int
CONTAINS SQL DETERMINISTIC  AS $$ select s; $$ ;。

--指定 LANGUAGE SQL
CREATE FUNCTION func_test3 (s int) RETURNS int
CONTAINS SQL LANGUAGE SQL AS $$ select s; $$ ;。

--指定 NO SQL
CREATE FUNCTION func_test4 (s int) RETURNS int
NO SQL AS $$ select s; $$ ;。

--指定  READS SQL DATA
CREATE FUNCTION func_test5 (s int) RETURNS int
CONTAINS SQL  READS SQL DATA  AS $$ select s; $$ ;。

--指定 MODIFIES SQL DATA
CREATE FUNCTION func_test6 (s int) RETURNS int
CONTAINS SQL LANGUAGE SQL NO SQL  MODIFIES SQL DATA AS $$ select s; $$ ;。

--指定 SECURITY DEFINER
CREATE FUNCTION func_test7 (s int) RETURNS int
NO SQL SQL SECURITY DEFINER AS $$ select s; $$ ;。

--指定 SECURITY INVOKER
CREATE FUNCTION func_test8 (s int) RETURNS int
SQL SECURITY INVOKER  READS SQL DATA LANGUAGE SQL AS $$ select s; $$ ;。

CREATE FUNCTION f1 (s CHAR(20)) RETURNS int
CONTAINS SQL AS $$ select 1 $$ ;。
--指定 NO SQL
ALTER FUNCTION f1 (s char(20)) NO SQL;。

 --指定 CONTAINS SQL
ALTER FUNCTION f1 (s char(20)) CONTAINS SQL;。

 --指定 LANGUAGE SQL
ALTER FUNCTION f1 (s char(20)) LANGUAGE SQL ;。

 --指定 MODIFIES SQL DATA
ALTER FUNCTION f1 (s char(20)) MODIFIES SQL DATA;。

 --指定  READS SQL DATA
ALTER FUNCTION f1 (s char(20)) READS SQL DATA;。

--指定 SECURITY INVOKER
ALTER FUNCTION f1 (s char(20)) SQL SECURITY INVOKER;。

--指定 SECURITY DEFINER
ALTER FUNCTION f1 (s char(20)) SQL SECURITY DEFINER;。

--MySQL风格语法格式
create function func(n int) returns varchar(50) return (select n+1);select func(1);。
select func(1);。

-- 更改分隔符
--ERROR: syntax error at end of input
--delimiter //。
--create function func10(b int) returns int
--begin
--if b > 0 then return b + 10;
--  else return -1;
--  end if;
--end//。
--delimiter ;。
--select func10(9);。

DROP FUNCTION func_test1 (s CHAR(20));。
DROP FUNCTION func_test2 (s int);。
DROP FUNCTION func_test3 (s int);。
DROP FUNCTION func_test4 (s int);。
DROP FUNCTION func_test5 (s int);。
DROP FUNCTION func_test6 (s int);。
DROP FUNCTION func_test7 (s int);。
DROP FUNCTION func_test8 (s int);。
DROP FUNCTION f1 (s CHAR(20));。
DROP FUNCTION func(n int);。
--DROP FUNCTION func10(b int);。
