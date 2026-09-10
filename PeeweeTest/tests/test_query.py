"""查询测试：条件、排序、分页、聚合、模糊匹配。"""
import pytest
from peewee import AutoField, CharField, IntegerField, Model, fn


def make_item(db):
    class Item(Model):
        id = AutoField()
        name = CharField(50)
        qty = IntegerField(default=0)
        cat = CharField(20)

        class Meta:
            database = db
            table_name = 'test_query_item'

    return Item


@pytest.fixture
def items(db):
    Item = make_item(db)
    db.create_tables([Item])
    data = [
        ('apple', 5, 'fruit'), ('banana', 8, 'fruit'),
        ('carrot', 3, 'veg'), ('durian', 12, 'fruit'),
        ('eggplant', 4, 'veg'),
    ]
    Item.insert_many(
        [{'name': n, 'qty': q, 'cat': c} for n, q, c in data]
    ).execute()
    return Item


def test_where_conditions(db, items):
    """等值、IN、范围条件。"""
    Item = items
    assert Item.select().where(Item.cat == 'fruit').count() == 3
    assert Item.select().where(Item.name.in_(['apple', 'durian'])).count() == 2
    assert Item.select().where(Item.qty >= 5).count() == 3
    veg_rows = (Item.select()
                .where((Item.qty >= 4) & (Item.cat == 'veg'))
                .order_by(Item.name))
    assert [row.name for row in veg_rows] == ['eggplant']


def test_contains_like(db, items):
    """模糊匹配 contains → LIKE '%xx%'。"""
    Item = items
    contains_rows = (Item.select()
                     .where(Item.name.contains('an'))
                     .order_by(Item.name))
    assert [row.name for row in contains_rows] == [
        'banana', 'durian', 'eggplant']
    assert Item.select().where(Item.name.startswith('a')).count() == 1
    assert Item.select().where(Item.name.endswith('t')).count() == 2


def test_order_and_limit(db, items):
    """排序 + 限量。"""
    Item = items
    rows = Item.select().order_by(Item.qty.desc()).limit(2)
    assert [r.name for r in rows] == ['durian', 'banana']


def test_paginate(db, items):
    """分页 paginate(页码从 1 开始)。"""
    Item = items
    page1 = list(Item.select().order_by(Item.id).paginate(1, 2))
    page2 = list(Item.select().order_by(Item.id).paginate(2, 2))
    assert len(page1) == 2 and len(page2) == 2
    assert page1[0].id != page2[0].id


def test_aggregate(db, items):
    """聚合：COUNT/SUM/AVG/MIN/MAX + GROUP BY。"""
    Item = items
    total = Item.select(fn.COUNT(Item.id), fn.SUM(Item.qty)).scalar(as_tuple=True)
    print('COUNT,SUM =', total)
    assert total[0] == 5
    groups = (Item.select(Item.cat, fn.COUNT(Item.id).alias('cnt'))
              .group_by(Item.cat).order_by(Item.cat))
    summary = {g.cat: g.cnt for g in groups}
    assert summary == {'fruit': 3, 'veg': 2}


def test_count_and_exists(db, items):
    """count() / exists()。"""
    Item = items
    assert Item.select().count() == 5
    assert Item.select().where(Item.name == 'apple').exists()
    assert not Item.select().where(Item.name == 'zzz').exists()
