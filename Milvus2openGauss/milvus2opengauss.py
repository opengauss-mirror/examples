import psycopg2
import csv
import json
from pymilvus import connections, Collection, utility
import configparser
import numpy as np
import os
import logging
from typing import List, Dict, Any, Optional, Union
from datetime import datetime

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[logging.FileHandler('migration.log'), logging.StreamHandler()]
)
logger = logging.getLogger(__name__)


class MilvusToOpenGaussMigrator:
    def __init__(self, config_file: str = 'config.ini'):
        self.config = self._load_config(config_file)
        self.csv_file_path = self._get_csv_file_path()
        self.fields = []
        self.MAX_WINDOW_SIZE = 16384  # Milvus default max query window
        self.SPARSE_DIMENSION = self.config.getint('SparseVector', 'default_dimension', fallback=1000)
        self.MAX_SPARSE_DIMENSION = 1000
        self.milvus_version = None

    def _load_config(self, config_file: str) -> configparser.ConfigParser:
        """Load configuration file"""
        config = configparser.ConfigParser()
        try:
            if not config.read(config_file):
                raise FileNotFoundError(f"Config file {config_file} not found")
            return config
        except Exception as e:
            logger.error(f"Failed to load config: {e}")
            raise

    def _get_csv_file_path(self) -> str:
        """Generate CSV file path with timestamp"""
        output_folder = self.config.get('Output', 'folder', fallback='output')
        os.makedirs(output_folder, exist_ok=True)
        milvus_collection = self.config.get('Table', 'milvus_collection_name')
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        return os.path.join(output_folder, f"{milvus_collection}_{timestamp}.csv")

    def _connect_milvus(self) -> Collection:
        """Connect to Milvus"""
        try:
            connections.connect(
                alias="default",
                host=self.config.get('Milvus', 'host'),
                port=self.config.get('Milvus', 'port')
            )

            self.milvus_version = utility.get_server_version()
            logger.info(f"Connected to Milvus {self.milvus_version}")

            collection_name = self.config.get('Table', 'milvus_collection_name')
            collection = Collection(collection_name)
            collection.load()
            self.fields = [field.name for field in collection.schema.fields]
            logger.info(f"Loaded collection: {collection_name}")
            return collection
        except Exception as e:
            logger.error(f"Milvus connection failed: {e}")
            raise

    def _connect_opengauss(self) -> psycopg2.extensions.connection:
        """Connect to openGauss"""
        try:
            conn = psycopg2.connect(
                user=self.config.get('openGauss', 'user'),
                password=self.config.get('openGauss', 'password'),
                host=self.config.get('openGauss', 'host'),
                port=self.config.get('openGauss', 'port'),
                database=self.config.get('openGauss', 'database')
            )
            logger.info("Connected to openGauss")
            return conn
        except Exception as e:
            logger.error(f"openGauss connection failed: {e}")
            raise

    def _process_sparse_vector(self, sparse_data: Union[dict, bytes, list], dimension: int) -> str:
        """Convert to openGauss SPARSEVEC format: '{indice:value,...}/dim'"""
        if sparse_data is None:
            return "NULL"

        try:
            # Convert to {index:value} dict
            if dimension is None or dimension <=0:
               dimension = self.MAX_SPARSE_DIMENSION

            sparse_dict = {}

            if isinstance(sparse_data, dict):
               sparse_dict = {
                  int(k+1): float(v)
                  for k, v in sparse_data.items()
               }
            else:
               raise ValueError(f"Unsupported format: {type(sparse_data)}")

            if not sparse_dict:
               return "{}/" + str(dimension)

            try:
               # Sort by index to ensure consistent output

               sorted_items = sorted(sparse_dict.items(), key=lambda x: x[0])
               entries = ",".join(f"{k}:{v}" for k, v in sorted_items)
               return "{" + entries + "}/" + str(dimension)
            except Exception as sort_error:
               logger.warning(f"Sorting failed, using unsorted vector: {sort_error}")
               entries = ",".join(f"{k}:{v}" for k, v in sparse_dict.items())
               return "{" + entries + "}/" + str(dimension)

        except Exception as e:
            logger.error(f"Sparse vector conversion failed: {e}")
            return "NULL"

    def _process_field_value(self, value: Any, field_type: str, dimension: Optional[int] = None) -> str:
        """Convert field value to CSV string"""
        if value is None:
            return "NULL"
        elif field_type == "SPARSE_FLOAT_VECTOR":
            return self._process_sparse_vector(value, dimension or self.SPARSE_DIMENSION)
        elif isinstance(value, (list, np.ndarray)):
            return "[" + ",".join(str(x) for x in value) + "]"
        elif isinstance(value, dict):
            return json.dumps(value)
        else:
            return str(value)

    def _process_field(self, collection: Collection, results: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Process raw Milvus query results into CSV-compatible format"""
        try:
            # 获取 schema 中每个字段的类型和维度
            field_meta = {
                f.name: {
                    "type": f.dtype.name.upper(),
                    "dim": getattr(f, 'dim', None)
                }
                for f in collection.schema.fields
            }

            processed = []
            for row in results:
                processed_row = {}
                for field in self.fields:
                    meta = field_meta.get(field, {"type": "TEXT", "dim": None})
                    value = row.get(field)

                    processed_row[field] = self._process_field_value(value, meta["type"], meta["dim"])

                processed.append(processed_row)

            return processed
        except Exception as e:
            logger.error(f"Field processing failed: {e}")
            raise

    def _create_opengauss_table(self, conn: psycopg2.extensions.connection, collection: Collection) -> None:
        """Create table with SPARSEVEC columns"""
        table_name = self.config.get('Table', 'opengauss_table_name')
        cursor = conn.cursor()

        try:
            # Check if table exists
            cursor.execute(f"SELECT EXISTS(SELECT 1 FROM pg_tables WHERE tablename = '{table_name}');")
            if cursor.fetchone()[0]:
                logger.warning(f"Table {table_name} exists, dropping it")
                cursor.execute(f"DROP TABLE {table_name};")

            # Build CREATE TABLE statement
            columns = []
            for field in collection.schema.fields:
               dim = field.dim if hasattr(field, 'dim') else None
               pg_type = self._milvus_to_opengauss_type(field.dtype.name, dim)
               columns.append(f"{field.name} {pg_type}")

            # Add primary key if exists
            pk_fields = [f.name for f in collection.schema.fields if f.is_primary]
            if pk_fields:
                columns.append(f"PRIMARY KEY ({', '.join(pk_fields)})")

            create_sql = f"CREATE TABLE {table_name} ({', '.join(columns)});"
            cursor.execute(create_sql)
            conn.commit()
            logger.info(f"Created table: {table_name}")
        except Exception as e:
            conn.rollback()
            logger.error(f"Table creation failed: {e}")
            raise
        finally:
            cursor.close()

    def _export_to_csv_chunked(self, collection: Collection) -> List[str]:
        file_paths = []
        chunk_id = 0
        total_rows = 0

        try:
            iterator = collection.query_iterator(batch_size=16384, output_fields=self.fields)
            while True:
                results = iterator.next()
                if not results:
                    break

                results = self._process_field(collection, results)
                chunk_id += 1
                chunk_file = f"{os.path.splitext(self.csv_file_path)[0]}_part{chunk_id}.csv"
                file_paths.append(chunk_file)

                with open(chunk_file, 'w', newline='') as f:
                    writer = csv.DictWriter(f, fieldnames=self.fields)
                    writer.writeheader()
                    writer.writerows(results)

                total_rows += len(results)
                logger.info(f"Created chunk {chunk_id}: {len(results)} rows, total {total_rows}")

            iterator.close()
            return file_paths
        except Exception as e:
            logger.error(f"Export failed: {e}")
            raise

    def _import_to_opengauss(self, conn: psycopg2.extensions.connection, file_paths: List[str]) -> None:
        """Import CSV data to openGauss"""
        table_name = self.config.get('Table', 'opengauss_table_name')
        cursor = conn.cursor()

        try:
            # Prepare for bulk import
            cursor.execute(f"TRUNCATE TABLE {table_name};")
            conn.commit()

            total_rows = 0
            for i, csv_file in enumerate(file_paths, 1):
                with open(csv_file, 'r') as f:
                    # Use COPY for bulk load
                    copy_sql = f"""
                    COPY {table_name} ({', '.join(self.fields)})
                    FROM STDIN WITH (FORMAT CSV, HEADER, NULL 'NULL');
                    """
                    cursor.copy_expert(copy_sql, f)
                    conn.commit()

                    rows_imported = cursor.rowcount
                    total_rows += rows_imported
                    logger.info(f"Imported {rows_imported} rows from {csv_file}")

            logger.info(f"Total imported: {total_rows} rows")
        except Exception as e:
            conn.rollback()
            logger.error(f"Import failed: {e}")
            raise
        finally:
            cursor.close()


    def _import_index(self, conn: psycopg2.extensions.connection, collection: Collection) -> None:
        cursor = conn.cursor()
        table_name = self.config.get('Table', 'opengauss_table_name')

        try:
            # 获取字段类型信息
            field_types = {field.name: field.dtype for field in collection.schema.fields}

            for idx in collection.indexes:
                index_name = idx.index_name
                field_name = idx.field_name
                index_params = idx.params
                index_type_name = index_params.get("index_type", "Unknown")
                index_type = self._milvus_to_opengauss_index_type(index_type_name)
                metric_type = index_params.get("metric_type", "L2")

                # 检查字段类型，只有向量类型才需要操作符类
                field_type = field_types.get(field_name)
                field_type_name = field_type.name
                is_vector_field = field_type_name in ["FLOAT_VECTOR", "BINARY_VECTOR", "SPARSE_FLOAT_VECTOR"]

                # 构建索引参数
                index_with_clause = self._build_index_with_clause(index_type_name, index_params)

                if field_type_name == "FLOAT_VECTOR":
                    # 映射度量类型到对应的操作符类
                    if index_type == "AUTOINDEX":
                        index_type = "IVFFLAT"
                    opclass_map = {
                        "L2": "vector_l2_ops",
                        "IP": "vector_ip_ops",
                        "COSINE": "vector_cosine_ops"
                    }
                    opclass = opclass_map.get(metric_type.upper(), "vector_l2_ops")

                    logger.info(f"Migrating vector index {index_name} on {field_name}, "
                                f"index type: {index_type}, metric: {metric_type}")

                    # 向量索引：包含操作符类和参数
                    index_sql = f"""
                    CREATE INDEX {index_name}_ogidx
                    ON {table_name}
                    USING {index_type}({field_name} {opclass})
                    {index_with_clause}
                    """

                elif field_type_name == "SPARSE_FLOAT_VECTOR":
                    opclass_map = {
                        "L2": "sparsevec_l2_ops",
                        "IP": "sparsevec_ip_ops",
                        "COSINE": "sparsevec_cosine_ops"
                    }
                    opclass = opclass_map.get(metric_type.upper(), "sparsevec_l2_ops")

                    logger.info(f"Migrating sparse vector index {index_name} on {field_name}, "
                                f"index type: {index_type}, metric: {metric_type}")

                    # 向量索引：包含操作符类和参数
                    index_sql = f"""
                    CREATE INDEX {index_name}_ogidx
                    ON {table_name}
                    USING {index_type}({field_name} {opclass})
                    {index_with_clause}
                    """

                else:
                    # 非向量字段：不需要操作符类，但可能包含参数
                    logger.info(f"Migrating non-vector index {index_name} on {field_name}, "
                                f"index type: {index_type}")

                    index_sql = f"""
                    CREATE INDEX {index_name}_ogidx
                    ON {table_name}
                    USING {index_type}({field_name})
                    {index_with_clause}
                    """
                cursor.execute(index_sql)

            conn.commit()
        except Exception as e:
            conn.rollback()
            logger.warning(f"Error occurred while creating index: {e}")
            logger.warning(f"Failed to create index {index_name} on {table_name}, please try it manually.")
        finally:
            cursor.close()


    def _build_index_with_clause(self, milvus_index_type: str, index_params: dict) -> str:
        """根据Milvus索引参数构建openGauss的WITH子句"""
        params = index_params.get("params", {})

        # 不同索引类型的参数映射
        if milvus_index_type == "IVF_FLAT" or milvus_index_type == "IVF_SQ8" or milvus_index_type == "IVF_PQ":
            # IVF系列索引：nlist -> lists
            nlist = params.get("nlist")
            if nlist:
                return f"WITH (lists = {nlist})"

        elif milvus_index_type == "HNSW":
            # HNSW索引：M -> m, efConstruction -> ef_construction
            with_params = []

            M = params.get("M")
            if M:
                with_params.append(f"m = {M}")

            ef_construction = params.get("efConstruction")
            if ef_construction:
                with_params.append(f"ef_construction = {ef_construction}")

            if with_params:
                return "WITH (" + ", ".join(with_params) + ")"

        elif milvus_index_type == "DISKANN":
            # DISKANN索引：没有额外参数
            return ""

        elif milvus_index_type == "FLAT":
            # FLAT索引：没有额外参数
            return ""

        elif milvus_index_type == "SCANN":
            # SCANN索引：没有额外参数或需要特殊处理
            return ""

        # 默认返回空字符串
        return ""


    def run_migration(self) -> None:
        """Execute full migration workflow"""
        start_time = datetime.now()
        logger.info("Starting migration")

        try:
            # Step 1: Connect to Milvus
            milvus_collection = self._connect_milvus()

            # Step 2: Export data to CSV
            csv_files = self._export_to_csv_chunked(milvus_collection)

            # Step 3: Connect to openGauss and create table
            opengauss_conn = self._connect_opengauss()
            self._create_opengauss_table(opengauss_conn, milvus_collection)

            # Step 4: Import to openGauss
            self._import_to_opengauss(opengauss_conn, csv_files)

            # Step 5: Import Index
            if self.config.getboolean('openGauss', 'create_index', fallback=False):
                self._import_index(opengauss_conn, milvus_collection)

            # Cleanup
            if self.config.getboolean('Migration', 'cleanup_temp_files', fallback=True):
                for f in csv_files:
                    try:
                        os.remove(f)
                    except Exception as e:
                        logger.warning(f"Failed to delete {f}: {e}")

            logger.info(f"Migration completed in {datetime.now() - start_time}")
        except Exception as e:
            logger.error(f"Migration failed: {e}")
            raise
        finally:
            if 'opengauss_conn' in locals():
                opengauss_conn.close()
            connections.disconnect("default")

    @staticmethod
    def _milvus_to_opengauss_type(milvus_type: str, dim: Optional[int] = None) -> str:
        """Map Milvus types to openGauss types"""
        type_map = {
            "Int64": "BIGINT",
            "Int32": "INTEGER",
            "Int16": "SMALLINT",
            "Int8": "SMALLINT",
            "Float": "REAL",
            "Double": "DOUBLE PRECISION",
            "Bool": "BOOLEAN",
            "VarChar": "VARCHAR",
            "String": "TEXT",
            "Json": "JSONB",
            "FLOAT_VECTOR": f"VECTOR({dim})" if dim else "VECTOR",
            "BINARY_VECTOR": f"BIT({dim})" if dim else "BIT",
            "SPARSE_FLOAT_VECTOR": f"SPARSEVEC({dim})" if dim else "SPARSEVEC"
        }
        return type_map.get(milvus_type, "TEXT")

    @staticmethod
    def _milvus_to_opengauss_index_type(milvus_index_type: str) -> str:
        """Map Milvus index types to openGauss index types"""
        index_type_map = {
            # 稠密向量索引类型映射
            "IVF_FLAT": "IVFFLAT",
            "IVF_SQ8": "IVFSQ8",
            "IVF_PQ": "IVFPQ",
            "HNSW": "HNSW",
            "DISKANN": "DISKANN",
            "FLAT": "IVFFLAT",

            # 稀疏向量索引类型映射
            "SPARSE_INVERTED_INDEX": "SPARSE_INVERTED_INDEX",
            "SPARSE_BM25": "SPARSE_BM25",

            # 标量索引类型映射
            "Trie": "BTREE",
            "STL_SORT": "BTREE",
            "INVERTED": "GIN",
            "BIN_FLAT": "IVFFLAT",
            "BIN_IVF_FLAT": "IVFFLAT",

            # 默认映射
            "GPU_IVF_FLAT": "IVFFLAT",
            "GPU_IVF_PQ": "IVFPQ",
            "GPU_BRUTE_FORCE": "IVFFLAT",
        }

        try:
            result = index_type_map[milvus_index_type]
            logger.info(f"Mapped Milvus index type '{milvus_index_type}' to openGauss '{result}'")
            return result
        except KeyError:
            # 对于未知的稀疏向量索引类型，默认使用GIN索引
            logger.warning(
                f"Warning: Milvus sparse index type '{milvus_index_type}' is not found in mapping, using GIN as default.")
            raise
        except Exception as e:
            logger.error(f"Error mapping index type '{milvus_index_type}': {e}")
            raise

if __name__ == "__main__":
    try:
        migrator = MilvusToOpenGaussMigrator()
        migrator.run_migration()
    except Exception as e:
        logger.error(f"Migration failed: {e}")
        exit(1)