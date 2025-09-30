import os, requests
from rag.pipeline.chunker import split_text
from rag.pipeline.ingest import ingest_texts

URL = "https://gitee.com/opengauss/website/raw/v2/app/zh/faq/index.md"
PATH = os.path.expanduser("~/opengauss_faq.md")

def ensure_md(path=PATH, url=URL):
    if not os.path.exists(path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        data = requests.get(url, timeout=60); data.raise_for_status()
        with open(path, "wb") as f:
            f.write(data.content)

if __name__ == "__main__":
    ensure_md()
    with open(PATH, "r", encoding="utf-8") as f:
        text = f.read()
    chunks = split_text(text, 800, 150)
    items = list(enumerate(chunks))
    n = ingest_texts(items)
    print(f"[ok] inserted {n} chunks.")
