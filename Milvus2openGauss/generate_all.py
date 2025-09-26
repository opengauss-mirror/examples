import random
import string
import numpy as np
from pymilvus import connections, FieldSchema, CollectionSchema, DataType, Collection, utility
import argparse

class MilvusDataGenerator:
    def __init__(self, host='localhost', port='19530'):
        self.host = host
        self.port = port
        self.random = random.Random()
        # 连接到Milvus服务器
        try:
            connections.connect(host=host, port=port)
            print(f"已连接到Milvus服务器: {host}:{port}")
        except Exception as e:
            print(f"连接Milvus服务器失败: {e}")
            raise

    def generate_random_data(self, field_schema, count):
        field_name = field_schema.name
        dtype = field_schema.dtype
        result = []

        if dtype == DataType.INT64:
            result = [random.randint(0, 10000) for _ in range(count)]

        elif dtype == DataType.VARCHAR:
            max_length = getattr(field_schema, 'max_length', 100)
            result = [''.join(random.choices(string.ascii_letters + string.digits, k=random.randint(5, max_length)))
                     for _ in range(count)]

        elif dtype == DataType.FLOAT:
            result = [random.uniform(0, 1000) for _ in range(count)]

        elif dtype == DataType.DOUBLE:
            result = [random.uniform(0, 1000) for _ in range(count)]

        elif dtype == DataType.BOOL:
            result = [random.choice([True, False]) for _ in range(count)]

        elif dtype == DataType.FLOAT_VECTOR:
            dim = field_schema.dim
            result = [[random.random() for _ in range(dim)] for _ in range(count)]

        elif dtype == DataType.BINARY_VECTOR:
            dim = field_schema.dim
            # 二进制向量的维度需要是8的倍数
            byte_length = dim // 8
            result = [bytes([random.randint(0, 255) for _ in range(byte_length)])
                     for _ in range(count)]

        elif dtype == DataType.ARRAY:
            # 假设数组类型存储整数数组
            result = [[random.randint(0, 100) for _ in range(random.randint(1, 10))]
                     for _ in range(count)]

        elif dtype == DataType.JSON:
            result = [{'key': ''.join(random.choices(string.ascii_letters, k=5)),
                      'value': random.randint(0, 100)} for _ in range(count)]

        elif dtype == DataType.SPARSE_FLOAT_VECTOR:
            # 生成稀疏向量
            result = self.generate_sparse_vectors(count, 1000)

        return result

    def generate_sparse_vectors(self, count, dim=1000, sparsity=0.98):
        vectors = []
        for _ in range(count):
            # 创建一个空字典来存储非零值
            sparse_vec = {}

            # 确定非零元素的数量 (基于稀疏度)
            non_zero_count = max(1, int(dim * (1 - sparsity)))

            # 随机选择非零位置
            indices = random.sample(range(dim), non_zero_count)

            # 为每个非零位置生成随机值
            for idx in indices:
                sparse_vec[idx] = random.random()  # 生成0-1之间的随机值

            vectors.append(sparse_vec)

        return vectors

    def create_collection(self, schema, collection_name):
        # 检查集合是否已存在
        if utility.has_collection(collection_name):
            print(f"集合 '{collection_name}' 已存在，正在删除...")
            utility.drop_collection(collection_name)

        # 创建新集合
        try:
            collection = Collection(
                name=collection_name,
                schema=schema,
                consistency_level="Strong"
            )
            print(f"已创建集合: {collection_name}")
            return collection
        except Exception as e:
            print(f"创建集合失败: {e}")
            # 打印schema详细信息以便调试
            print("Schema字段详情:")
            for i, field in enumerate(schema.fields):
                print(f"  字段 {i}: name={field.name}, dtype={field.dtype}, params={field.params}")
            raise

    def insert_data_to_collection(self, collection, schema, count, batch_size=5000):
        fields = schema.fields
        all_entities = {}

        # 检查是否有自增主键
        auto_id_field = None
        for field in fields:
            if field.is_primary and getattr(field, 'auto_id', False):
                auto_id_field = field.name
                break

        # 为每个字段生成所有数据
        for field in fields:
            if field.name == auto_id_field:
                continue  # 跳过自增主键
            all_entities[field.name] = self.generate_random_data(field, count)

        # 分批插入数据
        total_inserted = 0
        for start_idx in range(0, count, batch_size):
            end_idx = min(start_idx + batch_size, count)
            current_batch_size = end_idx - start_idx

            # 准备当前批次的数据
            batch_data = []
            for field in fields:
                if field.name == auto_id_field:
                    continue
                field_data = all_entities[field.name][start_idx:end_idx]
                batch_data.append(field_data)

            # 插入当前批次数据
            print(f"正在插入批次 {start_idx // batch_size + 1}/{(count - 1) // batch_size + 1}, "
                  f"数据条数: {current_batch_size}...")
            try:
                insert_result = collection.insert(batch_data)
                total_inserted += insert_result.insert_count
                print(f"成功插入 {insert_result.insert_count} 条数据，累计 {total_inserted}/{count}")
            except Exception as e:
                print(f"插入批次 {start_idx // batch_size + 1} 失败: {e}")
                # 可以在这里添加重试逻辑
                raise

        # 刷新数据确保持久化
        collection.flush()
        print(f"所有批次插入完成，总共插入 {total_inserted} 条数据")

        return all_entities, total_inserted

    def create_index(self, collection, field_name, index_params=None):
        # 获取字段信息以确定索引类型
        field = None
        for f in collection.schema.fields:
            if f.name == field_name:
                field = f
                break

        if field is None:
            raise ValueError(f"字段 '{field_name}' 不存在于集合中")

        # 为稀疏向量设置默认索引参数
        if field.dtype == DataType.SPARSE_FLOAT_VECTOR:
            if index_params is None:
                index_params = {
                    "index_type": "SPARSE_INVERTED_INDEX",  # 专门用于稀疏向量的索引类型
                    "metric_type": "IP",  # 内积
                    "params": {}
                }
            print(f"正在为稀疏向量字段 '{field_name}' 创建索引...")

        elif field.dtype == DataType.FLOAT_VECTOR:
            if index_params is None:
                index_params = {
                    "index_type": "IVF_FLAT",
                    "metric_type": "L2",
                    "params": {"nlist": 128}
                }
            print(f"正在为字段 '{field_name}' 创建索引...")

        # elif field.dtype == DataType.FLOAT_VECTOR:
        #     if index_params is None:
        #         index_params = {
        #             "index_type": "HNSW",  # 改为HNSW索引
        #             "metric_type": "L2",
        #             "params": {
        #                 "M": 16,  # HNSW参数：每个节点的最大连接数
        #                 "efConstruction": 200  # HNSW参数：构建时的搜索范围
        #             }
        #         }
        #     print(f"正在为字段 '{field_name}' 创建HNSW索引...")

        try:
            collection.create_index(field_name, index_params)
            print("索引创建完成")
        except Exception as e:
            print(f"创建索引失败: {e}")
            raise

    def print_data_table(self, entities, count=5):
        print("\n生成的数据示例（前{}条）:".format(count))
        print("-" * 150)

        # 打印表头
        headers = list(entities.keys())
        header_row = " | ".join(f"{header:25}" for header in headers)
        print(header_row)
        print("-" * 150)

        # 打印数据行
        for i in range(min(count, len(entities[headers[0]]))):
            row = []
            for header in headers:
                value = entities[header][i]

                # 处理稀疏向量的特殊显示
                if isinstance(value, dict) and all(isinstance(k, int) for k in value.keys()):
                    # 显示稀疏向量的摘要信息
                    non_zero_count = len(value)
                    display_value = f"Sparse vector: ({non_zero_count} None-zero element)"
                elif isinstance(value, (list, np.ndarray)):
                    # 对于稠密向量类型，只显示摘要
                    if len(value) > 5:
                        display_value = f"{value[:3]}...{value[-2:]} (dim={len(value)})"
                    else:
                        display_value = str(value)
                elif isinstance(value, dict):
                    display_value = str(value)
                elif isinstance(value, bytes):
                    display_value = f"Binary data: {value.hex()}"
                else:
                    display_value = str(value)

                # 截断过长内容
                if len(display_value) > 25:
                    display_value = display_value[:22] + "..."

                row.append(f"{display_value:25}")

            print(" | ".join(row))

# 使用示例
if __name__ == "__main__":
    from pymilvus import FieldSchema, CollectionSchema, DataType

    parser = argparse.ArgumentParser()
    parser.add_argument('-n', '--number', help='插入数据条数', type=int, default=1000)

    args = parser.parse_args()
    count = args.number

    # 示例Schema定义
    fields = [
        FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=True),
        FieldSchema(name="title", dtype=DataType.VARCHAR, max_length=100),
        FieldSchema(name="dense_embedding", dtype=DataType.FLOAT_VECTOR, dim=128),
        FieldSchema(name="sparse_embedding", dtype=DataType.SPARSE_FLOAT_VECTOR),
        FieldSchema(name="rating", dtype=DataType.FLOAT),
        # FieldSchema(name="tags", dtype=DataType.ARRAY, max_capacity=10),
        FieldSchema(name="metadata", dtype=DataType.JSON),
        FieldSchema(name="is_active", dtype=DataType.BOOL)
    ]

    schema = CollectionSchema(fields, "包含稀疏向量的示例集合")
    collection_name = f"test{count}"

    generator = MilvusDataGenerator(host='localhost', port='19530')

    collection = generator.create_collection(schema, collection_name)

    data_count = count
    entities, insert_result = generator.insert_data_to_collection(collection, schema, data_count)

    generator.create_index(collection, "dense_embedding")  # 稠密向量索引
    generator.create_index(collection, "sparse_embedding")  # 稀疏向量索引

    generator.print_data_table(entities)

    # 加载集合以便搜索
    collection.load()

    print(f"\n成功生成 {count} 条数据并插入到Milvus集合 '{collection_name}' 中！")

    print(f"\n集合统计信息:")
    print(f"实体数量: {collection.num_entities}")