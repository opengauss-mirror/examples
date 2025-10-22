from pydantic import BaseModel
from typing import List, Optional

class IngestItem(BaseModel):
    id: Optional[int] = None
    text: str

class IngestRequest(BaseModel):
    items: List[IngestItem]

class QueryRequest(BaseModel):
    question: str
    top_k: Optional[int] = None

class QueryResponse(BaseModel):
    question: str
    contexts: List[str]
    answer: str
