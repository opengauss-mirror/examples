import os
import random
from unittest.mock import AsyncMock

import numpy as np
import pytest

from llama_stack.apis.inference import EmbeddingsResponse, Inference
from llama_stack.apis.vector_dbs import VectorDB
from llama_stack.apis.vector_io import Chunk, QueryChunksResponse
from llama_stack.providers.remote.vector_io.opengauss.config import (
    OpenGaussVectorIOConfig,
)
from llama_stack.providers.remote.vector_io.opengauss.opengauss import (
    OpenGaussIndex,
    OpenGaussVectorIOAdapter,
)
from llama_stack.providers.utils.kvstore.config import (
    SqliteKVStoreConfig,
)

# 如果未设置所需的环境变量，则跳过此文件中的所有测试
pytestmark = pytest.mark.skipif(
    not all(
        os.getenv(var)
        for var in [
            "OPENGAUSS_HOST",
            "OPENGAUSS_PORT",
            "OPENGAUSS_DB",
            "OPENGAUSS_USER",
            "OPENGAUSS_PASSWORD",
        ]
    ),
    reason="未设置OpenGauss连接环境变量",
)


@pytest.fixture(scope="session")
def embedding_dimension() -> int:
    return 128


@pytest.fixture
def sample_chunks():
    """提供用于测试的示例块列表。"""
    return [
        Chunk(
            content="天空是蓝色的。",
            metadata={"document_id": "doc1", "topic": "自然"},
        ),
        Chunk(
            content="一天一苹果，医生远离我。",
            metadata={"document_id": "doc2", "topic": "健康"},
        ),
        Chunk(
            content="量子计算是一个新的前沿领域。",
            metadata={"document_id": "doc3", "topic": "技术"},
        ),
    ]


@pytest.fixture
def sample_embeddings(embedding_dimension, sample_chunks):
    """为示例块提供确定性的嵌入集。"""
    # 使用固定种子以确保可重现性
    rng = np.random.default_rng(42)
    return rng.random((len(sample_chunks), embedding_dimension), dtype=np.float32)


@pytest.fixture
def mock_inference_api(sample_embeddings):
    """模拟推理API以返回虚拟嵌入。"""
    mock_api = AsyncMock(spec=Inference)
    mock_api.embeddings = AsyncMock(return_value=EmbeddingsResponse(embeddings=sample_embeddings.tolist()))
    return mock_api


@pytest.fixture
def vector_db(embedding_dimension):
    """提供用于注册的示例VectorDB对象。"""
    return VectorDB(
        identifier=f"test_db_{random.randint(1, 10000)}",
        embedding_model="test_embedding_model",
        embedding_dimension=embedding_dimension,
        provider_id="opengauss",
    )


@pytest.fixture
async def opengauss_connection():
    """创建并管理与OpenGauss数据库的连接。"""
    import psycopg2

    conn = psycopg2.connect(
        host=os.getenv("OPENGAUSS_HOST"),
        port=int(os.getenv("OPENGAUSS_PORT")),
        database=os.getenv("OPENGAUSS_DB"),
        user=os.getenv("OPENGAUSS_USER"),
        password=os.getenv("OPENGAUSS_PASSWORD"),
    )
    conn.autocommit = True
    yield conn
    conn.close()


@pytest.fixture
async def opengauss_index(opengauss_connection, vector_db):
    """创建和清理OpenGaussIndex实例的固定装置。"""
    index = OpenGaussIndex(vector_db, vector_db.embedding_dimension, opengauss_connection)
    yield index
    await index.delete()


@pytest.fixture
async def opengauss_adapter(mock_inference_api):
    """设置和拆除OpenGaussVectorIOAdapter的固定装置。"""
    config = OpenGaussVectorIOConfig(
        host=os.getenv("OPENGAUSS_HOST"),
        port=int(os.getenv("OPENGAUSS_PORT")),
        db=os.getenv("OPENGAUSS_DB"),
        user=os.getenv("OPENGAUSS_USER"),
        password=os.getenv("OPENGAUSS_PASSWORD"),
        kvstore=SqliteKVStoreConfig(db_name="opengauss_test.db"),
    )
    adapter = OpenGaussVectorIOAdapter(config, mock_inference_api)
    await adapter.initialize()
    yield adapter
    if adapter.conn and not adapter.conn.closed:
        for db_id in list(adapter.cache.keys()):
            try:
                await adapter.unregister_vector_db(db_id)
            except Exception as e:
                print(f"清理{db_id}时出错: {e}")
    await adapter.shutdown()
    # 清理sqlite数据库文件
    if os.path.exists("opengauss_test.db"):
        os.remove("opengauss_test.db")


class TestOpenGaussIndex:
    async def test_add_and_query_vector(self, opengauss_index, sample_chunks, sample_embeddings):
        """测试添加带有嵌入的块并查询最相似的块。"""
        await opengauss_index.add_chunks(sample_chunks, sample_embeddings)

        # 使用第一个块的嵌入进行查询
        query_embedding = sample_embeddings[0]
        response = await opengauss_index.query_vector(query_embedding, k=1, score_threshold=0.0)

        assert isinstance(response, QueryChunksResponse)
        assert len(response.chunks) == 1
        assert response.chunks[0].content == sample_chunks[0].content
        # 与自身的距离应为0，导致无限大的分数
        assert response.scores[0] == float("inf")


class TestOpenGaussVectorIOAdapter:
    async def test_initialization(self, opengauss_adapter):
        """测试适配器初始化并连接到数据库。"""
        assert opengauss_adapter.conn is not None
        assert not opengauss_adapter.conn.closed

    async def test_register_and_unregister_vector_db(self, opengauss_adapter, vector_db):
        """测试向量数据库的注册和注销。"""
        await opengauss_adapter.register_vector_db(vector_db)
        assert vector_db.identifier in opengauss_adapter.cache

        table_name = opengauss_adapter.cache[vector_db.identifier].index.table_name
        with opengauss_adapter.conn.cursor() as cur:
            cur.execute(
                "SELECT EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = %s);",
                (table_name,),
            )
            assert cur.fetchone()[0]

        await opengauss_adapter.unregister_vector_db(vector_db.identifier)
        assert vector_db.identifier not in opengauss_adapter.cache

        with opengauss_adapter.conn.cursor() as cur:
            cur.execute(
                "SELECT EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = %s);",
                (table_name,),
            )
            assert not cur.fetchone()[0]

    async def test_adapter_end_to_end_query(self, opengauss_adapter, vector_db, sample_chunks):
        """
        测试完整的适配器流程：文本查询 -> 嵌入生成 -> 向量搜索。
        """
        # 1. 注册数据库并插入块。适配器将使用模拟的
        #    inference_api为这些块生成嵌入。
        await opengauss_adapter.register_vector_db(vector_db)
        await opengauss_adapter.insert_chunks(vector_db.identifier, sample_chunks)

        # 2. 用户查询是一个文本字符串。
        query_text = "天空是什么颜色？"

        # 3. 适配器现在将内部调用（模拟的）inference_api
        #    为query_text获取嵌入。
        response = await opengauss_adapter.query_chunks(vector_db.identifier, query_text)

        # 4. 断言
        assert isinstance(response, QueryChunksResponse)
        assert len(response.chunks) > 0

        # 由于模拟的inference_api返回随机嵌入，我们无法
        # 确定性地知道哪个块是"最接近的"。然而，在真实的
        # 集成测试中，使用真实模型，这个断言会更加具体。
        # 对于这个单元测试，我们只确认过程完成并返回数据。
        assert response.chunks[0].content in [c.content for c in sample_chunks]