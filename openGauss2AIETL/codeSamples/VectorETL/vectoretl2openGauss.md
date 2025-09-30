---
title: 'vectorETL对接openGauss的实践'
date: '2025-09-28'
category: 'blog'
tags: ['openGauss社区开发入门']
archives: '2025-09'
author: 'fancy3058'
summary: 'openGauss社区开发入门'
times: '17:30'
---

# 使用 VectorETL 将数据高效加载到 openGauss 中

在本教程中，我们将探讨如何使用专为向量数据库设计的轻量级 ETL 框架 VectorETL 将数据高效地加载到 openGauss 中。VectorETL 简化了从各种来源提取数据的过程，利用人工智能模型将数据转化为向量 Embeddings，并将其存储到 openGauss 中，以便进行快速、可扩展的检索。在本教程结束时，你将拥有一个可正常工作的 ETL 管道，让你轻松集成和管理向量搜索系统。让我们开始吧！

## 准备工作

### 依赖性和环境

```bash
pip install --upgrade vector-etl opengauss-python-driver sentence-transformers
```

VectorETL 支持多种数据源，包括 MySQL、PostgreSQL、本地文件等。在本教程中，我们将以 MySQL 作为数据源示例。

我们将从 MySQL 数据库加载文档。因此，你需要准备 MySQL 连接信息作为环境变量，以便安全访问数据库。此外，我们将使用 SentenceTransformer 的 `all-MiniLM-L6-v2` 嵌入模型为数据生成 embeddings。

```python
import os

os.environ["MYSQL_HOST"] = "your-mysql-host"
os.environ["MYSQL_PORT"] = "3306"
os.environ["MYSQL_USER"] = "your-mysql-username"
os.environ["MYSQL_PASSWORD"] = "your-mysql-password"
os.environ["MYSQL_DATABASE"] = "your-database-name"

os.environ["OPENGauss_HOST"] = "your-opengauss-host"
os.environ["OPENGauss_PORT"] = "5432"
os.environ["OPENGauss_USER"] = "your-opengauss-username"
os.environ["OPENGauss_PASSWORD"] = "your-opengauss-password"
os.environ["OPENGauss_DATABASE"] = "your-vector-database"
```

## 工作流程

### 定义数据源（MySQL）

在本例中，我们从 MySQL 数据库中提取文档。VectorETL 允许我们指定数据库连接信息、查询语句和数据分块参数。

```python
source = {
    "source_data_type": "MySQL",
    "host": os.environ["MYSQL_HOST"],
    "port": int(os.environ["MYSQL_PORT"]),
    "username": os.environ["MYSQL_USER"],
    "password": os.environ["MYSQL_PASSWORD"],
    "database_name": os.environ["MYSQL_DATABASE"],
    "query": "SELECT id, title, content, category FROM documents WHERE status = 'active'",
    "batch_size": 1000,
    "chunk_size": 1000,
    "chunk_overlap": 200,
}
```

### 配置嵌入模型（SentenceTransformer）

设置好数据源后，我们需要定义嵌入模型，将文本数据转换为向量嵌入。在本例中，我们使用 SentenceTransformer 的 `all-MiniLM-L6-v2` 模型，这是一个高效的本地嵌入模型。

```python
embedding = {
    "embedding_model": "SentenceTransformer",
    "model_name": "all-MiniLM-L6-v2",
    "device": "cpu",  # 使用 "cuda" 如果有 GPU
}
```

### 将 openGauss 设置为目标数据库

我们需要将生成的嵌入向量存储在 openGauss 中。在此，我们定义 openGauss 连接参数和表结构配置。

```python
target = {
    "target_database": "openGauss",
    "host": os.environ["OPENGauss_HOST"],
    "port": int(os.environ["OPENGauss_PORT"]),
    "username": os.environ["OPENGauss_USER"],
    "password": os.environ["OPENGauss_PASSWORD"],
    "database_name": os.environ["OPENGauss_DATABASE"],
    "schema": "public",
    "table_name": "document_vectors",
    "vector_dim": 384,  # 384 for all-MiniLM-L6-v2
    "vector_column": "embedding",
    "create_index": True,
}
```

对于 **host** 和 **port**：

将 `host` 设置为本地地址如 `"localhost"`，这是最方便的测试方法，适合开发和原型验证。

如果数据规模较大，可以在服务器上部署 openGauss 集群。在此设置中，请使用服务器地址作为 `host`。

如果您想使用华为云 openGauss 服务或其他云服务商的托管 openGauss，请调整 `host` 和认证信息以匹配云服务的连接参数。

### 指定 Embeddings 的列

现在，我们需要指定数据表中的哪些列应转换为 Embeddings。这样可以确保只处理相关的文本字段，优化效率和存储。

```python
embed_columns = ["title", "content"]
```

## 创建并执行 VectorETL 管道

所有配置就绪后，我们现在要初始化 ETL 管道、设置数据流并执行它。

```python
from vector_etl import create_flow

# 创建 ETL 流程实例
flow = create_flow()

# 配置数据源
flow.set_source(source)

# 配置嵌入模型
flow.set_embedding(embedding)

# 配置目标数据库
flow.set_target(target)

# 指定需要嵌入的列
flow.set_embed_columns(embed_columns)

# 执行流程
flow.execute()
```

## 完整代码示例

```python
import os
from vector_etl import create_flow

# 设置环境变量
os.environ["MYSQL_HOST"] = "localhost"
os.environ["MYSQL_USER"] = "root"
os.environ["MYSQL_PASSWORD"] = "your-mysql-password"
os.environ["MYSQL_DATABASE"] = "documents_db"

os.environ["OPENGauss_HOST"] = "localhost"
os.environ["OPENGauss_USER"] = "gaussdb"
os.environ["OPENGauss_PASSWORD"] = "your-opengauss-password"
os.environ["OPENGauss_DATABASE"] = "vector_db"

# 定义数据源
source = {
    "source_data_type": "MySQL",
    "host": os.environ["MYSQL_HOST"],
    "port": 3306,
    "username": os.environ["MYSQL_USER"],
    "password": os.environ["MYSQL_PASSWORD"],
    "database_name": os.environ["MYSQL_DATABASE"],
    "query": "SELECT id, title, content, category FROM documents WHERE status = 'active'",
    "batch_size": 1000,
    "chunk_size": 1000,
    "chunk_overlap": 200,
}

# 配置嵌入模型
embedding = {
    "embedding_model": "SentenceTransformer",
    "model_name": "all-MiniLM-L6-v2",
    "device": "cpu",
}

# 配置目标数据库
target = {
    "target_database": "openGauss",
    "host": os.environ["OPENGauss_HOST"],
    "port": 5432,
    "username": os.environ["OPENGauss_USER"],
    "password": os.environ["OPENGauss_PASSWORD"],
    "database_name": os.environ["OPENGauss_DATABASE"],
    "schema": "public",
    "table_name": "document_vectors",
    "vector_dim": 384,
    "vector_column": "embedding",
    "create_index": True,
}

# 指定嵌入列
embed_columns = ["title", "content"]

# 创建并执行 ETL 流程
flow = create_flow()
flow.set_source(source)
flow.set_embedding(embedding)
flow.set_target(target)
flow.set_embed_columns(embed_columns)

# 执行流程
flow.execute()

print("ETL 流程执行完成！数据已成功从 MySQL 迁移到 openGauss 向量数据库。")
```

## 验证结果

执行完成后，你可以连接到 openGauss 数据库验证数据是否成功加载：

```sql
-- 连接到 openGauss 数据库
\c vector_db;

-- 检查数据记录数
SELECT COUNT(*) FROM document_vectors;

-- 检查向量维度
SELECT array_length(embedding, 1) as vector_dimensions 
FROM document_vectors 
LIMIT 1;

-- 执行简单的向量相似度搜索
SELECT 
    id,
    title,
    embedding <-> (SELECT embedding FROM document_vectors WHERE id = 1) as distance
FROM document_vectors 
ORDER BY distance 
LIMIT 5;
```

## 性能优化建议

1. **批量大小调整**：根据你的硬件配置调整 `batch_size`，通常 500-2000 之间效果最佳
2. **分块策略**：根据文本长度调整 `chunk_size` 和 `chunk_overlap` 以获得更好的嵌入质量
3. **索引优化**：确保在 openGauss 中为向量列创建了适当的索引
4. **硬件利用**：如果有 GPU，将 `device` 设置为 `"cuda"` 以加速嵌入生成

通过本教程的学习，我们已经成功地建立了一个端到端的 ETL 管道，使用 VectorETL 将文档从 MySQL 数据库转移到 openGauss 向量数据库。VectorETL 的数据源非常灵活，你可以根据自己的具体应用需求选择任何数据源。借助 VectorETL 的模块化设计，你可以轻松扩展这个管道，支持其他数据源、嵌入模型，使其成为人工智能和数据工程工作流的强大工具！