"""Test OpenGaussVectorStore.count() method

Ensures count() correctly reflects the number of inserted vectors.
Configuration is loaded from opengauss_config, cleanup drops the table.
"""

import pytest
import os
import random

from kotaemon.storages.vectorstores import OpenGaussVectorStore
try:
    from .opengauss_config import get_config
except Exception:
    import os as _os, sys as _sys
    _sys.path.append(_os.path.dirname(__file__))
    from opengauss_config import get_config


class TestOpenGaussVectorStoreCount:
    """Test OpenGaussVectorStore count() method"""
    
    @pytest.fixture(autouse=True)
    def setup_and_cleanup(self):
        """Setup and cleanup before/after each test"""
        # Setup
        self.cfg = get_config()
        self.cfg = {**self.cfg, "table_name": "test_count_test"}
        self.vectorstore = OpenGaussVectorStore(**self.cfg)
        
        yield  # Run test
        
        # Cleanup
        try:
            self.vectorstore.drop()
            self.vectorstore.close()
        except Exception:
            pass  # Ignore cleanup errors
    
    def make_vectors(self, dim: int, n: int):
        """Generate random vectors for testing"""
        return [[random.random() for _ in range(dim)] for _ in range(n)]
    
    def test_count_initial_state(self):
        """Test count in initial state"""
        initial_count = self.vectorstore.count()
        assert initial_count == 0, f"Initial count should be 0, actual: {initial_count}"
    
    def test_count_after_add(self):
        """Test count after adding vectors"""
        dim = self.cfg["embed_dim"]
        n = 5
        vectors = self.make_vectors(dim, n)
        ids = [f"test-count-{i}" for i in range(n)]
        
        # Record count before adding
        before_count = self.vectorstore.count()
        
        # Add vectors
        added_ids = self.vectorstore.add(vectors, ids=ids, metadatas=[{"case": "count"}] * n)
        
        # Verify addition was successful
        assert added_ids == ids, f"Added IDs should match expected, actual: {added_ids}"
        
        # Verify count increased
        after_count = self.vectorstore.count()
        assert after_count >= before_count + n, f"Count should increase by at least {n} after adding, actual increase: {after_count - before_count}"
        
        # Cleanup
        self.vectorstore.delete(ids)
        final_count = self.vectorstore.count()
        assert final_count == before_count, f"Count should return to initial state after cleanup, actual: {final_count}"
    
    def test_count_after_multiple_adds(self):
        """Test count after multiple additions"""
        dim = self.cfg["embed_dim"]
        
        # First addition
        vectors1 = self.make_vectors(dim, 3)
        ids1 = [f"test-count-1-{i}" for i in range(3)]
        self.vectorstore.add(vectors1, ids=ids1, metadatas=[{"batch": 1}] * 3)
        
        count1 = self.vectorstore.count()
        assert count1 >= 3, f"Count after first addition should be at least 3, actual: {count1}"
        
        # Second addition
        vectors2 = self.make_vectors(dim, 2)
        ids2 = [f"test-count-2-{i}" for i in range(2)]
        self.vectorstore.add(vectors2, ids=ids2, metadatas=[{"batch": 2}] * 2)
        
        count2 = self.vectorstore.count()
        assert count2 >= count1 + 2, f"Count after second addition should increase by at least 2, actual: {count2}"
        
        # Cleanup
        self.vectorstore.delete(ids1 + ids2)
        final_count = self.vectorstore.count()
        assert final_count == 0, f"Count should be 0 after cleanup, actual: {final_count}"
    
    def test_count_after_delete(self):
        """Test count after deletion"""
        dim = self.cfg["embed_dim"]
        n = 4
        vectors = self.make_vectors(dim, n)
        ids = [f"test-count-delete-{i}" for i in range(n)]
        
        # Add vectors
        self.vectorstore.add(vectors, ids=ids, metadatas=[{"case": "delete"}] * n)
        count_after_add = self.vectorstore.count()
        assert count_after_add >= n, f"Count after addition should be at least {n}, actual: {count_after_add}"
        
        # Delete partial vectors
        ids_to_delete = ids[:2]  # Delete first two
        self.vectorstore.delete(ids_to_delete)
        count_after_partial_delete = self.vectorstore.count()
        assert count_after_partial_delete >= count_after_add - 2, f"Count should decrease after partial deletion, actual: {count_after_partial_delete}"
        
        # Delete remaining vectors
        remaining_ids = ids[2:]
        self.vectorstore.delete(remaining_ids)
        count_after_full_delete = self.vectorstore.count()
        assert count_after_full_delete == 0, f"Count should be 0 after full deletion, actual: {count_after_full_delete}"
    
    def test_count_with_empty_table(self):
        """Test count with empty table"""
        # Ensure table is empty
        count = self.vectorstore.count()
        assert count == 0, f"Empty table count should be 0, actual: {count}"
    
    def test_count_consistency(self):
        """Test count consistency"""
        dim = self.cfg["embed_dim"]
        vectors = self.make_vectors(dim, 2)
        ids = ["test-consistency-1", "test-consistency-2"]
        
        # Add vectors
        self.vectorstore.add(vectors, ids=ids, metadatas=[{"case": "consistency"}] * 2)
        
        # Multiple count queries should be consistent
        count1 = self.vectorstore.count()
        count2 = self.vectorstore.count()
        assert count1 == count2, f"Multiple count queries should be consistent, first: {count1}, second: {count2}"
        
        # Cleanup
        self.vectorstore.delete(ids)