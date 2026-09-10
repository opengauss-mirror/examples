"""CRUD 测试：插入（单条/批量）、查询、更新、删除、自增主键回读。"""
import pytest
from peewee import AutoField, CharField, DoesNotExist, IntegerField, Model


def make_item(db):
    class Item(Model):
        id = AutoField()
        name = CharField(50)
        qty = IntegerField(default=0)

        class Meta:
            database = db
            table_name = 'test_crud_item'

    return Item


def test_insert_and_lastrowid(db):
    """单条插入：自增主键必须能回读（cursor.lastrowid）。"""
    Item = make_item(db)
    db.create_tables([Item])
    item = Item.create(name='a', qty=1)
    assert item.id and item.id > 0, '自增主键回读失败（lastrowid 异常）'
    item2 = Item.create(name='b')
    assert item2.id > item.id
    print(f'id1={item.id} id2={item2.id}（记录自增值是否连续/跳号）')


def test_batch_insert(db):
    """批量插入 insert_many，返回受影响行数。"""
    Item = make_item(db)
    db.create_tables([Item])
    rows = [{'name': f'n{i}', 'qty': i} for i in range(10)]
    n = Item.insert_many(rows).as_rowcount().execute()
    assert n == 10
    assert Item.select().count() == 10


def test_select_by_id_and_get(db):
    """get_by_id / get / get_or_none。"""
    Item = make_item(db)
    db.create_tables([Item])
    item = Item.create(name='a', qty=5)
    assert Item.get_by_id(item.id).name == 'a'
    assert Item.get(Item.name == 'a').qty == 5
    assert Item.get_or_none(Item.name == 'not_exist') is None
    with pytest.raises(DoesNotExist):
        Item.get(Item.name == 'not_exist')


def test_update(db):
    """update 返回受影响行数，条件更新。"""
    Item = make_item(db)
    db.create_tables([Item])
    Item.create(name='a', qty=1)
    Item.create(name='b', qty=2)
    n = Item.update(qty=99).where(Item.name == 'a').execute()
    assert n == 1
    assert Item.get(Item.name == 'a').qty == 99
    assert Item.get(Item.name == 'b').qty == 2


def test_delete(db):
    """delete_by_id 与条件删除。"""
    Item = make_item(db)
    db.create_tables([Item])
    a = Item.create(name='a')
    b = Item.create(name='b')
    Item.delete_by_id(a.id)
    assert Item.select().count() == 1
    Item.delete().where(Item.id == b.id).execute()
    assert Item.select().count() == 0


def test_get_or_create_idempotent(db):
    """get_or_create 重复调用应返回同一记录，不重复插入。"""
    Item = make_item(db)
    db.create_tables([Item])
    first, first_created = Item.get_or_create(
        name='a', defaults={'qty': 1})
    second, second_created = Item.get_or_create(
        name='a', defaults={'qty': 10})
    assert first_created is True
    assert second_created is False
    assert second.id == first.id
    assert second.qty == 1
    assert Item.select().count() == 1
