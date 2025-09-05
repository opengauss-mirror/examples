"""Test OpenGaussVectorStore.drop() method

Verifies that dropping the table succeeds and that re-initialization recreates the table.
Configuration is loaded from opengauss_config.
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


class TestOpenGaussVectorStoreDrop:
    """Test OpenGaussVectorStore drop() method"""
    
    def test_drop_table(self):
        """Test table dropping functionality"""
        cfg = get_config()
        cfg = {**cfg, "table_name": "test_drop_test"}
        vectorstore = OpenGaussVectorStore(**cfg)
        
        # Verify table was created
        initial_count = vectorstore.count()
        assert initial_count == 0, f"Initial count should be 0, actual: {initial_count}"
        
        # Drop table
        vectorstore.drop()
        
        # Verify table was dropped (by trying to reinitialize should succeed)
        try:
            # Try to reinitialize should succeed (recreate table)
            new_vectorstore = OpenGaussVectorStore(**cfg)
            new_count = new_vectorstore.count()
            assert new_count == 0, f"Count after reinitialization should be 0, actual: {new_count}"
            
            # Cleanup
            new_vectorstore.drop()
            new_vectorstore.close()
        except Exception as e:
            pytest.fail(f"Reinitialization failed: {e}")
        finally:
            vectorstore.close()
    
    def test_drop_and_recreate(self):
        """Test dropping table and recreating"""
        cfg = get_config()
        cfg = {**cfg, "table_name": "test_drop_recreate"}
        
        # Create first instance
        vectorstore1 = OpenGaussVectorStore(**cfg)
        initial_count = vectorstore1.count()
        assert initial_count == 0, f"First instance initial count should be 0, actual: {initial_count}"
        
        # Drop table before closing connection
        vectorstore1.drop()
        
        # Close first instance
        vectorstore1.close()
        
        # Create second instance (should recreate table)
        vectorstore2 = OpenGaussVectorStore(**cfg)
        recreated_count = vectorstore2.count()
        assert recreated_count == 0, f"Count after recreation should be 0, actual: {recreated_count}"
        
        # Verify normal operations work
        test_vectors = [[0.1] * cfg["embed_dim"], [0.2] * cfg["embed_dim"]]
        test_ids = ["test_1", "test_2"]
        added_ids = vectorstore2.add(test_vectors, ids=test_ids, metadatas=[{"test": "drop_recreate"}] * 2)
        assert added_ids == test_ids, f"Should be able to add data normally after recreation"
        
        # Verify count
        count_after_add = vectorstore2.count()
        assert count_after_add == 2, f"Count after adding data should be 2, actual: {count_after_add}"
        
        # Cleanup
        vectorstore2.drop()
        vectorstore2.close()
    
    def test_drop_nonexistent_table(self):
        """Test dropping non-existent table (should not cause errors)"""
        cfg = get_config()
        cfg = {**cfg, "table_name": "test_nonexistent_table"}
        
        vectorstore = OpenGaussVectorStore(**cfg)
        
        # Drop table first
        vectorstore.drop()
        
        # Drop non-existent table again should not cause errors
        vectorstore.drop()
        
        # Cleanup
        vectorstore.close()
    
    def test_drop_after_operations(self):
        """Test dropping table after operations"""
        cfg = get_config()
        cfg = {**cfg, "table_name": "test_drop_after_ops"}
        
        vectorstore = OpenGaussVectorStore(**cfg)
        
        # Add some data
        test_vectors = [[0.1] * cfg["embed_dim"], [0.2] * cfg["embed_dim"], [0.3] * cfg["embed_dim"]]
        test_ids = ["test_op_1", "test_op_2", "test_op_3"]
        test_metadatas = [{"operation": "test", "id": i} for i in range(3)]
        
        added_ids = vectorstore.add(test_vectors, ids=test_ids, metadatas=test_metadatas)
        assert added_ids == test_ids, f"Adding data should succeed"
        
        # Verify data exists
        count_before_drop = vectorstore.count()
        assert count_before_drop == 3, f"Count after addition should be 3, actual: {count_before_drop}"
        
        # Drop table
        vectorstore.drop()
        
        # Recreate and verify table is empty
        new_vectorstore = OpenGaussVectorStore(**cfg)
        count_after_recreate = new_vectorstore.count()
        assert count_after_recreate == 0, f"Table should be empty after recreation, actual count: {count_after_recreate}"
        
        # Cleanup
        new_vectorstore.drop()
        new_vectorstore.close()
        vectorstore.close()
    
    def test_multiple_drop_calls(self):
        """Test multiple drop() method calls"""
        cfg = get_config()
        cfg = {**cfg, "table_name": "test_multiple_drop"}
        
        vectorstore = OpenGaussVectorStore(**cfg)
        
        # First drop
        vectorstore.drop()
        
        # Second drop (table already doesn't exist)
        vectorstore.drop()
        
        # Third drop (should still not cause errors)
        vectorstore.drop()
        
        # Verify can recreate
        new_vectorstore = OpenGaussVectorStore(**cfg)
        count = new_vectorstore.count()
        assert count == 0, f"Count after multiple drops and recreation should be 0, actual: {count}"
        
        # Cleanup
        new_vectorstore.drop()
        new_vectorstore.close()
        vectorstore.close()