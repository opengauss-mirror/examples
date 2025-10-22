"""
LlamaStack 客户端模块
==================
处理与LlamaStack服务的交互，包括模型查询、会话管理和聊天功能。
"""

import traceback
from llama_stack_client import Agent, LlamaStackClient

class LlamaClient:
    """LlamaStack客户端封装类"""
    
    def __init__(self, server_url, knowledge_base=None):
        """
        初始化LlamaStack客户端
        
        Args:
            server_url: LlamaStack服务的URL
            knowledge_base: KnowledgeBase 实例
        """
        self.server_url = server_url
        self.client = LlamaStackClient(base_url=server_url)
        self.agent = None
        self.last_session_id = None
        self.last_message = None
        self.knowledge_base = knowledge_base
    
    def get_models_info(self):
        """
        获取并格式化可用模型信息
        
        Returns:
            str: 格式化的模型信息字符串
        """
        try:
            models = self.client.models.list()
            
            if not models:
                return "未找到任何模型。请确保LlamaStack服务正在运行。"
            
            # 分类模型
            llm_models = [m for m in models if m.model_type == "llm"]
            embedding_models = [m for m in models if m.model_type == "embedding"]
            
            # 构建信息字符串
            info = f"找到 {len(models)} 个可用模型:\n\n"
            
            if llm_models:
                info += "=== LLM模型 ===\n"
                for i, model in enumerate(llm_models, 1):
                    info += f"{i}. ID: {model.identifier}\n"
                    info += f"   提供者: {model.provider_id}\n"
                    info += f"   元数据: {model.metadata}\n\n"
            else:
                info += "未找到任何LLM模型\n\n"
                
            if embedding_models:
                info += "=== 嵌入模型 ===\n"
                for i, model in enumerate(embedding_models, 1):
                    info += f"{i}. ID: {model.identifier}\n"
                    info += f"   提供者: {model.provider_id}\n"
                    info += f"   嵌入维度: {model.metadata.get('embedding_dimension', '未知')}\n\n"
            else:
                info += "未找到任何嵌入模型\n"
            
            return info
            
        except Exception as e:
            return f"获取模型列表时出错: {str(e)}"
    
    def create_agent(self, model_id, vector_db_ids):
        """
        创建对话代理
        
        Args:
            model_id: LLM模型ID
            vector_db_ids: 向量数据库ID列表
            
        Returns:
            tuple: (成功标志, 状态消息)
        """
        try:
            self.agent = Agent(
                self.client,
                model=model_id,
                instructions="You are a helpful assistant who answers questions based on the provided context.",
                tools=[
                    {
                        "name": "builtin::rag/knowledge_search",
                        "args": {"vector_db_ids": vector_db_ids},
                    }
                ],
            )
            return True, "对话代理创建成功"
        except Exception as e:
            return False, f"对话代理创建失败: {str(e)}"
    
    def chat_with_system(self, message, history):
        """
        与系统进行对话
        
        Args:
            message: 用户消息
            history: 聊天历史
            
        Returns:
            list: 更新后的聊天历史
        """
        if not self.agent:
            if not self.knowledge_base.is_initialized:
                history.append({"role": "user", "content": message})
                history.append({"role": "assistant", "content": "系统未初始化，请先在'系统设置'标签中点击'初始化系统'按钮。"})
                return history
                
            # 如果知识库已初始化但代理未创建，尝试自动创建
            llm_models = [m for m in self.client.models.list() if m.model_type == "llm"]
            if not llm_models:
                history.append({"role": "user", "content": message})
                history.append({"role": "assistant", "content": "错误：未找到任何LLM模型。"})
                return history
                
            success, status = self.create_agent(
                llm_models[0].identifier, 
                [self.knowledge_base.vector_db_id]
            )
            if not success:
                history.append({"role": "user", "content": message})
                history.append({"role": "assistant", "content": f"创建对话代理失败: {status}"})
                return history
        
        # 保存最后一条消息，用于可能的重新生成
        self.last_message = message
        
        # 创建新的历史记录
        new_history = list(history) if history else []
        new_history.append({"role": "user", "content": message})
        new_history.append({"role": "assistant", "content": ""})
        
        try:
            # 创建会话
            self.last_session_id = self.agent.create_session(f"session_{hash(message) % 10000}")
            
            # 发送消息并获取流式响应
            response = self.agent.create_turn(
                messages=[{"role": "user", "content": message}],
                session_id=self.last_session_id,
                stream=True,
            )
            
            # 处理流式响应
            full_response = ""
            print("--- LlamaClient: Waiting for response stream ---")
            for chunk in response:
                print(f"--- LlamaClient: Received chunk: {chunk}")
                try:
                    # 正确的路径来提取流式文本
                    if (hasattr(chunk, 'event') and 
                        hasattr(chunk.event, 'payload') and 
                        hasattr(chunk.event.payload, 'delta') and 
                        hasattr(chunk.event.payload.delta, 'text')):
                        
                        text = chunk.event.payload.delta.text
                        if text:
                            full_response += text
                            # 过滤掉工具调用信息再显示给用户
                            filtered_response = self.filter_tool_calls(full_response)
                            new_history[-1]["content"] = filtered_response
                            yield new_history
                except AttributeError:
                    # 忽略不包含文本信息的 chunk
                    pass

            print(f"--- LlamaClient: Stream finished. Full response: '{full_response}' ---")
            filtered_response = self.filter_tool_calls(full_response)
            print(f"--- LlamaClient: Filtered response: '{filtered_response}' ---")
            # 如果流处理完后没有内容，再显示错误
            if not full_response:
                new_history[-1]["content"] = "未能获取到响应，请检查模型设置。"
                yield new_history
                
        except Exception as e:
            error_message = f"处理回答时出错: {str(e)}\n{traceback.format_exc()[:300]}"
            new_history[-1]["content"] = error_message
            yield new_history
        
        return new_history
    
    def regenerate_response(self, history):
        """
        重新生成最后一条响应
        
        Args:
            history: 当前聊天历史
            
        Returns:
            list: 更新后的聊天历史
        """
        if not history or len(history) < 2:
            return history
            
        if not self.last_message or not self.agent:
            return history
        
        # 移除最后的用户消息和助手回复
        history = history[:-2]
        
        # 重新生成回复
        return self.chat_with_system(self.last_message, history)
    
    def filter_tool_calls(self, text):
        """
        过滤掉回复中的工具调用信息
        
        Args:
            text: 原始响应文本
            
        Returns:
            str: 过滤后的文本
        """
        import re
        # 匹配形如 [knowledge_search(query="xxx")] 的工具调用
        filtered_text = re.sub(r'\[.*?\]', '', text)
        return filtered_text