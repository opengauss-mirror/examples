# OpenGauss DataVec 目标连接器

<div align="center">

[![English](https://img.shields.io/badge/Language-English-blue)](openGauss_DataVec_Connector_Document.md)
[![中文](https://img.shields.io/badge/语言-中文-red)](openGauss_DataVec_Connector_Document.zh.md)

[English](openGauss_DataVec_Connector_Document.md) | **中文**

</div>

---

## 目录

- [概述](#概述)
- [前提条件](#前提条件)
- [步骤 1：设置 OpenGauss](#步骤-1设置-opengauss)
- [步骤 2：在 Airbyte 中设置 OpenGauss DataVec 连接器](#步骤-2在-airbyte-中设置-opengauss-datavec-连接器)
- [命名约定](#命名约定)
- [功能特性](#功能特性)
- [数据类型映射](#数据类型映射)
- [配置](#配置)
- [更新日志](#更新日志)

## 概述

本页面指导您完成 OpenGauss DataVec 目标连接器的设置过程。

此过程包含三个部分：
* **处理** - 将单个记录拆分为块，以适应上下文窗口，并决定哪些字段用作上下文，哪些是补充元数据。
* **嵌入** - 使用预训练模型将文本转换为向量表示。目前支持：
  * OpenAI 的 `text-embedding-ada-002`
  * Cohere 的 `embed-english-light-v2.0` 
  * Azure OpenAI 
  * 虚拟 `1536 维随机向量`
  * OpenAI 兼容接口
  * 即将支持：Hugging Face 的 `e5-base-v2`
* **OpenGauss 连接** - 存储向量的位置。通过安装 DataVec 扩展，使用具有 `VECTOR` 数据类型的 OpenGauss 表配置向量存储。

## 前提条件

要使用 OpenGauss DataVec 目标连接器，您需要：

- 根据您要使用的嵌入方法拥有 API 访问权限的账户。
- 支持向量引擎的 openGauss 数据库。请参阅 [OpenGauss DataVec 文档](https://docs.opengauss.org/zh/docs/latest/docs/DataVec/DataVec-Overview.html) 获取安装说明。

您需要以下信息来配置目标连接器：

- **嵌入服务 API 密钥** - 您的嵌入账户的 API 密钥和其他参数，具体取决于您的模型。
- **端口** - 服务器正在监听的端口号。默认为标准端口号 (5432)。
- **用户名**
- **密码**
- **默认模式名称** - 指定要在搜索路径中设置的模式（或用逗号分隔的多个模式）。这些模式将用于解析在此连接上执行的语句中使用的非限定对象名称。
- **数据库** - 数据库名称。默认是连接到与用户名同名的数据库。

### 配置网络访问

确保 Airbyte 可以访问您的 OpenGauss 数据库。如果您的数据库在 VPC 内，您可能需要允许从用于暴露 Airbyte 的 IP 进行访问。

## 步骤 1：设置 OpenGauss

### **权限**

您需要一个具有以下权限的 OpenGauss 用户：

- 可以创建表和写入行
- 可以创建模式，例如：

您可以通过运行以下命令创建这样的用户：

```sql
CREATE USER airbyte_user WITH PASSWORD '<password>';
GRANT CREATE, TEMPORARY ON DATABASE <database> TO airbyte_user;
```

您也可以使用现有用户，但我们强烈建议为 Airbyte 创建专用用户。

## 步骤 2：在 Airbyte 中设置 OpenGauss DataVec 连接器

### 目标数据库

您需要选择现有数据库或创建新数据库，用于存储来自 Airbyte 的同步数据。

## 命名约定

来自 [Postgres SQL 标识符语法](https://www.postgresql.org/docs/9.0/sql-syntax-lexical.html#SQL-SYNTAX-IDENTIFIERS)：

- SQL 标识符和关键字必须以字母（a-z，还包括带变音符号的字母和非拉丁字母）或下划线（_）开头。
- 标识符或关键字中的后续字符可以是字母、下划线、数字（0-9）或美元符号（$）。

  请注意，根据 SQL 标准，标识符中不允许使用美元符号，因此使用它们可能会降低应用程序的可移植性。SQL 标准不会定义包含数字或以下划线开头或结尾的关键字，因此这种形式的标识符可以安全地避免与标准的未来扩展产生可能的冲突。

- 系统使用标识符的不超过 NAMEDATALEN-1 字节；可以在命令中写入更长的名称，但它们将被截断。默认情况下，NAMEDATALEN 是 64，所以最大标识符长度是 63 字节。
- 引用的标识符可以包含任何字符，除了代码为零的字符。（要包含双引号，请写两个双引号。）这允许构造原本不可能的表或列名，例如包含空格或 & 符号的名称。长度限制仍然适用。
- 引用标识符也使其区分大小写，而未引用的名称总是折叠为小写。
- 为了使您的应用程序具有可移植性且不易出错，请对每个名称使用一致的引用（要么总是引用它，要么从不引用它）。

:::info

Airbyte OpenGauss DataVec 目标连接器将通过用下划线替换任何特殊字符来创建原始表和模式，使用未引用的标识符。所有最终表及其对应的列都使用引用的标识符创建，保持大小写敏感性。最终表中的特殊字符被替换为下划线。

:::

1. [登录您的 Airbyte Cloud](https://cloud.airbyte.com/workspaces) 账户。
2. 在左侧导航栏中，点击 **目标连接器**。在右上角，点击 **新建目标连接器**。
3. 在设置目标连接器页面上，输入 OpenGauss DataVec 连接器的名称，并从目标连接器类型下拉菜单中选择 **OpenGauss DataVec**。
4. 输入您的源的名称。
5. 输入处理信息。
6. 输入嵌入信息。
7. 对于 **主机**、**端口** 和 **数据库名称**，输入您的 OpenGauss 数据库的主机名、端口号和名称。
8. 输入 **默认模式**。

:::note

模式名称区分大小写。默认设置 'public' 模式。

:::

7. 对于 **用户** 和 **密码**，输入您在 [步骤 1](#步骤-1设置-opengauss) 中创建的用户名和密码。

## 功能特性

| 功能                        | 是否支持？           | 备注 |
| :----------------------------- | :------------------- | :---- |
| 完全刷新同步              | 是                  |       |
| 增量 - 追加同步      | 是                  |       |
| 增量 - 追加 + 去重 | 是                  |       |

## 数据类型映射

所有指定为元数据字段的字段将存储在文档的元数据对象中，可用于过滤。元数据字段允许以下数据类型：
* 字符串
* 数字（整数或浮点数，转换为 64 位浮点数）
* 布尔值（true、false）
* 字符串列表

所有其他字段将被忽略。

## 配置

### 处理

每个记录将根据"处理"部分中的配置分为文本字段和元字段。所有文本字段连接成单个字符串，然后分割成配置长度的块。如果指定，元数据字段将与嵌入的文本块一起按原样存储。请注意，元数据字段只能用于过滤而不能用于检索，并且必须是字符串、数字、布尔值类型（忽略所有其他值）。请注意，为每个条目保存的元数据总大小有 40kb 的限制。有关配置分块过程的选项使用 [Langchain Python 库](https://python.langchain.com/docs/get_started/introduction)。

在指定文本字段时，您可以使用点符号访问记录中的嵌套字段，例如 `user.name` 将访问 `user` 对象中的 `name` 字段。还可以使用通配符访问对象中的所有字段，例如 `users.*.name` 将访问 `users` 数组所有条目中的所有 `names` 字段。

块长度以 `tiktoken` 库产生的令牌来衡量。最大值为 8191 个令牌，这是 `text-embedding-ada-002` 模型支持的最大长度。

流名称作为元数据字段 `_ab_stream` 添加到每个文档中。如果可用，记录的主键用于标识文档，以避免在索引记录的更新版本时出现重复。它作为 `_ab_record_id` 元数据字段添加。

### 嵌入

连接器可以使用以下嵌入方法之一：

1. **OpenAI** - 使用 [OpenAI API](https://beta.openai.com/docs/api-reference/text-embedding)，连接器将使用具有 **1536 维** 的 `text-embedding-ada-002` 模型生成嵌入。此集成将受到 [OpenAI 嵌入 API 速度](https://platform.openai.com/docs/guides/rate-limits/overview) 的限制。

2. **Cohere** - 使用 [Cohere API](https://docs.cohere.com/reference/embed)，连接器将使用具有 **1024 维** 的 `embed-english-light-v2.0` 模型生成嵌入。

为了测试目的，也可以使用 [虚拟嵌入](https://python.langchain.com/docs/modules/data_connection/text_embedding/integrations/fake) 集成。它将生成随机嵌入，适用于测试数据管道而不产生嵌入成本。

### 索引/数据存储 

- 对于 **主机**、**端口** 和 **数据库名称**，输入您的 OpenGauss 数据库的主机名、端口号和名称。
- 列出 **默认模式**。

所有流将被索引/存储到同名的表中。如果表不存在将创建表。表将具有以下列： 
- `document_id`（字符串）- 文档的唯一标识符，通过附加流模式中的主键创建
- `chunk_id`（字符串）- 块的唯一标识符，通过将块编号附加到 document_id 创建
- `metadata`（变体）- 文档的元数据，以键值对形式存储
- `document_content`（字符串）- 块的文本内容
- `embedding`（向量）- 块的嵌入，以浮点数列表形式存储



> 注意：若表名超过63字节的最大标识符限制，会hash+截断处理

## 更新日志

<details>
  <summary>展开查看</summary>

| 版本 | 日期       | Pull Request                                                  | 主题                                                                                                                                              |
|:--------| :--------- |:--------------------------------------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------|
| 0.1.0   | 2025-09-01 | TBD     | 添加对 openGauss DateVec 作为向量目标连接器的支持。 |

</details>

---

<div align="center">

[🔝 返回顶部](#opengauss-datavec-目标连接器) | [🇺🇸 English Version](openGauss_DataVec_Connector_Document.md)

</div>
