# openGauss RAG (DataVec) — MVP

## 快速开始 Quick Start

### 1. 启动依赖
```bash
cd docker
docker compose up -d
```

> 初次（可选）：进入 ollama 容器拉取模型：`qwen2:7b`、`nomic-embed-text` 等。
>
> ```bash
> docker exec -it ollama bash
> ollama pull qwen2:7b
> ```

### 2. 安装依赖 + 配置
```bash
pip install -r requirements.txt
cp .env.example .env
# 按需编辑 .env
```

### 3. 初始化数据库
```bash
python scripts/init_db.py
```

### 4. 启动后端
```bash
uvicorn rag.service:app --reload --port 8000
```

### 5. 启动前端
```bash
streamlit run frontend/app.py
```

### 6. 快速导入示例 FAQ
```bash
python scripts/quick_ingest_md.py
```

## 目录
- `rag/`：配置、数据库层、嵌入适配、RAG 流水线与 FastAPI 服务
- `frontend/`：Streamlit 简易前端
- `docker/`：`docker-compose.yaml` 与 BentoML 嵌入服务
- `scripts/`：初始化与示例导入脚本

## 说明
- 嵌入后端在 `.env` 切换：`EMBED_BACKEND=hf|cohere|bento`
- openGauss 需使用 **DataVec** 镜像（已在 compose 中指定）
- 生成端默认使用本地 Ollama，可按需改为云端大模型 API
