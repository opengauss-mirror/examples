from typing import List
from sentence_transformers import SentenceTransformer
from rag.embeddings.base import EmbeddingBackend
from rag.config import settings

class HFBackend(EmbeddingBackend):
    def __init__(self):
        self.model = SentenceTransformer(settings.HF_MODEL, device=settings.HF_DEVICE)

    def embed(self, texts: List[str]) -> List[List[float]]:
        return self.model.encode(texts, normalize_embeddings=False).tolist()
