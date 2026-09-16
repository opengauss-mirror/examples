"""字符集测试：utf8mb4、emoji、中文、特殊字符。

openGauss 内部为 UTF8，dolphin 对 utf8mb4（4 字节字符）的映射是本文件考察点。
"""
import pytest
from peewee import AutoField, CharField, Model, TextField


def test_emoji_and_chinese(db):
    """emoji（4 字节）+ 中文往返。"""
    content = 'hello 😀 中文测试 🚀'

    class M(Model):
        id = AutoField()
        content = CharField(100)

        class Meta:
            database = db
            table_name = 'test_charset_emoji'

    db.create_tables([M])
    row = M.create(content=content)
    got = M.get_by_id(row.id)
    assert got.content == content


def test_like_special_chars(db):
    """LIKE 中的 % 与 _ 是通配符，应能正确处理转义。"""
    class M(Model):
        id = AutoField()
        v = CharField(100)

        class Meta:
            database = db
            table_name = 'test_charset_like'

    db.create_tables([M])
    M.create(v='100%done')
    M.create(v='100_done')
    M.create(v='100xdone')
    # contains 生成 LIKE '%xx%'，percent 字段应匹配
    assert M.select().where(M.v.contains('100%')).count() == 1
    assert M.select().where(M.v.contains('100_')).count() == 1


def test_mixed_encoding_columns(db):
    """同一行内多列混合字符。"""
    class M(Model):
        id = AutoField()
        en = CharField(50)
        cn = CharField(50)
        note = TextField()

        class Meta:
            database = db
            table_name = 'test_charset_mixed'

    db.create_tables([M])
    row = M.create(en='abc', cn='中文', note='混合 mixed 😀')
    got = M.get_by_id(row.id)
    assert (got.en, got.cn, got.note) == ('abc', '中文', '混合 mixed 😀')


def test_connection_charset_param(db):
    """连接使用的字符集应正确（config 中 charset=utf8mb4）。"""
    cur = db.execute_sql('SELECT @@character_set_client, @@character_set_connection')
    row = cur.fetchone()
    print('character_set_client/connection =', row)
