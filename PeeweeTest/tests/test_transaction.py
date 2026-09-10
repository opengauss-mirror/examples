"""事务测试：atomic 提交/回滚、保存点、autocommit 行为。

注意：dolphin 对 MySQL 协议的 BEGIN/COMMIT/ROLLBACK 支持度、隔离级别映射
（openGauss 默认 RC，MySQL 默认 RR）都是本文件的考察点，结果记入报告。
"""
import pytest
from peewee import AutoField, CharField, IntegerField, Model


def make_item(db):
    class Item(Model):
        id = AutoField()
        name = CharField(50)
        qty = IntegerField(default=0)

        class Meta:
            database = db
            table_name = 'test_txn_item'

    return Item


def test_atomic_commit(db):
    """atomic() 正常退出应提交。"""
    Item = make_item(db)
    db.create_tables([Item])
    with db.atomic():
        Item.create(name='a', qty=1)
        Item.create(name='b', qty=2)
    assert Item.select().count() == 2


def test_atomic_rollback_on_exception(db):
    """atomic() 内抛异常应整体回滚。"""
    Item = make_item(db)
    db.create_tables([Item])
    with pytest.raises(RuntimeError):
        with db.atomic():
            Item.create(name='a', qty=1)
            raise RuntimeError('触发回滚')
    assert Item.select().count() == 0


def test_explicit_rollback(db):
    """atomic() 上下文内主动 rollback()。"""
    Item = make_item(db)
    db.create_tables([Item])
    with db.atomic() as txn:
        Item.create(name='a', qty=1)
        txn.rollback()
    assert Item.select().count() == 0


def test_savepoint(db):
    """保存点：内层回滚不影响外层已提交部分。"""
    Item = make_item(db)
    db.create_tables([Item])
    with db.atomic():
        Item.create(name='keep', qty=1)
        with db.savepoint() as sp:
            Item.create(name='drop', qty=2)
            sp.rollback()
    names = {i.name for i in Item.select()}
    assert names == {'keep'}


def test_autocommit_visibility(db):
    """用独立物理连接验证自动提交可见，并覆盖未提交反例。"""
    Item = make_item(db)
    db.create_tables([Item])
    Item.create(name='a', qty=1)
    other = db.__class__(db.database, **db.connect_params)
    try:
        other.connect()
        other.execute_sql('SET autocommit = 1')
        assert other.connection() is not db.connection()
        assert other.execute_sql(
            'SELECT COUNT(*) FROM test_txn_item').fetchone()[0] == 1

        # 显式事务未提交时，观察连接不能看到新记录；退出后才可见。
        with db.atomic():
            Item.create(name='pending', qty=2)
            assert other.execute_sql(
                "SELECT COUNT(*) FROM test_txn_item "
                "WHERE name = 'pending'"
            ).fetchone()[0] == 0
        assert other.execute_sql(
            "SELECT COUNT(*) FROM test_txn_item WHERE name = 'pending'"
        ).fetchone()[0] == 1
    finally:
        if not other.is_closed():
            other.close()


def test_isolation_level_query(db):
    """记录当前事务隔离级别（openGauss 默认 RC，与 MySQL 默认 RR 的差异记入报告）。"""
    row = db.execute_sql('SELECT @@transaction_isolation').fetchone()
    assert row and row[0], '@@transaction_isolation 无返回'
    print('@@transaction_isolation =', row)
