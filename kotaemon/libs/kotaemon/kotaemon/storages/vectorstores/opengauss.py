from typing import Any, Dict, List, Optional, Union, Tuple
import psycopg2
from psycopg2 import sql
import numpy as np
import json
from datetime import datetime
import uuid

from kotaemon.base import DocumentWithEmbedding
from .base import BaseVectorStore


class OpenGaussVectorStore(BaseVectorStore):
    """OpenGauss vector store implementation for kotaemon framework.
    
    This implementation provides vector similarity search capabilities using OpenGauss
    with the vector extension. It follows the kotaemon BaseVectorStore interface
    and provides efficient vector operations.
    """

    def __init__(
        self,
        host: str = "localhost",
        port: int = 5432,
        database: str = "vectordb",
        user: str = "gaussdb",
        password: str = "password",
        table_name: str = "vectors",
        schema_name: str = "public",
        embed_dim: int = 1536,
        ssl: bool = False,
        **kwargs: Any,
    ):
        """Initialize OpenGauss vector store.
        
        Args:
            host: OpenGauss server host
            port: OpenGauss server port
            database: Database name
            user: Database username
            password: Database password
            table_name: Table name for storing vectors
            schema_name: Schema name
            embed_dim: Dimension of the embedding vectors
            ssl: Whether to use SSL connection
            **kwargs: Additional arguments
        """
        self._host = host
        self._port = port
        self._database = database
        self._user = user
        self._password = password
        self._table_name = table_name
        self._schema_name = schema_name
        self._embed_dim = embed_dim
        self._ssl = ssl
        self._kwargs = kwargs
        
        # Initialize connection and create table
        self._initialize_connection_and_table()

    def _initialize_connection_and_table(self):
        """Initialize the OpenGauss connection and create the vector table if it doesn't exist"""
        try:
            # Create connection
            self._conn = psycopg2.connect(
                host=self._host,
                port=self._port,
                database=self._database,
                user=self._user,
                password=self._password,
                sslmode='require' if self._ssl else 'disable'
            )
            self._conn.autocommit = True

            # Create schema if it doesn't exist
            with self._conn.cursor() as cursor:
                cursor.execute(f"CREATE SCHEMA IF NOT EXISTS {self._schema_name}")

            # Create vector table if it doesn't exist
            self._create_vector_table()
            
        except psycopg2.Error as e:
            raise ConnectionError(f"Failed to connect to OpenGauss database: {e}")

    def _create_vector_table(self):
        """Create the vector table only if it does not exist (idempotent)."""
        with self._conn.cursor() as cursor:
            # Create table only when it is missing to avoid data loss on restarts
            cursor.execute(f"""
                CREATE TABLE IF NOT EXISTS {self._schema_name}.{self._table_name} (
                    id VARCHAR(255) PRIMARY KEY,
                    embedding vector({self._embed_dim}),
                    metadata JSONB DEFAULT '{{}}'::jsonb
                );
            """)

            # Create index for efficient similarity search (prefer HNSW if available)
            try:
                cursor.execute(f"""
                    CREATE INDEX IF NOT EXISTS idx_{self._table_name}_embedding 
                    ON {self._schema_name}.{self._table_name} 
                    USING hnsw (embedding vector_l2_ops);
                """)
            except Exception as e:
                # Fall back to a regular index if HNSW is unavailable
                print(f"Warning: HNSW index creation failed: {e}")
                cursor.execute(f"""
                    CREATE INDEX IF NOT EXISTS idx_{self._table_name}_embedding 
                    ON {self._schema_name}.{self._table_name} (embedding);
                """)

            self._conn.commit()

    def add(
        self,
        embeddings: Union[List[List[float]], List[DocumentWithEmbedding]],
        ids: Optional[List[str]] = None,
        metadatas: Optional[List[Dict]] = None,
        **kwargs,
    ) -> List[str]:
        """Add vector embeddings to the vector store.
        
        Args:
            embeddings: List of embedding vectors or DocumentWithEmbedding objects
            ids: Optional list of ids for the embeddings
            metadatas: Optional list of metadata dictionaries
            **kwargs: Additional parameters
            
        Returns:
            List of added ids
        """
        if not embeddings:
            return []
        
        try:
            # Handle different input types
            if isinstance(embeddings[0], DocumentWithEmbedding):
                # Extract embeddings and metadata from DocumentWithEmbedding objects
                docs = embeddings
                embedding_vectors = [doc.embedding for doc in docs]
                if ids is None:
                    ids = [doc.doc_id for doc in docs]
                if metadatas is None:
                    metadatas = [doc.metadata for doc in docs]
            else:
                # Handle raw embedding lists
                embedding_vectors = embeddings
                if ids is None:
                    ids = [str(uuid.uuid4()) for _ in embeddings]
                if metadatas is None:
                    metadatas = [{} for _ in embeddings]
            
            with self._conn.cursor() as cursor:
                # Construct INSERT SQL statement
                insert_sql = "INSERT INTO {}.{} (id, embedding, metadata) VALUES (%s, %s, %s)".format(
                    self._schema_name, self._table_name
                )
                
                # Iterate through each data item and insert one by one
                for emb, doc_id, metadata in zip(embedding_vectors, ids, metadatas):
                    # Convert metadata to JSON string, default to '{}' if None or empty
                    metadata_json = json.dumps(metadata) if metadata else '{}'
                    embedding_str = '[' + ','.join(map(str, emb)) + ']'
                    
                    # Execute single insert
                    cursor.execute(insert_sql, (doc_id, embedding_str, metadata_json))
                
                    self._conn.commit()
                
            return ids
            
        except Exception as e:
            print(f"Error adding embeddings: {e}")
            # Rollback on error
            if self._conn:
                self._conn.rollback()
            raise

    def query(
        self,
        embedding: List[float],
        top_k: int = 1,
        ids: Optional[List[str]] = None,
        metadata_filter: Optional[Dict] = None,
        **kwargs,
    ) -> Tuple[List[List[float]], List[float], List[str]]:
        """Query the vector store for similar vectors with optional metadata filtering.
        
        Args:
            embedding: Query embedding vector
            top_k: Number of most similar vectors to return
            ids: Optional list of ids to restrict search to
            metadata_filter: Optional metadata filter dictionary
            **kwargs: Additional query parameters
            
        Returns:
            Tuple of (embeddings, similarities, ids)
        """
        try:
            # Build WHERE conditions and query parameters
            where_conditions = []
            query_params = []
            
            # Compatibility with different parameter names: doc_ids / ids
            if ids is None:
                ids = kwargs.pop("doc_ids", None)

            if ids:
                placeholders = ','.join(['%s'] * len(ids))
                where_conditions.append(f"id IN ({placeholders})")
                query_params.extend(ids)
            
            if metadata_filter:
                # Add metadata filtering conditions
                for key, value in metadata_filter.items():
                    if isinstance(value, (list, tuple)):
                        # Handle list values (e.g., tags) - use JSONB array contains operator
                        if len(value) == 1:
                            # Single value: use ? operator for array contains
                            where_conditions.append(f"metadata->'{key}' ? %s")
                            query_params.append(value[0])
                        else:
                            # Multiple values: use ?| operator for array contains any
                            placeholders = ','.join(['%s'] * len(value))
                            where_conditions.append(f"metadata->'{key}' ?| ARRAY[{placeholders}]")
                            query_params.extend(value)
                    elif isinstance(value, dict) and 'range' in value:
                        # Handle range queries (e.g., date ranges)
                        if 'min' in value and 'max' in value:
                            where_conditions.append(f"metadata->>'{key}' BETWEEN %s AND %s")
                            query_params.extend([value['min'], value['max']])
                        elif 'min' in value:
                            where_conditions.append(f"metadata->>'{key}' >= %s")
                            query_params.append(value['min'])
                        elif 'max' in value:
                            where_conditions.append(f"metadata->>'{key}' <= %s")
                            query_params.append(str(value['max']))
                    else:
                        # Simple equality match
                        where_conditions.append(f"metadata->>'{key}' = %s")
                        query_params.append(str(value))
            
            # Build the complete WHERE clause
            where_clause = " AND ".join(where_conditions) if where_conditions else "1=1"
            
            with self._conn.cursor() as cursor:
                # Reorganize SQL statement and parameter order
                # Parameter order: vector parameter + WHERE condition parameters + vector parameter + top_k parameter
                final_params = [embedding] + query_params + [embedding, top_k]
                
                # Reorganize SQL statement and parameter order
                sql_query = f"""
                    SELECT id, embedding, metadata,
                           embedding <-> CAST(%s AS vector) as distance
                    FROM {self._schema_name}.{self._table_name} 
                    WHERE {where_clause}
                    ORDER BY embedding <-> CAST(%s AS vector) 
                    LIMIT %s
                    """
                
                cursor.execute(sql_query, final_params)
                
                results = cursor.fetchall()
                
                if not results:
                    return [], [], []
                
                # Extract results
                result_ids = [row[0] for row in results]
                result_embeddings = [row[1] for row in results]
                distances = [float(row[3]) for row in results]  # distance is at index 3
                
                # Convert distances to similarities (1 / (1 + distance))
                similarities = [1.0 / (1.0 + dist) for dist in distances]
                
                return result_embeddings, similarities, result_ids
                
        except Exception as e:
            print(f"Error querying vector store: {e}")
            raise

    def delete(self, ids: List[str], **kwargs):
        """Delete vector embeddings from the vector store.
        
        Args:
            ids: List of ids of the embeddings to be deleted
            **kwargs: Additional parameters
        """
        if not ids:
            return
            
        try:
            with self._conn.cursor() as cursor:
                placeholders = ','.join(['%s'] * len(ids))
                cursor.execute(
                    f"DELETE FROM {self._schema_name}.{self._table_name} WHERE id IN ({placeholders})",
                    ids
                )
        except Exception as e:
            print(f"Error deleting vectors: {e}")
            raise

    def drop(self):
        """Delete the entire vector table from the database."""
        try:
            with self._conn.cursor() as cursor:
                cursor.execute(f"DROP TABLE IF EXISTS {self._schema_name}.{self._table_name}")
        except Exception as e:
            print(f"Error dropping table: {e}")
            raise

    def count(self) -> int:
        """Get the total count of vectors in the store."""
        try:
            with self._conn.cursor() as cursor:
                cursor.execute(f"SELECT COUNT(*) FROM {self._schema_name}.{self._table_name}")
                return cursor.fetchone()[0]
        except Exception as e:
            print(f"Error counting vectors: {e}")
            return 0

    def get_metadata(self, ids: List[str]) -> Dict[str, Dict]:
        """Get metadata for specific vector IDs.
        
        Args:
            ids: List of vector IDs to retrieve metadata for
            
        Returns:
            Dictionary mapping vector IDs to their metadata
        """
        if not ids:
            return {}
            
        try:
            with self._conn.cursor() as cursor:
                placeholders = ','.join(['%s'] * len(ids))
                cursor.execute(
                    f"""
                    SELECT id, metadata 
                    FROM {self._schema_name}.{self._table_name} 
                    WHERE id IN ({placeholders})
                    """,
                    ids
                )
                
                results = cursor.fetchall()
                metadata_dict = {}
                for row in results:
                    vector_id, metadata = row
                    # Parse JSON metadata back to Python dict
                    if metadata:
                        try:
                            metadata_dict[vector_id] = json.loads(metadata) if isinstance(metadata, str) else metadata
                        except (json.JSONDecodeError, TypeError):
                            metadata_dict[vector_id] = metadata
                    else:
                        metadata_dict[vector_id] = {}
                
                return metadata_dict
                
        except Exception as e:
            print(f"Error getting metadata: {e}")
            return {}

    def update_metadata(self, id: str, metadata: Dict) -> bool:
        """Update metadata for a specific vector ID.
        
        Args:
            id: Vector ID to update metadata for
            metadata: New metadata dictionary
            
        Returns:
            True if update was successful, False otherwise
        """
        try:
            with self._conn.cursor() as cursor:
                metadata_json = json.dumps(metadata) if metadata else '{}'
                cursor.execute(
                    f"""
                    UPDATE {self._schema_name}.{self._table_name} 
                    SET metadata = %s 
                    WHERE id = %s
                    """,
                    [metadata_json, id]
                )
                return cursor.rowcount > 0
        except Exception as e:
            print(f"Error updating metadata for ID {id}: {e}")
            return False

    def search_by_metadata(self, metadata_filter: Dict, limit: int = 100) -> List[str]:
        """Search for vector IDs based on metadata criteria.
        
        Args:
            metadata_filter: Metadata filter dictionary
            limit: Maximum number of results to return
            
        Returns:
            List of vector IDs matching the metadata criteria
        """
        try:
            where_conditions = []
            query_params = []
            
            for key, value in metadata_filter.items():
                if isinstance(value, (list, tuple)):
                    # Handle list values (e.g., tags)
                    placeholders = ','.join(['%s'] * len(value))
                    where_conditions.append(f"metadata->>'{key}' IN ({placeholders})")
                    query_params.extend(value)
                elif isinstance(value, dict) and 'range' in value:
                    # Handle range queries
                    if 'min' in value and 'max' in value:
                        where_conditions.append(f"metadata->>'{key}' BETWEEN %s AND %s")
                        query_params.extend([value['min'], value['max']])
                    elif 'min' in value:
                        where_conditions.append(f"metadata->>'{key}' >= %s")
                        query_params.append(value['min'])
                    elif 'max' in value:
                        where_conditions.append(f"metadata->>'{key}' <= %s")
                        query_params.append(str(value['max']))
                else:
                    # Simple equality match
                    where_conditions.append(f"metadata->>'{key}' = %s")
                    query_params.append(str(value))
            
            where_clause = " AND ".join(where_conditions) if where_conditions else "1=1"
            
            with self._conn.cursor() as cursor:
                cursor.execute(
                    f"""
                    SELECT id 
                    FROM {self._schema_name}.{self._table_name} 
                    WHERE {where_clause}
                    LIMIT %s
                    """,
                    query_params + [limit]
                )
                
                results = cursor.fetchall()
                return [row[0] for row in results]
                
        except Exception as e:
            print(f"Error searching by metadata: {e}")
            return []

    def close(self):
        """Close the database connection."""
        if hasattr(self, '_conn') and self._conn:
            try:
                self._conn.close()
            except Exception as e:
                print(f"Error closing connection: {e}")

    def __del__(self):
        """Cleanup when the object is destroyed."""
        self.close()

    def __persist_flow__(self):
        """Persist the flow configuration for serialization."""
        return {
            "host": self._host,
            "port": self._port,
            "database": self._database,
            "user": self._user,
            "password": self._password,
            "table_name": self._table_name,
            "schema_name": self._schema_name,
            "embed_dim": self._embed_dim,
            "ssl": self._ssl,
            **self._kwargs,
        }