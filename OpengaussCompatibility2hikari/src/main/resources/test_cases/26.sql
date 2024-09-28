-- dolphin
-- CREATE TABLESPACE

--使用ADD DATAFILE语法创建表空间。
CREATE TABLESPACE t_tbspace ADD DATAFILE 'my_tablespace' ENGINE = test_engine;。

--使用ADD DATAFILE语法创建表空间，输入路径以.ibd结尾
--WARNING:  Suffix ".ibd" of datafile path detected. The actual path will be renamed as "test_tbspace1_ibd"
CREATE TABLESPACE test_tbspace_ibd ADD DATAFILE 'test_tbspace1.ibd';。

SELECT
    t.table_name,
    c.column_name,
    c.data_type,
    c.is_nullable,
    c.column_default
FROM
    information_schema.tables AS t
JOIN
    information_schema.columns AS c ON t.table_name = c.table_name
WHERE
    t.table_name = 't_tbspace' AND
    t.table_schema = 'public';。

DROP TABLESPACE t_tbspace;。
DROP TABLESPACE test_tbspace_ibd;。