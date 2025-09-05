#!/usr/bin/env python3
"""Test OpenGaussVectorStore add() function - data insertion functionality

Configuration is loaded from opengauss_config. Cleanup drops the table.
"""

import pytest
import sys
import os

# Add project root to sys.path for direct execution
sys.path.append(os.path.join(os.path.dirname(__file__), '..', '..', '..', '..'))

from kotaemon.storages.vectorstores import OpenGaussVectorStore
from kotaemon.base import DocumentWithEmbedding
try:
    from .opengauss_config import get_config
except Exception:
    # Allow running as a standalone script: add current directory to sys.path
    import os as _os, sys as _sys
    _sys.path.append(_os.path.dirname(__file__))
    from opengauss_config import get_config


class TestOpenGaussVectorStoreAdd:
    """Test OpenGaussVectorStore add() method"""
    
    @pytest.fixture(autouse=True)
    def setup_and_cleanup(self):
        """Setup and cleanup before/after each test"""
        # Setup
        self.cfg = get_config()
        self.cfg = {**self.cfg, "table_name": "test_add_test"}
        self.vectorstore = OpenGaussVectorStore(**self.cfg)
        
        yield  # Run test
        
        # Cleanup
        try:
            self.vectorstore.drop()
            self.vectorstore.close()
        except Exception:
            pass  # Ignore cleanup errors
    
    def test_add_raw_vectors(self):
        """Test inserting raw vectors (with id, embedding, metadata)"""
        # Create test vectors (1536 dimensions)
        test_embeddings = [
            [0.1] * 1536,  # Vector filled with 0.1
            [0.2] * 1536,  # Vector filled with 0.2
            [0.3] * 1536   # Vector filled with 0.3
        ]
        test_ids = ['test_1', 'test_2', 'test_3']
        test_metadatas = [
            {'source': 'test1', 'category': 'debug', 'tags': ['test', 'debug']},
            {'source': 'test2', 'category': 'debug', 'tags': ['test', 'debug']},
            {'source': 'test3', 'category': 'debug', 'tags': ['test', 'debug']}
        ]
        
        # Call add() method
        added_ids = self.vectorstore.add(test_embeddings, ids=test_ids, metadatas=test_metadatas)
        
        # Verify insertion results
        assert added_ids == test_ids, f"Returned IDs should match input IDs, actual: {added_ids}"
        
        # Verify count
        count = self.vectorstore.count()
        assert count >= len(test_embeddings), f"Count after insertion should be at least {len(test_embeddings)}, actual: {count}"
        
        # Verify inserted data
        with self.vectorstore._conn.cursor() as cursor:
            cursor.execute("""
                SELECT id, 
                       embedding::text as vector_text,
                       metadata
                FROM public.test_add_test
                WHERE id IN %s
                ORDER BY id;
            """, (tuple(test_ids),))
            rows = cursor.fetchall()
            
            assert len(rows) == len(test_ids), f"Should insert {len(test_ids)} rows, actual: {len(rows)}"
            
            for i, row in enumerate(rows):
                assert row[0] == test_ids[i], f"Row {i} ID should be {test_ids[i]}, actual: {row[0]}"
                assert row[2] == test_metadatas[i], f"Row {i} metadata should match"
    
    def test_add_document_with_embedding(self):
        """Test inserting DocumentWithEmbedding objects"""
        # Create DocumentWithEmbedding objects
        docs = [
            DocumentWithEmbedding(
                text="First test document",
                embedding=[0.4] * 1536,
                id_="doc_1",
                metadata={'source': 'doc1', 'category': 'document', 'tags': ['doc', 'test']}
            ),
            DocumentWithEmbedding(
                text="Second test document", 
                embedding=[0.5] * 1536,
                id_="doc_2",
                metadata={'source': 'doc2', 'category': 'document', 'tags': ['doc', 'test']}
            )
        ]
        
        # Call add function
        added_doc_ids = self.vectorstore.add(docs)
        
        # Verify insertion results
        expected_ids = ['doc_1', 'doc_2']
        assert added_doc_ids == expected_ids, f"Returned document IDs should match expected, actual: {added_doc_ids}"
        
        # Verify count after insertion
        count_after_docs = self.vectorstore.count()
        assert count_after_docs >= len(docs), f"Count after inserting documents should be at least {len(docs)}, actual: {count_after_docs}"
        
        # Verify inserted document data
        with self.vectorstore._conn.cursor() as cursor:
            cursor.execute("""
                SELECT id, 
                       embedding::text as vector_text,
                       metadata
                FROM public.test_add_test
                WHERE id IN %s
                ORDER BY id;
            """, (tuple(expected_ids),))
            rows = cursor.fetchall()
            
            assert len(rows) == len(docs), f"Should insert {len(docs)} documents, actual: {len(rows)}"
            
            for i, row in enumerate(rows):
                assert row[0] == expected_ids[i], f"Document {i} ID should be {expected_ids[i]}, actual: {row[0]}"
                assert row[2] == docs[i].metadata, f"Document {i} metadata should match"
    
    def test_add_empty_vectors(self):
        """Test inserting empty vector list"""
        # Inserting empty list should not cause errors
        added_ids = self.vectorstore.add([])
        assert added_ids == [], "Inserting empty list should return empty list"
        
        # Count should remain unchanged
        count = self.vectorstore.count()
        assert count == 0, "Count should be 0 after inserting empty list"
    
    def test_add_with_duplicate_ids(self):
        """Test handling of duplicate IDs during insertion"""
        test_embeddings = [[0.1] * 1536, [0.2] * 1536]
        test_ids = ['duplicate_id', 'duplicate_id']  # Duplicate IDs
        test_metadatas = [{'test': '1'}, {'test': '2'}]
        
        # Inserting duplicate IDs should raise exception or handle properly
        with pytest.raises(Exception):
            self.vectorstore.add(test_embeddings, ids=test_ids, metadatas=test_metadatas)
    
    def test_add_without_ids(self):
        """Test insertion without providing IDs (should auto-generate IDs)"""
        test_embeddings = [[0.1] * 1536, [0.2] * 1536]
        test_metadatas = [{'test': '1'}, {'test': '2'}]
        
        # No IDs provided, should auto-generate
        added_ids = self.vectorstore.add(test_embeddings, metadatas=test_metadatas)
        
        # Verify IDs were returned
        assert len(added_ids) == len(test_embeddings), f"Should return {len(test_embeddings)} IDs, actual: {len(added_ids)}"
        assert all(isinstance(id_, str) for id_ in added_ids), "All IDs should be strings"
        
        # Verify count
        count = self.vectorstore.count()
        assert count == len(test_embeddings), f"Count after insertion should be {len(test_embeddings)}, actual: {count}"
