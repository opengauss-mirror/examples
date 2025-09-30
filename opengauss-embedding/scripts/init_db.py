from rag.config import settings
from rag.db import datavec

if __name__ == "__main__":
    conn = datavec.connect()
    datavec.check_datavec(conn)
    datavec.setup_table(conn, settings.TABLE_NAME, settings.EMB_DIM)
    datavec.create_hnsw_index(conn, settings.TABLE_NAME)
    conn.close()
    print("[ok] DataVec self-check + table/index ready.")
