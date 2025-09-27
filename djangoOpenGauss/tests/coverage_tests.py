#!/usr/bin/env python
"""Run coverage across the consolidated django-opengauss test-suite."""

from __future__ import annotations

import os
import socket
import subprocess
import sys
import time
import uuid
from pathlib import Path
from typing import Dict, Iterable, Optional, Tuple

from full_stack_env import apply_test_environment, psycopg2_connection_kwargs

PROJECT_ROOT = Path(__file__).parent.parent
ENV = apply_test_environment()
DEFAULT_CONN = psycopg2_connection_kwargs()


def ensure_dependencies() -> None:
    try:
        import coverage  # noqa: F401
    except ImportError:
        print("正在安装 coverage 依赖...")
        subprocess.run(
            [sys.executable, "-m", "pip", "install", "coverage", "pytest", "pytest-cov"],
            check=True,
        )


def build_test_targets() -> Iterable[Dict[str, object]]:
    tests_dir = PROJECT_ROOT / "tests"
    return [
        {
            "label": "功能测试",
            "script": tests_dir / "functional_tests.py",
            "args": [],
            "needs_db": False,
        },
        {
            "label": "性能测试 (quick 模式)",
            "script": tests_dir / "performance_tests.py",
            "args": ["quick"],
            "needs_db": True,
        },
    ]


def run_coverage_suite() -> None:
    print("=" * 70)
    print("Django-OpenGauss 统一测试覆盖率运行器")
    print("=" * 70)
    ensure_dependencies()

    subprocess.run([sys.executable, "-m", "coverage", "erase"], cwd=PROJECT_ROOT, check=True)

    for target in build_test_targets():
        script_path = target["script"]
        if not Path(script_path).exists():
            print(f"[warn] 跳过缺失脚本: {script_path}")
            continue

        label = target["label"]
        args = list(target["args"])
        needs_db = bool(target["needs_db"])

        print(f"\n运行测试: {label}")
        cmd = [
            sys.executable,
            "-m",
            "coverage",
            "run",
            "--source=django_opengauss",
            "--omit=*/tests/*.py",
            "-a",
            str(script_path),
            *args,
        ]

        extra_env: Optional[Dict[str, str]] = None
        container_name: Optional[str] = None
        if needs_db:
            try:
                extra_env, container_name = _ensure_temp_opengauss_for_performance()
            except Exception as exc:  # noqa: BLE001
                print(f"[warn] 无法自动准备 OpenGauss 环境: {exc}")

        run_env = os.environ.copy()
        if extra_env:
            run_env.update(extra_env)

        pythonpath_entries = [str(Path(__file__).parent)]
        if run_env.get('PYTHONPATH'):
            pythonpath_entries.append(run_env['PYTHONPATH'])
        run_env['PYTHONPATH'] = os.pathsep.join(pythonpath_entries)

        result = subprocess.run(
            cmd,
            cwd=PROJECT_ROOT,
            capture_output=True,
            text=True,
            env=run_env,
        )

        if result.returncode == 0:
            print(f"✓ {label} 完成")
        else:
            print(f"✗ {label} 失败 (exit={result.returncode})")
            if result.stderr:
                print(result.stderr.strip())

        if container_name:
            try:
                subprocess.run(["docker", "rm", "-f", container_name], capture_output=True)
            except Exception:
                pass

    print("\n生成覆盖率报告...")
    subprocess.run([sys.executable, "-m", "coverage", "report", "-m"], cwd=PROJECT_ROOT, check=True)

    html_dir = PROJECT_ROOT / "tests" / "htmlcov"
    print("\n生成 HTML 覆盖率报告...")
    subprocess.run(
        [sys.executable, "-m", "coverage", "html", f"--directory={html_dir}"],
        cwd=PROJECT_ROOT,
        check=True,
    )
    print(f"报告输出: {html_dir / 'index.html'}")


# ---------------------------------------------------------------------------
# Helper utilities (docker/network bootstrap)
# ---------------------------------------------------------------------------


def _cmd_exists(name: str) -> bool:
    return (
        subprocess.call(
            ["bash", "-lc", f"command -v {name} >/dev/null 2>&1"],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        == 0
    )


def _is_port_free(port: int, host: str = "127.0.0.1") -> bool:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.settimeout(0.5)
        try:
            sock.bind((host, port))
            return True
        except OSError:
            return False


def _pick_free_port(candidates=(5432, 5433, 15432, 25432, 35432, 45432, 55432)) -> int:
    for port in candidates:
        if _is_port_free(port):
            return port
    for port in range(40000, 60000, 7):
        if _is_port_free(port):
            return port
    raise RuntimeError("未找到可用端口用于映射 OpenGauss 容器")


def _wait_for_port(host: str, port: int, timeout: int = 120) -> bool:
    deadline = time.time() + timeout
    while time.time() < deadline:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.settimeout(1.0)
            try:
                if sock.connect_ex((host, port)) == 0:
                    return True
            except OSError:
                pass
        time.sleep(1)
    return False


def _current_conn_profile() -> Dict[str, object]:
    cfg = psycopg2_connection_kwargs()
    return {
        "host": cfg.get("host", "127.0.0.1"),
        "port": int(cfg.get("port", 5432)),
        "dbname": cfg.get("dbname", "postgres"),
        "user": cfg.get("user", "gaussdb"),
        "password": cfg.get("password", "Gauss@2025"),
    }


def _try_connect_current_config() -> Tuple[bool, str]:
    profile = _current_conn_profile()
    try:
        import psycopg2  # type: ignore

        conn = psycopg2.connect(
            host=profile["host"],
            port=profile["port"],
            dbname=profile["dbname"],
            user=profile["user"],
            password=profile["password"],
            connect_timeout=3,
        )
        conn.close()
        return True, ""
    except Exception as exc:  # noqa: BLE001
        return False, str(exc)


def _ensure_temp_opengauss_for_performance() -> Tuple[Optional[Dict[str, str]], Optional[str]]:
    ok, err = _try_connect_current_config()
    if ok:
        return None, None

    if not _cmd_exists("docker"):
        return None, None

    suffix = uuid.uuid4().hex[:8]
    container_name = f"opengauss-cov-{suffix}"
    mapped_port = _pick_free_port()
    password = f"OpenGauss@{suffix[:4]}"
    image = os.environ.get("OPENGAUSS_IMAGE", "opengauss/opengauss:latest")

    print(f"[info] 当前数据库不可用（{err.splitlines()[0] if err else 'unknown'}），尝试启动临时容器 {container_name}")

    run_cmd = [
        "docker",
        "run",
        "-d",
        "--name",
        container_name,
        "-e",
        f"GS_PASSWORD={password}",
        "-p",
        f"{mapped_port}:5432",
        image,
    ]
    proc = subprocess.run(run_cmd, capture_output=True, text=True)
    if proc.returncode != 0:
        raise RuntimeError(proc.stderr.strip() or "docker run 失败")

    if not _wait_for_port("127.0.0.1", mapped_port, timeout=180):
        raise RuntimeError("OpenGauss 容器端口就绪超时")

    ready = False
    last_err = ""
    start = time.time()
    while time.time() - start < 180:
        try:
            import psycopg2  # type: ignore

            conn = psycopg2.connect(
                host="127.0.0.1",
                port=mapped_port,
                dbname="postgres",
                user="gaussdb",
                password=password,
                connect_timeout=5,
            )
            with conn.cursor() as cur:
                cur.execute("SELECT 1")
                cur.fetchone()
            conn.close()
            ready = True
            break
        except Exception as exc:  # noqa: BLE001
            last_err = str(exc)
            time.sleep(2)

    if not ready:
        try:
            subprocess.run(["docker", "rm", "-f", container_name], capture_output=True)
        except Exception:
            pass
        raise RuntimeError(f"OpenGauss 容器未就绪: {last_err}")

    target_db = ENV.get("OG_DB_NAME", "django_opengauss_test")
    import psycopg2  # type: ignore

    conn = psycopg2.connect(
        host="127.0.0.1",
        port=mapped_port,
        dbname="postgres",
        user="gaussdb",
        password=password,
        connect_timeout=5,
    )
    conn.autocommit = True
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT 1 FROM pg_database WHERE datname=%s", (target_db,))
            if not cur.fetchone():
                cur.execute(f"CREATE DATABASE {target_db}")
    finally:
        conn.close()

    env_overrides = {
        "OG_DB_HOST": "127.0.0.1",
        "OG_DB_PORT": str(mapped_port),
        "OG_DB_NAME": target_db,
        "OG_DB_USER": "gaussdb",
        "OG_DB_PASSWORD": password,
    }
    return env_overrides, container_name


if __name__ == "__main__":
    run_coverage_suite()
