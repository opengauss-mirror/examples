"""Test OpenGaussVectorStore.delete() method

Validates that delete() correctly removes specific IDs.
Configuration is loaded from opengauss_config.get_config().
Drops the table and closes the connection at the end.
"""

import pytest
import os
import random
from typing import List

from kotaemon.storages.vectorstores import OpenGaussVectorStore
try:
    from .opengauss_config import get_config
except Exception:
    import os as _os, sys as _sys
    _sys.path.append(_os.path.dirname(__file__))
    from opengauss_config import get_config


class TestOpenGaussVectorStoreDelete:
    """Test OpenGaussVectorStore delete() method"""
    
    @pytest.fixture(autouse=True)
    def setup_and_cleanup(self):
        """Setup and cleanup before/after each test"""
        # Setup
        self.cfg = get_config()
        self.cfg = {**self.cfg, "table_name": "test_delete_test"}
        self.vectorstore = OpenGaussVectorStore(**self.cfg)
        
        yield  # Run test
        
        # Cleanup
        try:
            self.vectorstore.drop()
            self.vectorstore.close()
        except Exception:
            pass  # Ignore cleanup errors
    
    def make_vectors(self, dim: int, n: int) -> List[List[float]]:
        """Generate random vectors for testing"""
        return [[random.random() for _ in range(dim)] for _ in range(n)]
    
    def test_delete_specific_ids(self):
        """Test deleting specific IDs"""
        dim = self.cfg["embed_dim"]
        
        # Prepare data
        vectors = self.make_vectors(dim, 3)
        ids = [f"test-del-{i}" for i in range(3)]
        self.vectorstore.add(vectors, ids=ids, metadatas=[{"case": "delete"}] * 3)
        
        # Record count before deletion
        count_before = self.vectorstore.count()
        assert count_before >= 3, f"Count before deletion should be at least 3, actual: {count_before}"
        
        # Delete partial data
        to_delete = ids[:2]  # Delete first two
        self.vectorstore.delete(to_delete)
        
        # Verify deletion results
        count_after = self.vectorstore.count()
        assert count_after == count_before - len(to_delete), f"Count should decrease by {len(to_delete)} after deletion, actual decrease: {count_before - count_after}"
        
        # Verify remaining data still exists
        remaining_id = ids[2]
        with self.vectorstore._conn.cursor() as cursor:
            cursor.execute(f"SELECT id FROM {self.vectorstore._schema_name}.{self.vectorstore._table_name} WHERE id = %s", (remaining_id,))
            result = cursor.fetchone()
            assert result is not None, f"Remaining ID {remaining_id} should still exist"
        
        # Cleanup remaining data
        self.vectorstore.delete([remaining_id])
        final_count = self.vectorstore.count()
        assert final_count == 0, f"Count should be 0 after cleanup, actual: {final_count}"
    
    def test_delete_all_ids(self):
        """Test deleting all IDs"""
        dim = self.cfg["embed_dim"]
        
        # Prepare data
        vectors = self.make_vectors(dim, 4)
        ids = [f"test-del-all-{i}" for i in range(4)]
        self.vectorstore.add(vectors, ids=ids, metadatas=[{"case": "delete_all"}] * 4)
        
        # Verify data was inserted
        count_before = self.vectorstore.count()
        assert count_before >= 4, f"Count after insertion should be at least 4, actual: {count_before}"
        
        # Delete all data
        self.vectorstore.delete(ids)
        
        # Verify all deletion
        count_after = self.vectorstore.count()
        assert count_after == 0, f"Count should be 0 after deleting all data, actual: {count_after}"
    
    def test_delete_nonexistent_ids(self):
        """Test deleting non-existent IDs (should not cause errors)"""
        nonexistent_ids = ["nonexistent_1", "nonexistent_2"]
        
        # Deleting non-existent IDs should not raise exceptions
        self.vectorstore.delete(nonexistent_ids)
        
        # Count should remain unchanged
        count = self.vectorstore.count()
        assert count == 0, f"Count should remain 0 after deleting non-existent IDs, actual: {count}"
    
    def test_delete_empty_list(self):
        """Test deleting empty list"""
        # Deleting empty list should not cause errors
        self.vectorstore.delete([])
        
        # Count should remain unchanged
        count = self.vectorstore.count()
        assert count == 0, f"Count should remain 0 after deleting empty list, actual: {count}"
    
    def test_delete_partial_existing_ids(self):
        """Test deleting partially existing IDs"""
        dim = self.cfg["embed_dim"]
        
        # Prepare data
        vectors = self.make_vectors(dim, 2)
        existing_ids = [f"test-partial-{i}" for i in range(2)]
        self.vectorstore.add(vectors, ids=existing_ids, metadatas=[{"case": "partial"}] * 2)
        
        # Mix existing and non-existing IDs
        mixed_ids = existing_ids + ["nonexistent_1", "nonexistent_2"]
        
        count_before = self.vectorstore.count()
        
        # Delete mixed ID list
        self.vectorstore.delete(mixed_ids)
        
        # Should only delete existing IDs
        count_after = self.vectorstore.count()
        assert count_after == 0, f"After deleting mixed ID list, existing IDs should be deleted, actual count: {count_after}"
    
    def test_delete_verification(self):
        """Test verification after deletion"""
        dim = self.cfg["embed_dim"]
        
        # Prepare data
        vectors = self.make_vectors(dim, 3)
        ids = [f"test-verify-{i}" for i in range(3)]
        metadatas = [{"test_id": f"verify_{i}"} for i in range(3)]
        self.vectorstore.add(vectors, ids=ids, metadatas=metadatas)
        
        # Delete specific IDs
        to_delete = [ids[0], ids[2]]  # Delete first and third
        self.vectorstore.delete(to_delete)
        
        # Verify deleted IDs no longer exist
        with self.vectorstore._conn.cursor() as cursor:
            placeholders = ','.join(['%s'] * len(to_delete))
            cursor.execute(f"SELECT id FROM {self.vectorstore._schema_name}.{self.vectorstore._table_name} WHERE id IN ({placeholders})", to_delete)
            remaining_deleted = cursor.fetchall()
            assert len(remaining_deleted) == 0, f"Deleted IDs {to_delete} should not exist, but found: {[row[0] for row in remaining_deleted]}"
        
        # Verify non-deleted ID still exists
        remaining_id = ids[1]
        with self.vectorstore._conn.cursor() as cursor:
            cursor.execute(f"SELECT id, metadata FROM {self.vectorstore._schema_name}.{self.vectorstore._table_name} WHERE id = %s", (remaining_id,))
            result = cursor.fetchone()
            assert result is not None, f"Non-deleted ID {remaining_id} should still exist"
            assert result[1] == metadatas[1], f"Non-deleted ID metadata should remain unchanged"
        
        # Cleanup
        self.vectorstore.delete([remaining_id])