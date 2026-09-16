"""数据类型与边界测试：数值边界、NULL/空串、日期时间、大文本、精度。"""
from datetime import datetime, date, time
from decimal import Decimal

import pytest
from peewee import AutoField, BigIntegerField, CharField, DateField, \
    DateTimeField, DecimalField, IntegerField, Model, SmallIntegerField, \
    TextField, TimeField


def test_numeric_boundaries(db):
    """整型边界与 DECIMAL 精度往返。"""

    class Num(Model):
        id = AutoField()
        big = BigIntegerField()
        small = SmallIntegerField()
        price = DecimalField(10, 2)

        class Meta:
            database = db
            table_name = 'test_type_num'

    db.create_tables([Num])
    row = Num.create(big=2 ** 63 - 1, small=-32768, price=Decimal('99999999.99'))
    got = Num.get_by_id(row.id)
    assert got.big == 2 ** 63 - 1
    assert got.small == -32768
    assert got.price == Decimal('99999999.99')


def test_null_and_empty_string(db):
    """NULL 与空字符串的区别必须保留。"""

    class S(Model):
        id = AutoField()
        name = CharField(null=True)
        note = TextField(null=True)

        class Meta:
            database = db
            table_name = 'test_type_null'

    db.create_tables([S])
    row = S.create(name=None, note='')
    got = S.get_by_id(row.id)
    assert got.name is None
    assert got.note == ''


def test_datetime_roundtrip(db):
    """DATETIME/DATE/TIME 往返精度（秒级）。"""

    class T(Model):
        id = AutoField()
        ts = DateTimeField()
        d = DateField()
        tm = TimeField()

        class Meta:
            database = db
            table_name = 'test_type_dt'

    db.create_tables([T])
    ts = datetime(2026, 8, 3, 12, 30, 45)
    row = T.create(ts=ts, d=date(2026, 8, 3), tm=time(12, 30, 45))
    got = T.get_by_id(row.id)
    assert got.ts == ts
    assert got.d == date(2026, 8, 3)
    assert got.tm == time(12, 30, 45)


def test_long_text(db):
    """大文本（64KB 以上）往返。"""
    big = 'x' * 100_000

    class M(Model):
        id = AutoField()
        body = TextField()

        class Meta:
            database = db
            table_name = 'test_type_bigtext'

    db.create_tables([M])
    row = M.create(body=big)
    got = M.get_by_id(row.id)
    assert got.body == big
    assert len(got.body) == 100_000


def test_string_with_quotes_and_backslash(db):
    """含引号/反斜杠的字符串（参数化插入，验证转义）。"""
    tricky = "a'b\"c\\d"

    class S(Model):
        id = AutoField()
        v = CharField(100)

        class Meta:
            database = db
            table_name = 'test_type_quote'

    db.create_tables([S])
    standard_strings = db.execute_sql(
        'SHOW standard_conforming_strings').fetchone()[0]
    dolphin_mode = db.execute_sql('SHOW dolphin.sql_mode').fetchone()[0]
    insert_query = S.insert(v=tricky)
    sql, params = insert_query.sql()
    sent_sql = db.connection().cursor().mogrify(sql, params)
    print('standard_conforming_strings =', standard_strings)
    print('dolphin.sql_mode =', dolphin_mode)
    print('Peewee SQL =', sql, params)
    print('PyMySQL sent SQL =', sent_sql)
    row_id = insert_query.execute()
    assert S.get_by_id(row_id).v == tricky
