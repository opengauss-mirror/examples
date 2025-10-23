import os
import io
import logging
import pandas as pd
import requests
from huggingface_hub import snapshot_download
from tqdm import tqdm
API_BASE = "http://localhost:8001"

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def download_finRAG() -> str:
    """Download the finRAG dataset from Hugging Face to local cache.
    Returns the local directory path."""
    local_dir = snapshot_download(repo_id="parsee-ai/finRAG", repo_type="dataset")
    logger.info("Downloaded finRAG to %s", local_dir)
    return local_dir

def load_parquet(local_dir: str, fname: str = "rag_text.parquet") -> pd.DataFrame:
    """Load the specified Parquet file from the dataset folder."""
    path = os.path.join(local_dir, fname)
    if not os.path.exists(path):
        raise FileNotFoundError(f"{path} not found")
    df = pd.read_parquet(path)
    logger.info("Loaded Parquet with %d rows, columns: %s", len(df), list(df.columns))
    return df

def doc_row_to_text(row: pd.Series) -> str:
    """Format a DataFrame row into a single text blob for ingestion."""
    row_dict = row.to_dict()
    return "\n\n".join([f"{k.upper()}:\n{v}" for k, v in row_dict.items()])

def ingest_document_text(api_url: str, text: str, filename: str, timeout: int = 300) -> bool:
    """Upload a single document string via PrivateGPT Ingest File API."""
    files = {
        "file": (filename, io.BytesIO(text.encode("utf-8")), "text/plain"),
    }
    resp = requests.post(api_url, files=files, timeout=timeout)
    if resp.status_code == 200:
        return True
    else:
        logger.error("Failed ingest %s → %s: %s", filename, resp.status_code, resp.text)
        return False

def ingest_finRAG_to_privategpt(n: int = 100):
    """Main pipeline: download, parse, and ingest first n docs from finRAG."""
    local_dir = download_finRAG()
    df = load_parquet(local_dir, "rag_text.parquet")
    cols = [c for c in ["question", "context", "answer"] if c in df.columns]

    ingest_api = f"{API_BASE}/v1/ingest/file"
    success, fail = 0, 0

    for idx, row in tqdm(df.head(n).iterrows()):
        text = doc_row_to_text(row)
        fname = f"finRAG_{idx:06d}.txt"
        if ingest_document_text(ingest_api, text, fname):
            success += 1
        else:
            fail += 1

    logger.info("Ingestion finished: %d successes, %d failures", success, fail)

if __name__ == "__main__":
    ingest_finRAG_to_privategpt(n=10) 