import streamlit as st
import requests

st.set_page_config(page_title="openGauss RAG", layout="centered")
st.title("openGauss 向量数据库 RAG Demo")

st.subheader("检索问答")
q = st.text_input("问题 / Question", "openGauss 发布了哪些版本？")
top_k = st.number_input("Top-k", min_value=1, value=3, step=1)

if st.button("提问 / Ask"):
    r = requests.post("http://127.0.0.1:8000/query", json={"question": q, "top_k": int(top_k)}, timeout=120)
    if r.ok:
        data = r.json()
        st.write("### 回答 / Answer")
        st.write(data["answer"])
        st.write("### 证据 / Contexts")
        for i, c in enumerate(data["contexts"], 1):
            st.markdown(f"**{i}.** {c[:800]}{'...' if len(c)>800 else ''}")
    else:
        st.error(r.text)

st.divider()
st.subheader("批量入库 / Ingest")
texts = st.text_area("每行一段文本 / one chunk per line", "")
if st.button("入库 / Ingest"):
    items = [{"text": line} for line in texts.splitlines() if line.strip()]
    r = requests.post("http://127.0.0.1:8000/ingest", json={"items": items}, timeout=600)
    if r.ok:
        st.success(r.json())
    else:
        st.error(r.text)
