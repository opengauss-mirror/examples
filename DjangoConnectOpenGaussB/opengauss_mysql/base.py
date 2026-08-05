"""
Django database backend for openGauss B-mode via dolphin MySQL protocol.

Subclass of django.db.backends.mysql with probes softened for openGauss:
- avoid @@default_storage_engine / unsupported session vars
- fake a MySQL 8.0.28 version tuple so Django feature flags work
"""

from django.db.backends.mysql.base import DatabaseWrapper as MySQLDatabaseWrapper
from django.utils.asyncio import async_unsafe
from django.utils.functional import cached_property


class DatabaseWrapper(MySQLDatabaseWrapper):
    vendor = "mysql"
    display_name = "openGauss (MySQL protocol)"

    @cached_property
    def mysql_server_data(self):
        version = "8.0.28-openGauss"
        sql_mode = "STRICT_TRANS_TABLES"
        lower_case = True
        try:
            with self.temporary_connection() as cursor:
                cursor.execute("SELECT VERSION()")
                row = cursor.fetchone()
                if row and row[0]:
                    # Keep Django's parser happy by prefixing a MySQL-like version.
                    version = f"8.0.28-openGauss ({row[0]})"
                try:
                    cursor.execute("SELECT @@sql_mode")
                    mode = cursor.fetchone()
                    if mode and mode[0] is not None:
                        sql_mode = str(mode[0])
                except Exception:
                    pass
                try:
                    cursor.execute("SELECT @@lower_case_table_names")
                    lc = cursor.fetchone()
                    if lc and lc[0] is not None:
                        lower_case = bool(int(lc[0]))
                except Exception:
                    pass
        except Exception:
            pass
        return {
            "version": version,
            "sql_mode": sql_mode,
            "default_storage_engine": "InnoDB",
            "sql_auto_is_null": False,
            "lower_case_table_names": lower_case,
            "has_zoneinfo_database": False,
        }

    @cached_property
    def mysql_version(self):
        return (8, 0, 28)

    @cached_property
    def mysql_is_mariadb(self):
        return False

    def init_connection_state(self):
        # Skip MySQL-only session assignments that openGauss rejects.
        # Still honor isolation_level if configured.
        assignments = []
        if self.isolation_level:
            assignments.append(
                "SET SESSION TRANSACTION ISOLATION LEVEL %s"
                % self.isolation_level.upper()
            )
        if assignments:
            with self.cursor() as cursor:
                cursor.execute("; ".join(assignments))

    @async_unsafe
    def get_connection_params(self):
        return super().get_connection_params()
