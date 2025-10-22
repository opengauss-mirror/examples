from rag.config import settings
from .hf_adapter import HFBackend
from .cohere_adapter import CohereBackend
from .bento_adapter import BentoBackend
from .base import EmbeddingBackend

def get_backend() -> EmbeddingBackend:
    if settings.EMBED_BACKEND == "cohere":
        return CohereBackend()
    if settings.EMBED_BACKEND == "bento":
        return BentoBackend()
    return HFBackend()
