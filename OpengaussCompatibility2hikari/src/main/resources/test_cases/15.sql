-- PROCEDURE

-- 创建表 graderecord
CREATE TABLE IF NOT EXISTS graderecord
(
  number INTEGER,
  name CHAR(20),
  class CHAR(20),
  grade INTEGER
);。

-- 创建存储过程 insert_data 这个sql语句分隔与其他语句不同
CREATE PROCEDURE insert_data  (param1 INT = 0, param2 CHAR(20),param3 CHAR(20),param4 INT = 0 )
IS
 BEGIN
 INSERT INTO graderecord VALUES(param1,param2,param3,param4);
END;
/。


-- 调用存储过程插入数据
CALL insert_data(param1 := 210101, param2 := 'Alan', param3 := '21.01', param4 := 92);。
CALL insert_data(param1 := 210102, param2 := 'Bob', param3 := '21.01', param4 := 85);。
CALL insert_data(param1 := 210103, param2 := 'Cathy', param3 := '21.01', param4 := 88);。
CALL insert_data(param1 := 210104, param2 := 'David', param3 := '21.01', param4 := 90);。
CALL insert_data(param1 := 210105, param2 := 'Eva', param3 := '21.01', param4 := 95);。
CALL insert_data(param1 := 210106, param2 := 'Frank', param3 := '21.01', param4 := 91);。
CALL insert_data(param1 := 210107, param2 := 'Grace', param3 := '21.01', param4 := 87);。

-- 删除存储过程
DROP PROCEDURE insert_data;。
DROP TABLE IF EXISTS graderecord CASCADE;。
