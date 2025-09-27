"""
OpenGauss 数据库内省模块 - 修复与 Django 5.x 的兼容性问题
"""
from django.db.backends.postgresql.introspection import DatabaseIntrospection as PostgreSQLIntrospection


class DatabaseIntrospection(PostgreSQLIntrospection):
    """
    OpenGauss 数据库内省，基于 PostgreSQL 但处理差异
    """

    def get_table_list(self, cursor):
        """
        获取数据库中的表列表，兼容 OpenGauss
        """
        cursor.execute("""
            SELECT
                c.relname,
                CASE c.relkind
                    WHEN 'r' THEN 't'
                    WHEN 'p' THEN 't'
                    WHEN 'v' THEN 'v'
                    WHEN 'm' THEN 'v'
                    WHEN 'f' THEN 'v'
                    ELSE 'o'
                END AS type
            FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE c.relkind IN ('r','p','v','m','f')
              AND n.nspname NOT IN ('information_schema', 'pg_catalog', 'pg_toast')
              AND pg_table_is_visible(c.oid)
            ORDER BY c.relname
        """)
        
        from django.db.backends.base.introspection import TableInfo
        return [TableInfo(row[0], row[1]) for row in cursor.fetchall()]

    def get_constraints(self, cursor, table_name):
        """
        获取表约束信息，兼容 OpenGauss SQL 语法
        """
        cursor.execute("""
            SELECT
                c.conname,
                (
                    SELECT array_agg(att.attname ORDER BY s.idx)
                    FROM generate_subscripts(c.conkey, 1) AS s(idx)
                    JOIN pg_attribute att
                      ON att.attrelid = c.conrelid
                     AND att.attnum = c.conkey[s.idx]
                ) AS columns,
                c.contype,
                ft.relname AS foreign_table,
                (
                    SELECT array_agg(att.attname ORDER BY s.idx)
                    FROM generate_subscripts(c.confkey, 1) AS s(idx)
                    JOIN pg_attribute att
                      ON att.attrelid = c.confrelid
                     AND att.attnum = c.confkey[s.idx]
                ) AS foreign_columns,
                pg_get_constraintdef(c.oid, TRUE) AS definition,
                c.condeferrable,
                c.condeferred
            FROM pg_constraint c
            JOIN pg_class t ON c.conrelid = t.oid
            JOIN pg_namespace n ON n.oid = t.relnamespace
            LEFT JOIN pg_class ft ON ft.oid = c.confrelid
            WHERE t.relname = %s
              AND n.nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast')
              AND pg_table_is_visible(t.oid)
        """, [table_name])

        constraints = {}
        for constraint, columns, kind, f_table, f_columns, definition, deferrable, deferred in cursor.fetchall():
            constraints[constraint] = {
                'columns': list(columns) if columns else [],
                'primary_key': kind == 'p',
                'unique': kind in ['p', 'u'],
                'foreign_key': (f_table, list(f_columns)) if kind == 'f' and f_table else None,
                'check': kind == 'c',
                'index': False,
                'definition': definition,
                'options': {},
            }

        # 获取索引信息
        cursor.execute("""
            SELECT
                i.relname AS index_name,
                (
                    SELECT array_agg(
                        CASE WHEN att.attnum IS NULL THEN pg_get_indexdef(ix.indexrelid, k.idx, TRUE)
                             ELSE att.attname
                        END
                        ORDER BY k.idx
                    )
                    FROM generate_subscripts(ix.indkey, 1) AS k(idx)
                    LEFT JOIN pg_attribute att
                      ON att.attrelid = t.oid
                     AND att.attnum = ix.indkey[k.idx]
                ) AS columns,
                ix.indisunique,
                ix.indisprimary,
                pg_get_indexdef(ix.indexrelid) AS definition
            FROM pg_index ix
            JOIN pg_class i ON i.oid = ix.indexrelid
            JOIN pg_class t ON t.oid = ix.indrelid
            JOIN pg_namespace n ON n.oid = t.relnamespace
            WHERE t.relname = %s
              AND n.nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast')
              AND pg_table_is_visible(t.oid)
        """, [table_name])

        for index, columns, unique, primary, definition in cursor.fetchall():
            if index not in constraints:
                constraints[index] = {
                    'columns': list(columns) if columns else [],
                    'primary_key': primary,
                    'unique': unique,
                    'foreign_key': None,
                    'check': False,
                    'index': True,
                    'definition': definition,
                    'options': {},
                }

        return constraints
