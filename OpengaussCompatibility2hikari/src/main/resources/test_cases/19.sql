--CREATE FUNCTION
--定义函数为SQL查询。
CREATE FUNCTION func_add_sql(integer, integer) RETURNS integer
    AS 'select $1 + $2;'
    LANGUAGE SQL
    IMMUTABLE
    RETURNS NULL ON NULL INPUT;。

--利用参数名用 PL/pgSQL 自增一个整数。
CREATE OR REPLACE FUNCTION func_increment_plsql(i integer) RETURNS integer AS $$
        BEGIN
                RETURN i + 1;
        END;
$$ LANGUAGE plpgsql;。

--返回RECORD类型
CREATE OR REPLACE FUNCTION func_increment_sql(i int, out result_1 bigint, out result_2 bigint)
returns SETOF RECORD
as $$
begin
    result_1 = i + 1;
    result_2 = i * 10;
return next;
end;
$$language plpgsql;。

--返回一个包含多个输出参数的记录。
CREATE FUNCTION func_dup_sql(in int, out f1 int, out f2 text)
    AS $$ SELECT $1, CAST($1 AS text) || ' is text' $$
    LANGUAGE SQL;。

SELECT * FROM func_dup_sql(42);。

--计算两个整数的和，并返回结果 如果输入为null，则返回null。
CREATE FUNCTION func_add_sql2(num1 integer, num2 integer) RETURN integer
    AS
    BEGIN
    RETURN num1 + num2;
    END;
/。
--修改函数func_add_sql2的执行规则为IMMUTABLE，即参数不变时返回相同结果。
ALTER FUNCTION func_add_sql2(INTEGER, INTEGER) IMMUTABLE;。

--将函数func_add_sql2的名称修改为add_two_number。
ALTER FUNCTION func_add_sql2(INTEGER, INTEGER) RENAME TO add_two_number;。

--将函数add_two_number的属者改为omm。
ALTER FUNCTION add_two_number(INTEGER, INTEGER) OWNER TO omm;。

--删除函数。
DROP FUNCTION add_two_number;。
DROP FUNCTION func_increment_sql;。
DROP FUNCTION func_dup_sql;。
DROP FUNCTION func_increment_plsql;。
DROP FUNCTION func_add_sql;。