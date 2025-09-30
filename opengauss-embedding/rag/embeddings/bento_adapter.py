from typing import List
import requests
from rag.embeddings.base import EmbeddingBackend
from rag.config import settings

class BentoBackend(EmbeddingBackend):
    def embed(self, texts: List[str]) -> List[List[float]]:
        r = requests.post(f"{settings.BENTO_EMB_URL}", json={"texts": texts}, timeout=60)
        r.raise_for_status()
        return r.json()["embeddings"]
