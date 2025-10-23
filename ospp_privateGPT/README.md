# openGauss 向量数据库集成 PrivateGPT

## 项目简介

本项目是 **openGauss 向量数据库集成 PrivateGPT** 的完整实现，旨在完成 PrivateGPT 与 openGauss 的私有化部署集成，实现敏感数据本地化存储管理，输出安全增强方案及实施文档。

### 技术领域
- **Database**: openGauss 向量数据库
- **AI**: RAG/LLMs 大语言模型
- **编程语言**: Python, Java
- **部署技术**: Docker 容器化

## 核心功能

### 1. openGauss 本地存储模块
- **文档向量化全流程隔离**: 实现敏感文档的本地化向量存储
- **安全增强方案**: 提供包含审计机制的安全文档管理
- **私有化部署**: 支持金融/医疗等敏感领域的私有化案例

### 2. PrivateGPT 集成
- **文档问答系统**: 支持多种格式文档的智能问答
- **向量化存储**: 使用 openGauss 作为向量数据库后端
- **多模型支持**: 集成 Ollama 服务，支持多种开源大语言模型
- **私有化部署**: 确保数据安全和隐私保护

## 技术架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Private-GPT   │    │     Ollama      │    │   openGauss     │
│   (Web UI)      │◄──►│   (AI 推理)     │◄──►│  (向量数据库)   │
│   Port: 8001    │    │   Port: 11434   │    │   Port: 5432    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │     Traefik     │
                    │   (负载均衡)     │
                    │   Port: 8080    │
                    └─────────────────┘
```

### 组件说明
- **Private-GPT**: 核心应用，处理文档解析、向量化和问答逻辑
- **openGauss**: 向量数据库，存储文档向量和元数据，支持审计机制
- **Ollama**: AI 推理服务，提供大语言模型能力
- **Traefik**: 反向代理，提供负载均衡和路由功能

## 快速开始

### 环境要求
- Docker 和 Docker Compose
- 至少 8GB 内存
- 至少 50GB 磁盘空间

### 1. 克隆项目
```bash
git clone https://gitcode.com/opengauss/examples.git
cd examples/ospp_privateGPT
```

### 2. 启动服务
```bash
# 启动默认配置（推荐）
docker-compose up -d

# 启动 CUDA 版本（需要 NVIDIA GPU）
docker-compose --profile ollama-cuda up -d
```

### 3. 验证部署
```bash
# 查看服务状态
docker-compose ps

# 检查 openGauss 连接
docker-compose exec opengauss psql -U gaussdb -d postgres -c "SELECT version();"

# 检查 PrivateGPT 服务
curl http://localhost:8001/health
```

### 4. 访问服务
- **PrivateGPT Web 界面**: http://localhost:8001
- **Traefik 管理界面**: http://localhost:8080
- **openGauss 数据库**: localhost:5432

## 项目产出

### 1. 开发成果
- ✅ openGauss 本地存储模块
- ✅ 文档向量化全流程隔离实现
- ✅ 金融/医疗领域私有化案例
- ✅ 包含审计机制的安全文档
- ✅ 代码提交至社区仓库

### 2. 技术文档
- ✅ Docker 构建部署文档
- ✅ openGauss 集成配置指南
- ✅ 安全增强方案文档
- ✅ 私有化部署最佳实践

## 配置说明

### 环境变量
```bash
# .env 文件配置
HF_TOKEN=your_huggingface_token_here
PGPT_IMAGE=zylonai/private-gpt
PGPT_TAG=0.6.2

# openGauss 配置
PGPT_OPENGAUSS_API_HOST=opengauss-pg
PGPT_OPENGAUSS_API_PORT=5432
PGPT_OPENGAUSS_API_DATABASE=postgres
PGPT_OPENGAUSS_API_USER=gaussdb
PGPT_OPENGAUSS_API_PASSWORD=MyStrongPass$123
PGPT_OPENGAUSS_API_SCHEMA_NAME=private_gpt
```

### 数据持久化
- `./local_data`: Private-GPT 数据目录
- `./models`: Ollama 模型目录
- `opengauss_data`: openGauss 数据库数据卷

## 安全特性

### 1. 数据隔离
- 所有数据存储在本地 openGauss 数据库中
- 支持敏感数据的完全本地化处理
- 无外部网络数据传输

### 2. 审计机制
- 完整的操作日志记录
- 数据访问审计跟踪
- 安全事件监控

### 3. 访问控制
- 基于角色的访问控制
- 数据加密存储
- 安全连接配置

## 故障排除

### 1. 服务启动问题
```bash
# 查看详细日志
docker-compose logs -f private-gpt-ubuntu
docker-compose logs -f opengauss

# 重启服务
docker-compose restart
```

### 2. 数据库连接问题
```bash
# 检查 openGauss 状态
docker-compose exec opengauss bash
# 在容器内执行
gs_ctl status -D /var/lib/opengauss/data
```

### 3. 端口冲突
```bash
# 检查端口占用
lsof -i :8001
lsof -i :5432
lsof -i :11434
```

# 输出案例
TODO: examples


