"""Centralized configuration for OpenGauss test scripts

All test scripts should import and use get_config() to obtain connection
parameters from a local .env file (no environment variable overrides, no in-code defaults).
"""

from typing import Dict, Any
import os


def _load_local_env() -> Dict[str, str]:
    """Load key-value pairs from a local .env file

    Simple parser: lines like KEY=VALUE, ignores blank lines and lines
    starting with '#'. Quotes around values are stripped.
    """
    env: Dict[str, str] = {}
    base_dir = os.path.dirname(__file__)
    env_path = os.path.join(base_dir, ".env")
    if not os.path.exists(env_path):
        raise FileNotFoundError(f".env file not found: {env_path}")

    try:
        with open(env_path, "r", encoding="utf-8") as f:
            for raw in f.readlines():
                line = raw.strip()
                if not line or line.startswith("#"):
                    continue
                if "=" not in line:
                    continue
                key, value = line.split("=", 1)
                key = key.strip()
                value = value.strip().strip('"').strip("'")
                if key:
                    env[key] = value
    except Exception as e:
        raise RuntimeError(f"Failed to read .env file: {e}")
    return env


def get_config() -> Dict[str, Any]:
    """Return OpenGauss connection parameters for test scripts

    Environment variables can override defaults to simplify local testing.
    """
    local = _load_local_env()

    def _require(name: str) -> str:
        if name not in local:
            raise KeyError(f"Missing {name} in .env file")
        return local[name]

    return {
        "host": _require("OG_HOST"),
        "port": int(_require("OG_PORT")),
        "database": _require("OG_DB"),
        "user": _require("OG_USER"),
        "password": _require("OG_PASSWORD"),
        "table_name": _require("OG_TABLE"),
        "schema_name": _require("OG_SCHEMA"),
        "embed_dim": int(_require("OG_DIM")),
    }
