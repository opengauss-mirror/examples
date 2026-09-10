"""peewee 最小连接验证脚本（Windows 本机运行）

用法：先设置环境变量（同 connect_pymysql.py），再执行：
    python connect_peewee.py

重点观察输出：
    1. server_version —— peewee 执行 SELECT VERSION() 的解析结果
    2. inserted id —— 自增主键回读（lastrowid 链路）
"""
import os
import sys

from peewee import AutoField, CharField, IntegerField, Model, MySQLDatabase

HOST = os.environ.get('PEEWEE_TEST_HOST', '127.0.0.1')
PORT = int(os.environ.get('PEEWEE_TEST_PORT', '3308'))
USER = os.environ.get('PEEWEE_TEST_USER', '')
PASSWORD = os.environ.get('PEEWEE_TEST_PASSWORD', '')
DATABASE = os.environ.get('PEEWEE_TEST_DB', 'b_database')

db = MySQLDatabase(DATABASE, host=HOST, port=PORT, user=USER,
                   password=PASSWORD, charset='utf8mb4')


class Person(Model):
    id = AutoField()
    name = CharField(50)
    age = IntegerField(default=0)

    class Meta:
        database = db
        table_name = 't_person'


def main():
    if not USER or not PASSWORD:
        print('请先设置环境变量 PEEWEE_TEST_USER / PEEWEE_TEST_PASSWORD')
        return 1
    try:
        db.connect()
    except Exception as exc:
        print(f'[失败] 连接失败: {type(exc).__name__}: {exc}')
        return 1
    try:
        print('server_version =', db.server_version)
        Person.drop_table(safe=True)
        db.create_tables([Person])
        p = Person.create(name='alice', age=18)
        print('inserted id =', p.id)
        print('count =', Person.select().where(Person.age > 10).count())
        Person.drop_table(safe=True)
        print('[+] peewee 最小验证通过')
    except Exception as exc:
        print(f'[失败] 执行出错: {type(exc).__name__}: {exc}')
        return 1
    finally:
        db.close()
    return 0


if __name__ == '__main__':
    sys.exit(main())
