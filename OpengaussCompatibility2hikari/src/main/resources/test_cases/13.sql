-- UPDATE

-- 创建学生表 student1
CREATE TABLE IF NOT EXISTS student1
(
   stuno     INT,
   classno   INT
);。

-- 向学生表插入记录
INSERT INTO student1 VALUES(1, 1);。
INSERT INTO student1 VALUES(2, 2);。
INSERT INTO student1 VALUES(3, 3);。

-- 查询学生表中的所有记录
SELECT * FROM student1;。

-- 更新学生表中的班级编号，将班级编号乘以2
UPDATE student1 SET classno = classno * 2;。

-- 查询更新后的学生表中的所有记录
SELECT * FROM student1;。

-- 删除学生表
DROP TABLE IF EXISTS student1 CASCADE;。
