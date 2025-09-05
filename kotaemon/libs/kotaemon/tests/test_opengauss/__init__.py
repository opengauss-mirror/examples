"""OpenGauss Vector Store Test Module

This module contains all test cases for OpenGaussVectorStore.
Tests cover the following functionality:
- Data insertion (add)
- Data querying (query) 
- Data counting (count)
- Data deletion (delete)
- Table dropping (drop)
- Initialization (init)
- Metadata operations (metadata)
- Metadata searching (search_by_metadata)
- Connection closing (close)

All tests use pytest framework and include appropriate assertions to verify functionality correctness.
"""

# Import all test classes to make them discoverable by pytest
from .test_add import TestOpenGaussVectorStoreAdd
from .test_query import TestOpenGaussVectorStoreQuery
from .test_count import TestOpenGaussVectorStoreCount
from .test_delete import TestOpenGaussVectorStoreDelete
from .test_drop import TestOpenGaussVectorStoreDrop
from .test_init import TestOpenGaussVectorStoreInit
from .test_metadata import TestOpenGaussVectorStoreMetadata
from .test_search_by_metadata import TestOpenGaussVectorStoreSearchByMetadata
from .test_close import TestOpenGaussVectorStoreClose

__all__ = [
    'TestOpenGaussVectorStoreAdd',
    'TestOpenGaussVectorStoreQuery', 
    'TestOpenGaussVectorStoreCount',
    'TestOpenGaussVectorStoreDelete',
    'TestOpenGaussVectorStoreDrop',
    'TestOpenGaussVectorStoreInit',
    'TestOpenGaussVectorStoreMetadata',
    'TestOpenGaussVectorStoreSearchByMetadata',
    'TestOpenGaussVectorStoreClose',
]
