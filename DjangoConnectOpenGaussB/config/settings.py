from pathlib import Path
import os
import sys

BASE_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(BASE_DIR))

import pymysql_bootstrap  # noqa: F401

SECRET_KEY = os.environ.get("DJANGO_SECRET_KEY", "django-opengauss-b-demo-not-for-production")
DEBUG = os.environ.get("DJANGO_DEBUG", "1") == "1"
ALLOWED_HOSTS = os.environ.get("DJANGO_ALLOWED_HOSTS", "127.0.0.1,localhost").split(",")

INSTALLED_APPS = [
    "django.contrib.contenttypes",
    "django.contrib.auth",
    "demo_app",
]

MIDDLEWARE = []

ROOT_URLCONF = "config.urls"
WSGI_APPLICATION = "config.wsgi.application"

# Prefer native Django MySQL backend after Plugin adds @@default_storage_engine /
# @@sql_auto_is_null stubs. Override with OG_MYSQL_ENGINE=opengauss_mysql for
# older servers without the Plugin patch.
_ENGINE = os.environ.get("OG_MYSQL_ENGINE", "django.db.backends.mysql")

DATABASES = {
    "default": {
        "ENGINE": _ENGINE,
        "NAME": os.environ.get("OG_MYSQL_DB", "mysql_test_db"),
        "USER": os.environ.get("OG_MYSQL_USER", "omm"),
        "PASSWORD": os.environ["OG_MYSQL_PASSWORD"],
        "HOST": os.environ.get("OG_MYSQL_HOST", "127.0.0.1"),
        "PORT": os.environ.get("OG_MYSQL_PORT", "3308"),
        "OPTIONS": {
            "charset": "utf8mb4",
        },
    }
}

LANGUAGE_CODE = "zh-hans"
TIME_ZONE = "Asia/Shanghai"
USE_I18N = True
USE_TZ = True

DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"

SILENCED_SYSTEM_CHECKS = ["mysql.W002"]
