import requests
from rag.config import settings

SYSTEM_PROMPT = "你是数据库与开源生态方向的中文助理，必须基于已给定上下文回答问题，禁止编造。"

def generate_answer(question: str, contexts: list[str]) -> str:
    context = "\n\n".join(contexts)
    payload = {
        "model": settings.GEN_MODEL,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": f"请结合以下上下文回答：\n---\n{context}\n---\n问题：{question}\n只输出答案本身。"},
        ],
    }
    r = requests.post(f"{settings.OLLAMA_URL}/api/chat", json=payload, timeout=120)
    r.raise_for_status()
    return r.json()["message"]["content"]
