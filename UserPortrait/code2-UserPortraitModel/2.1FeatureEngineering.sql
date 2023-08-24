-- 在本脚本中将实现从openGauss中收集审计日志，并从审计日志中提取用户操作特征
-- Tang Wuguo, 2023-7-24, tangwg@csu.edu.cn


-- 1. 收集日志
CREATE SCHEMA pm;

SELECT * INTO pm.audit_log 
FROM pg_query_audit('2023-07-05 02:01:53','2023-07-09 11:00:00');

CREATE OR REPLACE VIEW pm.log_01 as 
select to_char(al."time", 'YYYY-MM-DD') as date, 
	al."type", al."result" , al."userid" , 
 	al."username" , al."database", al."client_conninfo" , 
  al."object_name", al."detail_info" 
FROM pm.audit_log al 
where username is not null and username not in ('opengauss','[unknown]');


-- 2. 提取各类特征
-- Count user login times per day
CREATE OR REPLACE VIEW pm.query_login as select "date", l.username, coalesce(count(*),0) as "login_count"  
from pm.log_01 l 
where l."type" in ('login_success') 
group by "date", l.username 
order by "date", "login_count" desc;

-- Count sys opetate
CREATE OR REPLACE VIEW pm.quey_sys as select l."date", l.username, coalesce(count(*),0) as "sys_count"  
from pm.log_01 l 
where l."type" in ('set_parameter', 'internal_event', 'grant_role', 'ddl_user') 
group by l."date", l.username 
order by l."date", "sys_count" desc;

-- Count user db operate times per day
CREATE OR REPLACE VIEW pm.query_db as select l."date", l.username, coalesce(count(*),0) as "db_count"  
from pm.log_01 l 
where l."type" in ('ddl_database', 'ddl_schema', 'ddl_table') 
group by l."date", l.username 
order by l."date", "db_count" desc;

-- Tabel insert
CREATE OR REPLACE VIEW pm.query_insert_all as select l."date", l.username, coalesce(count(*),0) as "table_count"  
from pm.log_01 l 
where l."type" in ('dml_action', 'copy_from') 
group by l."date", l.username 
order by l."date", "table_count" desc;
-- Score insert
CREATE OR REPLACE VIEW pm.query_insert_score as select l."date", l.username, coalesce(count(*),0) as "table_count"  
from pm.log_01 l 
where l."type" in ('dml_action', 'copy_from') and object_name in ('score')
group by l."date", l.username 
order by l."date", "table_count" desc;
-- Student insert
CREATE OR REPLACE VIEW pm.query_insert_student as select l."date", l.username, coalesce(count(*),0) as "table_count"  
from pm.log_01 l 
where l."type" in ('dml_action', 'copy_from') and object_name in ('student')
group by l."date", l.username 
order by l."date", "table_count" desc;
-- Teacher insert
CREATE OR REPLACE VIEW pm.query_insert_teacher as select l."date", l.username, coalesce(count(*),0) as "table_count"  
from pm.log_01 l 
where l."type" in ('dml_action', 'copy_from') and object_name in ('teacher')
group by l."date", l.username 
order by l."date", "table_count" desc;

-- Table select
CREATE OR REPLACE VIEW pm.query_sel_info as select l."date", l.username, coalesce(count(*),0) as "tab_sel_uinfo_count"  
from pm.log_01 l 
where l."type" in ('dml_action_select') and l.object_name in ('student', 'teacher', 'course')
group by l."date", l.username 
order by l."date", "tab_sel_uinfo_count" desc;
-- Score select
CREATE OR REPLACE VIEW pm.query_sel_score as select l."date", l.username, coalesce(count(*),0) as "tab_sel_score_count"  
from pm.log_01 l 
where l."type" in ('dml_action_select') and l.object_name in ('score')
group by l."date", l.username 
order by l."date", "tab_sel_score_count" desc;



-- 3. 汇总所有特征
-- Merge All Sub-view
CREATE OR REPLACE VIEW pm.log_refined as 
select t1.date, t1.username, 
	COALESCE(t1.login_count, 0) as login, 
	COALESCE(t2.sys_count, 0) as sys, 
	COALESCE(t3.db_count, 0) as db, 
	COALESCE(t4.table_count, 0) as insert_all, 
	COALESCE(t5.table_count, 0) as insert_score,
 	COALESCE(t6.table_count, 0) as insert_student,
 	COALESCE(t7.table_count, 0) as insert_teacher,
	COALESCE(t8.tab_sel_uinfo_count, 0) as sel_info, 
	COALESCE(t9.tab_sel_score_count, 0) as sel_score  
FROM pm.query_login t1 
LEFT JOIN pm.query_sys t2 ON t1."date" = t2."date" AND t1.username = t2.username 
LEFT JOIN pm.query_db t3 ON t1."date" = t3."date" AND t1.username = t3.username 
LEFT JOIN pm.query_insert_all t4 ON t1."date" = t4."date" AND t1.username = t4.username 
LEFT JOIN pm.query_insert_score t5 ON t1."date" = t5."date" AND t1.username = t5.username 
LEFT JOIN pm.query_insert_student t6 ON t1."date" = t6."date" AND t1.username = t6.username 
LEFT JOIN pm.query_insert_teacher t7 ON t1."date" = t7."date" AND t1.username = t7.username 
LEFT JOIN pm.query_sel_info t8 ON t1."date" = t8."date" AND t1.username = t8.username 
LEFT JOIN pm.query_sel_score t9 ON t1."date" = t9."date" AND t1.username = t9.username
ORDER BY t1.date, t1.username;

