import configparser
import logging
import psycopg2
import random
import time
import math
import numpy as np
from pymilvus import connections, Collection, utility, DataType

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[logging.FileHandler('test.log'), logging.StreamHandler()]
)
logger = logging.getLogger(__name__)

def load_config(config_file: str) -> configparser.ConfigParser:
    """Load configuration file"""
    config = configparser.ConfigParser()
    try:
        if not config.read(config_file):
            raise FileNotFoundError(f"Config file {config_file} not found")
        return config
    except Exception as e:
        logger.error(f"Failed to load config: {e}")
        raise

config = load_config('config.ini')

def connect_opengauss() -> psycopg2.extensions.connection:
    """Connect to openGauss"""
    try:
        conn = psycopg2.connect(
            user=config.get('openGauss', 'user'),
            password=config.get('openGauss', 'password'),
            host=config.get('openGauss', 'host'),
            port=config.get('openGauss', 'port'),
            database=config.get('openGauss', 'database')
        )
        logger.info("Connected to openGauss")
        return conn
    except Exception as e:
        logger.error(f"openGauss connection failed: {e}")
        raise


def connect_milvus() -> Collection:
    """Connect to Milvus"""
    try:
        connections.connect(
            alias="default",
            host=config.get('Milvus', 'host'),
            port=config.get('Milvus', 'port')
        )

        milvus_version = utility.get_server_version()
        logger.info(f"Connected to Milvus {milvus_version}")

        collection_name = config.get('Table', 'milvus_collection_name')
        collection = Collection(collection_name)
        collection.load()
        logger.info(f"Loaded collection: {collection_name}")
        return collection
    except Exception as e:
        logger.error(f"Milvus connection failed: {e}")
        raise

def check_data_existence(opengauss_conn, milvus_collection, sample_size=1000):
    logger.info("开始数据存在性检查...")

    try:
        # 从OpenGauss中随机选取数据
        cursor = opengauss_conn.cursor()
        cursor.execute(
            f"SELECT id FROM {config.get('Table', 'opengauss_table_name')} ORDER BY RANDOM() LIMIT {sample_size}")
        random_ids = [row[0] for row in cursor.fetchall()]

        logger.info(f"从OpenGauss中随机选取了 {len(random_ids)} 条数据")

        # 检查Milvus中是否存在这些数据
        start_time = time.time()

        # 根据Milvus collection的主键字段名和类型查询
        # 获取主键字段信息
        primary_field = None
        for field in milvus_collection.schema.fields:
            if field.is_primary:
                primary_field = field
                break

        if not primary_field:
            raise Exception("未找到主键字段")


        query_expr = f"{primary_field.name} in [{', '.join(map(str, random_ids))}]"

        results = milvus_collection.query(expr=query_expr)

        end_time = time.time()
        query_time = end_time - start_time

        found_ids = [result[primary_field.name] for result in results]

        logger.info(f"数据存在性检查完成:")
        logger.info(f"查询耗时: {query_time:.4f} 秒")
        logger.info(f"找到 {len(found_ids)} 条数据")
        logger.info(f"缺失 {len(random_ids) - len(found_ids)} 条数据")

        return query_time, len(found_ids)

    except Exception as e:
        logger.error(f"数据存在性检查失败: {e}")
        raise

def execute_dql_tests(opengauss_conn, milvus_collection):
    """执行DQL测试，包括向量、稀疏向量和标量检索"""
    logger.info("开始DQL测试...")

    results = {}

    try:
        # 获取Milvus collection的schema信息
        schema = milvus_collection.schema
        vector_field = None
        sparse_vector_field = None
        scalar_field = None

        # 识别字段类型
        for field in schema.fields:
            if field.dtype == DataType.FLOAT_VECTOR:
                vector_field = field.name
            elif field.dtype == DataType.SPARSE_FLOAT_VECTOR:
                sparse_vector_field = field.name
            elif field.dtype in [DataType.INT64, DataType.INT32, DataType.INT16, DataType.INT8,
                                 DataType.FLOAT, DataType.DOUBLE, DataType.BOOL, DataType.VARCHAR]:
                scalar_field = field.name

        logger.info(f"检测到向量字段: {vector_field}")
        logger.info(f"检测到稀疏向量字段: {sparse_vector_field}")
        logger.info(f"检测到标量字段: {scalar_field}")

        # 向量检索测试
        if vector_field:
            results['vector_search'] = test_vector_search(opengauss_conn, milvus_collection, vector_field)
            results['vector_consistancy'] = test_vector_consistency(opengauss_conn, milvus_collection, vector_field)

        # 稀疏向量检索测试
        if sparse_vector_field:
            results['sparse_vector_search'] = test_sparse_vector_search(opengauss_conn, milvus_collection,
                                                                        sparse_vector_field)
            results['sparse_vector_consistency'] = test_sparse_vector_consistency(opengauss_conn, milvus_collection,
                                                                                  sparse_vector_field)

        # 标量检索测试
        if scalar_field:
            results['scalar_search'] = test_scalar_search(opengauss_conn, milvus_collection, scalar_field)

    except Exception as e:
        logger.error(f"DQL测试失败: {e}")
        raise

    return results


def test_vector_search(opengauss_conn, milvus_collection, vector_field):
    """测试向量检索"""
    logger.info("开始向量检索测试...")

    try:
        # 从OpenGauss获取一个随机向量作为查询向量
        cursor = opengauss_conn.cursor()
        cursor.execute(
            f"SELECT {vector_field} FROM {config.get('Table', 'opengauss_table_name')} ORDER BY RANDOM() LIMIT 1")
        query_vector_str = cursor.fetchone()[0]

        # 将字符串形式的向量转换为Python列表
        # 假设向量存储格式为: [0.1, 0.2, 0.3, ...]
        if isinstance(query_vector_str, str):
            # 去除方括号并按逗号分割
            query_vector = [float(x) for x in query_vector_str.strip('[]').split(',')]
        else:
            # 如果已经是列表格式，直接使用
            query_vector = query_vector_str

        logger.info(f"查询向量长度: {len(query_vector)}")
        logger.info(f"查询向量前5个值: {query_vector[:5]}")

        # 在OpenGauss中执行向量检索
        start_time = time.time()
        cursor.execute(f"""
            SELECT id, {vector_field} 
            FROM {config.get('Table', 'opengauss_table_name')} 
            ORDER BY {vector_field} <-> %s::vector 
            LIMIT 10
        """, (query_vector,))
        opengauss_results = cursor.fetchall()
        opengauss_time = time.time() - start_time

        # 在Milvus中执行向量检索
        start_time = time.time()
        search_params = {"metric_type": "L2", "params": {"nprobe": 10}}
        milvus_results = milvus_collection.search(
            data=[query_vector],  # 注意：确保是二维列表
            anns_field=vector_field,
            param=search_params,
            limit=10,
            output_fields=["id"]
        )
        milvus_time = time.time() - start_time

        # 比较结果
        opengauss_ids = [row[0] for row in opengauss_results]
        opengauss_ids = [int(str_id) for str_id in opengauss_ids]
        milvus_ids = [hit.entity.get('id') for hit in milvus_results[0]]

        logger.info(f"向量检索测试完成:")
        logger.info(f"OpenGauss耗时: {opengauss_time:.4f} 秒")
        logger.info(f"Milvus耗时: {milvus_time:.4f} 秒")
        logger.info(f"OpenGauss结果ID: {opengauss_ids}")
        logger.info(f"Milvus结果ID: {milvus_ids}")

        # 计算结果重叠度
        common_ids = set(opengauss_ids) & set(milvus_ids)
        overlap_ratio = len(common_ids) / len(opengauss_ids)
        logger.info(f"结果重叠度: {overlap_ratio:.2%}")

        return {
            "opengauss_time": opengauss_time,
            "milvus_time": milvus_time,
            "overlap_ratio": overlap_ratio
        }

    except Exception as e:
        logger.error(f"向量检索测试失败: {e}")
        raise


def test_vector_consistency(opengauss_conn, milvus_collection, vector_field, sample_size=100, l2_threshold=1e-6):
    """随机选取ID，对比两个库中该ID该字段的数据一致性（L2距离在一定范围内）"""
    logger.info(f"开始向量数据一致性测试，采样数量: {sample_size}, L2阈值: {l2_threshold}")

    try:
        cursor = opengauss_conn.cursor()

        # 1. 随机选取若干个ID
        cursor.execute(f"""
            SELECT id FROM {config.get('Table', 'opengauss_table_name')} 
            ORDER BY RANDOM() LIMIT {sample_size}
        """)
        random_ids = [row[0] for row in cursor.fetchall()]

        if not random_ids:
            logger.warning("未找到可用的ID进行测试")
            return {"total_tested": 0, "consistent_count": 0, "consistency_rate": 0.0}

        consistent_count = 0
        inconsistent_records = []

        for record_id in random_ids:
            try:
                # 2. 从OpenGauss获取该ID的向量数据
                cursor.execute(f"""
                    SELECT {vector_field} FROM {config.get('Table', 'opengauss_table_name')} 
                    WHERE id = %s
                """, (record_id,))
                opengauss_result = cursor.fetchone()

                if not opengauss_result:
                    logger.warning(f"ID {record_id} 在OpenGauss中不存在")
                    inconsistent_records.append({
                        'id': record_id,
                        'reason': '在OpenGauss中不存在'
                    })
                    continue

                opengauss_vector_str = opengauss_result[0]

                # 转换OpenGauss向量格式
                if isinstance(opengauss_vector_str, str):
                    opengauss_vector = [float(x) for x in opengauss_vector_str.strip('[]').split(',')]
                else:
                    opengauss_vector = opengauss_vector_str

                # 3. 从Milvus获取该ID的向量数据
                milvus_results = milvus_collection.query(
                    expr=f"id == {record_id}",
                    output_fields=[vector_field]
                )

                if not milvus_results:
                    logger.warning(f"ID {record_id} 在Milvus中不存在")
                    inconsistent_records.append({
                        'id': record_id,
                        'reason': '在Milvus中不存在'
                    })
                    continue

                milvus_vector = milvus_results[0][vector_field]

                # 4. 计算L2距离
                if len(opengauss_vector) != len(milvus_vector):
                    logger.error(
                        f"ID {record_id} 向量维度不匹配: OpenGauss={len(opengauss_vector)}, Milvus={len(milvus_vector)}")
                    inconsistent_records.append({
                        'id': record_id,
                        'reason': f'向量维度不匹配: {len(opengauss_vector)} vs {len(milvus_vector)}'
                    })
                    continue

                l2_distance = 0.0
                for i in range(len(opengauss_vector)):
                    diff = opengauss_vector[i] - milvus_vector[i]
                    l2_distance += diff * diff
                l2_distance = math.sqrt(l2_distance)

                # 5. 判断是否在阈值范围内
                if l2_distance <= l2_threshold:
                    consistent_count += 1
                    logger.debug(f"ID {record_id} 数据一致, L2距离: {l2_distance:.10f}")
                else:
                    logger.warning(f"ID {record_id} 数据不一致, L2距离: {l2_distance:.10f} > 阈值 {l2_threshold}")
                    inconsistent_records.append({
                        'id': record_id,
                        'reason': f'L2距离超出阈值: {l2_distance:.10f} > {l2_threshold}',
                        'l2_distance': l2_distance,
                        'opengauss_sample': opengauss_vector[:5] if len(opengauss_vector) > 5 else opengauss_vector,
                        'milvus_sample': milvus_vector[:5] if len(milvus_vector) > 5 else milvus_vector
                    })

            except Exception as e:
                logger.error(f"测试ID {record_id} 时发生错误: {str(e)}")
                inconsistent_records.append({
                    'id': record_id,
                    'reason': f'测试错误: {str(e)}'
                })

        # 6. 统计结果
        total_tested = len(random_ids)
        consistency_rate = consistent_count / total_tested if total_tested > 0 else 0

        logger.info(f"向量数据一致性测试完成:")
        logger.info(f"测试总数: {total_tested}")
        logger.info(f"一致数量: {consistent_count}")
        logger.info(f"一致率: {consistency_rate:.2%}")

        if inconsistent_records:
            logger.info(f"不一致的记录详情:")
            for record in inconsistent_records:
                logger.info(f"  ID {record['id']}: {record['reason']}")

        return {
            "total_tested": total_tested,
            "consistent_count": consistent_count,
            "consistency_rate": consistency_rate,
            "inconsistent_records": inconsistent_records,
            "l2_threshold": l2_threshold
        }

    except Exception as e:
        logger.error(f"向量数据一致性测试失败: {str(e)}")
        return {
            "total_tested": 0,
            "consistent_count": 0,
            "consistency_rate": 0.0,
            "inconsistent_records": [],
            "error": str(e)
        }


def sparse_vector_str_to_dict(sparse_str):
    """
    将稀疏向量字符串转换为字典格式
    输入: '{136:0.7030771,138:0.6601265,178:0.30273142,...}'
    输出: {136: 0.7030771, 138: 0.6601265, 178: 0.30273142, ...}
    """
    try:
        # 去除字符串两端的空格和花括号
        sparse_str = sparse_str.split('/')[0]
        cleaned_str = sparse_str.strip().strip('{}')

        # 如果字符串为空，返回空字典
        if not cleaned_str:
            return {}

        # 按逗号分割键值对
        pairs = cleaned_str.split(',')

        result_dict = {}

        for pair in pairs:
            # 去除每个键值对两端的空格
            pair = pair.strip()
            if not pair:
                continue

            # 按冒号分割键和值
            if ':' in pair:
                key_str, value_str = pair.split(':', 1)

                # 去除键和值两端的空格并转换类型
                key = int(key_str.strip())
                value = float(value_str.strip())

                result_dict[key] = value

        return result_dict

    except Exception as e:
        raise ValueError(f"Failed to parse sparse vector string: {e}")

def test_sparse_vector_search(opengauss_conn, milvus_collection, sparse_vector_field):
    """测试稀疏向量检索"""
    logger.info("开始稀疏向量检索测试...")

    try:
        # 从OpenGauss获取一个随机稀疏向量作为查询向量
        cursor = opengauss_conn.cursor()
        cursor.execute(
            f"SELECT {sparse_vector_field} FROM {config.get('Table', 'opengauss_table_name')} ORDER BY RANDOM() LIMIT 1")

        query_sparse_str = cursor.fetchone()[0]

        # 将字符串形式的向量转换为Python列表
        # 假设向量存储格式为: [0.1, 0.2, 0.3, ...]
        if isinstance(query_sparse_str, str):
            # 去除方括号并按逗号分割
            query_sparse_vector = sparse_vector_str_to_dict(query_sparse_str)
        else:
            # 如果已经是列表格式，直接使用
            query_sparse_vector = query_sparse_str

        # 在OpenGauss中执行稀疏向量检索
        start_time = time.time()
        cursor.execute(f"""
            SELECT id, {sparse_vector_field} 
            FROM {config.get('Table', 'opengauss_table_name')} 
            ORDER BY {sparse_vector_field} <-> %s::sparsevec 
            LIMIT 10
        """, (query_sparse_str,))
        opengauss_results = cursor.fetchall()
        opengauss_time = time.time() - start_time

        # 在Milvus中执行稀疏向量检索
        start_time = time.time()
        search_params = {"metric_type": "IP", "params": {}}
        milvus_results = milvus_collection.search(
            data=[query_sparse_vector],
            anns_field=sparse_vector_field,
            param=search_params,
            limit=10,
            output_fields=["id"]
        )
        milvus_time = time.time() - start_time

        # 比较结果
        opengauss_ids = [row[0] for row in opengauss_results]
        opengauss_ids = [int(str_id) for str_id in opengauss_ids]
        milvus_ids = [hit.entity.get('id') for hit in milvus_results[0]]

        logger.info(f"稀疏向量检索测试完成:")
        logger.info(f"OpenGauss耗时: {opengauss_time:.4f} 秒")
        logger.info(f"Milvus耗时: {milvus_time:.4f} 秒")
        logger.info(f"OpenGauss结果ID: {opengauss_ids}")
        logger.info(f"Milvus结果ID: {milvus_ids}")

        # 计算结果重叠度
        common_ids = set(opengauss_ids) & set(milvus_ids)
        overlap_ratio = len(common_ids) / len(opengauss_ids) if opengauss_ids else 0
        logger.info(f"结果重叠度: {overlap_ratio:.2%}")

        return {
            "opengauss_time": opengauss_time,
            "milvus_time": milvus_time,
            "overlap_ratio": overlap_ratio
        }

    except Exception as e:
        logger.error(f"稀疏向量检索测试失败: {e}")
        raise


def test_sparse_vector_consistency(opengauss_conn, milvus_collection, sparse_vector_field, sample_size=100,
                                         l2_threshold=1e-6):
    """随机选取ID，对比两个库中该ID的稀疏向量数据一致性（L2距离在一定范围内）"""
    logger.info(f"开始稀疏向量数据一致性测试，采样数量: {sample_size}, L2阈值: {l2_threshold}")

    try:
        cursor = opengauss_conn.cursor()

        # 1. 随机选取若干个ID
        cursor.execute(f"""
            SELECT id FROM {config.get('Table', 'opengauss_table_name')} 
            WHERE {sparse_vector_field} IS NOT NULL 
            ORDER BY RANDOM() LIMIT {sample_size}
        """)
        random_ids = [row[0] for row in cursor.fetchall()]

        if not random_ids:
            logger.warning("未找到包含稀疏向量的ID进行测试")
            return {"total_tested": 0, "consistent_count": 0, "consistency_rate": 0.0}

        consistent_count = 0
        inconsistent_records = []

        for record_id in random_ids:
            try:
                # 2. 从OpenGauss获取该ID的稀疏向量数据
                cursor.execute(f"""
                    SELECT {sparse_vector_field} FROM {config.get('Table', 'opengauss_table_name')} 
                    WHERE id = %s
                """, (record_id,))
                opengauss_result = cursor.fetchone()

                if not opengauss_result:
                    logger.warning(f"ID {record_id} 在OpenGauss中不存在")
                    inconsistent_records.append({
                        'id': record_id,
                        'reason': '在OpenGauss中不存在'
                    })
                    continue

                opengauss_sparse_vec_str = opengauss_result[0]

                # 3. 从Milvus获取该ID的稀疏向量数据
                milvus_results = milvus_collection.query(
                    expr=f"id == {record_id}",
                    output_fields=[sparse_vector_field]
                )

                if not milvus_results:
                    logger.warning(f"ID {record_id} 在Milvus中不存在")
                    inconsistent_records.append({
                        'id': record_id,
                        'reason': '在Milvus中不存在'
                    })
                    continue

                milvus_sparse_vec = milvus_results[0][sparse_vector_field]

                # 4. 解析OpenGauss稀疏向量格式
                # OpenGauss格式: "{index1:value1,index2:value2,...}/dimension"
                if not opengauss_sparse_vec_str:
                    logger.warning(f"ID {record_id} 在OpenGauss中的稀疏向量为空")
                    inconsistent_records.append({
                        'id': record_id,
                        'reason': 'OpenGauss稀疏向量为空'
                    })
                    continue

                # 解析OpenGauss稀疏向量字符串
                try:
                    # 去除花括号和维度信息
                    vec_part = opengauss_sparse_vec_str.split('/')[0].strip('{}')
                    dimension = int(opengauss_sparse_vec_str.split('/')[1])

                    opengauss_sparse_dict = {}
                    if vec_part:  # 处理非空稀疏向量
                        pairs = vec_part.split(',')
                        for pair in pairs:
                            idx, val = pair.split(':')
                            # OpenGauss索引从1开始，转换为从0开始以与Milvus一致
                            opengauss_sparse_dict[int(idx) - 1] = float(val)
                except Exception as e:
                    logger.error(f"解析OpenGauss稀疏向量失败: {opengauss_sparse_vec_str}, 错误: {str(e)}")
                    inconsistent_records.append({
                        'id': record_id,
                        'reason': f'OpenGauss稀疏向量解析失败: {str(e)}'
                    })
                    continue

                # 5. 处理Milvus稀疏向量格式
                # Milvus格式: {"indices": [index1, index2, ...], "values": [value1, value2, ...]}
                if not milvus_sparse_vec:
                    logger.warning(f"ID {record_id} 在Milvus中的稀疏向量为空")
                    inconsistent_records.append({
                        'id': record_id,
                        'reason': 'Milvus稀疏向量为空'
                    })
                    continue

                try:
                    milvus_sparse_dict = {}
                    if 'indices' in milvus_sparse_vec and 'values' in milvus_sparse_vec:
                        for idx, val in zip(milvus_sparse_vec['indices'], milvus_sparse_vec['values']):
                            milvus_sparse_dict[idx] = val
                    else:
                        # 如果已经是字典格式，直接使用
                        milvus_sparse_dict = milvus_sparse_vec
                except Exception as e:
                    logger.error(f"解析Milvus稀疏向量失败: {milvus_sparse_vec}, 错误: {str(e)}")
                    inconsistent_records.append({
                        'id': record_id,
                        'reason': f'Milvus稀疏向量解析失败: {str(e)}'
                    })
                    continue

                # 6. 检查维度一致性
                max_dimension = max(
                    max(opengauss_sparse_dict.keys()) if opengauss_sparse_dict else 0,
                    max(milvus_sparse_dict.keys()) if milvus_sparse_dict else 0
                ) + 1  # 索引从0开始，所以维度是最大索引+1

                # 7. 将稀疏向量转换为稠密向量以便计算L2距离
                opengauss_dense = [0.0] * max_dimension
                for idx, val in opengauss_sparse_dict.items():
                    if idx < max_dimension:
                        opengauss_dense[idx] = val

                milvus_dense = [0.0] * max_dimension
                for idx, val in milvus_sparse_dict.items():
                    if idx < max_dimension:
                        milvus_dense[idx] = val

                # 8. 计算L2距离
                l2_distance = 0.0
                for i in range(max_dimension):
                    diff = opengauss_dense[i] - milvus_dense[i]
                    l2_distance += diff * diff
                l2_distance = math.sqrt(l2_distance)

                # 9. 判断是否在阈值范围内
                if l2_distance <= l2_threshold:
                    consistent_count += 1
                    logger.debug(f"ID {record_id} 稀疏向量数据一致, L2距离: {l2_distance:.10f}")
                else:
                    logger.warning(
                        f"ID {record_id} 稀疏向量数据不一致, L2距离: {l2_distance:.10f} > 阈值 {l2_threshold}")

                    # 记录非零元素的差异
                    non_zero_diff = []
                    all_indices = set(opengauss_sparse_dict.keys()) | set(milvus_sparse_dict.keys())
                    for idx in sorted(all_indices):
                        og_val = opengauss_sparse_dict.get(idx, 0.0)
                        milvus_val = milvus_sparse_dict.get(idx, 0.0)
                        if abs(og_val - milvus_val) > 1e-9:  # 忽略微小浮点误差
                            non_zero_diff.append({
                                'index': idx,
                                'opengauss': og_val,
                                'milvus': milvus_val,
                                'diff': abs(og_val - milvus_val)
                            })

                    inconsistent_records.append({
                        'id': record_id,
                        'reason': f'L2距离超出阈值: {l2_distance:.10f} > {l2_threshold}',
                        'l2_distance': l2_distance,
                        'dimension': max_dimension,
                        'non_zero_diff_count': len(non_zero_diff),
                        'non_zero_diff_sample': non_zero_diff[:3] if non_zero_diff else []  # 只记录前3个差异
                    })

            except Exception as e:
                logger.error(f"测试ID {record_id} 的稀疏向量时发生错误: {str(e)}")
                inconsistent_records.append({
                    'id': record_id,
                    'reason': f'测试错误: {str(e)}'
                })

        # 10. 统计结果
        total_tested = len(random_ids)
        consistency_rate = consistent_count / total_tested if total_tested > 0 else 0

        logger.info(f"稀疏向量数据一致性测试完成:")
        logger.info(f"测试总数: {total_tested}")
        logger.info(f"一致数量: {consistent_count}")
        logger.info(f"一致率: {consistency_rate:.2%}")

        if inconsistent_records:
            logger.info(f"不一致的记录详情:")
            for record in inconsistent_records:
                logger.info(f"  ID {record['id']}: {record['reason']}")
                if 'non_zero_diff_count' in record:
                    logger.info(f"    非零元素差异数量: {record['non_zero_diff_count']}")
                    if record['non_zero_diff_sample']:
                        logger.info(f"    差异示例: {record['non_zero_diff_sample']}")

        return {
            "total_tested": total_tested,
            "consistent_count": consistent_count,
            "consistency_rate": consistency_rate,
            "inconsistent_records": inconsistent_records,
            "l2_threshold": l2_threshold
        }

    except Exception as e:
        logger.error(f"稀疏向量数据一致性测试失败: {str(e)}")
        return {
            "total_tested": 0,
            "consistent_count": 0,
            "consistency_rate": 0.0,
            "inconsistent_records": [],
            "error": str(e)
        }

def test_scalar_search(opengauss_conn, milvus_collection, scalar_field):
    """测试标量检索"""
    logger.info("开始标量检索测试...")

    try:
        # 从OpenGauss获取一个随机标量值作为查询条件
        cursor = opengauss_conn.cursor()
        cursor.execute(
            f"SELECT {scalar_field} FROM {config.get('Table', 'opengauss_table_name')} ORDER BY RANDOM() LIMIT 1")
        scalar_value = cursor.fetchone()[0]

        # 在OpenGauss中执行标量检索
        start_time = time.time()
        cursor.execute(f"""
            SELECT id, {scalar_field} 
            FROM {config.get('Table', 'opengauss_table_name')} 
            WHERE {scalar_field} = %s 
            LIMIT 10
        """, (scalar_value,))
        opengauss_results = cursor.fetchall()
        opengauss_time = time.time() - start_time

        # 在Milvus中执行标量检索
        start_time = time.time()
        query_expr = f"{scalar_field} == {scalar_value}"
        milvus_results = milvus_collection.query(
            expr=query_expr,
            output_fields=["id", scalar_field],
            limit=10
        )
        milvus_time = time.time() - start_time

        # 比较结果
        opengauss_ids = [row[0] for row in opengauss_results]
        opengauss_ids = [int(str_id) for str_id in opengauss_ids]
        milvus_ids = [result['id'] for result in milvus_results]

        logger.info(f"标量检索测试完成:")
        logger.info(f"OpenGauss耗时: {opengauss_time:.4f} 秒")
        logger.info(f"Milvus耗时: {milvus_time:.4f} 秒")
        logger.info(f"OpenGauss结果ID: {opengauss_ids}")
        logger.info(f"Milvus结果ID: {milvus_ids}")

        # 检查结果一致性
        opengauss_ids_set = set(opengauss_ids)
        milvus_ids_set = set(milvus_ids)

        is_consistent = opengauss_ids_set == milvus_ids_set
        logger.info(f"结果一致性: {'一致' if is_consistent else '不一致'}")

        if not is_consistent:
            logger.warning(f"OpenGauss特有ID: {opengauss_ids_set - milvus_ids_set}")
            logger.warning(f"Milvus特有ID: {milvus_ids_set - opengauss_ids_set}")

        return {
            "opengauss_time": opengauss_time,
            "milvus_time": milvus_time,
            "is_consistent": is_consistent
        }

    except Exception as e:
        logger.error(f"标量检索测试失败: {e}")
        raise


def main():
    """主函数"""
    try:
        # 加载配置
        config = load_config('config.ini')

        # 连接数据库
        opengauss_conn = connect_opengauss()
        milvus_collection = connect_milvus()

        cursor = opengauss_conn.cursor()
        count_query = f"SELECT COUNT(*) FROM {config.get('Table', 'opengauss_table_name')};"
        cursor.execute(count_query)
        result = cursor.fetchone()
        data_count_opengauss = result[0]

        entity_count = milvus_collection.num_entities

        logger.info(f"Milvus数据条数：{entity_count}, Opengauss数据条数：{data_count_opengauss}")

        # 数据存在性检查
        check_data_existence(opengauss_conn, milvus_collection)

        # DQL测试
        dql_results = execute_dql_tests(opengauss_conn, milvus_collection)

        # 输出测试总结
        logger.info("=" * 50)
        logger.info("测试总结:")

        for test_name, result in dql_results.items():
            logger.info(f"{test_name}:")
            for key, value in result.items():
                logger.info(f"  {key}: {value}")

        logger.info("所有测试完成")

    except Exception as e:
        logger.error(f"测试过程中发生错误: {e}")
    finally:
        # 关闭连接
        if 'opengauss_conn' in locals():
            opengauss_conn.close()
            logger.info("OpenGauss连接已关闭")

        if 'milvus_collection' in locals():
            connections.disconnect("default")
            logger.info("Milvus连接已关闭")


if __name__ == "__main__":
    main()