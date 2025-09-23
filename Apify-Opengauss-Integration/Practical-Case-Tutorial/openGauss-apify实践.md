# 使用 Apify 和 openGauss 构建企业级 RAG 问答系统：实战教程

本文将通过一个完整的实例，向您展示如何利用 Apify 强大的云端数据抓取与集成能力，结合 openGauss 数据库的向量存储功能，快速构建一个针对 openGauss 官方文档的 RAG 问答机器人。

**Apify 是一个云端 Web 抓取和自动化平台。** 它允许开发者轻松地从任何网站提取数据，或将任何网站的工作流程自动化。其核心是 **"Actors"**——可以执行任意任务的无服务器云程序。

Apify 平台的优势在于：
*   **简化数据采集**：您无需关心基础设施、IP 轮换、浏览器指纹等复杂问题，Apify 会为您处理好一切。
*   **丰富的 Actor 市场**：Apify Store 中有大量预构建好的 Actors，例如通用的网站内容爬虫（Website Content Crawler）、搜索引擎爬虫等，可以开箱即用。
*   **强大的集成能力**：Apify 可以轻松地将采集到的数据推送到各种数据库、API 或云存储中，实现了从数据源到目的地的无缝衔接。

在本教程中，我们将使用两个关键的 Apify Actors：
1.  **`apify/website-content-crawler`**: 用于抓取 openGauss 官网的网页内容。
2.  **`wyswyz/opengauss-integration`**: 用于将抓取到的内容进行处理（分块、生成向量），并自动存入 openGauss 数据库。


## **前置准备工作**

1.  **Apify 账户**：注册一个 [Apify 账户](https://apify.com/) 并获取您的 API Token。
2.  **OpenAI 账户**：获取您的 [OpenAI API Key](https://platform.openai.com/api-keys)，用于内容向量化和问答生成。
3.  **openGauss 数据库**：您需要一个的 openGauss 数据库实例，并且支持向量数据库功能

### 使用docker部署openGauss
**获取镜像** ：
```bash
$ docker pull opengauss/opengauss-server:latest
```
**查看镜像状态**
```bash
$ docker images

REPOSITORY                   TAG                 IMAGE ID            CREATED             SIZE
opengauss/opengauss-server   latest              9763e8b26794        6 months ago        1.68GB
```
**运行容器**
```bash
$ docker run --name opengauss --privileged=true -d -e GS_PASSWORD=YourPassoword -p 8888:5432 opengauss/opengauss-server:latest
```
**验证容器运行状态**
```bash
$ docker ps

CONTAINER ID        IMAGE                               COMMAND                  CREATED             STATUS              PORTS                    NAMES
c5f0b44adf9a        opengauss/opengauss-server:latest   "entrypoint.sh gauss…"   7 weeks ago         Up 7 weeks          0.0.0.0:8888->5432/tcp   opengauss
```
至此，已经成功用docker部署openGauss数据库

**配置环境变量**：为了安全和方便，建议将密钥和数据库连接信息配置为环境变量。
  ```bash
  export APIFY_API_TOKEN="YOUR-APIFY-TOKEN"
  export OPENAI_API_KEY="YOUR-OPENAI-API-KEY"
  export OPENGAUSS_HOST="your-db-host"
  export OPENGAUSS_PORT="your-db-port"
  export OPENGAUSS_USER="your-db-user"
  export OPENGAUSS_PASSWORD="your-db-password"
  export OPENGAUSS_DBNAME="your-db-name"
  export OPENGAUSS_TABLE_NAME="opengauss_docs"
  ```

**Python 环境**：安装必要的库。
```bash
pip install apify-client langchain-core langchain-opengauss langchain-openai
```

## **数据采集 - 使用 Apify 网站内容爬虫**

这是 RAG 流程的第一步：获取知识。我们使用 Apify 预构建的 `website-content-crawler` Actor 来抓取 openGauss 官网的内容。

```python
import os
from apify_client import ApifyClient

# ... [环境变量加载] ...

client = ApifyClient(APIFY_API_TOKEN)

print("Starting Apify's Website Content Crawler")
print("Crawling will take some time")

# 调用 Actor 开始抓取
actor_call = client.actor(actor_id="apify/website-content-crawler").call(
    run_input={
        "maxCrawlPages": 10,
        "startUrls": [{"url": "https://opengauss.org/"}]
    }
)

print("Actor website content crawler has finished")
print(actor_call)
```

website-content-crawler的爬取行为可以通过run_input进行定制，我们选择爬取openGauss的主页。为了方便演示，设置最大爬取页数为10。等待actor爬取完成。

```bash
2025-09-22T13:16:41.429Z INFO  PlaywrightCrawler: Finished! Total 9 requests: 9 succeeded, 0 failed. 
```


## **数据处理与入库 - 使用 Apify openGauss 集成 Actor**

现在我们有了原始的网页数据，下一步是将其处理并存入 openGauss 向量数据库。这正是 `opengauss-integration` Actor 的用武之地。它会自动完成**分块 (Chunking)、向量化 (Embedding) 和存储 (Storing)** 这一系列繁琐的工作。

首先配置 openGauss 数据库的参数，并设置我们需要的字段以及分块、向量化等参数，所有参数信息可以参考[actor详情页](https://apify.com/wyswyz/opengauss-integration)

```python
# 准备 openGauss 集成 Actor 的输入参数
opengauss_integration_inputs = {
    # 数据库连接信息
    "opengaussHost": OPENGAUSS_HOST,
    "opengaussPort": OPENGAUSS_PORT,
    "opengaussUser": OPENGAUSS_USER,
    "opengaussPassword": OPENGAUSS_PASSWORD,
    "opengaussDBname": OPENGAUSS_DBNAME,
    "opengaussTableName": OPENGAUSS_TABLE_NAME,

    # 数据源和处理配置
    "datasetId": actor_call["defaultDatasetId"],
    "datasetFields": ["text"], 
    "performChunking": True, 
    "chunkSize": 2000,
    "chunkOverlap": 200,

    # 向量化配置
    "embeddingsProvider": "OpenAI",
    "embeddingsApiKey": OPENAI_API_KEY,
    "embeddingsConfig": {
        "model": "text-embedding-3-small",
    },
    
    # 其他可选配置
    "deltaUpdatesPrimaryDatasetFields": ["url"],
    "expiredObjectDeletionPeriodDays": 7,
}
```

接下来我们调用`wyswyz/opengauss-integration`将数据存储到openGauss数据库中

```python
print("Starting Apify's OpenGauss Integration")
# 调用 openGauss 集成 Actor
actor_call = client.actor("wyswyz/opengauss-integration").call(run_input=opengauss_integration_inputs)
print("Apify's OpenGauss Integration has finished")
print(actor_call)
```
当这个 Actor 运行完毕后，您的 openGauss 数据库中指定的表（`opengauss_docs`）就已经包含了处理好的知识数据和对应的向量。

## **构建 RAG 应用 - 使用 LangChain 进行问答**

现在我们的知识库已经准备就绪，可以使用 LangChain 来构建一个问答应用了。

```python
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import PromptTemplate
from langchain_core.runnables import RunnablePassthrough
from langchain_opengauss import OpenGauss, OpenGaussSettings
from langchain_openai import ChatOpenAI, OpenAIEmbeddings

# ... [接上文] ...

print("Question answering using OpenGauss database")

# 1. 配置 openGauss 连接
embeddings = OpenAIEmbeddings(model="text-embedding-3-small")
config = OpenGaussSettings(
    host=OPENGAUSS_HOST,
    port=OPENGAUSS_PORT,
    user=OPENGAUSS_USER,
    password=OPENGAUSS_PASSWORD,
    database=OPENGAUSS_DBNAME,
    table_name=OPENGAUSS_TABLE_NAME,
    embedding_dimension=384,
    index_type="HNSW",
    distance_strategy="cosine",
)
# 初始化 LangChain 的 openGauss 向量存储
vector_store = OpenGauss(embedding=embeddings, config=config)

# 2. 定义 Prompt 模板
prompt = PromptTemplate(
    input_variables=["context", "question"],
    template="Use the following pieces of retrieved context to answer the question. If you don't know the answer, "
    "just say that you don't know. \nQuestion: {question} \nContext: {context} \nAnswer:",
)

# 3. 定义文档格式化函数
def format_docs(docs):
    return "\n\n".join(doc.page_content for doc in docs)

# 4. 构建 RAG 链 (Chain)
rag_chain = (
    {
        "context": vector_store.as_retriever() | format_docs,
        "question": RunnablePassthrough(),
    }
    | prompt
    | ChatOpenAI(model="gpt-5-mini", temperature=1)
    | StrOutputParser()
)

# 5. 提问与回答
question = "什么是openGauss？"

print("Question:", question)
print("Answer:", rag_chain.invoke(question))
```

结果如下
```bash
Question: 什么是openGauss？
Answer: openGauss 是一个开源数据库及其社区（即 openGauss 社区）。该项目面向企业级应用，倡导开源开放、社区协作，提供完善的技术文档和实践案例（如资源池化、告警机制、容灾集群、一主两备部署、线程池与 RDMA 指导、在 openEuler 上安装、与 OpenStack 集成等），并通过 meetup 等线下/线上活动推动生态繁荣。
```

# 总结

通过本教程，我们利用 Apify 和 openGauss 成功构建了一个从数据采集到智能问答的端到端 RAG 应用。

此集成支持增量更新，仅更新已更改的数据。这种方法减少了不必要的嵌入计算和存储操作，使其适用于搜索和检索增强生成。

更多有关 Apify-openGauss 集成的信息，请参考集成[README文件](https://apify.com/wyswyz/opengauss-integration)。