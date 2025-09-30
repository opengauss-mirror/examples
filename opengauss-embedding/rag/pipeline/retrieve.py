from rag.config import settings
from rag.db import datavec
from rag.embeddings import get_backend

def topk_contexts(question: str, k: int | None = None):
    k = k or settings.TOP_K
    backend = get_backend()
    qv = backend.embed([question])[0]
    qv_literal = datavec.to_vector_literal(qv)
    conn = datavec.connect()
    ctx = datavec.topk_by_query_vector(conn, settings.TABLE_NAME, qv_literal, k)
    conn.close()
    return ctx
