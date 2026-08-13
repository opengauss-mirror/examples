"""
Bootstrap PyMySQL as MySQLdb so Django's mysql backend can talk to
openGauss dolphin MySQL protocol port.

SELECT VERSION() is a kernel builtin and still returns the openGauss banner
(cannot CREATE OR REPLACE). Django's mysql backend uses re.match() from the
start of that string, so it cannot see the 7.0.0 inside. Dolphin's MySQL
handshake already advertises 8.0.28-dophin-server; parse that same tuple
when the banner is openGauss.
"""
import pymysql

pymysql.install_as_MySQLdb()

# Handshake version used by dolphin (plugin_protocol/dqformat.cpp).
_OPENGAUSS_MYSQL_VERSION = (8, 0, 28)


def _install_opengauss_version_compat():
    try:
        from django.db.backends.mysql.base import DatabaseWrapper
        from django.utils.functional import cached_property
    except Exception:
        return

    orig = DatabaseWrapper.mysql_version.func

    def mysql_version(self):
        try:
            return orig(self)
        except Exception:
            info = getattr(self, "mysql_server_info", "") or ""
            if "opengauss" in info.lower():
                return _OPENGAUSS_MYSQL_VERSION
            raise

    DatabaseWrapper.mysql_version = cached_property(mysql_version)
    DatabaseWrapper.mysql_version.__set_name__(DatabaseWrapper, "mysql_version")


_install_opengauss_version_compat()
