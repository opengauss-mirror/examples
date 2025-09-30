from typing import List, Tuple
from rag.config import settings
from rag.db import datavec
from rag.embeddings import get_backend

def ingest_texts(items: List[Tuple[int, str]]) -> int:
    backend = get_backend()
    texts = [t for _, t in items]
    embs = backend.embed(texts)
    conn = datavec.connect()
    datavec.check_datavec(conn)
    datavec.setup_table(conn, settings.TABLE_NAME, settings.EMB_DIM)
    datavec.create_hnsw_index(conn, settings.TABLE_NAME)
    rows = []
    for (rid, content), e in zip(items, embs):
        rows.append((rid, content, datavec.to_vector_literal(e)))
    datavec.insert_rows(conn, settings.TABLE_NAME, rows)
    conn.close()
    return len(rows)
