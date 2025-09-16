import pytest

from llama_stack.apis.vector_io import Chunk


@pytest.fixture(scope="session")
def sample_chunks():
    """提供测试用的样本数据块集合。"""
    return [
        Chunk(
            content="Python是一种高级编程语言，强调代码可读性，使程序员能够用比C++或Java更少的代码行表达概念。",
            metadata={"document_id": "doc1"},
        ),
        Chunk(
            content="机器学习是人工智能的一个子集，它使系统能够自动学习并从经验中改进，而无需显式编程，使用统计技术赋予计算机系统在特定任务上逐步提高性能的能力。",
            metadata={"document_id": "doc2"},
        ),
        Chunk(
            content="数据结构对计算机科学至关重要，因为它们提供了高效存储和访问数据的组织方式，通过优化算法实现更快的数据处理，并为更复杂的软件系统提供构建块。",
            metadata={"document_id": "doc3"},
        ),
        Chunk(
            content="神经网络受动物大脑中生物神经网络的启发，使用称为人工神经元的互连节点通过加权连接处理信息，这些连接可以通过迭代学习来识别模式并解决复杂问题。",
            metadata={"document_id": "doc4"},
        ),
    ]


@pytest.fixture(scope="function")
def client_with_empty_registry(client_with_models):
    """提供带有空向量数据库注册表的客户端。"""
    def clear_registry():
        vector_dbs = [vector_db.identifier for vector_db in client_with_models.vector_dbs.list()]
        for vector_db_id in vector_dbs:
            client_with_models.vector_dbs.unregister(vector_db_id=vector_db_id)

    clear_registry()
    yield client_with_models

    # 如果对有状态的服务器实例运行测试，必须在最后一个测试后清理
    clear_registry()


def test_vector_db_retrieve(client_with_empty_registry, embedding_model_id, embedding_dimension):
    """测试检索向量数据库的功能。"""
    # 首先注册一个内存库
    vector_db_id = "test_vector_db"
    client_with_empty_registry.vector_dbs.register(
        vector_db_id=vector_db_id,
        embedding_model=embedding_model_id,
        embedding_dimension=embedding_dimension,
    )

    # 检索内存库并验证其属性
    response = client_with_empty_registry.vector_dbs.retrieve(vector_db_id=vector_db_id)
    assert response is not None
    assert response.identifier == vector_db_id
    assert response.embedding_model == embedding_model_id
    assert response.provider_resource_id == vector_db_id


def test_vector_db_register(client_with_empty_registry, embedding_model_id, embedding_dimension):
    """测试向量数据库的注册与注销功能。"""
    vector_db_id = "test_vector_db"
    client_with_empty_registry.vector_dbs.register(
        vector_db_id=vector_db_id,
        embedding_model=embedding_model_id,
        embedding_dimension=embedding_dimension,
    )

    vector_dbs_after_register = [vector_db.identifier for vector_db in client_with_empty_registry.vector_dbs.list()]
    assert vector_dbs_after_register == [vector_db_id]

    client_with_empty_registry.vector_dbs.unregister(vector_db_id=vector_db_id)

    vector_dbs = [vector_db.identifier for vector_db in client_with_empty_registry.vector_dbs.list()]
    assert len(vector_dbs) == 0


@pytest.mark.parametrize(
    "test_case",
    [
        ("Python与C++和Java有何不同？", "doc1"),
        ("系统如何在没有显式编程的情况下学习？", "doc2"),
        ("为什么数据结构在计算机科学中很重要？", "doc3"),
        ("神经网络的生物学灵感是什么？", "doc4"),
        ("机器学习如何随时间改进？", "doc2"),
    ],
)
def test_insert_chunks(client_with_empty_registry, embedding_model_id, embedding_dimension, sample_chunks, test_case):
    """测试插入数据块并通过不同查询检索相关内容的功能。"""
    vector_db_id = "test_vector_db"
    client_with_empty_registry.vector_dbs.register(
        vector_db_id=vector_db_id,
        embedding_model=embedding_model_id,
        embedding_dimension=embedding_dimension,
    )

    client_with_empty_registry.vector_io.insert(
        vector_db_id=vector_db_id,
        chunks=sample_chunks,
    )

    response = client_with_empty_registry.vector_io.query(
        vector_db_id=vector_db_id,
        query="法国的首都是什么？",
    )
    assert response is not None
    assert len(response.chunks) > 1
    assert len(response.scores) > 1

    query, expected_doc_id = test_case
    response = client_with_empty_registry.vector_io.query(
        vector_db_id=vector_db_id,
        query=query,
    )
    assert response is not None
    top_match = response.chunks[0]
    assert top_match is not None
    assert top_match.metadata["document_id"] == expected_doc_id, f"查询'{query}'应该匹配{expected_doc_id}"


def test_insert_chunks_with_precomputed_embeddings(client_with_empty_registry, embedding_model_id, embedding_dimension):
    """测试使用预计算嵌入向量插入数据块的功能。"""
    vector_io_provider_params_dict = {
        "inline::milvus": {"score_threshold": -1.0},
        "remote::qdrant": {"score_threshold": -1.0},
        "inline::qdrant": {"score_threshold": -1.0},
    }
    vector_db_id = "test_precomputed_embeddings_db"
    client_with_empty_registry.vector_dbs.register(
        vector_db_id=vector_db_id,
        embedding_model=embedding_model_id,
        embedding_dimension=embedding_dimension,
    )

    chunks_with_embeddings = [
        Chunk(
            content="这是一个带有预计算嵌入向量的测试数据块。",
            metadata={"document_id": "doc1", "source": "precomputed", "chunk_id": "chunk1"},
            embedding=[0.1] * int(embedding_dimension),
        ),
    ]

    client_with_empty_registry.vector_io.insert(
        vector_db_id=vector_db_id,
        chunks=chunks_with_embeddings,
    )

    provider = [p.provider_id for p in client_with_empty_registry.providers.list() if p.api == "vector_io"][0]
    response = client_with_empty_registry.vector_io.query(
        vector_db_id=vector_db_id,
        query="预计算嵌入向量测试",
        params=vector_io_provider_params_dict.get(provider, None),
    )

    # 验证顶部结果是否为预期文档
    assert response is not None
    assert len(response.chunks) > 0, (
        f"提供商参数 {provider} = {vector_io_provider_params_dict.get(provider, None)}"
    )
    assert response.chunks[0].metadata["document_id"] == "doc1"
    assert response.chunks[0].metadata["source"] == "precomputed"


# 预期此测试会失败
def test_query_returns_valid_object_when_identical_to_embedding_in_vdb(
    client_with_empty_registry, embedding_model_id, embedding_dimension
):
    """测试当查询与向量数据库中的嵌入向量完全相同时，能否返回有效对象。"""
    vector_io_provider_params_dict = {
        "inline::milvus": {"score_threshold": 0.0},
        "remote::qdrant": {"score_threshold": 0.0},
        "inline::qdrant": {"score_threshold": 0.0},
    }
    vector_db_id = "test_precomputed_embeddings_db"
    client_with_empty_registry.vector_dbs.register(
        vector_db_id=vector_db_id,
        embedding_model=embedding_model_id,
        embedding_dimension=embedding_dimension,
    )

    chunks_with_embeddings = [
        Chunk(
            content="重复内容",
            metadata={"document_id": "doc1", "source": "precomputed"},
            embedding=[0.1] * int(embedding_dimension),
        ),
    ]

    client_with_empty_registry.vector_io.insert(
        vector_db_id=vector_db_id,
        chunks=chunks_with_embeddings,
    )

    provider = [p.provider_id for p in client_with_empty_registry.providers.list() if p.api == "vector_io"][0]
    response = client_with_empty_registry.vector_io.query(
        vector_db_id=vector_db_id,
        query="重复内容",
        params=vector_io_provider_params_dict.get(provider, None),
    )

    # 验证顶部结果是否为预期文档
    assert response is not None
    assert len(response.chunks) > 0
    assert response.chunks[0].metadata["document_id"] == "doc1"
    assert response.chunks[0].metadata["source"] == "precomputed"