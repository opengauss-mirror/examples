"""peewee × openGauss B 兼容库 独立演示脚本

单文件演示 peewee 连接 openGauss B 库（dolphin MySQL 协议）的完整流程：
连接 → 模型 → 建表 → 插入(自增回读) → 批量 → 查询 → 事务 → 清理。
也是第 6 周《Peewee 开发指导文档》示例代码的来源，必须与文档保持一致。

运行前：
1. openGauss 已开启 MySQL 协议（enable_dolphin_proto=on, dolphin_server_port=3308）
2. 已创建 B 兼容物理数据库和连接参数对应的业务 schema，并已加载 dolphin 扩展
3. pip install peewee pymysql

配置：环境变量 PEEWEE_TEST_HOST/PORT/USER/PASSWORD/DB，缺省见下方默认值。
用法：python example_demo.py
"""
import os

from peewee import (AutoField, CharField, IntegerField, Model, MySQLDatabase,
                    fn)

HOST = os.environ.get('PEEWEE_TEST_HOST', '127.0.0.1')
PORT = int(os.environ.get('PEEWEE_TEST_PORT', '3308'))
USER = os.environ.get('PEEWEE_TEST_USER', '')
PASSWORD = os.environ.get('PEEWEE_TEST_PASSWORD', '')
DATABASE = os.environ.get('PEEWEE_TEST_DB', 'b_database')

db = MySQLDatabase(DATABASE, host=HOST, port=PORT, user=USER,
                   password=PASSWORD, charset='utf8mb4')


class Person(Model):
    """人员模型：AutoField 自增主键。"""
    id = AutoField()
    name = CharField(50)
    age = IntegerField(default=0)

    class Meta:
        database = db
        table_name = 'demo_person'


def main():
    if not USER:
        print('请先设置环境变量 PEEWEE_TEST_USER / PEEWEE_TEST_PASSWORD')
        return 1

    db.connect()
    print(f'[*] 连接成功，server_version = {db.server_version}')

    # 1. 建表（IF NOT EXISTS，可重复执行）
    db.create_tables([Person])

    # 2. 单条插入 + 自增主键回读
    alice = Person.create(name='alice', age=18)
    print(f'[*] 插入成功，自增 id = {alice.id}')

    # 3. 批量插入
    Person.insert_many(
        [{'name': f'user{i}', 'age': 20 + i} for i in range(5)]
    ).execute()

    # 4. 查询
    total = Person.select().count()
    adults = Person.select().where(Person.age >= 18).count()
    oldest = Person.select().order_by(Person.age.desc()).get()
    avg_age = Person.select(fn.AVG(Person.age)).scalar()
    print(f'[*] 总数={total} 成年={adults} 最大年龄={oldest.age} 平均年龄={avg_age:.1f}')

    # 5. 事务：失败自动回滚
    try:
        with db.atomic():
            Person.create(name='txn', age=99)
            raise RuntimeError('模拟异常，触发回滚')
    except RuntimeError:
        pass
    assert Person.select().where(Person.name == 'txn').count() == 0
    print('[*] 事务回滚验证通过')

    # 6. 清理演示数据（保留表结构）
    Person.delete().execute()
    print(f'[*] 清理完成，剩余记录 = {Person.select().count()}')

    db.close()
    print('[+] 全部演示步骤通过')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
