# Llama Stack 与 OpenGauss 集成示例

## 项目简介

本项目提供了一个端到端的示例，用于演示如何将 Llama Stack AI 框架与 OpenGauss 数据库进行集成，以构建一个完整的检索增强生成 (RAG) 应用。

该实现的核心亮点在于，它利用了 **OpenGauss 数据库内建的原生向量检索引擎**，而非依赖外部扩展。

此示例包含了一个完整的、可独立运行的 `opengauss` 向量提供商 (`vector_io`)，一个用于演示其功能的 Gradio Web 应用，以及所有必需的 Llama Stack 构建与运行时配置文件。

## 目录
- [前提条件](#前提条件)
- [运行流程](#运行流程)
  - [步骤 1：启动 OpenGauss 数据库](#步骤-1启动-opengauss-数据库)
  - [步骤 2：配置环境变量](#步骤-2配置环境变量)
  - [步骤 3：构建 Llama Stack 分发](#步骤-3构建-llama-stack-分发)
  - [步骤 4：运行 Llama Stack 服务器](#步骤-4运行-llama-stack-服务器)
  - [步骤 5：运行客户端 Gradio 应用](#步骤-5运行客户端-gradio-应用)
- [功能展示](#功能展示)
- [文件结构说明](#文件结构说明)

---

## 前提条件

运行本示例需要以下环境与软件：
- **Docker & Docker Compose:** 用于运行 OpenGauss 数据库容器。
- **Python 3.9+:** Llama Stack 运行所需。
- **Llama Stack:** 需要克隆 Llama Stack 的官方仓库，并根据其 `CONTRIBUTING.md` 完成基础开发环境的设置（包括 `uv`）。
- **Git:** 用于版本控制。

---

## 运行流程

请按照以下步骤执行，以启动并运行本示例。

### 步骤 1：启动 OpenGauss 数据库

本示例使用 Docker Compose 启动一个 OpenGauss 实例。

1.  在本地任意目录下，创建 `docker-compose.yaml` 文件，内容如下：
    ```yaml
    version: '3.8'
    services:
      opengauss-db:
        container_name: opengauss-db-for-llama
        image: opengauss/opengauss:latest
        environment:
          POSTGRES_DB: llama_vectors
          GS_PASSWORD: your_strong_password
        ports:
          - "5432:5432"
        volumes:
          - opengauss_data:/var/lib/opengauss/data
    volumes:
      opengauss_data:
    ```

2.  在该文件目录下，启动容器：
    ```bash
    docker compose up -d
    ```
    *注意：请确保所使用的 OpenGauss 实例已经初始化并启用了其原生向量支持功能。*

### 步骤 2：配置环境变量

在 Llama Stack 的项目根目录下，需创建一个 `.env` 文件来管理配置。

1.  将本示例目录下的 `.env.example` 文件复制为 `.env`。
    ```bash
    cp .env.example .env
    ```

2.  编辑 `.env` 文件，填入实际配置。
    ```env
    # .env
    TOGETHER_API_KEY="sk-your-together-api-key"

    # OpenGauss 数据库连接信息
    # 需与 docker-compose.yaml 中的设置匹配
    OPENGAUSS_HOST=localhost
    OPENGAUSS_PORT=5432
    OPENGAUSS_DB=llama_vectors
    OPENGAUSS_USER=your_username
    OPENGAUSS_PASSWORD=your_strong_password
    ```

### 步骤 3：构建 Llama Stack 分发

此步骤将根据本示例提供的 `build.yaml` 文件，构建一个包含 OpenGauss 提供商所需依赖的 Llama Stack 实例。

在 Llama Stack 的项目根目录下，运行：
```bash
# --distro 参数指向本示例中的构建配置文件
uv run --with llama-stack llama stack build --distro ./path/to/llama-stack-integration/opengauss-demo-build.yaml --image-type venv
```
*(请将 `./path/to/llama-stack-integration/` 替换为本示例目录的实际路径)*

### 步骤 4：运行 Llama Stack 服务器

构建完成后，使用对应的 `run.yaml` 文件来启动 Llama Stack 服务器。

在 Llama Stack 的项目根目录下，运行：
```bash
uv run --env-file .env --with llama-stack llama stack run ./path/to/llama-stack-integration/opengauss-demo-run.yaml
```
*(同样，请替换为本示例目录的实际路径)*

服务器成功启动后，终端将显示 `Uvicorn running on http://...:8321`。请保持此终端窗口运行。

### 步骤 5：运行客户端 Gradio 应用

打开一个**新的终端窗口**，进入 Llama Stack 项目的根目录，并运行本示例提供的 `app.py` 客户端应用。

```bash
uv run --with llama-stack-client,gradio ./path/to/llama-stack-integration/app.py
```
*(同样，请替换为本示例目录的实际路径)*

---

## 功能展示

`app.py` 脚本启动后，会在终端输出一个本地 URL，例如 `Running on local URL: http://127.0.0.1:7860`。

1.  在浏览器中访问此 URL，即可打开由 **Gradio** 生成的 Web 交互界面。
2.  用户可通过界面上的文本框**输入文档内容**，并通过**“注入文档 (Inject Document)”**按钮将其索引到 OpenGauss 数据库中。
3.  文档注入后，用户可在下方的**聊天框**中输入自然语言问题（例如：“How does technology reshape our world?”）。
4.  点击发送后，应用将通过 Llama Stack 和 `opengauss` 提供商，从已注入的文档中检索相关信息，并由大语言模型生成回答，最终呈现在聊天界面上。

**这个可视化的界面清晰地展示了整个 RAG 流程的成功闭环，证明了 OpenGauss 的原生向量能力已经成功地被集成到了 Llama Stack 生态中。**

---

