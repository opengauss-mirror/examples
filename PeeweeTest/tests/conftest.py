"""pytest 公共配置：连接管理、autocommit、用例后清理。

连接配置优先级：环境变量 PEEWEE_TEST_* > config.ini（config.example.ini 的副本）。
约定：测试建的表统一使用 test_ 前缀，用例结束后自动 DROP，保证可重复执行。
"""
import configparser
import os
from pathlib import Path

import pytest
from peewee import MySQLDatabase

CONFIG_FILE = Path(__file__).resolve().parent.parent / 'config.ini'


def load_config() -> dict:
    """读取连接配置。返回 {host, port, user, password, database}。"""
    cfg = {
        'host': '127.0.0.1',
        'port': '3308',
        'user': '',
        'password': '',
        'database': 'b_database',
    }
    if CONFIG_FILE.exists():
        # 连接参数必须按原文读取；默认插值会破坏含 % 或 %(...)s 的合法密码。
        parser = configparser.ConfigParser(interpolation=None)
        parser.read(CONFIG_FILE, encoding='utf-8')
        if parser.has_section('database'):
            for key in ('host', 'port', 'user', 'password', 'database'):
                if parser.has_option('database', key):
                    cfg[key] = parser.get('database', key)

    # 环境变量优先于 config.ini；只有显式设置的变量才覆盖文件配置。
    env_names = {
        'host': 'PEEWEE_TEST_HOST',
        'port': 'PEEWEE_TEST_PORT',
        'user': 'PEEWEE_TEST_USER',
        'password': 'PEEWEE_TEST_PASSWORD',
        'database': 'PEEWEE_TEST_DB',
    }
    for key, env_name in env_names.items():
        if env_name in os.environ:
            cfg[key] = os.environ[env_name]
    cfg['port'] = int(cfg['port'])
    if not cfg['user']:
        raise RuntimeError(
            '未配置数据库用户：请设置环境变量 PEEWEE_TEST_USER 或创建 config.ini'
        )
    return cfg


@pytest.fixture(scope='session')
def db():
    """会话级连接。环境不可用或配置不正确时直接判定测试失败。"""
    cfg = load_config()
    database = MySQLDatabase(
        cfg['database'],
        host=cfg['host'],
        port=cfg['port'],
        user=cfg['user'],
        password=cfg['password'],
        charset='utf8mb4',
    )
    try:
        database.connect()
    except Exception as exc:
        pytest.fail(f'数据库连接失败，无法执行兼容性验证: {exc}',
                    pytrace=False)

    # 显式打开并验证 autocommit，保证普通 DML 立即可见。
    try:
        database.execute_sql('SET autocommit = 1')
        autocommit = database.execute_sql(
            'SELECT @@autocommit'
        ).fetchone()
        assert autocommit and int(autocommit[0]) == 1
    except Exception as exc:
        database.close()
        pytest.fail(f'autocommit 配置或验证失败: {exc}', pytrace=False)

    try:
        yield database
    finally:
        if not database.is_closed():
            database.close()


@pytest.fixture(autouse=True)
def cleanup(db):
    """按外键依赖逆序删除本套件创建的表，并验证没有残留。"""
    _drop_test_tables(db, _get_test_tables(db))
    yield
    _drop_test_tables(db, _get_test_tables(db))
    leftovers = _get_test_tables(db)
    if leftovers:
        pytest.fail(
            f'测试清理后仍有 test_ 前缀残留表: {sorted(leftovers)}',
            pytrace=False,
        )


def _get_test_tables(db):
    """返回本套件约定使用的 test_ 前缀表；元数据失败必须显式报告。"""
    try:
        return {table for table in db.get_tables()
                if table.startswith('test_')}
    except Exception as exc:
        pytest.fail(f'获取测试表列表失败: {exc}', pytrace=False)


def _drop_test_tables(db, tables):
    """删除表图中的叶子节点，保证外键子表先于父表删除。"""
    remaining = set(tables)
    while remaining:
        referenced = set()
        for table in remaining:
            try:
                foreign_keys = db.get_foreign_keys(table)
            except Exception as exc:
                pytest.fail(
                    f'读取测试表 {table} 的外键元数据失败: {exc}',
                    pytrace=False,
                )
            referenced.update(
                fk.dest_table for fk in foreign_keys
                if fk.dest_table in remaining
            )

        # 没有被其他剩余表引用的表是当前可删除的子表/叶子表。
        candidates = remaining - referenced
        if not candidates:
            pytest.fail(
                f'测试表存在循环外键依赖，无法安全清理: {sorted(remaining)}',
                pytrace=False,
            )

        for table in sorted(candidates):
            quoted_table = table.replace('`', '``')
            try:
                db.execute_sql(f'DROP TABLE IF EXISTS `{quoted_table}`')
            except Exception as exc:
                pytest.fail(f'删除测试表 {table} 失败: {exc}', pytrace=False)
        remaining.difference_update(candidates)
