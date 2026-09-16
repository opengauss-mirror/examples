"""DDL 测试：建表、删表、IF NOT EXISTS、字段类型映射。

注意：peewee 对 MySQL 用反引号引用标识符，B 兼容模式应支持；
create_tables 默认带 IF NOT EXISTS，重复调用不应报错。
"""
import pytest
from peewee import AutoField, BooleanField, CharField, DateTimeField, \
    DecimalField, FloatField, IntegerField, Model, SQL, TextField


@pytest.fixture
def base_model(db):
    class Base(Model):
        class Meta:
            database = db
    return Base


def test_create_and_drop_table(db, base_model):
    """建表 → 表存在 → 删表 → 表消失。"""

    class T(base_model):
        id = AutoField()
        name = CharField(50)

        class Meta:
            table_name = 'test_ddl_t1'

    db.create_tables([T])
    assert 'test_ddl_t1' in db.get_tables()
    T.drop_table()
    assert 'test_ddl_t1' not in db.get_tables()


def test_create_table_if_not_exists_idempotent(db, base_model):
    """create_tables 重复执行（IF NOT EXISTS）不应报错。"""

    class T(base_model):
        id = AutoField()
        name = CharField(50)

        class Meta:
            table_name = 'test_ddl_t2'

    db.create_tables([T])
    db.create_tables([T])  # 第二次调用

    # 确认建出的表实际可用
    T.create(name='x')
    assert T.select().count() == 1


def test_field_type_mapping(db, base_model):
    """验证 peewee 各字段类型建出的实际列类型，记录到报告。"""

    class Types(base_model):
        id = AutoField()
        vchar = CharField(255)
        text = TextField()
        flag = BooleanField()
        created = DateTimeField()
        score = FloatField()
        price = DecimalField(10, 2)
        age = IntegerField()

        class Meta:
            table_name = 'test_ddl_types'

    db.create_tables([Types])
    cols = {c.name: c for c in db.get_columns('test_ddl_types')}
    for name, col in cols.items():
        print(f'{name}: data_type={col.data_type} null={col.null} '
              f'pk={col.primary_key} default={col.default}')
    assert {'id', 'vchar', 'text', 'flag', 'created', 'score', 'price', 'age'} \
        <= set(cols)


def test_default_value(db, base_model):
    """字段默认值（数据库层 DEFAULT）应生效。"""

    class D(base_model):
        id = AutoField()
        status = IntegerField(constraints=[SQL('DEFAULT 0')])

        class Meta:
            table_name = 'test_ddl_default'

    db.create_tables([D])
    # 原生 INSERT 故意省略 status，随后重新读取实际落库值。
    db.execute_sql('INSERT INTO `test_ddl_default` (`id`) VALUES (NULL)')
    row = D.select().order_by(D.id.desc()).get()
    assert row.status == 0
