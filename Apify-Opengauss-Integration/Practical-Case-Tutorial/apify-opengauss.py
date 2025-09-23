# type: ignore

import os

from apify_client import ApifyClient
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import PromptTemplate
from langchain_core.runnables import RunnablePassthrough
from langchain_opengauss import OpenGauss, OpenGaussSettings
from langchain_openai import ChatOpenAI, OpenAIEmbeddings

APIFY_API_TOKEN = os.getenv("APIFY_API_TOKEN") or "YOUR-APIFY-TOKEN"
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY") or "YOUR-OPENAI-API-KEY"

OPENGAUSS_HOST = os.getenv("OPENGAUSS_HOST")
OPENGAUSS_PORT = os.getenv("OPENGAUSS_PORT")
OPENGAUSS_USER = os.getenv("OPENGAUSS_USER")
OPENGAUSS_PASSWORD = os.getenv("OPENGAUSS_PASSWORD")
OPENGAUSS_DBNAME = os.getenv("OPENGAUSS_DBNAME")
OPENGAUSS_TABLE_NAME = os.getenv("OPENGAUSS_TABLE_NAME")

client = ApifyClient(APIFY_API_TOKEN)

embeddings = OpenAIEmbeddings(model="text-embedding-3-small")

print("Starting Apify's Website Content Crawler")
print("Crawling will take some time ... you can check the progress in the Apify console")

actor_call = client.actor(actor_id="apify/website-content-crawler").call(
    run_input={"maxCrawlPages": 10, "startUrls": [{"url": "https://opengauss.org/"}]}
)

print("Actor website content crawler has finished")
print(actor_call)

opengauss_integration_inputs = {
    "opengaussHost": OPENGAUSS_HOST,
    "opengaussPort": OPENGAUSS_PORT,
    "opengaussUser": OPENGAUSS_USER,
    "opengaussPassword": OPENGAUSS_PASSWORD,
    "opengaussDBname": OPENGAUSS_DBNAME,
    "opengaussTableName": OPENGAUSS_TABLE_NAME,
    "datasetFields": ["text"],
    "datasetId": actor_call["defaultDatasetId"],
    "deltaUpdatesPrimaryDatasetFields": ["url"],
    "expiredObjectDeletionPeriodDays": 7,
    "embeddingsApiKey": OPENAI_API_KEY,
    "embeddingsConfig": {
        "model": "text-embedding-3-small",
    },
    "embeddingsProvider": "OpenAI",
    "performChunking": True,
    "chunkSize": 2000,
    "chunkOverlap": 200
}

print("Starting Apify's OpenGauss Integration")
actor_call = client.actor("wyswyz/opengauss-integration").call(run_input=opengauss_integration_inputs)
print("Apify's OpenGauss Integration has finished")
print(actor_call)

print("Question answering using OpenGauss database")

config = OpenGaussSettings(
    host=OPENGAUSS_HOST,
    port=OPENGAUSS_PORT,
    user=OPENGAUSS_USER,
    password=OPENGAUSS_PASSWORD,
    database=OPENGAUSS_DBNAME,
    table_name=OPENGAUSS_TABLE_NAME,
    embedding_dimension=384,
    index_type="HNSW",
    distance_strategy="cosine",
)
vector_store = OpenGauss(embedding=embeddings, config=config)

prompt = PromptTemplate(
    input_variables=["context", "question"],
    template="Use the following pieces of retrieved context to answer the question. If you don't know the answer, "
    "just say that you don't know. \nQuestion: {question} \nContext: {context} \nAnswer:",
)


def format_docs(docs):
    return "\n\n".join(doc.page_content for doc in docs)


rag_chain = (
    {
        "context": vector_store.as_retriever() | format_docs,
        "question": RunnablePassthrough(),
    }
    | prompt
    | ChatOpenAI(model="gpt-5-mini", temperature=1)
    | StrOutputParser()
)

question = "什么是openGauss？"

print("Question:", question)
print("Answer:", rag_chain.invoke(question))
