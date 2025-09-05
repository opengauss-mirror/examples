OpenGauss Vector Store Integration
=================================

This document explains how to integrate the OpenGauss-based vector store into the Kotaemon framework, what code changes are involved, the functionality of `opengauss.py`, and how to run the test suite.

Prerequisites
-------------
- A running OpenGauss (or compatible PostgreSQL) instance with the `vector` extension.
- Python dependencies for Kotaemon and `psycopg2` available.
- pytest for running the test suite.

How to Enable OpenGauss Vector Store in Kotaemon
-----------------------------------------------
1) Configure the vector store in `flowsettings.py`:

```python
KH_VECTORSTORE = {
    "__type__": "kotaemon.storages.OpenGaussVectorStore",
    "host": "localhost",
    "port": 5432,  # use your actual port
    "database": "postgres",
    "user": "gaussdb",
    "password": "<password>",
    "table_name": "kotaemon_vectors",
    "schema_name": "public",
    "embed_dim": 1536,
    "ssl": False,
}
```

2) Ensure your embedding model dimension matches `embed_dim`.
   - For example, if you use OpenAI `text-embedding-3-small` (1536 dims), keep `embed_dim=1536`.
   - If you use a model with a different dimension, update `embed_dim` accordingly.

3) Start the app and build an index through the UI or pipeline. The vector store table will be created on first use if it does not exist.

What `opengauss.py` Implements
------------------------------
The file `opengauss.py` contains the `OpenGaussVectorStore` class which implements the Kotaemon `BaseVectorStore` interface. Key methods:

- Initialization and table creation
  - Establishes a connection to OpenGauss using `psycopg2`.
  - Creates the schema/table if missing using `CREATE TABLE IF NOT EXISTS`.
  - Attempts to create an HNSW index (if supported) and falls back to a regular index.

- `add(embeddings, ids=None, metadatas=None)`
  - Inserts embedding vectors and metadata rows.
  - If `ids` are not provided, UUIDs are generated.
  - Current behavior raises on duplicate `id` (primary key conflict). To make it idempotent, you can switch to UPSERT with `ON CONFLICT (id) DO UPDATE`.

- `query(embedding, top_k=1, ids=None, metadata_filter=None, **kwargs)`
  - Performs nearest-neighbor search using `embedding <-> CAST(%s AS vector)`.
  - Accepts `ids` and also the alias `doc_ids` via `**kwargs` for compatibility with the retrieval pipeline.
  - Supports basic metadata filtering (equality, list membership, range-like).

- `delete(ids)` / `drop()` / `count()`
  - Delete specific ids, drop the entire table, or count rows.

- `get_metadata(ids)` / `update_metadata(id, metadata)` / `search_by_metadata(metadata_filter, limit=100)`
  - Retrieve, update, or search rows by metadata conditions.

Test Suite
----------
Located under `tests/test_opengauss/`, this comprehensive test suite validates all OpenGaussVectorStore functionality using pytest. Tests are organized by functionality and include proper setup/cleanup.

### Test Configuration
Tests use a local `.env` file for configuration. Create a `.env` file in the `tests/test_opengauss/` directory with:

```
OG_HOST=localhost
OG_PORT=5432
OG_DB=postgres
OG_USER=gaussdb
OG_PASSWORD=your_password
OG_TABLE=test_vectors
OG_SCHEMA=public
OG_DIM=1536
```

### Test Files

- `test_init.py`
  - Tests initialization, connection, and table creation logic.
  - Validates schema and table structure.

- `test_add.py`
  - Tests `add()` with both raw embeddings and DocumentWithEmbedding objects.
  - Validates insertion results and data integrity.

- `test_query.py`
  - Tests `query()` behavior, including optional filters and limits.
  - Validates similarity scores and result ordering.

- `test_delete.py`
  - Tests `delete()` removes targeted ids and that `count()` reflects the change.
  - Validates partial and complete deletion scenarios.

- `test_drop.py`
  - Tests `drop()` and re-initialization to recreate the table afterward.
  - Validates table recreation functionality.

- `test_count.py`
  - Tests `count()` before and after insertions.
  - Validates count consistency and accuracy.

- `test_metadata.py`
  - Tests `get_metadata()` and `update_metadata()` round-trip.
  - Validates metadata integrity and partial updates.

- `test_search_by_metadata.py`
  - Tests `search_by_metadata()` for equality and range-like filters.
  - Validates complex metadata search scenarios.

- `test_close.py`
  - Tests `close()` behavior and multiple close safety.
  - Validates connection cleanup.

### Running Tests
Run the complete test suite:

```bash
# Run all OpenGauss tests
pytest libs/kotaemon/tests/test_opengauss/

# Run specific test file
pytest libs/kotaemon/tests/test_opengauss/test_query.py

# Run with verbose output
pytest libs/kotaemon/tests/test_opengauss/ -v

# Run specific test method
pytest libs/kotaemon/tests/test_opengauss/test_add.py::TestOpenGaussVectorStoreAdd::test_add_raw_vectors
```

Notes and Recommendations
-------------------------
- Ensure the database user has privileges to create schemas/tables and indexes.
- Match `embed_dim` to your embedding model output dimension to avoid type errors.
- If you index the same document multiple times with the same ids, consider using UPSERT semantics in `add()` to update rows instead of raising on conflicts.
- All tests include proper cleanup to avoid leaving test data in the database.
- Tests use dedicated table names to avoid conflicts with production data.


