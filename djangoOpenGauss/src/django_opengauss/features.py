"""
OpenGauss 数据库特性声明
定义 OpenGauss 支持的功能和特性
"""
from django.db.backends.postgresql import features as postgresql_features


class DatabaseFeatures(postgresql_features.DatabaseFeatures):
    # OpenGauss 数据库名称
    display_name = 'OpenGauss'
    
    # 基本特性支持
    allows_group_by_pk = True  # 允许按主键分组
    allows_group_by_selected_pks = True  # 允许按选定的主键分组
    can_return_id_from_insert = True  # 支持 RETURNING 子句
    can_return_ids_from_bulk_insert = True  # 批量插入支持 RETURNING
    has_real_datatype = True  # 支持 REAL 数据类型
    has_native_uuid_field = True  # 原生支持 UUID 类型
    has_native_duration_field = True  # 支持时间间隔类型
    has_native_json_field = True  # 支持 JSON 数据类型
    can_defer_constraint_checks = True  # 支持延迟约束检查
    has_select_for_update = True  # 支持 SELECT ... FOR UPDATE
    has_select_for_update_nowait = True  # 支持 NOWAIT 选项
    has_select_for_update_skip_locked = True  # 支持 SKIP LOCKED
    has_select_for_update_of = True  # 支持 OF 子句
    
    # 事务特性
    uses_savepoints = True  # 支持保存点
    can_release_savepoints = True  # 可以释放保存点
    supports_transactions = True  # 支持事务
    supports_foreign_keys = True  # 支持外键
    requires_explicit_null_ordering_when_grouping = True  # GROUP BY 需要明确的 NULL 排序
    
    # 索引和约束特性
    supports_table_check_constraints = True  # 支持表级检查约束
    supports_column_check_constraints = True  # 支持列级检查约束
    supports_tablespaces = True  # 支持表空间
    supports_indexes_on_text_fields = True  # 支持文本字段索引
    supports_partial_indexes = True  # 支持部分索引
    supports_expression_indexes = True  # 支持表达式索引
    supports_covering_indexes = True  # 支持覆盖索引
    
    # 序列和自增特性
    supports_sequence_reset = True  # 支持序列重置
    can_introspect_autofield = True  # 可以内省自增字段
    supports_identity_columns = False  # 禁用 IDENTITY 列，使用 serial 类型
    can_create_inline_fk = False  # 避免复杂的内联外键创建
    
    # JSON 特性
    has_json_object_function = True  # 支持 JSON 对象函数
    supports_json_field_contains = True  # 支持 JSON 包含查询
    has_jsonb_datatype = True  # 支持 JSONB 数据类型
    
    # 时间和日期特性
    has_zoneinfo_database = True  # 有时区数据库
    supports_timezones = True  # 支持时区
    
    # 聚合和窗口函数
    supports_aggregate_filter_clause = True  # 支持聚合过滤子句
    supports_over_clause = True  # 支持窗口函数
    
    # 并发控制
    supports_select_for_update_with_limit = True  # FOR UPDATE 支持 LIMIT
    
    # OpenGauss 特有功能
    supports_temporal_tables = False  # 暂不支持时态表
    has_native_array_field = True  # 支持数组类型
    supports_collation_on_charfield = True  # 字符字段支持排序规则
    supports_collation_on_textfield = True  # 文本字段支持排序规则
    
    # 批量操作特性
    supports_bulk_insert_with_conflicts = True  # 支持冲突处理的批量插入
    supports_update_conflicts = True  # 支持 ON CONFLICT UPDATE
    supports_ignore_conflicts = True  # 支持 ON CONFLICT IGNORE
    
    # 性能优化特性
    can_rollback_ddl = True  # DDL 操作可以回滚
    supports_combined_alters = True  # 支持组合的 ALTER 语句
    
    # 数据类型限制
    max_query_params = 65535  # 最大查询参数数
    
    # SQL 功能
    greatest_least_ignores_nulls = False  # GREATEST/LEAST 不忽略 NULL
    
    # 索引操作
    supports_index_column_ordering = True  # 支持索引列排序
    
    # 视图和物化视图
    can_introspect_materialized_views = True  # 可以内省物化视图
    
    # 高级特性
    supports_slicing_ordering_in_compound = True  # 复合查询中支持切片排序
    allows_auto_pk_0 = False  # 不允许自动主键为 0
    
    # 测试数据库特性
    test_collations = {
        'ci': 'en_US.utf8',  # 大小写不敏感
        'cs': 'C',  # 大小写敏感
        'non_default': 'zh_CN.utf8',  # 非默认排序规则
    }
    
    # 自定义方法
    def allows_group_by_lob(self):
        """
        是否允许对大对象字段进行 GROUP BY
        OpenGauss 支持对大对象进行分组
        """
        return True
    
    def supports_explaining_query_execution(self):
        """
        是否支持 EXPLAIN 查询执行计划
        OpenGauss 完全支持 EXPLAIN 和 EXPLAIN ANALYZE
        """
        return True
    
    def supports_transactions_in_tests(self):
        """
        测试中是否支持事务
        """
        return True
    
    def can_clone_databases(self):
        """
        是否可以克隆数据库
        OpenGauss 支持使用 CREATE DATABASE ... TEMPLATE 克隆数据库
        """
        return True
    
    def introspected_field_types(self):
        """
        内省字段类型映射
        将 OpenGauss 数据类型映射到 Django 字段类型
        """
        base_mapping = super().introspected_field_types
        if callable(base_mapping):
            base_mapping = base_mapping()
        else:
            base_mapping = base_mapping.copy()

        base_mapping.update(
            {
                'json': 'JSONField',
                'jsonb': 'JSONField',
                'uuid': 'UUIDField',
                'xml': 'TextField',
                'tsvector': 'TextField',
            }
        )
        return base_mapping
