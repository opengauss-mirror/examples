from fastapi import FastAPI
from rag.schemas import IngestRequest, QueryRequest, QueryResponse
from rag.pipeline.ingest import ingest_texts
from rag.pipeline.retrieve import topk_contexts
from rag.pipeline.generate import generate_answer

app = FastAPI(title="openGauss RAG Service")

@app.post("/ingest")
def ingest(req: IngestRequest):
    items = []
    next_id = 0
    for it in req.items:
        rid = it.id if it.id is not None else next_id
        items.append((rid, it.text))
        next_id = rid + 1
    n = ingest_texts(items)
    return {"inserted": n}

@app.post("/query", response_model=QueryResponse)
def query(req: QueryRequest):
    ctx = topk_contexts(req.question, req.top_k)
    ans = generate_answer(req.question, ctx)
    return QueryResponse(question=req.question, contexts=ctx, answer=ans)
