# Letta-OpenGauss 跨平台部署指南

## 🚀 一键部署

```bash
# 1. 克隆项目
git clone https://github.com/william4s/letta-openGauss.git
cd letta-openGauss

# 2. 复制环境配置
cp .env.example .env

# 3. 启动服务栈
docker-compose -f docker-compose.opengauss.yml up -d
```

## 🌐 网络配置说明

### 方案1: host.docker.internal (推荐)

**适用场景**: 所有平台（Windows、macOS、Linux）
**配置方式**: 使用默认的 `.env.example` 配置

```bash
# .env 文件配置
OPENAI_API_BASE=http://host.docker.internal:8000/v1
BGE_API_BASE=http://host.docker.internal:8003/v1
VLLM_API_BASE=http://host.docker.internal:8000/v1
EMBEDDING_API_BASE=http://host.docker.internal:8003/v1
```

**优点**:
- ✅ 跨平台兼容，无需修改配置
- ✅ Docker 自动解析为宿主机IP
- ✅ 适合团队协作和CI/CD

### 方案2: 具体IP地址

**适用场景**: 网络环境复杂或需要远程访问
**配置方式**: 获取宿主机IP并更新配置

```bash
# 1. 获取宿主机IP
hostname -I | awk '{print $1}'

# 2. 更新 .env 文件
OPENAI_API_BASE=http://YOUR_HOST_IP:8000/v1
BGE_API_BASE=http://YOUR_HOST_IP:8003/v1
# ... 其他配置
```

**优点**:
- ✅ 明确的网络配置
- ✅ 支持远程服务访问
- ❌ 需要手动配置IP

### 方案3: 网络模式配置

**适用场景**: 需要容器直接使用宿主机网络

```yaml
# docker-compose.opengauss.yml 中添加
services:
  letta-server:
    network_mode: "host"  # 使用宿主机网络
    # 移除 ports 配置，因为使用宿主机网络
```

## 🔧 服务依赖

### 必需服务

在启动 Letta-OpenGauss 之前，确保以下服务在宿主机上运行：

1. **LLM 服务** (端口 8000)
```bash
# 示例：启动 vLLM 服务
python -m vllm.entrypoints.openai.api_server \
  --model /path/to/your/llm/model \
  --port 8000
```

2. **BGE 嵌入服务** (端口 8003)
```bash
# 示例：启动 BGE 服务
python -m vllm.entrypoints.openai.api_server \
  --model /path/to/bge-m3 \
  --port 8003
```

### 服务健康检查

```bash
# 检查 LLM 服务
curl http://localhost:8000/v1/models

# 检查 BGE 服务
curl http://localhost:8003/v1/models

# 检查 Letta 服务
curl http://localhost:8283/v1/health
```

## 🐛 故障排除

### 问题1: 容器无法访问宿主机服务

**症状**: "All connection attempts failed"

**解决方案**:
1. 确认服务在宿主机上正常运行
2. 检查防火墙设置
3. 尝试不同的网络配置方案

### 问题2: host.docker.internal 不工作

**症状**: DNS 解析错误

**解决方案**:
```bash
# 使用具体IP地址
hostname -I | awk '{print $1}'
# 更新 .env 文件中的端点配置
```

### 问题3: OpenGauss 连接失败

**症状**: 数据库认证或连接错误

**解决方案**:
1. 检查密码复杂度是否符合 OpenGauss 要求
2. 确认用户权限设置正确
3. 查看容器日志：`docker logs letta-opengauss-db`

## 📊 验证部署

```bash
# 1. 检查容器状态
docker ps --filter name=letta

# 2. 运行示例脚本
cd letta/examples
python memory_block_rag.py

# 3. 预期输出
# ✅ 智能体创建成功
# ✅ PDF文档解析完成
# ✅ 问答功能正常
```

## 🔄 更新和维护

```bash
# 更新代码
git pull origin main

# 重新构建容器
docker-compose -f docker-compose.opengauss.yml build

# 重启服务
docker-compose -f docker-compose.opengauss.yml restart
```

## 📝 环境变量参考

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `OPENAI_API_BASE` | `http://host.docker.internal:8000/v1` | LLM API 端点 |
| `BGE_API_BASE` | `http://host.docker.internal:8003/v1` | 嵌入模型 API 端点 |
| `LETTA_PG_HOST` | `opengauss` | OpenGauss 主机名 |
| `LETTA_PG_USER` | `gaussdb` | OpenGauss 用户名 |
| `LETTA_PG_PASSWORD` | `OpenGauss@123` | OpenGauss 密码 |

## 🤝 贡献

如果遇到问题或有改进建议，欢迎提交 Issue 或 Pull Request！