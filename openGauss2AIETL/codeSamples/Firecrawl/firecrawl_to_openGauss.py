import psycopg2
import numpy as np
from sentence_transformers import SentenceTransformer
from typing import List, Dict, Any, Optional
import json
import requests
import time
from urllib.parse import urlparse
import asyncio
import aiohttp

class FirecrawlOpenGaussVectorDB:
    def __init__(self, host: str = "localhost", port: int = 5432,
                 database: str = "postgres", user: str = "your_username",
                 password: str = "your_password",
                 firecrawl_api_key: Optional[str] = None,
                 firecrawl_base_url: str = "https://api.firecrawl.dev"):
        # 数据库配置
        self.connection_params = {
            "host": host,
            "port": port,
            "database": database,
            "user": user,
            "password": password
        }

        # Firecrawl 配置
        self.firecrawl_api_key = firecrawl_api_key
        self.firecrawl_base_url = firecrawl_base_url.rstrip('/')

        # 模型初始化
        self.embedding_model = SentenceTransformer('all-MiniLM-L6-v2')

        # 初始化数据库
        self.init_database()

    def get_connection(self):
        """建立数据库连接"""
        return psycopg2.connect(**self.connection_params)

    def init_database(self):
        """初始化数据库表和向量扩展"""
        try:
            conn = self.get_connection()
            cursor = conn.cursor()

            # cursor.execute("CREATE EXTENSION IF NOT EXISTS vector;")

            create_table_query = """
                                 CREATE TABLE IF NOT EXISTS crawled_documents \
                                 ( \
                                     id \
                                     SERIAL \
                                     PRIMARY \
                                     KEY, \
                                     url \
                                     TEXT \
                                     NOT \
                                     NULL \
                                     UNIQUE, \
                                     title \
                                     TEXT, \
                                     content \
                                     TEXT \
                                     NOT \
                                     NULL, \
                                     metadata \
                                     JSONB, \
                                     embedding \
                                     vector \
                                 ( \
                                     384 \
                                 ),
                                     crawled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     domain TEXT,
                                     word_count INTEGER
                                     ); \
                                 """
            cursor.execute(create_table_query)

            create_index_query = """
                                 CREATE INDEX IF NOT EXISTS crawled_docs_embedding_idx
                                     ON crawled_documents
                                     USING ivfflat (embedding vector_l2_ops)
                                     WITH (lists = 100); \
                                 """
            cursor.execute(create_index_query)

            conn.commit()
            print("Database initialized successfully")

        except Exception as e:
            print(f"Error initializing database: {e}")
        finally:
            if conn:
                cursor.close()
                conn.close()

    def generate_embedding(self, text: str) -> List[float]:
        """生成本地文本嵌入"""
        # 限制文本长度以避免模型输入限制
        if len(text) > 512:
            text = text[:512]
        embedding = self.embedding_model.encode(text)
        return embedding.tolist()

    def array_to_vector_string(self, array: List[float]) -> str:
        """将数组转换为openGauss向量格式字符串"""
        return '[' + ','.join(map(str, array)) + ']'

    async def crawl_url(self, url: str, options: Dict[str, Any] = None) -> Optional[Dict[str, Any]]:
        """使用 Firecrawl 抓取单个 URL"""
        if not self.firecrawl_api_key:
            raise ValueError("Firecrawl API key is required for crawling")

        default_options = {
            "formats": ["markdown"],
            "onlyMainContent": True,
        }

        final_options = {**default_options, **(options or {})}

        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {self.firecrawl_api_key}"
        }

        payload = {
            "url": url,
            "formats": final_options["formats"],
            "onlyMainContent": final_options["onlyMainContent"],
        }

        try:
            async with aiohttp.ClientSession() as session:
                async with session.post(
                        f"{self.firecrawl_base_url}/v1/scrape",
                        headers=headers,
                        json=payload,
                        timeout=30
                ) as response:

                    if response.status == 200:
                        data = await response.json()
                        print(f"Raw response for {url}: {json.dumps(data, ensure_ascii=False, indent=2)}")

                        if data.get("success"):
                            return data
                        else:
                            print(f"Firecrawl error: {data.get('error')}")
                            return None
                    else:
                        error_text = await response.text()
                        print(f"HTTP error {response.status} for {url}: {error_text}")
                        return None

        except Exception as e:
            print(f"Error crawling {url}: {e}")
            return None

    def process_firecrawl_response(self, response_data: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """处理 Firecrawl 响应数据"""
        try:
            if not response_data.get("success"):
                print(f"Firecrawl request failed: {response_data.get('error')}")
                return None

            data = response_data.get("data", {})

            # 调试：打印完整的响应结构
            print(f"Processing response data: {json.dumps(data, ensure_ascii=False, indent=2)}")

            # 提取关键信息 - 根据实际的 Firecrawl 响应结构调整
            url = data.get("url", data.get("metadata", {}).get("url", ""))

            # 尝试不同的内容字段
            content = ""
            if "markdown" in data:
                content = data["markdown"]
            elif "text" in data:
                content = data["text"]
            elif "html" in data:
                content = data["html"]
            elif "content" in data:
                content = data["content"]

            # 提取标题
            title = data.get("title", "")
            if not title and "metadata" in data:
                title = data["metadata"].get("title", "")

            # 提取元数据
            metadata = data.get("metadata", {})
            if not metadata:
                metadata = {
                    "url": url,
                    "title": title,
                    "source": "Firecrawl"
                }

            if not url:
                print("No URL found in response data")
                return None

            if not content:
                print(f"No content found for {url}")
                return None

            return {
                "url": url,
                "title": title,
                "content": content,
                "metadata": metadata
            }

        except Exception as e:
            print(f"Error processing Firecrawl response: {e}")
            return None

    def store_document(self, document_data: Dict[str, Any]) -> bool:
        """存储文档到数据库"""
        try:
            url = document_data.get("url")
            title = document_data.get("title", "")
            content = document_data.get("content", "")
            metadata = document_data.get("metadata", {})

            if not url:
                print("Cannot store document: missing URL")
                return False

            if not content:
                print(f"Cannot store document {url}: missing content")
                return False

            # 生成嵌入
            embedding_text = f"{title} {content}"[:500]  # 限制长度
            embedding = self.generate_embedding(embedding_text)

            # 提取域名
            domain = urlparse(url).netloc

            # 计算字数
            word_count = len(content.split())

            conn = self.get_connection()
            cursor = conn.cursor()

            insert_query = """
                           INSERT INTO crawled_documents
                               (url, title, content, metadata, embedding, domain, word_count)
                           VALUES (%s, %s, %s, %s, %s::vector, %s, %s)
                           """

            cursor.execute(insert_query, (
                url,
                title,
                content,
                json.dumps(metadata),
                self.array_to_vector_string(embedding),
                domain,
                word_count
            ))

            conn.commit()
            print(f"Successfully stored: {url} ({word_count} words)")
            return True

        except Exception as e:
            print(f"Error storing document {document_data.get('url', 'unknown')}: {e}")
            return False
        finally:
            if conn:
                cursor.close()
                conn.close()

    async def crawl_and_store_url(self, url: str, options: Dict[str, Any] = None) -> bool:
        """完整的抓取和存储流程"""
        print(f"🕷️  Crawling: {url}")

        # 抓取 URL
        response = await self.crawl_url(url, options)
        if not response:
            print(f"❌ Failed to crawl {url}")
            return False

        # 处理响应
        document_data = self.process_firecrawl_response(response)
        if not document_data:
            print(f"❌ Failed to process response for {url}")
            return False

        # 存储到数据库
        success = self.store_document(document_data)
        if success:
            print(f"✅ Successfully processed and stored: {url}")
        else:
            print(f"❌ Failed to store: {url}")

        return success

    async def crawl_multiple_urls(self, urls: List[str], options: Dict[str, Any] = None) -> List[bool]:
        """批量抓取多个 URLs"""
        results = []

        for url in urls:
            success = await self.crawl_and_store_url(url, options)
            results.append(success)
            # 避免请求过于频繁
            await asyncio.sleep(2)

        return results

    def similarity_search(self, query: str, top_k: int = 5, domain: str = None) -> List[Any]:
        """基于相似性搜索抓取的文档"""
        try:
            query_embedding = self.generate_embedding(query)
            conn = self.get_connection()
            cursor = conn.cursor()

            if domain:
                search_query = """
                               SELECT id, \
                                      url, \
                                      title, \
                                      content, \
                                      metadata,
                                      l2_distance(embedding, %s::vector) as distance
                               FROM crawled_documents
                               WHERE domain = %s
                               ORDER BY distance
                                   LIMIT %s; \
                               """
                cursor.execute(search_query, (
                    self.array_to_vector_string(query_embedding),
                    domain,
                    top_k
                ))
            else:
                search_query = """
                               SELECT id, \
                                      url, \
                                      title, \
                                      content, \
                                      metadata,
                                      l2_distance(embedding, %s::vector) as distance
                               FROM crawled_documents
                               ORDER BY distance
                                   LIMIT %s; \
                               """
                cursor.execute(search_query, (
                    self.array_to_vector_string(query_embedding),
                    top_k
                ))

            results = cursor.fetchall()
            return results

        except Exception as e:
            print(f"Error in similarity search: {e}")
            return []
        finally:
            if conn:
                cursor.close()
                conn.close()

    def get_crawl_statistics(self) -> Dict[str, Any]:
        """获取抓取统计信息"""
        try:
            conn = self.get_connection()
            cursor = conn.cursor()

            stats = {}

            # 总文档数
            cursor.execute("SELECT COUNT(*) FROM crawled_documents")
            stats['total_documents'] = cursor.fetchone()[0]

            # 总字数
            cursor.execute("SELECT SUM(word_count) FROM crawled_documents")
            stats['total_words'] = cursor.fetchone()[0] or 0

            # 域名分布
            cursor.execute("""
                           SELECT domain, COUNT (*) as count, SUM (word_count) as total_words
                           FROM crawled_documents
                           GROUP BY domain
                           ORDER BY count DESC
                           """)
            stats['domain_distribution'] = cursor.fetchall()

            return stats

        except Exception as e:
            print(f"Error getting statistics: {e}")
            return {}
        finally:
            if conn:
                cursor.close()
                conn.close()

    def list_all_documents(self) -> List[Any]:
        """列出所有存储的文档"""
        try:
            conn = self.get_connection()
            cursor = conn.cursor()

            cursor.execute("""
                           SELECT id, url, title, domain, word_count, crawled_at
                           FROM crawled_documents
                           ORDER BY crawled_at DESC
                           """)

            return cursor.fetchall()

        except Exception as e:
            print(f"Error listing documents: {e}")
            return []
        finally:
            if conn:
                cursor.close()
                conn.close()


# 使用示例
async def demo():
    # 初始化（需要设置你的 Firecrawl API key）
    firecrawl_api_key = "fc-xxxxxxxxx"  # 替换为你的实际 API key

    if firecrawl_api_key == "your_firecrawl_api_key_here":
        print("❌ Please set your Firecrawl API key")
        return

    vector_db = FirecrawlOpenGaussVectorDB(
        firecrawl_api_key=firecrawl_api_key
    )

    # 要抓取的 URLs - 使用一些可靠的测试URL
    urls_to_crawl = [
        "https://lilianweng.github.io/posts/2024-07-07-hallucination/",
        "https://lilianweng.github.io/posts/2024-11-28-reward-hacking/",
        "https://lilianweng.github.io/posts/2025-05-01-thinking/"
    ]

    print("Starting web crawling...")

    # 抓取网页
    results = await vector_db.crawl_multiple_urls(urls_to_crawl)

    success_count = sum(results)
    print(f"\nCrawling completed: {success_count} successful, {len(results) - success_count} failed")

    # 列出所有存储的文档
    print("\n📋 Stored documents:")
    documents = vector_db.list_all_documents()
    for doc_id, url, title, domain, word_count, crawled_at in documents:
        print(f"  - {title} ({domain}, {word_count} words)")

    # 执行相似性搜索
    if documents:
        print("\n🔍 Testing similarity search...")
        queries = [
            "machine learning",
            "artificial intelligence",
            "neural networks"
        ]

        for query in queries:
            print(f"\nSearch query: '{query}'")
            results = vector_db.similarity_search(query, top_k=2)

            if results:
                print("Most relevant results:")
                for i, (doc_id, url, title, content, metadata, distance) in enumerate(results):
                    print(f"{i + 1}. 📍 Distance: {distance:.4f}")
                    print(f"   🏷️  Title: {title}")
                    print(f"   🌐 URL: {url}")
                    print(f"   📝 Preview: {content[:100]}...")
                    print()
            else:
                print("No results found")

    # 显示统计信息
    stats = vector_db.get_crawl_statistics()
    print(f"\n📊 Crawl statistics:")
    print(f"   Total documents: {stats.get('total_documents', 0)}")
    print(f"   Total words: {stats.get('total_words', 0)}")
    print("   Domain distribution:")
    for domain, count, total_words in stats.get('domain_distribution', []):
        print(f"     {domain}: {count} documents, {total_words} words")


# 测试函数 - 先测试数据处理而不实际调用 API
def test_data_processing():
    """测试数据处理逻辑"""
    print("Testing data processing...")

    vector_db = FirecrawlOpenGaussVectorDB(
        firecrawl_api_key="test"  # 不需要真实 API key 来测试
    )

    # 模拟 Firecrawl 响应数据
    sample_response = {
        "success": True,
        "data": {
            "url": "https://example.com/test",
            "title": "Test Page",
            "markdown": "This is test content about machine learning and AI.",
            "metadata": {
                "title": "Test Page",
                "description": "A test page for Firecrawl",
                "source": "Firecrawl"
            }
        }
    }

    # 测试数据处理
    processed = vector_db.process_firecrawl_response(sample_response)
    print(f"Processed data: {json.dumps(processed, indent=2, ensure_ascii=False)}")

    if processed:
        # 测试存储
        success = vector_db.store_document(processed)
        print(f"Storage successful: {success}")


if __name__ == "__main__":
    # 先测试数据处理
    test_data_processing()

    # 然后运行实际的爬虫演示（取消注释来运行）
    # asyncio.run(demo())