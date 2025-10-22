#!/usr/bin/env python3
"""Test OpenGaussVectorStore initialization functionality

Configuration is loaded from opengauss_config. At the end of the flow the table is dropped and connection is closed.
"""

import pytest
import sys
import os

# Add project root to sys.path for direct execution
sys.path.append(os.path.join(os.path.dirname(__file__), '..', '..', '..', '..'))

from kotaemon.storages.vectorstores import OpenGaussVectorStore
try:
    from .opengauss_config import get_config
except Exception:
    import os as _os, sys as _sys
    _sys.path.append(_os.path.dirname(__file__))
    from opengauss_config import get_config


class TestOpenGaussVectorStoreInit:
    """Test OpenGaussVectorStore initialization functionality"""
    
    def test_initialization(self):
        """Test initialization and table/index creation"""
        cfg = get_config()
        # Use dedicated table for this test
        cfg = {**cfg, "table_name": "test_init_test"}
        vectorstore = OpenGaussVectorStore(**cfg)
        
        try:
            # Verify connection was established
            assert hasattr(vectorstore, '_conn'), "Vector store should have connection object"
            assert vectorstore._conn is not None, "Connection object should not be None"
            
            # Verify table exists
            with vectorstore._conn.cursor() as cursor:
                cursor.execute("""
                    SELECT EXISTS (
                        SELECT 1 FROM information_schema.tables
                        WHERE table_schema = 'public'
                        AND table_name = 'test_init_test'
                    );
                """)
                table_exists = cursor.fetchone()[0]
                assert table_exists, "Table should exist"
            
            # Check column structure
            with vectorstore._conn.cursor() as cursor:
                cursor.execute("""
                    SELECT column_name, data_type, is_nullable
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                    AND table_name = 'test_init_test'
                    ORDER BY ordinal_position;
                """)
                columns = cursor.fetchall()
                
                # Verify necessary columns exist
                column_names = [col[0] for col in columns]
                assert 'id' in column_names, "Table should have 'id' column"
                assert 'embedding' in column_names, "Table should have 'embedding' column"
                assert 'metadata' in column_names, "Table should have 'metadata' column"
            
            # Check indexes
            with vectorstore._conn.cursor() as cursor:
                cursor.execute("""
                    SELECT indexname, indexdef
                    FROM pg_indexes
                    WHERE tablename = 'test_init_test';
                """)
                indexes = cursor.fetchall()
                # Indexes may be empty, but should not cause errors
                assert isinstance(indexes, list), "Index query should return list"
            
            # Verify normal operations work
            initial_count = vectorstore.count()
            assert initial_count == 0, f"Initial count should be 0, actual: {initial_count}"
            
        finally:
            # Cleanup
            vectorstore.drop()
            vectorstore.close()
    
    def test_table_recreation(self):
        """Test re-initialization after drop to ensure table recreation"""
        cfg = get_config()
        cfg = {**cfg, "table_name": "test_recreate_table", "embed_dim": 1536}
        
        # Create first instance
        vectorstore1 = OpenGaussVectorStore(**cfg)
        assert vectorstore1 is not None, "First instance should be created successfully"
        
        # Verify initial state
        count1 = vectorstore1.count()
        assert count1 == 0, f"First instance initial count should be 0, actual: {count1}"
        
        # Drop table and close first instance
        vectorstore1.drop()
        vectorstore1.close()
        
        # Create second instance (recreate table with different dimension)
        cfg2 = get_config()
        cfg2 = {**cfg2, "table_name": "test_recreate_table", "embed_dim": 3}
        vectorstore2 = OpenGaussVectorStore(**cfg2)
        assert vectorstore2 is not None, "Second instance should be created successfully"
        
        # Verify recreated state
        count2 = vectorstore2.count()
        assert count2 == 0, f"Count after recreation should be 0, actual: {count2}"
        
        # Verify normal operations work with correct dimension
        test_vectors = [[0.1, 0.2, 0.3], [0.4, 0.5, 0.6]]
        test_ids = ["recreate_1", "recreate_2"]
        added_ids = vectorstore2.add(test_vectors, ids=test_ids, metadatas=[{"test": "recreation"}] * 2)
        assert added_ids == test_ids, f"Should be able to add data normally after recreation"
        
        # Cleanup
        vectorstore2.drop()
        vectorstore2.close()
    
    def test_initialization_with_different_configs(self):
        """Test initialization with different configurations"""
        base_cfg = get_config()
        
        # Test different table names
        cfg1 = {**base_cfg, "table_name": "test_config_1"}
        vectorstore1 = OpenGaussVectorStore(**cfg1)
        assert vectorstore1 is not None, "Should be able to initialize with different table name"
        
        # Test different embedding dimensions
        cfg2 = {**base_cfg, "table_name": "test_config_2", "embed_dim": 512}
        vectorstore2 = OpenGaussVectorStore(**cfg2)
        assert vectorstore2 is not None, "Should be able to initialize with different embedding dimension"
        
        # Verify both instances work normally
        count1 = vectorstore1.count()
        count2 = vectorstore2.count()
        assert count1 == 0, f"First instance count should be 0, actual: {count1}"
        assert count2 == 0, f"Second instance count should be 0, actual: {count2}"
        
        # Cleanup
        vectorstore1.drop()
        vectorstore1.close()
        vectorstore2.drop()
        vectorstore2.close()
    
    def test_connection_parameters(self):
        """Test connection parameters correctness"""
        cfg = get_config()
        cfg = {**cfg, "table_name": "test_connection_params"}
        vectorstore = OpenGaussVectorStore(**cfg)
        
        try:
            # Verify connection parameters
            assert hasattr(vectorstore, '_conn'), "Should have connection object"
            if hasattr(vectorstore._conn, 'get_dsn_parameters'):
                dsn_params = vectorstore._conn.get_dsn_parameters()
                assert 'host' in dsn_params, "DSN parameters should include host"
                assert 'port' in dsn_params, "DSN parameters should include port"
                assert 'dbname' in dsn_params, "DSN parameters should include dbname"
                assert 'user' in dsn_params, "DSN parameters should include user"
            
            # Verify configuration parameters
            assert vectorstore._table_name == "test_connection_params", f"Table name should be correct, actual: {vectorstore._table_name}"
            assert vectorstore._embed_dim == cfg["embed_dim"], f"Embedding dimension should be correct, actual: {vectorstore._embed_dim}"
            
        finally:
            vectorstore.drop()
            vectorstore.close()
    
    def test_initialization_error_handling(self):
        """Test initialization error handling"""
        # Test invalid configuration
        invalid_cfg = {
            "host": "invalid_host",
            "port": 5432,
            "database": "invalid_db",
            "user": "invalid_user",
            "password": "invalid_password",
            "table_name": "test_error",
            "schema_name": "public",
            "embed_dim": 1536
        }
        
        # Using invalid configuration should raise exception
        with pytest.raises(Exception):
            OpenGaussVectorStore(**invalid_cfg)
    
    def test_schema_and_table_creation(self):
        """Test schema and table creation"""
        cfg = get_config()
        cfg = {**cfg, "table_name": "test_schema_creation"}
        vectorstore = OpenGaussVectorStore(**cfg)
        
        try:
            # Verify schema exists
            with vectorstore._conn.cursor() as cursor:
                cursor.execute("""
                    SELECT EXISTS (
                        SELECT 1 FROM information_schema.schemata
                        WHERE schema_name = %s
                    );
                """, (cfg["schema_name"],))
                schema_exists = cursor.fetchone()[0]
                assert schema_exists, f"Schema {cfg['schema_name']} should exist"
            
            # Verify table exists in correct schema
            with vectorstore._conn.cursor() as cursor:
                cursor.execute("""
                    SELECT table_schema, table_name
                    FROM information_schema.tables
                    WHERE table_name = %s;
                """, (cfg["table_name"],))
                result = cursor.fetchone()
                assert result is not None, f"Table {cfg['table_name']} should exist"
                assert result[0] == cfg["schema_name"], f"Table should be in schema {cfg['schema_name']}, actual: {result[0]}"
            
        finally:
            vectorstore.drop()
            vectorstore.close()
    
    def test_initialization_without_opengauss_database(self):
        """Test that initialization fails when openGauss database is not available"""
        # Use invalid database configuration to simulate database unavailability
        invalid_cfg = {
            "host": "invalid_host",
            "port": 5432,
            "database": "invalid_db",
            "user": "invalid_user",
            "password": "invalid_password",
            "table_name": "test_invalid",
            "schema_name": "public",
            "embed_dim": 1536
        }
        
        # OpenGaussVectorStore initialization should fail with invalid configuration
        with pytest.raises(Exception):
            OpenGaussVectorStore(**invalid_cfg)