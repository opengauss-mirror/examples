# finRAG 示例

这个示例展示了如何使用 PrivateGPT 处理 finRAG 金融数据集。finRAG 是一个包含金融问答对的数据集，可以用于训练和测试金融领域的 RAG (Retrieval-Augmented Generation) 系统。

## 功能特性

- 自动下载 finRAG 数据集从 Hugging Face
- 解析 Parquet 格式的金融数据
- 将数据上传到 PrivateGPT 进行向量化和存储
- 支持 OpenGauss 数据库作为向量存储后端

## 前置要求

- Docker 和 Docker Compose
- Python 3.10+ (如果本地运行)
- uv (Python 包管理器)

## 快速开始

### 1. 启动服务

首先启动 PrivateGPT 和相关服务：

```bash
# 在项目根目录下运行
docker compose up -d
```

这将启动以下服务：
- OpenGauss 数据库 (端口 5432)
- PrivateGPT 服务 (端口 8001)
- Ollama 服务 (端口 11434)

### 2. 等待服务就绪

等待所有服务启动完成，特别是 OpenGauss 数据库的健康检查通过。

### 3. 下载并上传数据

进入 finRAG 示例目录并运行数据下载和上传脚本：

```bash
cd examples/finRAG
uv run python download_dataset.py
```

这个脚本会：
1. 从 Hugging Face 下载 finRAG 数据集
2. 解析 `rag_text.parquet` 文件
3. 将前 10 条记录转换为文本格式
4. 通过 PrivateGPT API 上传到向量数据库

## 配置说明

### 环境变量

脚本使用以下默认配置：
- API 地址: `http://localhost:8001`
- 数据集: `parsee-ai/finRAG`
- 处理记录数: 10 条 (可在代码中修改)

### 修改处理数量

要处理更多数据，编辑 `download_dataset.py` 文件中的最后一行：

```python
if __name__ == "__main__":
    ingest_finRAG_to_privategpt(n=100)  # 修改这里的数字
```

## 数据格式

finRAG 数据集包含以下字段：
- `question`: 金融相关问题
- `context`: 相关上下文信息
- `answer`: 对应的答案

每条记录会被转换为以下格式的文本文件：
```
QUESTION:
[问题内容]

CONTEXT:
[上下文内容]

ANSWER:
[答案内容]
```

## API 端点

上传完成后，你可以通过以下 API 端点与 PrivateGPT 交互：

- 健康检查: `GET http://localhost:8001/health`
- 文档列表: `GET http://localhost:8001/v1/ingest/list`
- 搜索文档: `POST http://localhost:8001/v1/completions`

## 故障排除

### 常见问题

1. **容器启动失败**
   ```bash
   # 检查容器状态
   docker compose ps
   
   # 查看日志
   docker compose logs private-gpt-ubuntu
   ```

2. **API 连接失败**
   - 确保 PrivateGPT 服务已完全启动
   - 检查端口 8001 是否被占用
   - 等待 OpenGauss 数据库健康检查通过

3. **数据下载失败**
   - 检查网络连接
   - 确保有足够的磁盘空间
   - 验证 Hugging Face 访问权限

### 清理资源

停止并清理所有服务：

```bash
docker compose down -v
```

这将停止所有容器并删除相关卷。

## 项目结构

```
examples/finRAG/
├── README.md              # 本文档
├── download_dataset.py    # 数据下载和上传脚本
├── main.py               # 主程序入口
├── pyproject.toml        # Python 项目配置
└── uv.lock              # 依赖锁定文件
```

## 依赖项

- `huggingface-hub`: 从 Hugging Face 下载数据集
- `pandas`: 数据处理
- `pyarrow`: Parquet 文件支持
- `requests`: HTTP 请求
- `tqdm`: 进度条显示
