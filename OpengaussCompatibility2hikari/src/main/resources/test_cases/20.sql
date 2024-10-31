--CREATE TRIGGER

--创建源表及触发表
CREATE TABLE IF NOT EXISTS test_trigger_src_tbl(id1 INT, id2 INT, id3 INT);。
CREATE TABLE IF NOT EXISTS test_trigger_des_tbl(id1 INT, id2 INT, id3 INT);。

--创建触发器函数
CREATE OR REPLACE FUNCTION tri_insert_func() RETURNS TRIGGER AS
           $$
           DECLARE
           BEGIN
                   INSERT INTO test_trigger_des_tbl VALUES(NEW.id1, NEW.id2, NEW.id3);
                   RETURN NEW;
           END
           $$ LANGUAGE PLPGSQL;。

CREATE OR REPLACE FUNCTION tri_update_func() RETURNS TRIGGER AS
           $$
           DECLARE
           BEGIN
                   UPDATE test_trigger_des_tbl SET id3 = NEW.id3 WHERE id1=OLD.id1;
                   RETURN OLD;
           END
           $$ LANGUAGE PLPGSQL;。

CREATE OR REPLACE FUNCTION TRI_DELETE_FUNC() RETURNS TRIGGER AS
           $$
           DECLARE
           BEGIN
                   DELETE FROM test_trigger_des_tbl WHERE id1=OLD.id1;
                   RETURN OLD;
           END
           $$ LANGUAGE PLPGSQL;。

--创建INSERT触发器
CREATE TRIGGER insert_trigger
           BEFORE INSERT ON test_trigger_src_tbl
           FOR EACH ROW
           EXECUTE PROCEDURE tri_insert_func();。

--创建UPDATE触发器
CREATE TRIGGER update_trigger
           AFTER UPDATE ON test_trigger_src_tbl  
           FOR EACH ROW
           EXECUTE PROCEDURE tri_update_func();。

--创建DELETE触发器
CREATE TRIGGER delete_trigger
           BEFORE DELETE ON test_trigger_src_tbl
           FOR EACH ROW
           EXECUTE PROCEDURE tri_delete_func();。

--执行INSERT触发事件并检查触发结果
INSERT INTO test_trigger_src_tbl VALUES(100,200,300);。
SELECT * FROM test_trigger_src_tbl;。
SELECT * FROM test_trigger_des_tbl;。

--执行UPDATE触发事件并检查触发结果
UPDATE test_trigger_src_tbl SET id3=400 WHERE id1=100;。
SELECT * FROM test_trigger_src_tbl;。
SELECT * FROM test_trigger_des_tbl;。

--执行DELETE触发事件并检查触发结果
DELETE FROM test_trigger_src_tbl WHERE id1=100;。
SELECT * FROM test_trigger_src_tbl;。
SELECT * FROM test_trigger_des_tbl;。

--修改触发器
ALTER TRIGGER delete_trigger ON test_trigger_src_tbl RENAME TO delete_trigger_renamed;。

--禁用insert_trigger触发器
ALTER TABLE test_trigger_src_tbl DISABLE TRIGGER insert_trigger;。

--禁用当前表上所有触发器
ALTER TABLE test_trigger_src_tbl DISABLE TRIGGER ALL;。

--删除触发器
DROP TRIGGER IF EXISTS insert_trigger ON test_trigger_src_tbl;。
DROP TRIGGER IF EXISTS update_trigger ON test_trigger_src_tbl;。
DROP TRIGGER IF EXISTS delete_trigger_renamed ON test_trigger_src_tbl;。


--mysql兼容数据库
--创建触发器定义用户
create user if not exists test_user password 'Gauss@123';。
--创建原表及触发表
create table if not exists test_mysql_trigger_src_tbl (id INT);。
create table if not exists test_mysql_trigger_des_tbl (id INT);。
create table if not exists animals (id INT, name CHAR(30));。
create table if not exists food (id INT, foodtype VARCHAR(32), remark VARCHAR(32), time_flag TIMESTAMP);。
--创建MySQL兼容definer语法触发器
create definer=test_user trigger trigger1
					after insert on test_mysql_trigger_src_tbl
					for each row
					begin 
    				 insert into test_mysql_trigger_des_tbl values(1);
					end;。
--创建MySQL兼容trigger_order语法触发器
create trigger animal_trigger1
 					after insert on animals
					for each row
					begin
    				 insert into food(id, foodtype, remark, time_flag) values (1,'ice cream', 'sdsdsdsd', now());
					end;。
--创建MySQL兼容FOLLOWS触发器
create trigger animal_trigger2
					after insert on animals
					for each row
					follows animal_trigger1
					begin
    				 insert into food(id, foodtype, remark, time_flag) values (2,'chocolate', 'sdsdsdsd', now());
					end;。
create trigger animal_trigger3
          after insert on animals
          for each row
          follows animal_trigger1
          begin
              insert into food(id, foodtype, remark, time_flag) values (3,'cake', 'sdsdsdsd', now());
          end;。
create trigger animal_trigger4
          after insert on animals
          for each row
          follows animal_trigger1
          begin
              insert into food(id, foodtype, remark, time_flag) values (4,'sausage', 'sdsdsdsd', now());
          end;。
--执行insert触发事件并检查触发结果
insert into animals (id, name) values(1,'lion');。
select * from animals;。
select id, foodtype, remark from food;。
--创建MySQL兼容PROCEDES触发器
create trigger animal_trigger5
          after insert on animals
          for each row
          precedes animal_trigger3
          begin
              insert into food(id, foodtype, remark, time_flag) values (5,'milk', 'sdsds', now());
          end;。
create trigger animal_trigger6
          after insert on animals
          for each row
          precedes animal_trigger2
          begin
              insert into food(id, foodtype, remark, time_flag) values (6,'strawberry', 'sdsds', now());
          end;。
--执行insert触发事件并检查触发结果
--WARNING:  re-compile function 'animal_trigger1_animals_inlinefunc_1' due to strict mode.
--WARNING:  re-compile function 'animal_trigger4_animals_inlinefunc_1' due to strict mode.
--WARNING:  re-compile function 'animal_trigger3_animals_inlinefunc_1' due to strict mode.
--WARNING:  re-compile function 'animal_trigger2_animals_inlinefunc_1' due to strict mode.
insert into animals (id, name) values(2, 'dog');。
select * from animals;。
select id, foodtype, remark from food;。
--创建MySQL兼容if not exists语法触发器
create trigger if not exists animal_trigger1
          after insert on animals
          for each row
          begin
              insert into food(id, foodtype, remark, time_flag) values (1,'ice cream', 'sdsdsdsd', now());
          end;
          /。
--mysql兼容删除触发器语法
drop trigger if exists animal_trigger1;。
drop trigger if exists animal_trigger1;。
--在指定模式下创建、重命名、删除触发器语法，触发器的模式需要与表模式相同
create schema if not exists testscm;。
create table if not exists food (id int, foodtype varchar(32), remark varchar(32), time_flag timestamp);。
create table if not exists testscm.animals_scm (id int, name char(30));。
-- 在指定模式下创建触发器
create trigger testscm.animals_trigger
    after insert on testscm.animals_scm
    for each row
    begin
        insert into food(id, foodtype, remark, time_flag) values (1,'bamboo', 'healthy', now());
    end;
    /。
create trigger if not exists testscm.animals_trigger
    after insert on testscm.animals_scm
    for each row
    begin
        insert into food(id, foodtype, remark, time_flag) values (1,'bamboo', 'healthy', now());
    end;。
-- 重命名指定模式下的触发器
-- 由于重命名触发器不支持修改触发器所属模式，因此新触发器名不支持携带模式名
alter trigger testscm.animals_trigger on testscm.animals_scm rename to animals_trigger_new;。
-- 删除指定模式下的触发器
drop trigger if exists testscm.animals_trigger_new;。
drop trigger if exists testscm.animals_trigger_new;。
-- 当删除触发器不指定所在表时，若存在多个同名触发器，不支持通过指定触发器的模式名来确定所在表以及触发器，如下示例：
create schema if not exists testscm;。
create table if not exists food (id int, foodtype varchar(32), remark varchar(32), time_flag timestamp);。
create table if not exists animals (id int, name char(30));。
create table if not exists testscm.animals_scm (id int, name char(30));。

drop TRIGGER if exists testscm.animals_trigger;。
drop TRIGGER if exists animal_trigger2;。
drop TRIGGER if exists animal_trigger3;。
drop TRIGGER if exists animal_trigger4;。
drop TRIGGER if exists animal_trigger5;。
drop TRIGGER if exists animal_trigger6;。
drop TRIGGER if exists trigger1;。

drop table if exists test_mysql_trigger_des_tbl cascade;。
drop table if exists test_mysql_trigger_src_tbl cascade;。
drop table if exists test_trigger_des_tbl cascade;。
drop table if exists test_trigger_src_tbl cascade;。
drop table if exists food cascade;。
drop table if exists animals cascade;。

drop schema if exists testscm cascade;。
drop schema if exists test_user cascade;。

drop user if exists test_user cascade;。