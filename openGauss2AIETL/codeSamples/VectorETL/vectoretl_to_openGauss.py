from vector_etl import create_flow
import os

# 数据源配置 - MySQL
source = {
    "source_data_type": "database",
    "db_type": "mysql",
    "host": "localhost",
    "port": "3306",
    "database_name": "book-v",
    "username": "your_username",
    "password": "your_password",
    "query": "SELECT * FROM book",
    "batch_size": 1000,
    "chunk_size": 1000,
    "chunk_overlap": 0,
}

# 嵌入模型配置 - 本地SentenceTransformer模型
embedding = {
    "embedding_model": "SentenceTransformer",
    "model_name": "all-MiniLM-L6-v2",
    "device": "cpu"  # 使用CPU，如需GPU可改为 "cuda"
}

# 目标数据库配置 - openGauss
target = {
    "target_database": "openGauss",
    "host": "localhost",
    "port": "5432",
    "database_name": "postgres",
    "username": "fancy",
    "password": "fancy@123",
    "schema": "public",
    "table_name": "document_embeddings",
    "vector_dimension": 384,  # all-MiniLM-L6-v2模型的维度是384
    "vector_column": "embedding_vector",
    "create_index": True,  # 自动创建向量索引
    "index_type": "ivfflat"  # 向量索引类型
}

# 指定需要转换为嵌入向量的列
embed_columns = ["info", "theme"]

# 创建并配置ETL流程
flow = create_flow()
flow.set_source(source)
flow.set_embedding(embedding)
flow.set_target(target)
flow.set_embed_columns(embed_columns)

# 执行ETL流程
flow.execute()