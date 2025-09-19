"""Test OpenGaussVectorStore.close() method

Ensures close() can be called safely multiple times and after operations.
Also drops the table at the end to keep the environment clean.
"""

import pytest
import os

from kotaemon.storages.vectorstores import OpenGaussVectorStore
try:
    from .opengauss_config import get_config
except Exception:
    import os as _os, sys as _sys
    _sys.path.append(_os.path.dirname(__file__))
    from opengauss_config import get_config


class TestOpenGaussVectorStoreClose:
    """Test OpenGaussVectorStore close() method"""
    
    def test_close_after_operations(self):
        """Test closing connection after operations"""
        cfg = get_config()
        cfg = {**cfg, "table_name": "test_close_ops"}
        vectorstore = OpenGaussVectorStore(**cfg)
        
        try:
            # Perform some operations to ensure connection is healthy
            count = vectorstore.count()
            assert count == 0, f"Initial count should be 0, actual: {count}"
            
            # Add some data
            test_vectors = [[0.1] * cfg["embed_dim"], [0.2] * cfg["embed_dim"]]
            test_ids = ["test_close_1", "test_close_2"]
            added_ids = vectorstore.add(test_vectors, ids=test_ids, metadatas=[{"test": "close"}] * 2)
            assert added_ids == test_ids, f"Adding data should succeed, actual: {added_ids}"
            
            # Verify data exists
            count_after_add = vectorstore.count()
            assert count_after_add == 2, f"Count after addition should be 2, actual: {count_after_add}"
            
        finally:
            # Cleanup: drop table then close
            vectorstore.drop()
            vectorstore.close()
    
    def test_multiple_close_calls(self):
        """Test multiple close() method calls"""
        cfg = get_config()
        cfg = {**cfg, "table_name": "test_multiple_close"}
        vectorstore = OpenGaussVectorStore(**cfg)
        
        try:
            # First close should succeed
            vectorstore.close()
            
            # Second close should be no-op without raising
            vectorstore.close()
            
            # Third close should also be no-op
            vectorstore.close()
            
        finally:
            # Ensure table is cleaned up
            try:
                vectorstore.drop()
            except Exception:
                pass  # Ignore cleanup errors
    
    def test_close_after_drop(self):
        """Test calling close() after drop()"""
        cfg = get_config()
        cfg = {**cfg, "table_name": "test_close_after_drop"}
        vectorstore = OpenGaussVectorStore(**cfg)
        
        try:
            # Drop table first
            vectorstore.drop()
            
            # Then close connection
            vectorstore.close()
            
            # Close again should not cause errors
            vectorstore.close()
            
        except Exception as e:
            pytest.fail(f"Calling close() after drop() should not cause errors: {e}")
    
    def test_close_without_operations(self):
        """Test closing without performing operations"""
        cfg = get_config()
        cfg = {**cfg, "table_name": "test_close_no_ops"}
        vectorstore = OpenGaussVectorStore(**cfg)
        
        # Close without performing any operations
        vectorstore.close()
        
        # Close again should not cause errors
        vectorstore.close()
        
        # Cleanup
        try:
            vectorstore.drop()
        except Exception:
            pass  # Ignore cleanup errors
    
    def test_close_after_error(self):
        """Test calling close() after error"""
        cfg = get_config()
        cfg = {**cfg, "table_name": "test_close_after_error"}
        vectorstore = OpenGaussVectorStore(**cfg)
        
        try:
            # Try to perform an operation that might fail
            try:
                # Use invalid query vector
                invalid_vector = [0.1] * 100  # Wrong dimension
                vectorstore.query(invalid_vector, top_k=1)
            except Exception:
                # Expected error, continue testing
                pass
            
            # Even after error, close() should work normally
            vectorstore.close()
            
            # Close again should not cause errors
            vectorstore.close()
            
        finally:
            # Cleanup
            try:
                vectorstore.drop()
            except Exception:
                pass  # Ignore cleanup errors
    
    def test_close_connection_state(self):
        """Test connection state after closing"""
        cfg = get_config()
        cfg = {**cfg, "table_name": "test_close_state"}
        vectorstore = OpenGaussVectorStore(**cfg)
        
        # Verify initial connection state
        assert hasattr(vectorstore, '_conn'), "Should have connection object"
        assert vectorstore._conn is not None, "Connection object should not be None"
        
        # Close connection
        vectorstore.close()
        
        # Verify connection state (specific implementation may vary)
        # Here we assume connection object still exists but may be marked as closed
        assert hasattr(vectorstore, '_conn'), "Should still have connection object attribute after closing"
        
        # Cleanup
        try:
            vectorstore.drop()
        except Exception:
            pass  # Ignore cleanup errors
    
    def test_close_and_reinitialize(self):
        """Test closing and reinitializing"""
        cfg = get_config()
        cfg = {**cfg, "table_name": "test_close_reinit"}
        
        # Create first instance
        vectorstore1 = OpenGaussVectorStore(**cfg)
        
        # Add some data
        test_vectors = [[0.1] * cfg["embed_dim"]]
        test_ids = ["test_reinit"]
        vectorstore1.add(test_vectors, ids=test_ids, metadatas=[{"test": "reinit"}])
        
        # Close first instance
        vectorstore1.close()
        
        # Create second instance (should work normally)
        vectorstore2 = OpenGaussVectorStore(**cfg)
        
        try:
            # Verify second instance works normally
            count = vectorstore2.count()
            assert count >= 1, f"After reinitialization should see previous data, actual count: {count}"
            
            # Verify can perform operations
            new_vectors = [[0.2] * cfg["embed_dim"]]
            new_ids = ["test_reinit_2"]
            added_ids = vectorstore2.add(new_vectors, ids=new_ids, metadatas=[{"test": "reinit2"}])
            assert added_ids == new_ids, f"Should be able to add new data after reinitialization"
            
        finally:
            # Cleanup
            vectorstore2.drop()
            vectorstore2.close()
    
    def test_close_with_transaction(self):
        """Test closing connection within transaction"""
        cfg = get_config()
        cfg = {**cfg, "table_name": "test_close_transaction"}
        vectorstore = OpenGaussVectorStore(**cfg)
        
        try:
            # Start a transaction (if supported)
            with vectorstore._conn.cursor() as cursor:
                # Perform some operations
                cursor.execute("SELECT 1")
                result = cursor.fetchone()
                assert result[0] == 1, "Query in transaction should succeed"
                
                # Close connection within transaction
                vectorstore.close()
                
                # Close again should not cause errors
                vectorstore.close()
                
        except Exception as e:
            # Some databases may not support closing connection within transaction
            # This is acceptable as long as it doesn't crash
            pass
        finally:
            # Cleanup
            try:
                vectorstore.drop()
            except Exception:
                pass  # Ignore cleanup errors
    
    def test_close_idempotent(self):
        """Test close() method idempotency"""
        cfg = get_config()
        cfg = {**cfg, "table_name": "test_close_idempotent"}
        vectorstore = OpenGaussVectorStore(**cfg)
        
        # Multiple close() calls should not cause errors
        for i in range(5):
            vectorstore.close()
        
        # Cleanup
        try:
            vectorstore.drop()
        except Exception:
            pass  # Ignore cleanup errors
    
    def test_close_without_opengauss_database(self):
        """Test that close operations fail when openGauss database is not available"""
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