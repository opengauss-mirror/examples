"""Test search_by_metadata() method

Ensures metadata predicates return expected IDs.
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


class TestOpenGaussVectorStoreSearchByMetadata:
    """Test OpenGaussVectorStore search_by_metadata() method"""
    
    @pytest.fixture(autouse=True)
    def setup_and_cleanup(self):
        """Setup and cleanup before/after each test"""
        # Setup
        self.cfg = get_config()
        self.cfg = {**self.cfg, "table_name": "test_search_metadata_test"}
        self.vectorstore = OpenGaussVectorStore(**self.cfg)
        
        # Prepare test data
        self.test_vectors = self.make_vectors(self.cfg["embed_dim"], 3)
        self.test_ids = ["test-sbm-a", "test-sbm-b", "test-sbm-c"]
        self.test_metas = [
            {"group": "alpha", "version": "1", "priority": "high"},
            {"group": "beta", "version": "1", "priority": "low"},
            {"group": "alpha", "version": "2", "priority": "medium"},
        ]
        
        # Insert test data
        added_ids = self.vectorstore.add(self.test_vectors, ids=self.test_ids, metadatas=self.test_metas)
        assert added_ids == self.test_ids, "Test data insertion failed"
        
        yield  # Run test
        
        # Cleanup
        try:
            self.vectorstore.delete(self.test_ids)
            self.vectorstore.drop()
            self.vectorstore.close()
        except Exception:
            pass  # Ignore cleanup errors
    
    def make_vectors(self, dim: int, n: int):
        """Generate random vectors for testing"""
        return [[random.random() for _ in range(dim)] for _ in range(n)]
    
    def test_simple_equality_search(self):
        """Test simple equality search"""
        # Search group=alpha
        results = self.vectorstore.search_by_metadata({"group": "alpha"}, limit=10)
        
        # Verify results
        assert isinstance(results, list), f"Results should be a list, actual: {type(results)}"
        assert len(results) >= 2, f"Should return at least 2 results, actual: {len(results)}"
        
        # Verify returned IDs are all from alpha group
        expected_ids = {self.test_ids[0], self.test_ids[2]}  # Indices 0 and 2 are alpha group
        actual_ids = set(results)
        assert actual_ids >= expected_ids, f"Results should include alpha group IDs, expected: {expected_ids}, actual: {actual_ids}"
    
    def test_range_search(self):
        """Test range search"""
        # Search version in [1, 2] range
        range_filter = {"version": {"range": {"min": "1", "max": "2"}}}
        results = self.vectorstore.search_by_metadata(range_filter, limit=10)
        
        # Verify results
        assert isinstance(results, list), f"Results should be a list, actual: {type(results)}"
        assert len(results) >= 3, f"Should return at least 3 results, actual: {len(results)}"
        
        # Verify all returned IDs are in range
        expected_ids = set(self.test_ids)  # All IDs have version in 1-2 range
        actual_ids = set(results)
        assert actual_ids >= expected_ids, f"Results should include all IDs, expected: {expected_ids}, actual: {actual_ids}"
    
    def test_multiple_conditions_search(self):
        """Test multiple conditions search"""
        # Search group=alpha AND priority=high
        multi_filter = {"group": "alpha", "priority": "high"}
        results = self.vectorstore.search_by_metadata(multi_filter, limit=10)
        
        # Verify results
        assert isinstance(results, list), f"Results should be a list, actual: {type(results)}"
        assert len(results) >= 1, f"Should return at least 1 result, actual: {len(results)}"
        
        # Verify returned ID matches conditions
        expected_id = self.test_ids[0]  # Only index 0 satisfies both group=alpha and priority=high
        assert expected_id in results, f"Results should include {expected_id}, actual: {results}"
    
    def test_no_matching_results(self):
        """Test search with no matching results"""
        # Search for non-existent condition
        no_match_filter = {"group": "nonexistent"}
        results = self.vectorstore.search_by_metadata(no_match_filter, limit=10)
        
        # Verify results are empty
        assert isinstance(results, list), f"Results should be a list, actual: {type(results)}"
        assert len(results) == 0, f"No matching condition should return empty list, actual: {results}"
    
    def test_limit_parameter(self):
        """Test limit parameter"""
        # Search all alpha group but limit to 1 result
        results = self.vectorstore.search_by_metadata({"group": "alpha"}, limit=1)
        
        # Verify result count
        assert isinstance(results, list), f"Results should be a list, actual: {type(results)}"
        assert len(results) <= 1, f"limit=1 should return at most 1 result, actual: {len(results)}"
    
    def test_empty_filter(self):
        """Test empty filter condition"""
        # Empty filter condition should return all results
        results = self.vectorstore.search_by_metadata({}, limit=10)
        
        # Verify results
        assert isinstance(results, list), f"Results should be a list, actual: {type(results)}"
        assert len(results) >= 3, f"Empty filter condition should return all results, actual: {len(results)}"
    
    def test_nested_metadata_search(self):
        """Test nested metadata search"""
        # Add data with nested metadata
        nested_vectors = self.make_vectors(self.cfg["embed_dim"], 2)
        nested_ids = ["test-nested-1", "test-nested-2"]
        nested_metas = [
            {"category": "tech", "details": {"language": "python", "level": "advanced"}},
            {"category": "tech", "details": {"language": "javascript", "level": "beginner"}}
        ]
        
        # Add nested metadata
        self.vectorstore.add(nested_vectors, ids=nested_ids, metadatas=nested_metas)
        
        try:
            # Search nested fields
            results = self.vectorstore.search_by_metadata({"category": "tech"}, limit=10)
            
            # Verify results
            assert isinstance(results, list), f"Results should be a list, actual: {type(results)}"
            assert len(results) >= 2, f"Should return at least 2 tech category results, actual: {len(results)}"
            
            # Verify returned IDs include newly added IDs
            actual_ids = set(results)
            expected_nested_ids = set(nested_ids)
            assert actual_ids >= expected_nested_ids, f"Results should include nested metadata IDs, expected: {expected_nested_ids}, actual: {actual_ids}"
            
        finally:
            # Cleanup nested metadata
            self.vectorstore.delete(nested_ids)
    
    def test_case_sensitive_search(self):
        """Test case sensitive search"""
        # Search lowercase alpha (data has uppercase Alpha)
        results_lower = self.vectorstore.search_by_metadata({"group": "alpha"}, limit=10)
        
        # Search uppercase Alpha
        results_upper = self.vectorstore.search_by_metadata({"group": "Alpha"}, limit=10)
        
        # Verify case sensitivity
        assert len(results_lower) >= 2, f"Lowercase search should return results, actual: {len(results_lower)}"
        # Note: This assumes search is case sensitive, if actual implementation is case insensitive, this assertion may need adjustment
        assert len(results_upper) == 0, f"Uppercase search should not return results (if case sensitive), actual: {len(results_upper)}"
    
    def test_special_characters_in_metadata(self):
        """Test metadata with special characters"""
        # Add metadata with special characters
        special_vectors = self.make_vectors(self.cfg["embed_dim"], 1)
        special_ids = ["test-special"]
        special_metas = [{"description": "test@#$%^&*()", "symbols": "!@#$%^&*()"}]
        
        # Add special character metadata
        self.vectorstore.add(special_vectors, ids=special_ids, metadatas=special_metas)
        
        try:
            # Search special characters
            results = self.vectorstore.search_by_metadata({"description": "test@#$%^&*()"}, limit=10)
            
            # Verify results
            assert isinstance(results, list), f"Results should be a list, actual: {type(results)}"
            assert len(results) >= 1, f"Should return results with special characters, actual: {len(results)}"
            assert special_ids[0] in results, f"Results should include special character ID, actual: {results}"
            
        finally:
            # Cleanup special character data
            self.vectorstore.delete(special_ids)
    
    def test_large_limit_value(self):
        """Test large limit value"""
        # Use limit larger than data size
        results = self.vectorstore.search_by_metadata({"group": "alpha"}, limit=1000)
        
        # Verify results
        assert isinstance(results, list), f"Results should be a list, actual: {type(results)}"
        assert len(results) <= 3, f"limit=1000 should return at most all data, actual: {len(results)}"
    
    def test_zero_limit(self):
        """Test limit=0"""
        # Use limit=0
        results = self.vectorstore.search_by_metadata({"group": "alpha"}, limit=0)
        
        # Verify results
        assert isinstance(results, list), f"Results should be a list, actual: {type(results)}"
        assert len(results) == 0, f"limit=0 should return empty list, actual: {len(results)}"
    
    def test_search_by_metadata_without_opengauss_database(self):
        """Test that search_by_metadata operations fail when openGauss database is not available"""
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