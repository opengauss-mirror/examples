"""Shared test database configuration for django-opengauss test suite.

The module centralises connection settings so functional, performance,
and coverage helpers all read from the same source.  Environment variables
are applied lazily so local overrides can still be provided through the
OS or CI pipeline without editing the repository.
"""

from __future__ import annotations

import os
from dataclasses import dataclass
from typing import Dict, Mapping, MutableMapping

__all__ = [
    "DEFAULT_CONN",
    "TestConnection",
    "apply_test_environment",
    "django_database_settings",
    "psycopg2_connection_kwargs",
]


@dataclass(frozen=True)
class TestConnection:
    name: str = "django_opengauss_test"
    user: str = "gaussdb"
    password: str = "Gauss@2025"
    host: str = "127.0.0.1"
    port: str = "5432"

    def as_env(self) -> Dict[str, str]:
        return {
            "OG_DB_NAME": self.name,
            "OG_DB_USER": self.user,
            "OG_DB_PASSWORD": self.password,
            "OG_DB_HOST": self.host,
            "OG_DB_PORT": self.port,
        }

    def as_psycopg2(self) -> Dict[str, object]:
        return {
            "dbname": self.name,
            "user": self.user,
            "password": self.password,
            "host": self.host,
            "port": int(self.port),
        }


DEFAULT_CONN = TestConnection()


def apply_test_environment(overrides: Mapping[str, str] | None = None) -> Dict[str, str]:
    """Populate process environment variables for the driver test-suite.

    Values present in ``overrides`` take priority over :data:`DEFAULT_CONN`.
    Existing environment variables already exported by the user are respected.
    The resolved mapping is returned so callers can inspect the final values.
    """

    overrides = dict(overrides or {})
    resolved: MutableMapping[str, str] = {}

    for key, value in DEFAULT_CONN.as_env().items():
        resolved[key] = os.environ.get(key, overrides.get(key, value))
        os.environ[key] = resolved[key]

    optional_defaults = {
        "OG_APP_NAME": "OG-Driver-Test",
        "OG_TARGET_SESSION_ATTRS": "read-write",
        "OG_KEEPALIVES": "1",
        "OG_KEEPALIVES_IDLE": "30",
        "OG_DB_TIMEZONE": "UTC",
    }

    for key, value in optional_defaults.items():
        resolved[key] = os.environ.get(key, overrides.get(key, value))
        os.environ[key] = resolved[key]

    return dict(resolved)


def django_database_settings(alias: str = "default") -> Dict[str, object]:
    """Return a Django DATABASES entry for the configured OpenGauss backend."""

    env = apply_test_environment()
    options = {
        "application_name": env.get("OG_APP_NAME", "OG-Driver-Test"),
        "cursor_itersize": 1024,
    }

    if env.get("OG_TARGET_SESSION_ATTRS"):
        options["target_session_attrs"] = env["OG_TARGET_SESSION_ATTRS"]
    if env.get("OG_KEEPALIVES"):
        options["keepalives"] = int(env["OG_KEEPALIVES"])
    if env.get("OG_KEEPALIVES_IDLE"):
        options["keepalives_idle"] = int(env["OG_KEEPALIVES_IDLE"])

    return {
        alias: {
            "ENGINE": "django_opengauss",
            "NAME": env["OG_DB_NAME"],
            "USER": env["OG_DB_USER"],
            "PASSWORD": env["OG_DB_PASSWORD"],
            "HOST": env["OG_DB_HOST"],
            "PORT": env["OG_DB_PORT"],
            "OPTIONS": options,
        }
    }


def psycopg2_connection_kwargs() -> Dict[str, object]:
    """Return keyword arguments for establishing a psycopg2 connection."""

    apply_test_environment()
    return DEFAULT_CONN.as_psycopg2()
