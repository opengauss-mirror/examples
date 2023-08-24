# openGauss中的AI模块调研
*Tang Wuguo,   tangwg@csu.edu.cn*

这篇博客中对openGauss中的AI模块进行介绍，并且以iris鸢尾花数据集为例介绍AI模块的使用.

openGauss将AI与数据库结合，其中的AI特性大致可分为AI4DB和DB4AI两个部分:

- **AI4DB**就是指用人工智能技术优化数据库的性能，从而获得更好地执行表现；也可以通过人工智能的手段实现自治、免运维等。主要包括自调优、自诊断、自安全、自运维、自愈等子领域；
- **DB4AI**就是指打通数据库到人工智能应用的端到端流程，通过数据库来驱动AI任务，统一人工智能技术栈，达到开箱即用、高性能、节约成本等目的。例如通过SQL-like语句实现推荐系统、图像检索、时序预测等功能，充分发挥数据库的高并行、列存储等优势，既可以避免数据和碎片化存储的代价，又可以避免因信息泄漏造成的安全风险；

## DB4AI概要
在我们的用户操作模型的构建中，主要使用DB4AI模块，借助其中的类似SQL语句的形式，直接在openGauss数据库中对数据进行建模、训练及预测。
![DB4AI中的关键字](https://cdn.nlark.com/yuque/0/2023/png/21528568/1688912742558-8b17a5b9-7835-4494-9a26-98e102e82bf6.png#averageHue=%23a9baa8&clientId=ua651ef91-cbbb-4&from=paste&height=398&id=ua8d13ce8&originHeight=497&originWidth=956&originalType=binary&ratio=1.25&rotation=0&showTitle=true&size=33658&status=done&style=none&taskId=ubfbc2031-bc2f-4586-b0e7-21bd33e12bd&title=DB4AI%E4%B8%AD%E7%9A%84%E5%85%B3%E9%94%AE%E5%AD%97&width=764.8 "DB4AI中的关键字")
![DB4AI支持的算法](https://cdn.nlark.com/yuque/0/2023/png/21528568/1688913278733-f973add3-8df8-48f5-bcac-f34e01b79085.png#averageHue=%23eaeacd&clientId=ua651ef91-cbbb-4&from=paste&height=594&id=uf6558da4&originHeight=743&originWidth=930&originalType=binary&ratio=1.25&rotation=0&showTitle=true&size=41287&status=done&style=none&taskId=u5538221a-4ea2-4e48-9f5a-fa7db6b3f45&title=DB4AI%E6%94%AF%E6%8C%81%E7%9A%84%E7%AE%97%E6%B3%95&width=744 "DB4AI支持的算法")
![算法对应的超参数](https://cdn.nlark.com/yuque/0/2023/png/21528568/1688913513261-294838fc-164e-4ff4-8158-1e4e601cbfa1.png#averageHue=%23efefd0&clientId=ua651ef91-cbbb-4&from=paste&height=545&id=ud2629f78&originHeight=681&originWidth=915&originalType=binary&ratio=1.25&rotation=0&showTitle=true&size=119740&status=done&style=none&taskId=ua0478dd5-804b-422f-8fb7-bdd6758d875&title=%E7%AE%97%E6%B3%95%E5%AF%B9%E5%BA%94%E7%9A%84%E8%B6%85%E5%8F%82%E6%95%B0&width=732 "算法对应的超参数")

## 鸢尾花分类模型实践
下面我们已鸢尾花数据集为例，使用openGauss中的DB4AI模块搭建两种模型：二分类模型，多分类模型。同时还利用内置的函数`_gs_explain_model_`查看模型的详细信息，`_PREDICT BY_`关键字来进行模型的推理。
### Iris数据集
iris数据集是ML中最经典的数据集之一，其中共包括150个样本，对于每个样本有花萼长度、花萼宽度、花瓣长度、花瓣宽度4个特征，我们需要依据这4个特征区分3种花型：山鸢尾、变色鸢尾还是维吉尼亚鸢尾。
![image.png](https://cdn.nlark.com/yuque/0/2023/png/21528568/1689905148831-3470aab1-3871-46de-b762-313ec710f821.png#averageHue=%23050403&clientId=u1918a2c5-2fd8-4&from=paste&height=359&id=ub78bca1f&originHeight=359&originWidth=895&originalType=binary&ratio=1&rotation=0&showTitle=false&size=19999&status=done&style=none&taskId=udfdfd0ed-c801-4b51-bc69-9970db879a1&title=&width=895)
### 划分数据集
我们对iris数据集随机打乱，选择80%的数据作为训练集，剩下的作为测试集。
训练集用于模型的训练，模型训练好后可以分别在计算训练集和测试集上的准确率。
```sql
CREATE VIEW iris_random AS
SELECT *,
       ROW_NUMBER() OVER () AS row_num,
       COUNT(*) OVER () AS total_rows
FROM iris_1
ORDER BY RANDOM();

CREATE VIEW iris_train AS
SELECT *
FROM iris_random
WHERE row_num <= total_rows * 0.8;

CREATE VIEW iris_test AS
SELECT *
FROM iris_random
WHERE row_num > total_rows * 0.8;

```

### 二分类模型
假定我们现在的目标是要区分是否为山鸢尾，yes or no，这是一个二分类问题，这里我们选择使用逻辑回归来解决。

**CREATE MODEL**
```sql
CREATE MODEL iris_m1 USING logistic_regression 
FEATURES sepal_length, sepal_width,petal_length,petal_width 
TARGET target_id < 2 
FROM iris_train 
WITH batch_size=20;
```
执行上面的脚本后正常会输出：`MODEL CREATED. PROCESSED 1`
表示模型构建好了，下面测试模型的预测效果
通过查询`gs_model_warehouse`表可以看到数据库中的所有模型
![image.png](https://cdn.nlark.com/yuque/0/2023/png/21528568/1689908710289-e0e5598d-1663-41b5-b548-30d7634e82e7.png#averageHue=%230d0a06&clientId=u1918a2c5-2fd8-4&from=paste&height=295&id=ue1c9ecea&originHeight=295&originWidth=1351&originalType=binary&ratio=1&rotation=0&showTitle=false&size=34756&status=done&style=none&taskId=uda9291a2-b250-4773-8739-b50fea9be9f&title=&width=1351)
通过使用`gs_explain_model`函数可以查看指定模型的详细参数：
`select gs_explain_model('iris_m1');`
![image.png](https://cdn.nlark.com/yuque/0/2023/png/21528568/1689908757667-b92f9a0f-d725-409a-90ae-bb4db56a779b.png#averageHue=%23020201&clientId=u1918a2c5-2fd8-4&from=paste&height=625&id=ud46d24ff&originHeight=625&originWidth=1706&originalType=binary&ratio=1&rotation=0&showTitle=false&size=31306&status=done&style=none&taskId=u1a8174e3-9c8f-41b9-af61-b5e3906bbda&title=&width=1706)
![image.png](https://cdn.nlark.com/yuque/0/2023/png/21528568/1689908832575-392f5e6e-c01f-44a5-a688-27c4bc363131.png#averageHue=%23040301&clientId=u1918a2c5-2fd8-4&from=paste&height=447&id=uced53a58&originHeight=447&originWidth=1084&originalType=binary&ratio=1&rotation=0&showTitle=false&size=16641&status=done&style=none&taskId=ub641b4e5-7a7c-406a-a3e4-4ae393f7b6d&title=&width=1084)

**PREDICT BY**
```sql
SELECT id, 
PREDICT BY iris_m1 (FEATURES sepal_length,sepal_width,petal_length,petal_width) as "PREDICT", 
target_id < 2 as "LABEL" 
FROM iris_train limit 20;
```
```sql
SELECT id, 
PREDICT BY iris_m1 (FEATURES sepal_length,sepal_width,petal_length,petal_width) as "PREDICT", 
target_id < 2 as "LABEL" 
FROM iris_test;
```
![image.png](https://cdn.nlark.com/yuque/0/2023/png/21528568/1689908938404-268daf45-26ef-40f0-8494-d7c8b73ca7e6.png#averageHue=%23010101&clientId=u1918a2c5-2fd8-4&from=paste&height=525&id=u863f6f89&originHeight=525&originWidth=1672&originalType=binary&ratio=1&rotation=0&showTitle=false&size=21268&status=done&style=none&taskId=ub1d91d50-8181-444e-a495-bd86b1b5776&title=&width=1672)
![image.png](https://cdn.nlark.com/yuque/0/2023/png/21528568/1689910200441-04103459-4afb-4908-ab3e-a6c5fe19840d.png#averageHue=%23020201&clientId=u1918a2c5-2fd8-4&from=paste&height=654&id=u9332e713&originHeight=654&originWidth=964&originalType=binary&ratio=1&rotation=0&showTitle=false&size=24319&status=done&style=none&taskId=u537e2315-adb6-4ffb-bbd7-aae94f1d4e7&title=&width=964)

**计算分类准确率**
准备率 = 分类正确的数量 / 样本总数
```sql
SELECT id, 
PREDICT BY iris_m1 (FEATURES sepal_length,sepal_width,petal_length,petal_width) as "PREDICT", 
target_id < 2 as "LABEL" 
INTO temp_pred 
FROM iris_train limit 20;

SELECT
    COUNT(*) AS total_count,
    SUM(CASE WHEN "PREDICT"= "LABEL" THEN 1 ELSE 0 END) AS correct_count,
    CASE
        WHEN COUNT(*) = 0 THEN 0.0
        ELSE (SUM(CASE WHEN "PREDICT" = "LABEL" THEN 1 ELSE 0 END)::FLOAT / COUNT(*)) * 100.0
    END AS accuracy
FROM temp_pred;
```
![image.png](https://cdn.nlark.com/yuque/0/2023/png/21528568/1689910258939-bcf185ba-f13c-4dd5-b40e-4a13295a087e.png#averageHue=%23050403&clientId=u1918a2c5-2fd8-4&from=paste&height=97&id=uf92bcd78&originHeight=97&originWidth=1236&originalType=binary&ratio=1&rotation=0&showTitle=false&size=4228&status=done&style=none&taskId=u029530e4-fd79-44af-8f9d-1050b76e77c&title=&width=1236)

### 多分类模型
假定现在的任务是给定一个样本要预测是3种花型中的哪一种，这是一个多分类的问题。
```sql
CREATE MODEL iris_m2 USING multiclass 
FEATURES sepal_length, sepal_width,petal_length,petal_width 
TARGET target_id 
FROM iris_train 
WITH classifier="logistic_regression", batch_size=20,max_iterations=300,learning_rate = 1.0;

drop table temp_pred;

SELECT id, 
PREDICT BY iris_m2 (FEATURES sepal_length,sepal_width,petal_length,petal_width) as "PREDICT", 
target_id as "LABEL" 
INTO temp_pred
FROM iris_test;

SELECT
    COUNT(*) AS total_count,
    SUM(CASE WHEN "PREDICT"= "LABEL" THEN 1 ELSE 0 END) AS correct_count,
    CASE
        WHEN COUNT(*) = 0 THEN 0.0
        ELSE (SUM(CASE WHEN "PREDICT" = "LABEL" THEN 1 ELSE 0 END)::FLOAT / COUNT(*)) * 100.0
    END AS accuracy
FROM temp_pred;
```
batch_size=20
![image.png](https://cdn.nlark.com/yuque/0/2023/png/21528568/1689911528931-4a1c0444-7541-4125-bae1-e0f2631ab1cb.png#averageHue=%23060504&clientId=u1918a2c5-2fd8-4&from=paste&height=69&id=ua51023e9&originHeight=69&originWidth=509&originalType=binary&ratio=1&rotation=0&showTitle=false&size=2084&status=done&style=none&taskId=u91ebd585-9a27-4d86-b118-359bb92b9f4&title=&width=509)
![image.png](https://cdn.nlark.com/yuque/0/2023/png/21528568/1689911625210-ad7c89a7-fc76-4f9d-9fc7-2babeabc4b45.png#averageHue=%23040403&clientId=u1918a2c5-2fd8-4&from=paste&height=70&id=u96b5144e&originHeight=70&originWidth=420&originalType=binary&ratio=1&rotation=0&showTitle=false&size=1844&status=done&style=none&taskId=u3280af96-a388-4274-ab16-e767965c57a&title=&width=420)

经过调节超参数batch_size=4，在训练集上的结果可以提升
![image.png](https://cdn.nlark.com/yuque/0/2023/png/21528568/1689911352977-03b556ed-b321-483b-a771-d1f72e062c6f.png#averageHue=%23040403&clientId=u1918a2c5-2fd8-4&from=paste&height=73&id=ue3370894&originHeight=73&originWidth=430&originalType=binary&ratio=1&rotation=0&showTitle=false&size=2000&status=done&style=none&taskId=ud6f60a09-cfa8-429b-b429-da1cb8f80ee&title=%E8%AE%AD%E7%BB%83%E9%9B%86%E4%B8%8A%E7%BB%93%E6%9E%9C&width=430)
![image.png](https://cdn.nlark.com/yuque/0/2023/png/21528568/1689911363647-25e11954-1953-49f2-8519-ecca0a73ae6f.png#averageHue=%23070504&clientId=u1918a2c5-2fd8-4&from=paste&height=65&id=u33bf56ed&originHeight=65&originWidth=491&originalType=binary&ratio=1&rotation=0&showTitle=false&size=1871&status=done&style=none&taskId=u6ee262f6-cdb6-4ed1-a4d0-8ba0e52999f&title=%E6%B5%8B%E8%AF%95%E9%9B%86&width=491)

## 小结
最后总结下使用openGauss中AI模块的流程

1. 处理数据集
   1. 数据集中的每个样本通常包括多个属性列和标签列，建议添加一个id列用于标识每个样本
   2. 划分数据集，在ML中需要在训练集上训练模型，在测试集上评估
   3. 明确属于是哪种任务类型（分类or回归），才能选择后面相应的算法
2. 创建模型
   1. `CREATE MODEL`关键字创建模型
   2. `DROP MODEL xxx` 删除模型
   3. `SELECT gs_explain_model('xxx')` 查看模型详细信息
   4. `SELECT modelname, createtime, processedtuples,iterations,modeltype, outputtype FROM gs_model_warehouse LIMIT 5;`查看数据库中所有模型
3. 模型推理
   1. 通产推理结果应该包含样本的id, 预测值PREDICT, 标签值LABEL
   2. 使用`PREDICT BY`关键字进行模型的推理，得到PREDICT列
4. 模型评估
   1. 可以通过SQL语句计算ACC准确率（for 分类）、MSE均方误差（for 回归）等指标
   2. 也可以将预测的结果导出到CSV文件使用其他工具来分析，`COPY(...)TO '/path' WITH(FORMAT CSV, HEADER)`
5. 模型调参
   1. 通过观察模型在训练集上的结果，我们可以通过调整不同的超参数来创建不同的模型
   2. 不同的模型有这不同的可调参数，可以参考文档中列出的[这张表](https://docs.opengauss.org/zh/docs/5.0.0/docs/AIFeatureGuide/DB4AI-Query-%E6%A8%A1%E5%9E%8B%E8%AE%AD%E7%BB%83%E5%92%8C%E6%8E%A8%E6%96%AD.html#:~:text=%E8%A1%A8%203-,%E8%B6%85%E5%8F%82%E7%9A%84%E9%BB%98%E8%AE%A4%E5%80%BC%E4%BB%A5%E5%8F%8A%E5%8F%96%E5%80%BC%E8%8C%83%E5%9B%B4,-%E7%AE%97%E5%AD%90)。
   3. 当你觉的效果足够满意了，就将这组参数选定作为最终的模型

*参考资料*
[openGauss Doc-Ai特性](https://docs.opengauss.org/zh/docs/5.0.0/docs/AIFeatureGuide/AI%E7%89%B9%E6%80%A7.html)
[openGauss Doc-原生Db4Ai引擎](https://docs.opengauss.org/zh/docs/5.0.0/docs/AIFeatureGuide/%E5%8E%9F%E7%94%9FDB4AI%E5%BC%95%E6%93%8E.html)