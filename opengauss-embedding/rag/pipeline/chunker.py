from typing import List

def split_text(text: str, chunk_size: int = 800, overlap: int = 150) -> List[str]:
    text = text.strip()
    n = len(text)
    chunks = []
    i = 0
    while i < n:
        end = min(i + chunk_size, n)
        chunks.append(text[i:end])
        if end == n:
            break
        i = max(end - overlap, 0)
    return [c for c in chunks if len(c.strip()) > 0]
