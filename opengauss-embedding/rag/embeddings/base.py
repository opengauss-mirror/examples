from typing import List, Protocol

class EmbeddingBackend(Protocol):
    def embed(self, texts: List[str]) -> List[List[float]]:
        ...
