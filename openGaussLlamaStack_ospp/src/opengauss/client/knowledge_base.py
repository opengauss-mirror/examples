"""
知识库模块
========
处理向量数据库注册和知识文档注入等操作。
"""

from llama_stack_client import RAGDocument

class KnowledgeBase:
    """知识库管理类"""
    
    def __init__(self, llama_client, vector_db_id):
        """
        初始化知识库
        
        Args:
            llama_client: LlamaClient实例
            vector_db_id: 向量数据库ID
        """
        self.llama_client = llama_client
        self.vector_db_id = vector_db_id
        self.is_initialized = False
        self.embedding_model_id = None
        self.embedding_dimension = None
    
    def initialize_system(self, document_text, provider_id="faiss", chunk_size=256):
        """
        初始化整个系统，包括向量数据库和知识文档注入
        
        Args:
            document_text: 知识文档内容
            provider_id: 向量数据库提供者ID
            chunk_size: 文档分块大小
            
        Returns:
            str: 初始化状态信息
        """
        client = self.llama_client.client
        
        # 1. 获取模型
        try:
            models = client.models.list()
            llm_models = [m for m in models if m.model_type == "llm"]
            embedding_models = [m for m in models if m.model_type == "embedding"]
            
            if not llm_models:
                return "错误: 未找到任何 LLM 模型!"
            
            if not embedding_models:
                return "错误: 未找到任何 embedding 模型!"
            
            model_id = llm_models[0].identifier
            em = embedding_models[0]
            self.embedding_model_id = em.identifier
            self.embedding_dimension = em.metadata.get("embedding_dimension", 384)
            
            status = f"选择的 LLM 模型: {model_id}\n"
            status += f"选择的 embedding 模型: {self.embedding_model_id}\n"
            status += f"嵌入维度: {self.embedding_dimension}\n"
            
        except Exception as e:
            return f"获取模型时出错: {str(e)}"
        
        # 2. 设置向量数据库
        try:
            client.vector_dbs.register(
                vector_db_id=self.vector_db_id,
                embedding_model=self.embedding_model_id,
                embedding_dimension=int(self.embedding_dimension),
                provider_id=provider_id,
            )
            status += f"向量数据库注册成功 (provider: {provider_id})\n"
        except Exception as e:
            status += f"向量数据库注册错误 (可能已存在): {str(e)}\n"
        
        # 3. 注入知识文档
        if document_text.strip():
            try:
                document = RAGDocument(
                    document_id=f"document_{hash(document_text) % 10000}",
                    content=document_text,
                    mime_type="text/plain",
                    metadata={},
                )
                
                client.tool_runtime.rag_tool.insert(
                    documents=[document],
                    vector_db_id=self.vector_db_id,
                    chunk_size_in_tokens=chunk_size,
                )
                status += f"文档注入成功! (分块大小: {chunk_size} tokens)\n"
            except Exception as e:
                status += f"文档注入失败: {str(e)}\n"
                return status
        
        # 4. 创建代理
        success, message = self.llama_client.create_agent(
            model_id, 
            [self.vector_db_id]
        )
        status += message + "\n"
        
        if success:
            self.is_initialized = True
        
        return status