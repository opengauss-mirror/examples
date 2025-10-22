"""
OpenGauss 数据库操作适配器
处理 SQL 操作的生成和转换，继承自 PostgreSQL 但针对 OpenGauss 进行优化
"""
from django.db.backends.postgresql import operations as postgresql_operations
from django.db.backends.base.operations import BaseDatabaseOperations
from django.conf import settings
from django.utils import timezone
import datetime


class DatabaseOperations(postgresql_operations.DatabaseOperations):
    
    def __init__(self, connection):
        super().__init__(connection)
    
    def sql_flush(self, style, tables, reset_sequences=True, allow_cascade=False):
        """
        返回清空数据库表的 SQL 语句列表
        OpenGauss 支持 TRUNCATE ... CASCADE
        """
        if not tables:
            return []
        
        sql_list = []
        
        # 使用 TRUNCATE 清空表数据，支持级联删除
        if allow_cascade:
            sql = 'TRUNCATE %s CASCADE;' % ', '.join(
                style.SQL_FIELD(self.quote_name(table)) for table in tables
            )
        else:
            sql = 'TRUNCATE %s;' % ', '.join(
                style.SQL_FIELD(self.quote_name(table)) for table in tables
            )
        sql_list.append(sql)
        
        # 重置序列 (如果需要)
        if reset_sequences:
            # 对于 OpenGauss，TRUNCATE ... RESTART IDENTITY 可以重置序列
            # 但为了兼容性，我们保留单独重置的选项
            pass
        
        return sql_list
    
    def sequence_reset_by_name_sql(self, style, sequences):
        """
        返回重置序列的 SQL 语句列表
        OpenGauss 使用 RESTART IDENTITY 语法
        """
        sql = []
        for sequence_info in sequences:
            table_name = sequence_info['table']
            column_name = sequence_info['column']
            sequence_name = '%s_%s_seq' % (table_name, column_name)
            sql.append(
                "ALTER SEQUENCE %s RESTART WITH 1;" % 
                self.quote_name(sequence_name)
            )
        return sql
    
    def bulk_insert_sql(self, fields, placeholder_rows):
        """
        生成批量插入的 SQL
        OpenGauss 支持多行 VALUES 语法
        """
        placeholder_rows_sql = ", ".join(
            "(%s)" % ", ".join(row) for row in placeholder_rows
        )
        return "VALUES %s" % placeholder_rows_sql
    
    def adapt_datetimefield_value(self, value):
        """
        将 datetime 值转换为数据库格式
        OpenGauss 兼容 PostgreSQL 的时间戳格式
        """
        if value is None:
            return None
        
        if timezone.is_aware(value):
            value = timezone.make_naive(value, self.connection.timezone)
        
        return value.isoformat()
    
    def adapt_timefield_value(self, value):
        """
        将 time 值转换为数据库格式
        """
        if value is None:
            return None
        
        if isinstance(value, str):
            return value
        
        return value.isoformat()
    
    def adapt_datefield_value(self, value):
        """
        将 date 值转换为数据库格式
        """
        if value is None:
            return None
        
        return value.isoformat()
    
    def max_name_length(self):
        """
        返回 OpenGauss 标识符的最大长度
        OpenGauss 支持最长 63 个字符的标识符
        """
        return 63
    
    def quote_name(self, name):
        """
        给标识符加引号
        OpenGauss 使用双引号包围标识符
        """
        if name.startswith('"') and name.endswith('"'):
            return name  # 已经有引号了
        return '"%s"' % name.replace('"', '""')
    
    def last_executed_query(self, cursor, sql, params):
        """
        返回最后执行的查询，用于调试
        将参数值替换到 SQL 中
        """
        # 如果启用了查询日志，返回完整的 SQL
        if settings.DEBUG:
            return cursor.mogrify(sql, params).decode('utf-8')
        return sql
    
    def return_insert_id(self):
        """
        返回 INSERT 语句的 RETURNING 子句
        兼容 Django 不同版本的返回值要求
        """
        return "RETURNING %s", ()

    def return_insert_columns(self, fields):
        """
        支持多列 RETURNING 语法，兼容 Django ORM 的 returning_fields
        """
        if not fields:
            return super().return_insert_columns(fields)

        column_names = []
        for field in fields:
            column = getattr(field, 'db_column', None) or field.column
            column_names.append(self.quote_name(column))

        return "RETURNING %s" % ", ".join(column_names), ()
    
    def bulk_batch_size(self, fields, objs):
        """
        返回批量操作的批次大小
        OpenGauss 可以处理较大的批次
        """
        # 默认批次大小为 1000
        return 1000
    
    def combine_expression(self, connector, sub_expressions):
        """
        组合表达式，用于复杂查询
        OpenGauss 支持标准 SQL 连接符
        """
        if connector == 'AND':
            return '(%s)' % ' AND '.join(sub_expressions)
        elif connector == 'OR':
            return '(%s)' % ' OR '.join(sub_expressions)
        elif connector == '+':
            return '(%s)' % ' + '.join(sub_expressions)
        elif connector == '-':
            return '(%s)' % ' - '.join(sub_expressions)
        elif connector == '*':
            return '(%s)' % ' * '.join(sub_expressions)
        elif connector == '/':
            return '(%s)' % ' / '.join(sub_expressions)
        elif connector == '%':
            return '(%s)' % ' %% '.join(sub_expressions)  # 模运算
        elif connector == '&':
            return '(%s)' % ' & '.join(sub_expressions)  # 位与
        elif connector == '|':
            return '(%s)' % ' | '.join(sub_expressions)  # 位或
        elif connector == '#':
            return '(%s)' % ' # '.join(sub_expressions)  # 位异或
        elif connector == '<<':
            return '(%s)' % ' << '.join(sub_expressions)  # 左移
        elif connector == '>>':
            return '(%s)' % ' >> '.join(sub_expressions)  # 右移
        else:
            raise ValueError("Unknown connector: %s" % connector)
    
    def integer_field_range(self, internal_type):
        """
        返回整数字段的值范围
        OpenGauss 整数类型范围与 PostgreSQL 相同
        """
        ranges = {
            'SmallIntegerField': (-32768, 32767),
            'IntegerField': (-2147483648, 2147483647),
            'BigIntegerField': (-9223372036854775808, 9223372036854775807),
            'PositiveSmallIntegerField': (0, 32767),
            'PositiveIntegerField': (0, 2147483647),
            'SmallAutoField': (-32768, 32767),
            'AutoField': (-2147483648, 2147483647),
            'BigAutoField': (-9223372036854775808, 9223372036854775807),
        }
        return ranges.get(internal_type, (None, None))
    
    def distinct_sql(self, fields, params):
        """
        生成 DISTINCT 查询的 SQL
        OpenGauss 支持 DISTINCT ON 语法
        """
        if fields:
            # DISTINCT ON (field1, field2, ...)
            fields_sql = ', '.join(fields)
            return ('DISTINCT ON (%s)' % fields_sql, params)
        else:
            return ('DISTINCT', params)
