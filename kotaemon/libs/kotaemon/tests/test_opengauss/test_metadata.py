"""Test get_metadata() and update_metadata() methods

Validates metadata retrieval and update round-trip behavior.
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


class TestOpenGaussVectorStoreMetadata:
    """Test OpenGaussVectorStore metadata-related methods"""
    
    @pytest.fixture(autouse=True)
    def setup_and_cleanup(self):
        """Setup and cleanup before/after each test"""
        # Setup
        self.cfg = get_config()
        self.cfg = {**self.cfg, "table_name": "test_metadata_test"}
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
    
    def test_get_metadata(self):
        """Test metadata retrieval functionality"""
        dim = self.cfg["embed_dim"]
        
        # Prepare test data
        vectors = self.make_vectors(dim, 2)
        ids = ["test-meta-1", "test-meta-2"]
        metas = [{"tag": "v1", "category": "test"}, {"tag": "v2", "category": "test"}]
        
        # Add data
        added_ids = self.vectorstore.add(vectors, ids=ids, metadatas=metas)
        assert added_ids == ids, f"Adding data should succeed, actual: {added_ids}"
        
        # Get metadata
        got = self.vectorstore.get_metadata(ids)
        
        # Verify metadata
        assert len(got) == len(ids), f"Should return {len(ids)} metadata items, actual: {len(got)}"
        assert got[ids[0]]["tag"] == "v1", f"First ID tag should be 'v1', actual: {got[ids[0]]['tag']}"
        assert got[ids[1]]["tag"] == "v2", f"Second ID tag should be 'v2', actual: {got[ids[1]]['tag']}"
        assert got[ids[0]]["category"] == "test", f"First ID category should be 'test', actual: {got[ids[0]]['category']}"
        assert got[ids[1]]["category"] == "test", f"Second ID category should be 'test', actual: {got[ids[1]]['category']}"
    
    def test_update_metadata(self):
        """Test metadata update functionality"""
        dim = self.cfg["embed_dim"]
        
        # Prepare test data
        vectors = self.make_vectors(dim, 2)
        ids = ["test-update-1", "test-update-2"]
        metas = [{"tag": "original", "version": "1.0"}, {"tag": "original2", "version": "1.0"}]
        
        # Add data
        self.vectorstore.add(vectors, ids=ids, metadatas=metas)
        
        # Get original metadata
        original_metadata = self.vectorstore.get_metadata(ids)
        assert original_metadata[ids[0]]["tag"] == "original", "Original metadata should be correct"
        
        # Update first ID metadata
        new_metadata = {"tag": "updated", "extra": "new_field", "version": "2.0"}
        success = self.vectorstore.update_metadata(ids[0], new_metadata)
        assert success, "update_metadata() should return True"
        
        # Verify updated metadata
        updated_metadata = self.vectorstore.get_metadata(ids)
        assert updated_metadata[ids[0]]["tag"] == "updated", f"Updated tag should be 'updated', actual: {updated_metadata[ids[0]]['tag']}"
        assert updated_metadata[ids[0]]["extra"] == "new_field", f"Should include new field 'extra', actual: {updated_metadata[ids[0]].get('extra')}"
        assert updated_metadata[ids[0]]["version"] == "2.0", f"Version should be updated to '2.0', actual: {updated_metadata[ids[0]]['version']}"
        
        # Verify second ID metadata unchanged
        assert updated_metadata[ids[1]]["tag"] == "original2", f"Second ID metadata should not change, actual: {updated_metadata[ids[1]]['tag']}"
    
    def test_get_metadata_nonexistent_ids(self):
        """Test getting metadata for non-existent IDs"""
        nonexistent_ids = ["nonexistent_1", "nonexistent_2"]
        
        # Getting metadata for non-existent IDs should return empty dict
        result = self.vectorstore.get_metadata(nonexistent_ids)
        assert result == {}, f"Getting metadata for non-existent IDs should return empty dict, actual: {result}"
    
    def test_update_metadata_nonexistent_id(self):
        """Test updating metadata for non-existent ID"""
        nonexistent_id = "nonexistent_id"
        new_metadata = {"tag": "updated"}
        
        # Updating metadata for non-existent ID should return False or raise exception
        success = self.vectorstore.update_metadata(nonexistent_id, new_metadata)
        assert not success, f"Updating metadata for non-existent ID should return False, actual: {success}"
    
    def test_metadata_round_trip(self):
        """Test metadata round-trip operations"""
        dim = self.cfg["embed_dim"]
        
        # Prepare complex metadata
        vectors = self.make_vectors(dim, 3)
        ids = ["test-round-1", "test-round-2", "test-round-3"]
        complex_metas = [
            {
                "name": "Document 1",
                "category": "Technology",
                "tags": ["python", "testing"],
                "priority": 1,
                "active": True,
                "nested": {"level": 1, "value": "test"}
            },
            {
                "name": "Document 2", 
                "category": "Documentation",
                "tags": ["markdown", "guide"],
                "priority": 2,
                "active": False,
                "nested": {"level": 2, "value": "example"}
            },
            {
                "name": "Document 3",
                "category": "Technology",
                "tags": ["javascript", "frontend"],
                "priority": 3,
                "active": True,
                "nested": {"level": 1, "value": "demo"}
            }
        ]
        
        # Add data
        self.vectorstore.add(vectors, ids=ids, metadatas=complex_metas)
        
        # Get metadata
        retrieved_metadata = self.vectorstore.get_metadata(ids)
        
        # Verify metadata integrity
        for i, id_ in enumerate(ids):
            original = complex_metas[i]
            retrieved = retrieved_metadata[id_]
            
            assert retrieved["name"] == original["name"], f"ID {id_} name should match"
            assert retrieved["category"] == original["category"], f"ID {id_} category should match"
            assert retrieved["tags"] == original["tags"], f"ID {id_} tags should match"
            assert retrieved["priority"] == original["priority"], f"ID {id_} priority should match"
            assert retrieved["active"] == original["active"], f"ID {id_} active should match"
            assert retrieved["nested"] == original["nested"], f"ID {id_} nested should match"
    
    def test_metadata_partial_update(self):
        """Test metadata update (complete replacement)"""
        dim = self.cfg["embed_dim"]
        
        # Prepare test data
        vectors = self.make_vectors(dim, 1)
        ids = ["test-partial"]
        original_meta = {"field1": "value1", "field2": "value2", "field3": "value3"}
        
        # Add data
        self.vectorstore.add(vectors, ids=ids, metadatas=[original_meta])
        
        # Update metadata (complete replacement, not partial merge)
        new_metadata = {"field2": "updated_value2", "field4": "new_field4"}
        success = self.vectorstore.update_metadata(ids[0], new_metadata)
        assert success, "Metadata update should succeed"
        
        # Verify update results
        updated_metadata = self.vectorstore.get_metadata(ids)
        retrieved = updated_metadata[ids[0]]
        
        # Verify new fields are present
        assert retrieved["field2"] == "updated_value2", f"field2 should be updated, actual: {retrieved['field2']}"
        assert retrieved["field4"] == "new_field4", f"field4 should be added, actual: {retrieved.get('field4')}"
        
        # Verify old fields are replaced (not merged)
        assert "field1" not in retrieved, f"field1 should be removed in complete replacement, actual keys: {list(retrieved.keys())}"
        assert "field3" not in retrieved, f"field3 should be removed in complete replacement, actual keys: {list(retrieved.keys())}"
    
    def test_metadata_empty_values(self):
        """Test metadata with empty values"""
        dim = self.cfg["embed_dim"]
        
        # Prepare metadata with empty values
        vectors = self.make_vectors(dim, 2)
        ids = ["test-empty-1", "test-empty-2"]
        empty_metas = [
            {"empty_string": "", "null_value": None, "empty_list": [], "empty_dict": {}},
            {"normal_field": "value", "empty_field": ""}
        ]
        
        # Add data
        self.vectorstore.add(vectors, ids=ids, metadatas=empty_metas)
        
        # Get and verify metadata
        retrieved_metadata = self.vectorstore.get_metadata(ids)
        
        # Verify empty value handling
        assert retrieved_metadata[ids[0]]["empty_string"] == "", f"Empty string should remain empty string"
        assert retrieved_metadata[ids[0]]["empty_list"] == [], f"Empty list should remain empty list"
        assert retrieved_metadata[ids[0]]["empty_dict"] == {}, f"Empty dict should remain empty dict"
        assert retrieved_metadata[ids[1]]["normal_field"] == "value", f"Normal field should remain"
        assert retrieved_metadata[ids[1]]["empty_field"] == "", f"Empty field should remain empty string"
    
    def test_metadata_without_opengauss_database(self):
        """Test that metadata operations fail when openGauss database is not available"""
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