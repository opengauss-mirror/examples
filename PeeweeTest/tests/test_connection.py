"""连接与基础能力测试（对应思维导图：连接与配置分支）。

重点验证：
1. SELECT VERSION() 的返回格式（peewee 会解析它作为 server_version）
2. cursor.lastrowid（自增主键回读，peewee AutoField 依赖）
3. SHOW TABLES / information_schema 元数据查询
4. SET autocommit 支持情况
"""
import pytest
from peewee import MySQLDatabase


def test_connect_and_server_version(db):
    """连接成功，且能取到服务端版本号。"""
    assert db.is_connection_usable()
    assert db.server_version, 'server_version 为空，SELECT VERSION() 可能未返回 MySQL 格式版本串'
    print('server_version =', db.server_version)


def test_select_version_format(db):
    """记录 SELECT VERSION() 原始返回，便于判断驱动解析是否受影响。"""
    cur = db.execute_sql('SELECT VERSION()')
    row = cur.fetchone()
    assert row and row[0], 'SELECT VERSION() 无返回'
    print('VERSION() =', row[0])


def test_raw_insert_lastrowid(db):
    """原生 SQL 插入后 lastrowid 必须大于 0，否则 peewee AutoField 无法回读主键。"""
    db.execute_sql(
        'CREATE TABLE IF NOT EXISTS test_conn_t1 '
        '(id INT AUTO_INCREMENT PRIMARY KEY, v VARCHAR(20))'
    )
    cur = db.execute_sql('INSERT INTO test_conn_t1(v) VALUES (%s)', ('hello',))
    assert cur.lastrowid and cur.lastrowid > 0, f'lastrowid 异常: {cur.lastrowid}'


def test_show_tables(db):
    """peewee 的 get_tables() 依赖 SHOW TABLES，dolphin 必须支持。"""
    db.execute_sql(
        'CREATE TABLE IF NOT EXISTS test_conn_t2 (id INT AUTO_INCREMENT PRIMARY KEY)'
    )
    tables = db.get_tables()
    assert 'test_conn_t2' in tables, f'get_tables() 结果异常: {tables}'


def test_get_columns_introspection(db):
    """模型内省：peewee 的 get_columns() 查 information_schema，列信息应完整。"""
    db.execute_sql(
        'CREATE TABLE IF NOT EXISTS test_conn_t3 '
        '(id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50) NOT NULL, '
        'remark TEXT NULL, price DECIMAL(10,2) DEFAULT 0.00)'
    )
    raw_columns = db.execute_sql(
        'SELECT column_name, is_nullable, data_type, column_default, '
        'column_type, extra FROM information_schema.columns '
        'WHERE table_name = %s AND table_schema = DATABASE() '
        'ORDER BY ordinal_position',
        ('test_conn_t3',),
    ).fetchall()
    raw_indexes = db.execute_sql('SHOW INDEX FROM `test_conn_t3`').fetchall()
    print('raw information_schema.columns =', raw_columns)
    print('raw SHOW INDEX =', raw_indexes)

    raw_by_name = {column[0]: column for column in raw_columns}
    assert raw_by_name['id'][5] == 'auto_increment', (
        'Dolphin information_schema.columns.extra 未按 MySQL 约定返回 '
        f"auto_increment: {raw_by_name['id']}"
    )
    assert (raw_by_name['price'][2], raw_by_name['price'][4]) == (
        'decimal', 'decimal(10,2)'), (
        'Dolphin 未在 information_schema.columns 中保留 DECIMAL 类型与精度: '
        f"{raw_by_name['price']}"
    )
    assert any(index[2] == 'PRIMARY' and index[4] == 'id'
               for index in raw_indexes), (
        f'Dolphin SHOW INDEX 未按 MySQL 约定返回 PRIMARY 主键名: {raw_indexes}'
    )

    cols = db.get_columns('test_conn_t3')
    by_name = {c.name: c for c in cols}
    assert 'id' in by_name and 'name' in by_name
    for column in cols:
        print('column metadata =', column._asdict())
    id_metadata = by_name['id']
    assert (id_metadata.primary_key, id_metadata.identity) == (True, True), (
        'Peewee 未从 Dolphin 元数据识别主键/自增属性: '
        f'{id_metadata._asdict()}'
    )
    price_metadata = by_name['price']
    assert (price_metadata.data_type, price_metadata.full_type) == (
        'decimal', 'decimal(10,2)'), (
        'Peewee 未从 Dolphin 元数据保留 DECIMAL 精度: '
        f'{price_metadata._asdict()}'
    )


def test_set_autocommit(db):
    """dolphin 是否支持 SET autocommit（peewee 默认 autocommit=True 会用到）。"""
    cur = db.execute_sql('SET autocommit = 1')
    cur = db.execute_sql('SELECT @@autocommit')
    row = cur.fetchone()
    assert row, 'SELECT @@autocommit 无返回'
    print('@@autocommit =', row[0])


def test_sql_mode_value(db):
    """记录 @@sql_mode 值；若 Unknown system variable 则记入限制清单。"""
    try:
        cur = db.execute_sql('SELECT @@sql_mode')
        row = cur.fetchone()
        print('@@sql_mode =', row)
    except Exception as exc:
        pytest.fail(f'@@sql_mode 查询失败（dolphin 系统变量支持有限，记入报告）: {exc}')


def test_database_type(db):
    """连接对象应为 peewee 的 MySQLDatabase。"""
    assert isinstance(db, MySQLDatabase)
