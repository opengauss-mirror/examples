import psycopg2
from typing import List, Sequence
from rag.config import settings

def connect():
    return psycopg2.connect(
        host=settings.OG_HOST,
        port=settings.OG_PORT,
        database=settings.OG_DB,
        user=settings.OG_USER,
        password=settings.OG_PASSWORD,
    )

def check_datavec(conn) -> None:
    cur = conn.cursor()
    cur.execute("SELECT typname FROM pg_type WHERE typname='vector';")
    if not cur.fetchone():
        cur.close()
        raise RuntimeError("DataVec 'vector' type not found. Use DataVec image.")
    cur.execute("SELECT '[1,2,3]'::vector <-> '[1,2,3]'::vector;")
    cur.fetchone()
    cur.close()

def setup_table(conn, table: str, dim: int) -> None:
    cur = conn.cursor()
    cur.execute(f"CREATE TABLE IF NOT EXISTS {table} (id INT PRIMARY KEY, content TEXT, emb vector({dim}));")
    conn.commit()
    cur.close()

def create_hnsw_index(conn, table: str) -> None:
    cur = conn.cursor()
    cur.execute(f"CREATE INDEX IF NOT EXISTS {table}_hnsw_idx ON {table} USING hnsw (emb vector_l2_ops);")
    conn.commit()
    cur.close()

def to_vector_literal(arr: Sequence[float]) -> str:
    return "[" + ",".join(f"{float(x):.6f}" for x in arr) + "]"

def insert_rows(conn, table: str, rows: Sequence[tuple]) -> None:
    cur = conn.cursor()
    for rid, content, e_txt in rows:
        cur.execute(
            f"INSERT INTO {table} (id, content, emb) VALUES (%s, %s, %s::vector) "
            f"ON CONFLICT (id) DO UPDATE SET content=EXCLUDED.content, emb=EXCLUDED.emb;",
            (rid, content, e_txt)
        )
    conn.commit()
    cur.close()

def topk_by_query_vector(conn, table: str, qv_literal: str, k: int) -> List[str]:
    cur = conn.cursor()
    cur.execute(
        f"SELECT content FROM {table} ORDER BY emb <-> %s::vector LIMIT %s;",
        (qv_literal, k)
    )
    rows = cur.fetchall()
    cur.close()
    return [r[0] for r in rows]
