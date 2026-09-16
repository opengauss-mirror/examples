"""异常与错误处理测试。

重点：记录各种失败场景的**异常类型**（驱动层/服务端层），
并验证 peewee 提供的异常（IntegrityError / DoesNotExist / InternalError /
OperationalError）是否可用。
"""
import pymysql
import pytest
from peewee import AutoField, CharField, DoesNotExist, IntegrityError, \
    InternalError, Model, MySQLDatabase, OperationalError


def assert_error_details(exception, error_code, message_fragment):
    """精确检查服务端错误码和能标识目标失败原因的消息片段。"""
    assert exception.args[0] == error_code
    assert message_fragment.lower() in str(exception).lower()


def verify_config_file_fallback(monkeypatch, tmp_path):
    """验证 config.ini 原样读取，以及环境变量覆盖文件配置。"""
    import conftest

    config_file = tmp_path / 'config.ini'
    for env_name in (
        'PEEWEE_TEST_HOST', 'PEEWEE_TEST_PORT', 'PEEWEE_TEST_USER',
        'PEEWEE_TEST_PASSWORD', 'PEEWEE_TEST_DB',
    ):
        monkeypatch.delenv(env_name, raising=False)
    monkeypatch.setattr(conftest, 'CONFIG_FILE', config_file)

    # 默认 ConfigParser 会破坏这两类值；连接配置必须逐字保留。
    for config_password in ('Abc%de123!', 'Abc%(literal)s'):
        config_file.write_text(
            '[database]\n'
            'host = config-only.example\n'
            'port = 3309\n'
            'user = config_user\n'
            f'password = {config_password}\n'
            'database = config_database\n',
            encoding='utf-8',
        )
        assert conftest.load_config() == {
            'host': 'config-only.example',
            'port': 3309,
            'user': 'config_user',
            'password': config_password,
            'database': 'config_database',
        }

    env_config = {
        'PEEWEE_TEST_HOST': 'env.example',
        'PEEWEE_TEST_PORT': '3310',
        'PEEWEE_TEST_USER': 'env_user',
        'PEEWEE_TEST_PASSWORD': 'Env%password',
        'PEEWEE_TEST_DB': 'env_database',
    }
    for env_name, value in env_config.items():
        monkeypatch.setenv(env_name, value)
    assert conftest.load_config() == {
        'host': 'env.example',
        'port': 3310,
        'user': 'env_user',
        'password': 'Env%password',
        'database': 'env_database',
    }


def test_connect_failure_wrong_port(db):
    """连接不可达：应抛出驱动层异常（记录具体类型）。"""
    bad = MySQLDatabase('x', host='127.0.0.1', port=1, user='x', password='x')
    with pytest.raises(OperationalError):
        bad.connect()
    bad.close()


def test_connect_failure_auth(db, monkeypatch, tmp_path):
    """复用有效目标且仅替换密码，精确验证认证拒绝。"""
    verify_config_file_fallback(monkeypatch, tmp_path)

    valid_params = dict(db.connect_params)
    bad_params = dict(valid_params)
    bad_params['password'] = valid_params.get('password', '') + \
        '__definitely_wrong__'
    bad = MySQLDatabase(db.database, **bad_params)
    assert bad.database == db.database
    for key in ('host', 'port', 'user', 'charset'):
        assert bad.connect_params[key] == valid_params[key]
    assert bad.connect_params['password'] != valid_params.get('password')

    try:
        with pytest.raises(InternalError) as exc_info:
            bad.connect()
        assert_error_details(exc_info.value, 514, 'failed in auth check')
        print('认证失败异常类型:', type(exc_info.value).__name__,
              exc_info.value)
    finally:
        if not bad.is_closed():
            bad.close()


def test_missing_table(db):
    """查询不存在的表：精确断言当前协议错误类型、错误码和特征。"""
    with pytest.raises(InternalError) as exc_info:
        db.execute_sql('SELECT * FROM test_nonexistent_table')
    assert_error_details(exc_info.value, 132, 'does not exist')
    print('不存在表异常:', type(exc_info.value).__name__, exc_info.value)


def test_integrity_violation(db):
    """唯一约束冲突：PyMySQL 与 Peewee 均应映射为 IntegrityError。"""

    connection = db.connection()
    cursor = connection.cursor()
    try:
        cursor.execute('DROP TABLE IF EXISTS test_exc_pymysql_unique')
        cursor.execute(
            'CREATE TABLE test_exc_pymysql_unique '
            '(id INT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(20) UNIQUE)'
        )
        cursor.execute(
            'INSERT INTO test_exc_pymysql_unique(code) VALUES (%s)', ('dup',)
        )
        connection.begin()
        with pytest.raises(pymysql.err.IntegrityError) as driver_error:
            cursor.execute(
                'INSERT INTO test_exc_pymysql_unique(code) VALUES (%s)',
                ('dup',),
            )
        assert driver_error.value.args[0] == 1062
    finally:
        try:
            connection.rollback()
        finally:
            cursor.close()

    class U(Model):
        id = AutoField()
        code = CharField(20, unique=True)

        class Meta:
            database = db
            table_name = 'test_exc_unique'

    db.create_tables([U])
    U.create(code='dup')
    with pytest.raises(IntegrityError) as orm_error:
        with db.atomic():
            U.create(code='dup')
    assert orm_error.value.args[0] == 1062


def test_does_not_exist(db):
    """get 无结果应抛 peewee.DoesNotExist。"""

    class M(Model):
        id = AutoField()
        v = CharField(20)

        class Meta:
            database = db
            table_name = 'test_exc_missing'

    db.create_tables([M])
    with pytest.raises(DoesNotExist):
        M.get(M.id == 999999)


def test_peewee_error_mapping(db):
    """精确验证 Peewee 错误映射，并防止成功/无关异常被误判。"""
    with pytest.raises(InternalError) as exc_info:
        db.execute_sql('SELECT * FROM test_also_nonexistent')
    assert_error_details(exc_info.value, 132, 'does not exist')
    print('异常类型:', type(exc_info.value).__name__)
    print('MRO:', [c.__name__ for c in type(exc_info.value).__mro__])

    # 反例一：SQL 正常返回时，pytest.raises 必须令验证失败。
    with pytest.raises(pytest.fail.Exception):
        with pytest.raises(InternalError):
            None

    # 反例二：无关异常不能被目标数据库异常断言吞掉。
    def raise_unrelated_error():
        raise RuntimeError('unrelated failure')

    with pytest.raises(RuntimeError, match='unrelated failure'):
        with pytest.raises(InternalError):
            raise_unrelated_error()
