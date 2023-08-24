-- 在这里我们将使用openGauss中的DB4AI模块对用户的画像
-- 需要注意只有企业版的openGauss才具有AI特性，如果是轻量版请安装最新的企业版软件


-- 1. 整理数据
-- 为了适应模型的输入要求，需要对原始数据进行进一步修改
CREATE OR REPLACE VIEW pm.log_train as 
SELECT row_number() OVER () AS id, "date", username,
    dense_rank() OVER (ORDER BY username) AS name_id,
    login::double precision, sys::double precision, db::double precision, 
	insert_all::double precision, insert_score::double precision, insert_student::double precision, insert_teacher::double precision, 
	sel_info::double precision, sel_score::double precision
   FROM log_refined
  ORDER BY log_refined."date", dense_rank() OVER (ORDER BY log_refined.username);


-- 2. 创建模型
-- 模型m0，没有使用insert_student, insert_teacher特征
CREATE MODEL log_m0 
USING multiclass 
FEATURES login, sys, db, insert_all, insert_score, sel_info, sel_score 
TARGET name_id 
FROM log_train 
WITH classifier="logistic_regression";
-- 模型m1
CREATE MODEL log_m1 
USING multiclass 
FEATURES login, sys, db, insert_score, insert_student, insert_teacher, sel_info, sel_score 
TARGET name_id 
FROM log_train 
WITH classifier="logistic_regression", batch_size=5;

-- 查看数据库中所有模型
select modelname, createtime, processedtuples,iterations,modeltype, outputtype 
from gs_model_warehouse limit 20;

--  检查模型log_m1的详细参数信息
SELECT gs_explain_model('log_m1');


-- 3. 模型预测
SELECT id, 
PREDICT BY log_m1 
	(FEATURES login, sys, db, insert_score, insert_student, insert_teacher, sel_info, sel_score)
 	as "PREDICT", 
name_id as "LABEL" 
INTO temp_pred_train
FROM log_train;

SELECT id, 
PREDICT BY log_m1 
	(FEATURES login, sys, db, insert_score, insert_student, insert_teacher, sel_info, sel_score)
 	as "PREDICT", 
name_id as "LABEL" 
INTO temp_pred_test
FROM log_test;


-- 4. 模型评估
-- 计算分类结果的准确率
SELECT
    COUNT(*) AS total_count,
    SUM(CASE WHEN "PREDICT"= "LABEL" THEN 1 ELSE 0 END) AS correct_count,
    CASE
        WHEN COUNT(*) = 0 THEN 0.0
        ELSE (SUM(CASE WHEN "PREDICT" = "LABEL" THEN 1 ELSE 0 END)::FLOAT / COUNT(*)) * 100.0
    END AS accuracy
FROM temp_pred_train;


-- 5. 模型的应用
-- 5.1 用户画像
--  选择各特征下四分位值作为比较的阈值
CREATE VIEW pm.log_25 AS
SELECT
    CASE WHEN PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY login) = 0 THEN 1 ELSE PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY login) END AS login_25,
    CASE WHEN PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY sys) = 0 THEN 1 ELSE PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY sys) END AS sys_25,
    CASE WHEN PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY db) = 0 THEN 1 ELSE PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY db) END AS db_25,
    CASE WHEN PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY insert_all) = 0 THEN 1 ELSE PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY insert_all) END AS insert_all_25,
    CASE WHEN PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY insert_score) = 0 THEN 1 ELSE PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY insert_score) END AS insert_score_25,
    CASE WHEN PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY sel_info) = 0 THEN 1 ELSE PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY sel_info) END AS sel_info_25,
    CASE WHEN PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY sel_score) = 0 THEN 1 ELSE PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY sel_score) END AS sel_score_25
FROM
    pm.log_refined;

-- 比较各个样本，得用户标签
SELECT
    date,
    username,
    array_remove(ARRAY[
        CASE WHEN login > login_25 THEN '活跃' ELSE NULL END,
        CASE WHEN sys > sys_25 THEN '系统维护员' ELSE NULL END,
        CASE WHEN db > db_25 THEN '数据库维护员' ELSE NULL END,
        CASE WHEN insert_all > insert_all_25 THEN '维护数据表' ELSE NULL END,
        CASE WHEN insert_score > insert_score_25 THEN '维护成绩表' ELSE NULL END,
        CASE WHEN sel_info > sel_info_25 THEN '查询多' ELSE NULL END,
        CASE WHEN sel_score > sel_score_25 THEN '关注成绩' ELSE NULL END
    ], NULL) AS labels
FROM
    log_refined, (SELECT * FROM log_25 LIMIT 1) AS quartiles;

-- 5.2 识别危险用户
SELECT id, username, date,
PREDICT BY log_m1 (FEATURES login, sys, db, insert_score, insert_student, insert_teacher, sel_info, sel_score)
 	as "PREDICT",
name_id as "LABEL"
FROM log_test2  where "PREDICT"!="LABEL";