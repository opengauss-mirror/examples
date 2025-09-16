"""
LlamaStack 智能聊天应用
=======================
一个基于 LlamaStack 服务的智能聊天应用，支持知识库检索和流式响应。
"""

import gradio as gr
from config import AppConfig
from llama_client import LlamaClient
from knowledge_base import KnowledgeBase

def main():
    """主函数，初始化并启动应用"""
    # 初始化配置
    config = AppConfig()
    
    # 初始化客户端
    client = LlamaClient(config.server_url)
    
    # 初始化知识库
    knowledge_base = KnowledgeBase(client, config.vector_db_id)
    
    # 将知识库实例设置到客户端，以供后续使用
    client.knowledge_base = knowledge_base
    
    # 创建应用界面
    app = create_app(client, knowledge_base, config)
    
    # 启动应用
    # 启动应用
    app.queue().launch(
        server_name=config.server_name,
        server_port=config.server_port,
        share=config.share_link,
        debug=config.debug_mode
    )

def create_app(client, knowledge_base, config):
    """创建Gradio应用界面"""
    with gr.Blocks(title=config.app_title, theme=gr.themes.Soft()) as app:
        gr.Markdown(f"# 🦙 {config.app_title}")
        
        with gr.Tab("系统设置"):
            with gr.Row():
                with gr.Column():
                    # 模型信息区
                    model_info_btn = gr.Button("获取可用模型", variant="primary")
                    model_info = gr.Textbox(label="模型信息", lines=10)
                    
                    # 知识库设置区
                    gr.Markdown("### 知识库设置")
                    document_text = gr.Textbox(
                        label="知识文档",
                        placeholder="请输入知识文档内容...",
                        lines=8,
                        value=config.default_document
                    )
                    
                    with gr.Row():
                        provider_dropdown = gr.Dropdown(
                            config.vector_db_providers, 
                            label="向量数据库类型",
                            value=config.default_provider
                        )
                        chunk_size = gr.Slider(
                            minimum=50, 
                            maximum=1000, 
                            value=256, 
                            step=50,
                            label="分块大小（tokens）"
                        )
                    
                    # 初始化按钮
                    init_btn = gr.Button("初始化系统", variant="primary")
                    init_status = gr.Textbox(label="初始化状态", lines=6)
        
        with gr.Tab("对话"):
            chatbot = gr.Chatbot(height=500, type='messages')
            
            # 创建一个行布局，包含文本框和发送按钮
            with gr.Row():
                msg = gr.Textbox(
                    label="发送消息",
                    placeholder="请输入您的问题...",
                    lines=2,
                    show_label=False
                )
                send_btn = gr.Button("发送", variant="primary")
            
            with gr.Row():
                clear = gr.Button("清空对话")
                regenerate = gr.Button("重新生成")
        
        # 绑定事件处理函数
        model_info_btn.click(client.get_models_info, inputs=[], outputs=model_info)
        
        init_btn.click(
            knowledge_base.initialize_system,
            inputs=[document_text, provider_dropdown, chunk_size],
            outputs=init_status
        )
        
        def send_message(message, history):
            """处理发送消息并清空输入框"""
            if not message:
                return

            response_generator = client.chat_with_system(message, history)
            for new_history in response_generator:
                yield new_history, ""
        
        # 绑定聊天相关事件
        msg.submit(send_message, [msg, chatbot], [chatbot, msg])
        send_btn.click(send_message, [msg, chatbot], [chatbot, msg])
        clear.click(lambda: [], None, chatbot, queue=False)
        regenerate.click(
            client.regenerate_response,
            [chatbot],
            [chatbot]
        )
        
    return app

if __name__ == "__main__":
    main()