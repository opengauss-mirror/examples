"""关系模型测试：外键、一对多、多对多。

注意：外键在 openGauss B 兼容模式下的行为（级联、约束检查）是本文件考察点。
"""
import pytest
from peewee import AutoField, CharField, ForeignKeyField, ManyToManyField, Model


def test_foreign_key_and_backref(db):
    """外键 + backref 反向查询。"""

    class Company(Model):
        id = AutoField()
        name = CharField(50)

        class Meta:
            database = db
            table_name = 'test_rel_company'

    class Employee(Model):
        id = AutoField()
        name = CharField(50)
        company = ForeignKeyField(Company, backref='employees')

        class Meta:
            database = db
            table_name = 'test_rel_employee'

    db.create_tables([Company, Employee])
    c = Company.create(name='acme')
    e = Employee.create(name='bob', company=c)
    assert e.company.id == c.id          # 正向
    assert c.employees.count() == 1      # 反向 backref
    assert c.employees[0].name == 'bob'


def test_join_query(db):
    """JOIN 查询。"""

    class Company(Model):
        id = AutoField()
        name = CharField(50)

        class Meta:
            database = db
            table_name = 'test_rel_company2'

    class Employee(Model):
        id = AutoField()
        name = CharField(50)
        company = ForeignKeyField(Company, backref='employees')

        class Meta:
            database = db
            table_name = 'test_rel_employee2'

    db.create_tables([Company, Employee])
    c1 = Company.create(name='acme')
    c2 = Company.create(name='globex')
    Employee.create(name='bob', company=c1)
    Employee.create(name='alice', company=c1)
    Employee.create(name='carol', company=c2)

    rows = (Employee.select(Employee.name, Company.name)
            .join(Company)
            .where(Company.name == 'acme')
            .order_by(Employee.name))
    assert [(r.name, r.company.name) for r in rows] == [('alice', 'acme'), ('bob', 'acme')]


def test_many_to_many(db):
    """多对多 ManyToManyField + 自动中间表。"""

    class Student(Model):
        id = AutoField()
        name = CharField(50)

        class Meta:
            database = db
            table_name = 'test_rel_student'

    class Course(Model):
        id = AutoField()
        title = CharField(50)
        students = ManyToManyField(Student, backref='courses')

        class Meta:
            database = db
            table_name = 'test_rel_course'

    through = Course.students.get_through_model()
    db.create_tables([Student, Course, through])
    s1 = Student.create(name='s1')
    s2 = Student.create(name='s2')
    c = Course.create(title='python')
    c.students.add([s1, s2])
    assert c.students.count() == 2
    assert s1.courses.count() == 1      # backref
    c.students.remove(s1)
    assert c.students.count() == 1
