import ollama
import psycopg2

table_name = "item_collection_website_content_crawler"

def embedding(text):
    vector = ollama.embeddings(model="nomic-embed-text", prompt=text)
    return vector["embedding"]

conn = psycopg2.connect(
    database="postgres",
    user="hly",
    password="Hly@1234",
    host="192.168.219.134",
    port="5432"
)

cur = conn.cursor()
# 设置搜索路径
cur.execute("SET search_path TO public;")
# 创建索引
cur.execute("CREATE INDEX IF NOT EXISTS idx_{}_embedding_hnsw ON {} USING hnsw (embedding vector_l2_ops);".format(table_name, table_name))
conn.commit()

question = "介绍一下openGauss DataVec"
emb_data = embedding(question)
dimensions = len(emb_data)

cur = conn.cursor()
cur.execute("select document_content from {} order by embedding <-> '{}' limit 3;".format(table_name, emb_data))
conn.commit()

rows = cur.fetchall()
print(rows)

cur.close()
conn.close()


# 指定容器所在主机或容器名、端口（如果需要的话）, 或通过设置环境变量
# client = ollama.Client(host="http://<宿主机 IP>:11434")

context = rows
# 测试
# context = ""

SYSTEM_PROMPT = "你作为一个对话 AI 助手，结合上下文信息简练高效的回答用户提出的问题"
USER_PROMPT = f"请结合{context}信息来回答{question}的问题，不需要额外的无用回答"

response = ollama.chat(
    model="deepseek-r1:1.5b",
    stream=True,
    messages=[
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user",   "content": USER_PROMPT}
    ]
)

print("\n\n")
# 实时打印每一段内容
for chunk in response:
    print(chunk["message"]["content"], end="", flush=True)

# print(response["message"]["content"])
