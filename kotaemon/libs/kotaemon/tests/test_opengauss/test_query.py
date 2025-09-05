#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Test OpenGaussVectorStore.query() method

Validates basic KNN results and parameter combinations. Configuration is loaded from opengauss_config.
Cleanup drops the table and closes the connection.
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
    import os as _os, sys as _sys
    _sys.path.append(_os.path.dirname(__file__))
    from opengauss_config import get_config
import numpy as np


class TestOpenGaussVectorStoreQuery:
    """Test OpenGaussVectorStore query() method"""
    
    @pytest.fixture(autouse=True)
    def setup_and_cleanup(self):
        """Setup and cleanup before/after each test"""
        # Setup
        self.cfg = get_config()
        self.cfg = {**self.cfg, "table_name": "test_query_test"}
        self.vectorstore = OpenGaussVectorStore(**self.cfg)
        
        # Prepare test data
        self.test_embeddings = [
            [0.1] * 1536,
            [0.2] * 1536,
            [0.3] * 1536,
            [0.4] * 1536,
            [0.5] * 1536,
        ]
        
        self.test_ids = ['query_test_1', 'query_test_2', 'query_test_3', 'query_test_4', 'query_test_5']
        
        self.test_metadatas = [
            {'source': 'doc1', 'category': 'A', 'tags': ['test', 'query']},
            {'source': 'doc2', 'category': 'B', 'tags': ['test', 'query']},
            {'source': 'doc3', 'category': 'A', 'tags': ['test', 'query']},
            {'source': 'doc4', 'category': 'C', 'tags': ['test', 'query']},
            {'source': 'doc5', 'category': 'B', 'tags': ['test', 'query']},
        ]
        
        # Insert test data
        added_ids = self.vectorstore.add(self.test_embeddings, ids=self.test_ids, metadatas=self.test_metadatas)
        assert added_ids == self.test_ids, "Test data insertion failed"
        
        yield  # Run test
        
        # Cleanup
        try:
            self.vectorstore.delete(self.test_ids)
            self.vectorstore.drop()
            self.vectorstore.close()
        except Exception:
            pass  # Ignore cleanup errors
    
    def test_basic_query(self):
        """Test basic query functionality"""
        # Use first vector as query vector
        query_vector = [0.1] * 1536
        
        # top_k=3
        embeddings, similarities, ids = self.vectorstore.query(
            embedding=query_vector,
            top_k=3
        )
        
        # Verify results
        assert len(ids) == 3, f"Should return 3 results, actual: {len(ids)}"
        assert len(similarities) == 3, f"Should return 3 similarities, actual: {len(similarities)}"
        assert len(embeddings) == 3, f"Should return 3 embeddings, actual: {len(embeddings)}"
        
        # Verify similarities are in descending order
        for i in range(len(similarities) - 1):
            assert similarities[i] >= similarities[i + 1], f"Similarities should be in descending order, position {i}: {similarities[i]} < {similarities[i + 1]}"
    
    def test_query_with_id_filter(self):
        """Test query with ID filtering"""
        # Query subset of specific IDs
        specific_ids = ['query_test_1', 'query_test_3']
        query_vector = [0.1] * 1536
        
        # Run query
        embeddings, similarities, ids = self.vectorstore.query(
            embedding=query_vector,
            top_k=5,
            ids=specific_ids
        )
        
        # Verify all returned IDs are in target list
        if len(ids) > 0:
            for emb_id in ids:
                assert emb_id in specific_ids, f"Returned ID {emb_id} not in target list {specific_ids}"
        else:
            # If empty results, check if target IDs actually exist
            with self.vectorstore._conn.cursor() as cursor:
                placeholders = ','.join(['%s'] * len(specific_ids))
                cursor.execute(f"SELECT id FROM {self.vectorstore._schema_name}.{self.vectorstore._table_name} WHERE id IN ({placeholders})", specific_ids)
                existing_ids = [row[0] for row in cursor.fetchall()]
                assert len(existing_ids) == 0, f"Target IDs {specific_ids} exist but query returned empty results"
    
    def test_query_with_metadata_filter(self):
        """Test query with metadata filtering"""
        query_vector = [0.1] * 1536
        
        # Filter by category
        metadata_filter = {'category': 'A'}
        embeddings, similarities, ids = self.vectorstore.query(
            embedding=query_vector,
            top_k=5,
            metadata_filter=metadata_filter
        )
        
        # Verify results are not empty (we know there's data with category 'A')
        assert len(ids) > 0, f"Category filter should return results, actual: {len(ids)}"
        
        # Verify returned results actually match filter condition
        for i, emb_id in enumerate(ids):
            # Find corresponding metadata
            idx = self.test_ids.index(emb_id)
            metadata = self.test_metadatas[idx]
            assert metadata['category'] == 'A', f"Result {i} category should be 'A', actual: {metadata['category']}"
    
    def test_query_with_tag_filter(self):
        """Test query with tag filtering"""
        query_vector = [0.1] * 1536
        
        # Filter by tags
        tag_filter = {'tags': ['test']}
        embeddings, similarities, ids = self.vectorstore.query(
            embedding=query_vector,
            top_k=5,
            metadata_filter=tag_filter
        )
        
        # Verify results are not empty (we know there's data with 'test' tag)
        assert len(ids) > 0, f"Tag filter should return results, actual: {len(ids)}"
        
        # Verify returned results actually contain 'test' tag
        for i, emb_id in enumerate(ids):
            idx = self.test_ids.index(emb_id)
            metadata = self.test_metadatas[idx]
            assert 'test' in metadata.get('tags', []), f"Result {i} should contain 'test' tag, actual: {metadata.get('tags', [])}"
    
    def test_query_edge_cases(self):
        """Test query edge cases"""
        query_vector = [0.1] * 1536
        
        # top_k=0 should return empty results
        embeddings, similarities, ids = self.vectorstore.query(
            embedding=query_vector,
            top_k=0
        )
        assert len(ids) == 0, f"top_k=0 should return empty results, actual: {len(ids)}"
        
        # top_k larger than data size should only return existing data
        embeddings, similarities, ids = self.vectorstore.query(
            embedding=query_vector,
            top_k=100
        )
        assert len(ids) <= 5, f"top_k=100 should return at most 5 results, actual: {len(ids)}"
    
    def test_query_with_invalid_embedding_dimension(self):
        """Test query with invalid embedding dimension"""
        # Use query vector with wrong dimension
        invalid_query_vector = [0.1] * 100  # Wrong: should be 1536 dimensions
        
        with pytest.raises(Exception):
            self.vectorstore.query(
                embedding=invalid_query_vector,
                top_k=3
            )
    
    def test_query_similarity_scores(self):
        """Test similarity scores reasonableness"""
        query_vector = [0.1] * 1536
        
        embeddings, similarities, ids = self.vectorstore.query(
            embedding=query_vector,
            top_k=3
        )
        
        # Verify similarity scores are in reasonable range (usually 0-1)
        for i, sim in enumerate(similarities):
            assert 0 <= sim <= 1, f"Similarity score should be between 0-1, position {i}: {sim}"
        
        # Verify most similar should be the query vector itself (if exists)
        if 'query_test_1' in ids:  # Query vector is same as first test vector
            first_result_idx = ids.index('query_test_1')
            assert similarities[first_result_idx] == 1.0, f"Same vector should have similarity 1.0, actual: {similarities[first_result_idx]}"