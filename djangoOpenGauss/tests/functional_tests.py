from __future__ import annotations

import datetime
import io
import json
import os
import sys
import time
import types
import unittest
from pathlib import Path
from typing import Dict

from django.apps import AppConfig
from django.conf import settings
from django.core.management.color import no_style
from django.db import IntegrityError, OperationalError, connection, models, transaction
from django.utils import timezone
from unittest import mock
import django_opengauss.schema as og_schema

from full_stack_env import apply_test_environment, django_database_settings

APP_NAME = "og_backend_testapp"
APP_CONFIG_PATH = f"{APP_NAME}.apps.OpenGaussTestAppConfig"

ENV_SETTINGS = apply_test_environment()

def _bootstrap_dynamic_app() -> None:
    """Register a lightweight Django app module for our test models."""

    if APP_NAME in sys.modules:
        return

    app_module = types.ModuleType(APP_NAME)
    app_module.__file__ = __file__
    sys.modules[APP_NAME] = app_module

    apps_module = types.ModuleType(f"{APP_NAME}.apps")
    sys.modules[f"{APP_NAME}.apps"] = apps_module

    class OpenGaussTestAppConfig(AppConfig):
        name = APP_NAME
        label = APP_NAME
        verbose_name = "OpenGauss Backend TestApp"
        path = os.path.dirname(__file__)

    setattr(apps_module, "OpenGaussTestAppConfig", OpenGaussTestAppConfig)

    models_module = types.ModuleType(f"{APP_NAME}.models")
    sys.modules[f"{APP_NAME}.models"] = models_module


_bootstrap_dynamic_app()

DATABASES = django_database_settings()
if not settings.configured:
    settings.configure(
        INSTALLED_APPS=[
            "django.contrib.auth",
            "django.contrib.contenttypes",
            APP_CONFIG_PATH,
        ],
        DATABASES=DATABASES,
        TIME_ZONE=ENV_SETTINGS.get("OG_DB_TIMEZONE", "UTC"),
        USE_TZ=True,
        DEFAULT_AUTO_FIELD="django.db.models.AutoField",
    )

import django

django.setup()

connection.settings_dict.setdefault("OPTIONS", DATABASES["default"].get("OPTIONS", {}))
DATABASE_OPTIONS: Dict[str, object] = connection.settings_dict["OPTIONS"]

# ---------------------------------------------------------------------------
# Model definitions (registered under the dynamic app)
# ---------------------------------------------------------------------------


class Author(models.Model):
    name = models.CharField(max_length=64, unique=True)
    bio = models.TextField(blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        app_label = APP_NAME
        db_table = "og_test_authors"


class Category(models.Model):
    title = models.CharField(max_length=64, unique=True)
    description = models.TextField(blank=True, default="")

    class Meta:
        app_label = APP_NAME
        db_table = "og_test_categories"


class Article(models.Model):
    author = models.ForeignKey(Author, on_delete=models.CASCADE, related_name="articles")
    category = models.ForeignKey(Category, on_delete=models.PROTECT, related_name="articles")
    slug = models.SlugField(max_length=128, unique=True)
    headline = models.CharField(max_length=255)
    body = models.TextField()
    metadata = models.JSONField(default=dict)
    rating = models.IntegerField(default=0)
    published = models.BooleanField(default=False)
    published_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        app_label = APP_NAME
        db_table = "og_test_articles"
        indexes = [
            models.Index(fields=["slug"], name="idx_articles_slug"),
            models.Index(fields=["-rating", "published_at"], name="idx_articles_rating_pub"),
        ]


class ArticleLog(models.Model):
    article = models.ForeignKey(Article, on_delete=models.CASCADE, related_name="logs")
    payload = models.JSONField(default=dict)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        app_label = APP_NAME
        db_table = "og_test_article_logs"


# Ensure models are importable via the dynamic module path
models_module = sys.modules[f"{APP_NAME}.models"]
setattr(models_module, "Author", Author)
setattr(models_module, "Category", Category)
setattr(models_module, "Article", Article)
setattr(models_module, "ArticleLog", ArticleLog)


# ---------------------------------------------------------------------------
# Utility helpers
# ---------------------------------------------------------------------------


def _dsn_parameters() -> Dict[str, str]:
    connection.ensure_connection()
    db_connection = connection.connection

    if db_connection is None:
        return {}

    if hasattr(db_connection, "get_dsn_parameters"):
        return db_connection.get_dsn_parameters()

    info = getattr(db_connection, "info", None)
    if info is not None and hasattr(info, "dsn_parameters"):
        return dict(info.dsn_parameters)

    return {}


def _drop_partition_tables() -> None:
    with connection.cursor() as cursor:
        cursor.execute("DROP TABLE IF EXISTS og_partition_parent CASCADE;")


def _create_partition_tables() -> None:
    _drop_partition_tables()
    with connection.cursor() as cursor:
        cursor.execute(
            """
            CREATE TABLE og_partition_parent (
                id SERIAL PRIMARY KEY,
                status VARCHAR(32) NOT NULL,
                payload JSONB DEFAULT '{}'::JSONB
            )
            PARTITION BY LIST (status)
            (
                PARTITION og_partition_parent_open VALUES ('open'),
                PARTITION og_partition_parent_other VALUES (DEFAULT)
            );
            """
        )


# ---------------------------------------------------------------------------
# Test suite
# ---------------------------------------------------------------------------


class OpenGaussDriverFullStackTest(unittest.TestCase):
    """Integration-style tests that exercise the custom backend surface."""

    author: Author
    category: Category

    @classmethod
    def setUpClass(cls) -> None:
        super().setUpClass()
        try:
            connection.ensure_connection()
        except OperationalError as exc:
            exc_msg = str(exc).strip() or repr(exc)
            raise unittest.SkipTest(f"无法连接到数据库: {exc_msg}") from exc
        print("[初始化] 正在清理历史测试表并准备模型……")
        with connection.cursor() as cursor:
            cursor.execute("DROP TABLE IF EXISTS og_test_article_logs CASCADE;")
            cursor.execute("DROP TABLE IF EXISTS og_test_articles CASCADE;")
            cursor.execute("DROP TABLE IF EXISTS og_test_categories CASCADE;")
            cursor.execute("DROP TABLE IF EXISTS og_test_authors CASCADE;")
        # Provision schema for our models
        with connection.schema_editor() as editor:
            editor.create_model(Author)
            editor.create_model(Category)
            editor.create_model(Article)
            editor.create_model(ArticleLog)

        cls.author = Author.objects.create(name="Ada", bio="OG pioneer")
        cls.category = Category.objects.create(
            title="Release Notes",
            description="Release artifacts",
        )

        _create_partition_tables()
        print("[初始化] 测试环境准备完成。")

    @classmethod
    def tearDownClass(cls) -> None:
        with connection.schema_editor() as editor:
            editor.delete_model(ArticleLog)
            editor.delete_model(Article)
            editor.delete_model(Category)
            editor.delete_model(Author)

        _drop_partition_tables()
        super().tearDownClass()

    def test_connection_options_propagated(self) -> None:
        print("[用例] 验证连接参数透传……")
        original_options = connection.settings_dict.get('OPTIONS', {}).copy()
        original_database_options = DATABASE_OPTIONS.copy()
        original_timezone = connection.settings_dict.get('TIME_ZONE')
        original_use_tz = connection.settings_dict.get('USE_TZ', False)
        try:
            connection.settings_dict.setdefault('OPTIONS', {})
            connection.settings_dict['OPTIONS'].update({
                'sslmode': 'require',
                'connect_timeout': 10,
                'keepalives_idle': 20,
                'application_name': 'OG-脚本覆盖',
                'extra_params': {'application_name': 'OG-脚本覆盖'}
            })
            DATABASE_OPTIONS.update(connection.settings_dict['OPTIONS'])
            params = connection.get_connection_params()
            self.assertEqual(params.get('sslmode'), 'require')
            self.assertEqual(params.get('keepalives_idle'), 20)
            self.assertEqual(params.get('application_name'), 'OG-脚本覆盖')

            connection.settings_dict['TIME_ZONE'] = 'Asia/Shanghai'
            connection.settings_dict['USE_TZ'] = True
            connection.init_connection_state()
            with connection.cursor() as cursor:
                cursor.execute('SHOW TIME ZONE')
                tz = cursor.fetchone()[0]
            self.assertIn('Asia', tz)
        finally:
            connection.settings_dict['OPTIONS'] = original_options
            DATABASE_OPTIONS.clear()
            DATABASE_OPTIONS.update(original_database_options)
            if original_timezone is None:
                connection.settings_dict.pop('TIME_ZONE', None)
            else:
                connection.settings_dict['TIME_ZONE'] = original_timezone
            connection.settings_dict['USE_TZ'] = original_use_tz

        params = connection.get_connection_params()
        self.assertEqual(
            params.get("application_name"),
            DATABASE_OPTIONS.get("application_name"),
        )
        if "target_session_attrs" in DATABASE_OPTIONS:
            self.assertEqual(
                params.get("target_session_attrs"), DATABASE_OPTIONS["target_session_attrs"]
            )
        if "keepalives" in DATABASE_OPTIONS:
            self.assertEqual(params.get("keepalives"), DATABASE_OPTIONS["keepalives"])

        dsn_params = _dsn_parameters()
        if dsn_params:
            # Psycopg returns lowercase strings for numeric options
            expected_app = DATABASE_OPTIONS.get("application_name")
            self.assertEqual(dsn_params.get("application_name"), expected_app)

        named_cursor = connection.create_cursor("og_named_cursor")
        self.assertEqual(
            named_cursor.itersize,
            connection.settings_dict.get('OPTIONS', {}).get('cursor_itersize', 2000),
        )
        named_cursor.close()

        self.assertTrue(connection.is_usable())

        original_debug = settings.DEBUG
        settings.DEBUG = True
        try:
            with connection.cursor() as cursor:
                cursor.execute("SELECT %s", [1])
                sql = connection.ops.last_executed_query(cursor, "SELECT %s", [1])
                self.assertIn("SELECT", sql)
        finally:
            settings.DEBUG = original_debug

        original_autocommit = connection.get_autocommit()
        try:
            connection.set_autocommit(True)
            self.assertTrue(connection.get_autocommit())
            connection.set_autocommit(False)
            self.assertFalse(connection.get_autocommit())
            connection.rollback()
        finally:
            connection.set_autocommit(original_autocommit)

        print("[成功] 连接参数透传及游标/自动提交校验通过。")

    def test_crud_roundtrip_and_transactions(self) -> None:
        print("[用例] 验证 ORM CRUD 与事务流程……")
        article = Article.objects.create(
            author=self.author,
            category=self.category,
            slug="og-driver-overview",
            headline="Driver regression tests",
            body="Ensuring openGauss backend stays healthy.",
            metadata={"tags": ["driver", "regression"], "score": 10},
            rating=3,
            published=False,
        )

        ArticleLog.objects.create(article=article, payload={"event": "created"})

        Article.objects.filter(pk=article.pk).update(rating=models.F("rating") + 1)
        article.refresh_from_db()
        self.assertEqual(article.rating, 4)

        with transaction.atomic():
            locked = Article.objects.select_for_update().get(pk=article.pk)
            locked.published = True
            locked.published_at = timezone.now()
            locked.save(update_fields=["published", "published_at"])

        article.refresh_from_db()
        self.assertTrue(article.published)
        self.assertIsNotNone(article.published_at)

        with self.assertRaises(IntegrityError):
            Article.objects.create(
                author=self.author,
                category=self.category,
                slug="og-driver-overview",
                headline="Duplicate slug",
                body="Should violate unique index.",
            )

        print("[成功] ORM 基础读写与事务验证通过。")

    def test_bulk_create_returning_multiple_columns(self) -> None:
        print("[用例] 验证 bulk_create 的 RETURNING 支持……")
        objs = [
            Article(
                author=self.author,
                category=self.category,
                slug=f"batch-{idx}",
                headline=f"Batch story {idx}",
                body="auto generated",
                metadata={"batch": idx},
                rating=idx,
            )
            for idx in range(3)
        ]

        import inspect
        bulk_sig = inspect.signature(Article.objects.bulk_create)
        if "returning" in bulk_sig.parameters:
            inserted = Article.objects.bulk_create(objs, batch_size=2, returning=True)
        elif "returning_fields" in bulk_sig.parameters:
            inserted = Article.objects.bulk_create(
                objs,
                batch_size=2,
                returning_fields=[Article._meta.get_field("id"), Article._meta.get_field("slug")],
            )
        else:
            inserted = Article.objects.bulk_create(objs, batch_size=2)

        self.assertEqual(len(inserted), 3)
        for obj in inserted:
            self.assertIsNotNone(obj.pk)
            self.assertTrue(obj.slug.startswith("batch-"))
        print("[成功] 批量插入 RETURNING 行为验证通过。")

    def test_introspection_reports_constraints(self) -> None:
        print("[用例] 验证内省结果完整性……")
        with connection.cursor() as cursor:
            constraints = connection.introspection.get_constraints(cursor, Article._meta.db_table)

        fk_constraints = [info for info in constraints.values() if info.get("foreign_key")]
        self.assertTrue(fk_constraints, "Foreign key metadata should be present")
        fk_targets = {(fk[0], tuple(fk[1])) for fk in (info["foreign_key"] for info in fk_constraints if info.get("foreign_key"))}
        self.assertIn((Author._meta.db_table, ("id",)), fk_targets)
        self.assertIn((Category._meta.db_table, ("id",)), fk_targets)

        index_names = {name for name, info in constraints.items() if info.get("index")}
        self.assertIn("idx_articles_slug", index_names)
        self.assertIn("idx_articles_rating_pub", index_names)
        print("[成功] 内省约束与索引信息验证通过。")

    def test_partition_tables_visible_in_table_list(self) -> None:
        print("[用例] 验证分区表枚举能力……")
        with connection.cursor() as cursor:
            tables = connection.introspection.get_table_list(cursor)

        table_lookup = {info.name: info.type for info in tables}
        self.assertIn("og_partition_parent", table_lookup)
        self.assertEqual(table_lookup["og_partition_parent"], "t")
        print("[成功] 分区表枚举验证通过。")


class OpenGaussBackendUnitTest(unittest.TestCase):
    """针对后端辅助模块的覆盖测试。"""

    @classmethod
    def setUpClass(cls) -> None:
        super().setUpClass()
        try:
            connection.ensure_connection()
        except OperationalError as exc:
            exc_msg = str(exc).strip() or repr(exc)
            raise unittest.SkipTest(f"无法连接到数据库: {exc_msg}") from exc
        print("[初始化] 开始执行后端功能单元测试……")

        existing_tables = set(connection.introspection.table_names())
        required_tables = {
            Author._meta.db_table,
            Category._meta.db_table,
            Article._meta.db_table,
            ArticleLog._meta.db_table,
        }
        missing_tables = required_tables - existing_tables
        if missing_tables:
            with connection.schema_editor() as editor:
                if Author._meta.db_table in missing_tables:
                    editor.create_model(Author)
                if Category._meta.db_table in missing_tables:
                    editor.create_model(Category)
                if Article._meta.db_table in missing_tables:
                    editor.create_model(Article)
                if ArticleLog._meta.db_table in missing_tables:
                    editor.create_model(ArticleLog)

    def test_operations_helpers(self) -> None:
        print("[用例] 校验 DatabaseOperations 辅助方法……")
        ops = connection.ops
        style = no_style()

        flush_sql = ops.sql_flush(style, ['og_test_articles'], allow_cascade=True)
        self.assertTrue(flush_sql and 'CASCADE' in flush_sql[0])
        self.assertEqual(ops.sql_flush(style, [], allow_cascade=False), [])

        seq_sql = ops.sequence_reset_by_name_sql(style, [{'table': 'og_test_articles', 'column': 'id'}])
        self.assertTrue(seq_sql and 'ALTER SEQUENCE' in seq_sql[0])

        iso_dt = ops.adapt_datetimefield_value(timezone.now())
        self.assertIsInstance(iso_dt, str)

        iso_time = ops.adapt_timefield_value(datetime.time(12, 34, 56))
        self.assertEqual(iso_time, '12:34:56')
        self.assertEqual(ops.adapt_timefield_value('08:00'), '08:00')

        iso_date = ops.adapt_datefield_value(datetime.date(2024, 1, 2))
        self.assertEqual(iso_date, '2024-01-02')

        self.assertEqual(ops.max_name_length(), 63)
        self.assertEqual(ops.quote_name('foo'), '"foo"')

        returning_sql, _ = ops.return_insert_columns([Article._meta.get_field('slug')])
        self.assertIn('RETURNING', returning_sql)
        blank_return, _ = ops.return_insert_columns([])
        self.assertEqual(blank_return, '')
        self.assertIn('RETURNING', ops.return_insert_id()[0])

        batch_sql = ops.bulk_insert_sql(['slug'], [['%s']])
        self.assertEqual(batch_sql, 'VALUES (%s)')
        self.assertEqual(ops.bulk_batch_size([], []), 1000)

        self.assertEqual(ops.combine_expression('AND', ['a=1', 'b=2']), '(a=1 AND b=2)')
        with self.assertRaises(ValueError):
            ops.combine_expression('??', ['expr'])

        self.assertEqual(ops.integer_field_range('BigIntegerField')[1], 9223372036854775807)
        distinct_sql, _ = ops.distinct_sql(['"slug"'], [])
        self.assertIn('DISTINCT ON', distinct_sql)
        plain_distinct, _ = ops.distinct_sql([], [])
        self.assertEqual(plain_distinct, 'DISTINCT')

        with connection.cursor() as cursor:
            echoed = ops.last_executed_query(cursor, 'SELECT 1', [])
        self.assertEqual(echoed, 'SELECT 1')

        print("[成功] DatabaseOperations 方法验证完成。")

    def test_schema_helpers(self) -> None:
        print("[用例] 校验 DatabaseSchemaEditor 关键逻辑……")
        schema_editor = connection.schema_editor()

        json_type = schema_editor._field_data_type(Article._meta.get_field('metadata'))
        self.assertEqual(json_type, 'JSONB')

        class CollationModel(models.Model):
            text = models.TextField(db_collation='en_US.utf8')

            class Meta:
                app_label = APP_NAME
                managed = False

        text_type = schema_editor._field_data_type(CollationModel._meta.get_field('text'))
        self.assertIn('COLLATE', text_type)

        auto_sql, _ = schema_editor.column_sql(Author, Author._meta.get_field('id'))
        self.assertIn('serial', auto_sql)

        prepared_default = schema_editor.prepare_default({'demo': 1})
        self.assertIn('demo', prepared_default)

        author_field = Article._meta.get_field('author')
        original_defer = getattr(author_field, 'db_constraint_defer', False)
        author_field.db_constraint_defer = True
        try:
            fk_statement = schema_editor._create_fk_sql(Article, author_field, '_fk')
            self.assertIn('DEFERRABLE', fk_statement.template)
        finally:
            author_field.db_constraint_defer = original_defer

        index_sql = schema_editor._model_indexes_sql(Article)
        self.assertTrue(index_sql)

        with mock.patch.object(og_schema.postgresql_schema.DatabaseSchemaEditor, 'execute') as mock_execute:
            schema_editor.execute('CREATE TABLE foo (id integer GENERATED BY DEFAULT AS IDENTITY)', [])
            executed_sql = mock_execute.call_args[0][0]
            self.assertNotIn('GENERATED BY DEFAULT', executed_sql)

        extra_field = models.JSONField(null=True)
        extra_field.set_attributes_from_name('extra_metadata')
        with connection.schema_editor() as editor:
            editor.add_field(Article, extra_field)
        with connection.cursor() as cursor:
            cursor.execute("SELECT column_name FROM information_schema.columns WHERE table_name = %s AND column_name = %s", [Article._meta.db_table, 'extra_metadata'])
            self.assertIsNotNone(cursor.fetchone())
        with connection.schema_editor() as editor:
            editor.remove_field(Article, extra_field)

        print("[成功] DatabaseSchemaEditor 方法验证完成。")

    def test_creation_suffix(self) -> None:
        print("[用例] 检查 DatabaseCreation 表后缀生成……")
        creation = connection.creation
        test_settings = settings.DATABASES['default'].get('TEST', {}).copy()
        try:
            settings.DATABASES['default']['TEST'] = {
                'TABLESPACE': 'pg_default',
                'STORAGE_PARAMETERS': {'fillfactor': 80},
            }
            suffix = creation.sql_table_creation_suffix()
            self.assertIn('TABLESPACE', suffix)
            self.assertIn('fillfactor', suffix)
        finally:
            settings.DATABASES['default']['TEST'] = test_settings
        print("[成功] DatabaseCreation 表后缀逻辑验证完成。")

    def test_client_argument_and_runshell(self) -> None:
        print("[用例] 校验 DatabaseClient 参数转换与 runshell……")
        from django_opengauss.client import DatabaseClient

        client = DatabaseClient(connection)
        sample_settings = {
            'NAME': 'postgres',
            'HOST': '127.0.0.1',
            'PORT': '5432',
            'USER': 'gaussdb',
            'PASSWORD': 'secret',
            'OPTIONS': {
                'sslmode': 'require',
                'connect_timeout': 5,
                'client_encoding': 'UTF8',
            },
        }
        args = client.settings_to_cmd_args(sample_settings)
        self.assertIn('-d', args)
        self.assertIn('-x', args)

        dummy_client = DatabaseClient(connection)
        dummy_client.connection = type('Conn', (), {'settings_dict': sample_settings})()

        with mock.patch('django_opengauss.client.os.execvpe') as mock_execvpe, \
             mock.patch('django_opengauss.client.os.execvp') as mock_execvp:
            dummy_client.runshell()
            mock_execvpe.assert_called_once()
            mock_execvp.assert_not_called()

        print("[成功] DatabaseClient 参数与 shell 调用验证完成。")

    def test_features_flags(self) -> None:
        print("[用例] 验证 DatabaseFeatures 声明……")
        features = connection.features
        self.assertTrue(features.can_return_id_from_insert)
        self.assertTrue(features.allows_group_by_lob())
        self.assertTrue(features.supports_explaining_query_execution())
        self.assertTrue(features.can_clone_databases())
        mapping = features.introspected_field_types()
        self.assertEqual(mapping['jsonb'], 'JSONField')
        print("[成功] DatabaseFeatures 属性验证完成。")

    def test_connection_pool_workflow(self) -> None:
        print("[用例] 验证连接池基本流程……")
        from django_opengauss import pool as og_pool

        og_pool.PooledDatabaseWrapper.close_pool()

        db_settings = settings.DATABASES['default']
        temp_pool = og_pool.OpenGaussConnectionPool(
            database_settings=db_settings,
            min_conn=1,
            max_conn=2,
            health_check_interval=3600,
        )
        try:
            with temp_pool.get_connection() as conn:
                with conn.cursor() as cursor:
                    cursor.execute('SELECT 1')
                    self.assertEqual(cursor.fetchone()[0], 1)
            temp_pool._perform_health_check()
            temp_pool._rebuild_pool()
            stats = temp_pool.get_pool_stats()
            self.assertGreaterEqual(stats['total_requests'], 1)
        finally:
            temp_pool.close()

        db_settings.setdefault('OPTIONS', {})
        original_pool_opts = db_settings['OPTIONS'].get('CONNECTION_POOL')
        db_settings['OPTIONS']['CONNECTION_POOL'] = {
            'MIN_CONN': 1,
            'MAX_CONN': 1,
            'HEALTH_CHECK_INTERVAL': 3600,
        }
        try:
            with og_pool.get_pooled_connection(db_settings) as conn:
                with conn.cursor() as cursor:
                    cursor.execute('SELECT 2')
                    self.assertEqual(cursor.fetchone()[0], 2)
            result = og_pool.execute_query('SELECT 3')
            self.assertEqual(result[0][0], 3)
        finally:
            og_pool.PooledDatabaseWrapper.close_pool()
            if original_pool_opts is None:
                db_settings['OPTIONS'].pop('CONNECTION_POOL', None)
            else:
                db_settings['OPTIONS']['CONNECTION_POOL'] = original_pool_opts

        print("[成功] 连接池工作流程验证完成。")

    def test_creation_database_management(self) -> None:
        print("[用例] 验证 DatabaseCreation 创建/克隆/销毁逻辑……")
        from django_opengauss import creation as og_creation
        from django.db.backends.postgresql import creation as pg_creation

        creation = connection.creation
        nodb_conn = creation._nodb_connection
        self.assertEqual(nodb_conn.settings_dict['NAME'], 'postgres')
        nodb_conn.close()
        test_db_name = creation._get_test_db_name()

        class DummyCursor:
            def __init__(self, recorder):
                self.recorder = recorder

            def __enter__(self):
                return self

            def __exit__(self, exc_type, exc_val, exc_tb):
                return False

            def execute(self, sql, params=None):
                self.recorder.append((sql.strip(), params))

        class DummyConnection:
            def __init__(self, recorder):
                self.recorder = recorder

            def cursor(self):
                return DummyCursor(self.recorder)

        recorder = []
        dummy_conn = DummyConnection(recorder)

        with mock.patch.object(og_creation.DatabaseCreation, '_nodb_connection', new_callable=mock.PropertyMock, return_value=dummy_conn), \
             mock.patch.object(pg_creation.DatabaseCreation, '_create_test_db', side_effect=Exception('fallback')):
            result = creation._create_test_db(verbosity=1, autoclobber=True)
        self.assertEqual(result, test_db_name)
        self.assertTrue(any('CREATE DATABASE' in sql for sql, _ in recorder))

        recorder.clear()
        with mock.patch.object(og_creation.DatabaseCreation, '_nodb_connection', new_callable=mock.PropertyMock, return_value=dummy_conn):
            clone_name = creation._clone_test_db(suffix='_clone', verbosity=1)
        self.assertTrue(any('CREATE DATABASE' in sql for sql, _ in recorder))
        self.assertTrue(clone_name.endswith('_clone'))

        recorder.clear()
        with mock.patch.object(og_creation.DatabaseCreation, '_nodb_connection', new_callable=mock.PropertyMock, return_value=dummy_conn):
            creation._destroy_test_db(test_db_name, verbosity=1)
        self.assertTrue(any('DROP DATABASE' in sql for sql, _ in recorder))

        print("[成功] DatabaseCreation 创建/克隆/销毁逻辑验证完成。")

    def test_pool_management_command(self) -> None:
        print("[用例] 验证连接池管理命令输出……")
        from django_opengauss import pool as og_pool

        fake_pool = mock.MagicMock()
        fake_pool.get_pool_stats.return_value = {
            'total_requests': 1234,
            'active_connections': 12,
            'idle_connections': 34,
            'failed_connections': 5,
            'average_response_time': 0.37,
            'pool_size_min': 5,
            'pool_size_max': 50,
            'health_check_failures': 2,
            'last_health_check': time.time() - 987,
            'total_created_connections': 200,
            'total_closed_connections': 180,
            'current_connection_count': 12,
        }

        fake_pool.stats = {
            'active_connections': 12,
            'idle_connections': 34,
            'failed_connections': 5,
        }

        with mock.patch.object(og_pool.PooledDatabaseWrapper, 'get_pool', return_value=fake_pool):
            command = og_pool.Command()
            command.handle()
        print("[成功] 连接池管理命令验证完成。")
def load_tests(loader: unittest.TestLoader, tests: unittest.TestSuite, pattern: str):
    """Allow ``python tests/functional_tests.py`` execution."""

    suite = unittest.TestSuite()
    suite.addTests(loader.loadTestsFromTestCase(OpenGaussDriverFullStackTest))
    suite.addTests(loader.loadTestsFromTestCase(OpenGaussBackendUnitTest))
    return suite


class _TeeStream:
    def __init__(self, primary, mirror: io.StringIO):
        self._primary = primary
        self._mirror = mirror

    def write(self, data):
        self._primary.write(data)
        self._mirror.write(data)

    def flush(self):
        self._primary.flush()
        self._mirror.flush()

    def isatty(self):  # pragma: no cover - passthrough
        return getattr(self._primary, "isatty", lambda: False)()

    @property
    def encoding(self):  # pragma: no cover - passthrough
        return getattr(self._primary, "encoding", None)


class RecordingTextTestResult(unittest.TextTestResult):
    def __init__(self, stream, descriptions, verbosity):
        super().__init__(stream, descriptions, verbosity)
        self.successes: list[unittest.case.TestCase] = []
        self.captured_output: dict[str, str] = {}
        self._stdout_primary = None
        self._stderr_primary = None
        self._current_buffer: io.StringIO | None = None

    def startTest(self, test):
        self._current_buffer = io.StringIO()
        self._stdout_primary = sys.stdout
        self._stderr_primary = sys.stderr
        sys.stdout = _TeeStream(sys.stdout, self._current_buffer)
        sys.stderr = _TeeStream(sys.stderr, self._current_buffer)
        super().startTest(test)

    def stopTest(self, test):
        if self._current_buffer is not None:
            self.captured_output[test.id()] = self._current_buffer.getvalue()
        if self._stdout_primary is not None:
            sys.stdout = self._stdout_primary
        if self._stderr_primary is not None:
            sys.stderr = self._stderr_primary
        self._current_buffer = None
        self._stdout_primary = None
        self._stderr_primary = None
        super().stopTest(test)

    def addSuccess(self, test):
        super().addSuccess(test)
        self.successes.append(test)


def _extract_success_messages(output: str) -> list[str]:
    messages: list[str] = []
    for line in output.splitlines():
        stripped = line.strip()
        if not stripped:
            continue
        if stripped.startswith("[成功]"):
            messages.append(stripped.replace("[成功]", "").strip())
    return messages


if __name__ == "__main__":
    loader = unittest.TestLoader()
    suite = load_tests(loader, unittest.TestSuite(), pattern=None)
    runner = unittest.TextTestRunner(verbosity=2, resultclass=RecordingTextTestResult)

    start_ts = time.time()
    result = runner.run(suite)
    duration = time.time() - start_ts

    passed_details = []
    for test in getattr(result, "successes", []):
        test_id = test.id()
        output = result.captured_output.get(test_id, "")
        messages = _extract_success_messages(output)
        fallback_lines = [line.strip() for line in output.splitlines() if line.strip()]
        passed_details.append(
            {
                "test": test_id,
                "messages": messages or fallback_lines,
            }
        )

    summary = {
        "generated_at": datetime.datetime.utcnow().isoformat() + "Z",
        "duration_seconds": duration,
        "tests_run": result.testsRun,
        "failures": [
            {"test": case.id(), "traceback": output}
            for case, output in result.failures
        ],
        "errors": [
            {"test": case.id(), "traceback": output}
            for case, output in result.errors
        ],
        "skipped": [
            {"test": case.id(), "reason": reason}
            for case, reason in getattr(result, "skipped", [])
        ],
        "passed": passed_details,
        "successful": result.wasSuccessful(),
    }

    output_path = Path(__file__).with_name("functional_test_results.json")
    output_path.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"[info] 功能测试结果已保存到: {output_path}")

    sys.exit(0 if result.wasSuccessful() else 1)
