"""
RAG Demo with openGauss(DataVec) + Ollama (DeepSeek + nomic-embed-text)
本示例完成：下载语料 → 切分 → 向量化入库 → HNSW 相似度检索 → 基于上下文生成答案

⚠ 先决条件 / Prerequisites
1) openGauss 使用“DataVec 镜像”并已启动，提供了 vector 类型与 HNSW 索引
   例如（x86_64）:
     docker pull swr.cn-north-4.myhuaweicloud.com/opengauss-x86-64/opengauss-datavec:latest
     docker run --name og-datavec -d -e GS_PASSWORD=你的密码 -p 8888:5432 \
       -v /home/test/opengauss:/var/lib/opengauss \
       swr.cn-north-4.myhuaweicloud.com/opengauss-x86-64/opengauss-datavec:latest

2) pip 安装:
     pip install ollama psycopg2  # 若编译困难可 pip install psycopg2-binary

3) 本地已启动 Ollama:
     ollama serve &
     ollama pull deepseek-r1
     ollama pull nomic-embed-text

Author: You + ChatGPT
"""

import os
import sys
import json
import time
import math
import typing as T

# ---- 依赖：数据库与大模型客户端 / Deps: DB & LLM client ----
import psycopg2
from psycopg2.extras import execute_values

# Ollama Python SDK
from ollama import embeddings as ollama_embeddings
from ollama import chat as ollama_chat

# ---- 配置区 / Configs ----
# 语料（Markdown）本地路径与下载地址 / Corpus local path & URL
CORPUS_FILE = r"E:\ospp_opengauss\index.md"  # ← 按需修改
CORPUS_URL  = "https://gitee.com/opengauss/website/raw/v2/app/zh/faq/index.md"

# 数据库连接参数 / DB connection
DB_CFG = dict(
    database="postgres",
    user="gaussdb",
    password="asdF@123",      # ← 修改为你的密码
    host="127.0.0.1",
    port="8888",
)

# 表名与嵌入维度 / Table name & embedding dimension
TABLE_NAME = "opengauss_data"
EMB_DIM = 768  # nomic-embed-text 的默认维度通常为 768

# 模型名 / Model names
EMBED_MODEL = "nomic-embed-text"
GEN_MODEL   = "qwen2:7b"

# 检索与生成参数 / Retrieval & generation
TOP_K = 3
SYSTEM_PROMPT = "你是数据库与开源生态方向的中文助理，必须基于已给定上下文回答问题，禁止编造。"


# ---------- 工具函数 / Utilities ----------

def log(msg: str):
    print(msg, flush=True)


def http_get(url: str, timeout: int = 30) -> bytes:
    """下载工具（优先 requests，失败用 urllib）/ HTTP downloader."""
    try:
        import requests  # type: ignore
        resp = requests.get(url, timeout=timeout)
        resp.raise_for_status()
        return resp.content
    except Exception:
        # fallback to urllib
        from urllib.request import urlopen
        with urlopen(url, timeout=timeout) as r:
            return r.read()


def ensure_corpus(path: str = CORPUS_FILE, url: str = CORPUS_URL) -> None:
    """确保本地存在语料文件 / Ensure corpus file exists locally."""
    if not os.path.exists(path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        data = http_get(url)
        with open(path, "wb") as f:
            f.write(data)
        log(f"[corpus] downloaded to: {path}")
    else:
        log(f"[corpus] found: {path}")


def read_and_chunk_md(path: str) -> T.List[str]:
    """
    读取 Markdown 并进行简单切分：
    - 以 '##' 二级标题切分
    - 去除空白与超短片段
    Read Markdown and chunk by '##' headers.
    """
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    parts = [p.strip() for p in content.split("##") if p.strip()]
    # 丢弃长度过短的段落 / drop too-short chunks
    parts = [p for p in parts if len(p) > 30]
    return parts


def embed_text(text: str) -> T.List[float]:
    """
    使用 Ollama 的嵌入模型获取向量 / Get embedding via Ollama.
    返回 Python list[float]
    """
    vec = ollama_embeddings(model=EMBED_MODEL, prompt=text)
    emb = vec["embedding"]
    # 保守起见确保是 float 列表 / ensure float list
    return [float(x) for x in emb]


def to_vector_literal(arr: T.Sequence[float]) -> str:
    """
    将 Python 浮点数组转为 openGauss DataVec 的向量字面量字符串
    Convert Python list[float] to DataVec vector literal string: "[1.0,2.0,...]"
    """
    return "[" + ",".join(f"{float(x):.6f}" for x in arr) + "]"


def connect_db():
    """建立数据库连接 / Get DB connection."""
    return psycopg2.connect(**DB_CFG)


def check_datavec(conn) -> None:
    """
    自检：确认 vector 类型与 HNSW 能力可用
    Self-check: ensure 'vector' type exists and HNSW operator works.
    """
    cur = conn.cursor()
    # 1) check vector type
    cur.execute("SELECT typname FROM pg_type WHERE typname='vector';")
    row = cur.fetchone()
    if not row:
        cur.close()
        raise RuntimeError(
            "当前 openGauss 未检测到 DataVec 的 'vector' 类型。\n"
            "请确认你使用的是 DataVec 镜像并连接到了正确实例。"
        )
    # 2) try <-> operator
    cur.execute("SELECT '[1,2,3]'::vector <-> '[1,2,3]'::vector;")
    _ = cur.fetchone()
    cur.close()


def setup_table(conn, table: str = TABLE_NAME, dim: int = EMB_DIM) -> None:
    """
    创建数据表（id, content, emb）并清空旧表
    Create table with vector column.
    """
    cur = conn.cursor()
    cur.execute(f"DROP TABLE IF EXISTS {table};")
    cur.execute(f"CREATE TABLE {table} (id INT PRIMARY KEY, content TEXT, emb vector({dim}));")
    conn.commit()
    cur.close()
    log(f"[db] table created: {table} (vector({dim}))")


def create_hnsw_index(conn, table: str = TABLE_NAME) -> None:
    """
    创建 HNSW 索引（L2 距离）
    Create HNSW index using L2 distance.
    """
    cur = conn.cursor()
    cur.execute(f"CREATE INDEX ON {table} USING hnsw (emb vector_l2_ops);")
    conn.commit()
    cur.close()
    log(f"[db] HNSW index created on {table}.emb (L2)")


def ingest_paragraphs(conn, paragraphs: T.Sequence[str], table: str = TABLE_NAME) -> None:
    """
    批量入库（向量使用字面量 + ::vector 显式转换，最稳妥）
    Batch insert paragraphs with embeddings.
    """
    cur = conn.cursor()
    rows = []
    for i, p in enumerate(paragraphs):
        e = embed_text(p)
        e_txt = to_vector_literal(e)  # "[...]" 字符串
        rows.append((i, p, e_txt))
        if (i + 1) % 10 == 0:
            log(f"[ingest] inserted {i+1}/{len(paragraphs)}")

    # 逐条参数化插入（执行时 cast 为 vector）
    for rid, content, e_txt in rows:
        cur.execute(
            f"INSERT INTO {table} (id, content, emb) VALUES (%s, %s, %s::vector);",
            (rid, content, e_txt)
        )
    conn.commit()
    cur.close()
    log(f"[ingest] completed: {len(paragraphs)} rows")


def vector_search(conn, question: str, k: int = TOP_K, table: str = TABLE_NAME) -> T.List[str]:
    """
    相似度检索：将 query 向量化 → 字面量 → ::vector → ORDER BY <-> LIMIT k
    Vector similarity search with <-> operator.
    """
    qv = embed_text(question)
    qv_txt = to_vector_literal(qv)
    cur = conn.cursor()
    cur.execute(
        f"SELECT content FROM {table} ORDER BY emb <-> %s::vector LIMIT {k};",
        (qv_txt,)
    )
    rows = cur.fetchall()
    cur.close()
    return [r[0] for r in rows]


def generate_answer(question: str, context: str) -> str:
    """
    使用 deepseek-r1 基于上下文生成答案 / Use deepseek-r1 to answer with context.
    """
    user_prompt = (
        f"请结合以下上下文回答：\n---\n{context}\n---\n"
        f"问题：{question}\n只输出答案本身。"
    )
    resp = ollama_chat(
        model=GEN_MODEL,
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_prompt},
        ],
    )
    return resp["message"]["content"]


# ---------- 主流程 / Main pipeline ----------

def main():
    # 1) 语料准备 / Prepare corpus
    ensure_corpus(CORPUS_FILE, CORPUS_URL)
    paras = read_and_chunk_md(CORPUS_FILE)
    log(f"[ingest] 段落数: {len(paras)}")

    # 2) 连接数据库并自检 DataVec / Connect DB & self-check
    conn = connect_db()
    check_datavec(conn)  # 若失败会抛错

    # 3) 建表 & 入库 & 建索引 / Create table, ingest, create index
    setup_table(conn, TABLE_NAME, EMB_DIM)
    ingest_paragraphs(conn, paras, TABLE_NAME)
    create_hnsw_index(conn, TABLE_NAME)

    # 4) 检索 & 生成 / Search & Generate
    question = "openGauss 发布了哪些版本？"
    top_contexts = vector_search(conn, question, k=TOP_K, table=TABLE_NAME)
    context = "\n\n".join(top_contexts)

    answer = generate_answer(question, context)

    # 输出结果 / Print results
    print("\n[Q] ", question)
    print("[Context]\n", context[:800], "...\n")
    print("[A] ", answer)

    conn.close()


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        # 友好错误提示 / Friendly error hints
        msg = str(e)
        if "type \"vector\" does not exist" in msg or "UndefinedObject" in msg:
            print(
                "\n[ERROR] 未检测到 DataVec 向量类型 'vector'。\n"
                "请确认你使用的是 openGauss DataVec 镜像，并连接到了该实例。\n",
                file=sys.stderr
            )
        elif "operator does not exist: vector <->" in msg:
            print(
                "\n[ERROR] 向量比较 <-> 失败：请确认右操作数已显式 cast 为 ::vector。\n"
                "本脚本已使用 to_vector_literal() + %s::vector 进行修复，如果仍报错请检查数据库端。\n",
                file=sys.stderr
            )
        else:
            print("\n[ERROR]", msg, file=sys.stderr)
        raise

"""
RAG Demo with openGauss(DataVec) + Ollama (DeepSeek + nomic-embed-text)
本示例完成：下载语料 → 切分 → 向量化入库 → HNSW 相似度检索 → 基于上下文生成答案

⚠ 先决条件 / Prerequisites
1) openGauss 使用“DataVec 镜像”并已启动，提供了 vector 类型与 HNSW 索引
   例如（x86_64）:
     docker pull swr.cn-north-4.myhuaweicloud.com/opengauss-x86-64/opengauss-datavec:latest
     docker run --name og-datavec -d -e GS_PASSWORD=你的密码 -p 8888:5432 \
       -v /home/test/opengauss:/var/lib/opengauss \
       swr.cn-north-4.myhuaweicloud.com/opengauss-x86-64/opengauss-datavec:latest

2) pip 安装:
     pip install ollama psycopg2  # 若编译困难可 pip install psycopg2-binary

3) 本地已启动 Ollama:
     ollama serve &
     ollama pull deepseek-r1
     ollama pull nomic-embed-text

Author: You + ChatGPT
"""

import os
import sys
import json
import time
import math
import typing as T

# ---- 依赖：数据库与大模型客户端 / Deps: DB & LLM client ----
import psycopg2
from psycopg2.extras import execute_values

# Ollama Python SDK
from ollama import embeddings as ollama_embeddings
from ollama import chat as ollama_chat

# ---- 配置区 / Configs ----
# 语料（Markdown）本地路径与下载地址 / Corpus local path & URL
CORPUS_FILE = r"E:\ospp_opengauss\index.md"  # ← 按需修改
CORPUS_URL  = "https://gitee.com/opengauss/website/raw/v2/app/zh/faq/index.md"

# 数据库连接参数 / DB connection
DB_CFG = dict(
    database="postgres",
    user="gaussdb",
    password="asdF@123",      # ← 修改为你的密码
    host="127.0.0.1",
    port="8888",
)

# 表名与嵌入维度 / Table name & embedding dimension
TABLE_NAME = "opengauss_data"
EMB_DIM = 768  # nomic-embed-text 的默认维度通常为 768

# 模型名 / Model names
EMBED_MODEL = "nomic-embed-text"
GEN_MODEL   = "qwen2:7b"

# 检索与生成参数 / Retrieval & generation
TOP_K = 3
SYSTEM_PROMPT = "你是数据库与开源生态方向的中文助理，必须基于已给定上下文回答问题，禁止编造。"


# ---------- 工具函数 / Utilities ----------

def log(msg: str):
    print(msg, flush=True)


def http_get(url: str, timeout: int = 30) -> bytes:
    """下载工具（优先 requests，失败用 urllib）/ HTTP downloader."""
    try:
        import requests  # type: ignore
        resp = requests.get(url, timeout=timeout)
        resp.raise_for_status()
        return resp.content
    except Exception:
        # fallback to urllib
        from urllib.request import urlopen
        with urlopen(url, timeout=timeout) as r:
            return r.read()


def ensure_corpus(path: str = CORPUS_FILE, url: str = CORPUS_URL) -> None:
    """确保本地存在语料文件 / Ensure corpus file exists locally."""
    if not os.path.exists(path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        data = http_get(url)
        with open(path, "wb") as f:
            f.write(data)
        log(f"[corpus] downloaded to: {path}")
    else:
        log(f"[corpus] found: {path}")


def read_and_chunk_md(path: str) -> T.List[str]:
    """
    读取 Markdown 并进行简单切分：
    - 以 '##' 二级标题切分
    - 去除空白与超短片段
    Read Markdown and chunk by '##' headers.
    """
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    parts = [p.strip() for p in content.split("##") if p.strip()]
    # 丢弃长度过短的段落 / drop too-short chunks
    parts = [p for p in parts if len(p) > 30]
    return parts


def embed_text(text: str) -> T.List[float]:
    """
    使用 Ollama 的嵌入模型获取向量 / Get embedding via Ollama.
    返回 Python list[float]
    """
    vec = ollama_embeddings(model=EMBED_MODEL, prompt=text)
    emb = vec["embedding"]
    # 保守起见确保是 float 列表 / ensure float list
    return [float(x) for x in emb]


def to_vector_literal(arr: T.Sequence[float]) -> str:
    """
    将 Python 浮点数组转为 openGauss DataVec 的向量字面量字符串
    Convert Python list[float] to DataVec vector literal string: "[1.0,2.0,...]"
    """
    return "[" + ",".join(f"{float(x):.6f}" for x in arr) + "]"


def connect_db():
    """建立数据库连接 / Get DB connection."""
    return psycopg2.connect(**DB_CFG)


def check_datavec(conn) -> None:
    """
    自检：确认 vector 类型与 HNSW 能力可用
    Self-check: ensure 'vector' type exists and HNSW operator works.
    """
    cur = conn.cursor()
    # 1) check vector type
    cur.execute("SELECT typname FROM pg_type WHERE typname='vector';")
    row = cur.fetchone()
    if not row:
        cur.close()
        raise RuntimeError(
            "当前 openGauss 未检测到 DataVec 的 'vector' 类型。\n"
            "请确认你使用的是 DataVec 镜像并连接到了正确实例。"
        )
    # 2) try <-> operator
    cur.execute("SELECT '[1,2,3]'::vector <-> '[1,2,3]'::vector;")
    _ = cur.fetchone()
    cur.close()


def setup_table(conn, table: str = TABLE_NAME, dim: int = EMB_DIM) -> None:
    """
    创建数据表（id, content, emb）并清空旧表
    Create table with vector column.
    """
    cur = conn.cursor()
    cur.execute(f"DROP TABLE IF EXISTS {table};")
    cur.execute(f"CREATE TABLE {table} (id INT PRIMARY KEY, content TEXT, emb vector({dim}));")
    conn.commit()
    cur.close()
    log(f"[db] table created: {table} (vector({dim}))")


def create_hnsw_index(conn, table: str = TABLE_NAME) -> None:
    """
    创建 HNSW 索引（L2 距离）
    Create HNSW index using L2 distance.
    """
    cur = conn.cursor()
    cur.execute(f"CREATE INDEX ON {table} USING hnsw (emb vector_l2_ops);")
    conn.commit()
    cur.close()
    log(f"[db] HNSW index created on {table}.emb (L2)")


def ingest_paragraphs(conn, paragraphs: T.Sequence[str], table: str = TABLE_NAME) -> None:
    """
    批量入库（向量使用字面量 + ::vector 显式转换，最稳妥）
    Batch insert paragraphs with embeddings.
    """
    cur = conn.cursor()
    rows = []
    for i, p in enumerate(paragraphs):
        e = embed_text(p)
        e_txt = to_vector_literal(e)  # "[...]" 字符串
        rows.append((i, p, e_txt))
        if (i + 1) % 10 == 0:
            log(f"[ingest] inserted {i+1}/{len(paragraphs)}")

    # 逐条参数化插入（执行时 cast 为 vector）
    for rid, content, e_txt in rows:
        cur.execute(
            f"INSERT INTO {table} (id, content, emb) VALUES (%s, %s, %s::vector);",
            (rid, content, e_txt)
        )
    conn.commit()
    cur.close()
    log(f"[ingest] completed: {len(paragraphs)} rows")


def vector_search(conn, question: str, k: int = TOP_K, table: str = TABLE_NAME) -> T.List[str]:
    """
    相似度检索：将 query 向量化 → 字面量 → ::vector → ORDER BY <-> LIMIT k
    Vector similarity search with <-> operator.
    """
    qv = embed_text(question)
    qv_txt = to_vector_literal(qv)
    cur = conn.cursor()
    cur.execute(
        f"SELECT content FROM {table} ORDER BY emb <-> %s::vector LIMIT {k};",
        (qv_txt,)
    )
    rows = cur.fetchall()
    cur.close()
    return [r[0] for r in rows]


def generate_answer(question: str, context: str) -> str:
    """
    使用 deepseek-r1 基于上下文生成答案 / Use deepseek-r1 to answer with context.
    """
    user_prompt = (
        f"请结合以下上下文回答：\n---\n{context}\n---\n"
        f"问题：{question}\n只输出答案本身。"
    )
    resp = ollama_chat(
        model=GEN_MODEL,
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_prompt},
        ],
    )
    return resp["message"]["content"]


# ---------- 主流程 / Main pipeline ----------

def main():
    # 1) 语料准备 / Prepare corpus
    ensure_corpus(CORPUS_FILE, CORPUS_URL)
    paras = read_and_chunk_md(CORPUS_FILE)
    log(f"[ingest] 段落数: {len(paras)}")

    # 2) 连接数据库并自检 DataVec / Connect DB & self-check
    conn = connect_db()
    check_datavec(conn)  # 若失败会抛错

    # 3) 建表 & 入库 & 建索引 / Create table, ingest, create index
    setup_table(conn, TABLE_NAME, EMB_DIM)
    ingest_paragraphs(conn, paras, TABLE_NAME)
    create_hnsw_index(conn, TABLE_NAME)

    # 4) 检索 & 生成 / Search & Generate
    question = "openGauss 发布了哪些版本？"
    top_contexts = vector_search(conn, question, k=TOP_K, table=TABLE_NAME)
    context = "\n\n".join(top_contexts)

    answer = generate_answer(question, context)

    # 输出结果 / Print results
    print("\n[Q] ", question)
    print("[Context]\n", context[:800], "...\n")
    print("[A] ", answer)

    conn.close()


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        # 友好错误提示 / Friendly error hints
        msg = str(e)
        if "type \"vector\" does not exist" in msg or "UndefinedObject" in msg:
            print(
                "\n[ERROR] 未检测到 DataVec 向量类型 'vector'。\n"
                "请确认你使用的是 openGauss DataVec 镜像，并连接到了该实例。\n",
                file=sys.stderr
            )
        elif "operator does not exist: vector <->" in msg:
            print(
                "\n[ERROR] 向量比较 <-> 失败：请确认右操作数已显式 cast 为 ::vector。\n"
                "本脚本已使用 to_vector_literal() + %s::vector 进行修复，如果仍报错请检查数据库端。\n",
                file=sys.stderr
            )
        else:
            print("\n[ERROR]", msg, file=sys.stderr)
        raise
