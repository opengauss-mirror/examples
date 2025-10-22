import os
from dotenv import load_dotenv

load_dotenv()

class Settings:
    OG_HOST = os.getenv("OG_HOST", "127.0.0.1")
    OG_PORT = int(os.getenv("OG_PORT", "8888"))
    OG_DB = os.getenv("OG_DB", "postgres")
    OG_USER = os.getenv("OG_USER", "gaussdb")
    OG_PASSWORD = os.getenv("OG_PASSWORD", "Gauss@123")

    TABLE_NAME = os.getenv("TABLE_NAME", "opengauss_data")
    EMB_DIM = int(os.getenv("EMB_DIM", "768"))

    EMBED_BACKEND = os.getenv("EMBED_BACKEND", "hf")
    HF_MODEL = os.getenv("HF_MODEL", "sentence-transformers/all-MiniLM-L6-v2")
    HF_DEVICE = os.getenv("HF_DEVICE", "cpu")

    COHERE_API_KEY = os.getenv("COHERE_API_KEY")
    COHERE_MODEL = os.getenv("COHERE_MODEL", "embed-multilingual-v3.0")

    BENTO_EMB_URL = os.getenv("BENTO_EMB_URL", "http://127.0.0.1:3001/emb")

    OLLAMA_URL = os.getenv("OLLAMA_URL", "http://127.0.0.1:11434")
    GEN_MODEL = os.getenv("GEN_MODEL", "qwen2:7b")

    TOP_K = int(os.getenv("TOP_K", "3"))

settings = Settings()
