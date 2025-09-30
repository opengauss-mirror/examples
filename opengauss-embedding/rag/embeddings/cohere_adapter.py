from typing import List
import cohere
from rag.embeddings.base import EmbeddingBackend
from rag.config import settings

class CohereBackend(EmbeddingBackend):
    def __init__(self):
        self.client = cohere.Client(settings.COHERE_API_KEY)
        self.model = settings.COHERE_MODEL

    def embed(self, texts: List[str]) -> List[List[float]]:
        resp = self.client.embed(texts=texts, model=self.model)
        return [v for v in resp.embeddings]
