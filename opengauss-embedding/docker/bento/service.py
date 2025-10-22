import bentoml
from bentoml.io import JSON
from sentence_transformers import SentenceTransformer

model = SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")

svc = bentoml.Service("emb_svc")

@svc.api(input=JSON(), output=JSON())
def emb(payload: dict):
    texts = payload["texts"]
    vecs = model.encode(texts, normalize_embeddings=False).tolist()
    return {"embeddings": vecs}
