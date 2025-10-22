"""Unified performance utilities for django-opengauss.

The module focuses on the ``compare`` benchmark – a multi-database latency
comparison capable of exporting PNG/JSON artefacts (merged from the former
``perf_benchmark_multi``). Connection defaults are sourced from
:mod:`full_stack_env`, keeping configuration aligned with the functional test
suite.
"""

from __future__ import annotations

import argparse
import json
import math
import os
import random
import socket
import statistics
import subprocess
import sys
import time
import uuid
from concurrent.futures import ThreadPoolExecutor, as_completed
from contextlib import contextmanager
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Dict, Iterable, List, Mapping, MutableMapping, Optional, Tuple

try:  # Optional heavy dependencies for the "compare" mode
    import numpy as np
except Exception:  # pragma: no cover - numpy is optional at runtime
    np = None  # type: ignore

try:  # pragma: no cover - matplotlib is optional for CI environments
    import matplotlib

    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
except Exception:  # pragma: no cover
    matplotlib = None  # type: ignore
    plt = None  # type: ignore

try:
    import psycopg2
except ImportError as exc:  # pragma: no cover - surfaced during runtime use
    raise SystemExit("psycopg2 is required for performance tests") from exc

# Third-party imports that are part of Django's public API are loaded lazily.

from full_stack_env import apply_test_environment, psycopg2_connection_kwargs

# Ensure environment variables are populated for downstream components.
ENV = apply_test_environment()
DEFAULT_CONN = psycopg2_connection_kwargs()


def _median(values):
    if not values:
        return float("nan")
    if np is not None:
        try:
            return float(np.median(values))
        except Exception:
            pass
    return float(statistics.median(values))


def _is_nan(value: float) -> bool:
    try:
        return math.isnan(value)
    except TypeError:
        return False



def _cmd_exists(name: str) -> bool:
    return subprocess.call(
        ["bash", "-lc", f"command -v {name} >/dev/null 2>&1"],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    ) == 0


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
    for port in range(40000, 60000, 3):
        if _is_port_free(port):
            return port
    raise RuntimeError("未找到可用端口用于数据库容器")


def _wait_for_port(host: str, port: int, timeout: int = 120) -> bool:
    deadline = time.time() + timeout
    while time.time() < deadline:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.settimeout(1)
            try:
                if sock.connect_ex((host, port)) == 0:
                    return True
            except OSError:
                pass
        time.sleep(1)
    return False


@dataclass
class ManagedContainer:
    kind: str
    display_name: str
    container_name: str
    host: str
    port: int
    user: str
    password: str
    database: str






METRIC_LABELS: Dict[str, str] = {
    "connect_ms": "Connect (ms)",
    "concurrent_100": "Connect x100 (ms)",
    "read_select_1": "SELECT 1 (ms)",
    "read_bulk": "Batch Read (ms)",
    "read_aggregate": "Aggregate Read (ms)",
    "write_batch_insert": "Batch Insert (ms)",
    "update_batch": "Batch Update (ms)",
    "delete_batch": "Batch Delete (ms)",
    "transaction_commit": "Tx Commit (ms)",
    "transaction_rollback": "Tx Rollback (ms)",
    "agg_count_ms": "Count (ms)",
    "agg_avg_ms": "Avg (ms)",
    "pagination_ms": "Pagination (ms)",
    "create_table_ms": "Create Table (ms)",
}


def _metric_label(name: str) -> str:
    return METRIC_LABELS.get(name, name.replace('_', ' ').title())


def _prepare_chart_data(
    native_results: Dict[str, Dict[str, float]],
    django_results: Dict[str, Dict[str, float]],
) -> tuple[list[str], list[str], list[str], list[list[float]]]:
    metric_order: list[str] = []
    for results in list(native_results.values()) + list(django_results.values()):
        for key in results.keys():
            if key not in metric_order:
                metric_order.append(key)
    if not metric_order:
        metric_order = ["connect_ms", "read_select_1"]

    database_order: list[str] = []
    for collection in (native_results, django_results):
        for name in collection.keys():
            if name not in database_order:
                database_order.append(name)

    values: list[list[float]] = []
    for db in database_order:
        row: list[float] = []
        native = native_results.get(db, {})
        django = django_results.get(db, {})
        for metric in metric_order:
            val = native.get(metric)
            if val is None:
                val = django.get(metric)
            if val is None:
                row.append(float('nan'))
            else:
                row.append(float(val))
        values.append(row)

    filtered_metrics: list[str] = []
    filtered_labels: list[str] = []
    filtered_values: list[list[float]] = [[] for _ in values]

    for idx, metric in enumerate(metric_order):
        column = [rows[idx] for rows in values]
        if all(_is_nan(val) for val in column):
            continue
        filtered_metrics.append(metric)
        filtered_labels.append(_metric_label(metric))
        for series, val in zip(filtered_values, column):
            series.append(val)

    if not filtered_metrics:
        filtered_metrics = metric_order
        filtered_labels = [_metric_label(metric) for metric in metric_order]
        filtered_values = values

    return database_order, filtered_metrics, filtered_labels, filtered_values


def _max_value_from_series(series: list[list[float]]) -> float:
    max_value = 0.0
    for row in series:
        for val in row:
            if not _is_nan(val):
                max_value = max(max_value, val)
    return max_value




def _log_scale_matrix(values: list[list[float]]) -> tuple[list[list[float]], float]:
    scaled: list[list[float]] = []
    max_scaled = 0.0
    for row in values:
        scaled_row: list[float] = []
        for val in row:
            if _is_nan(val) or val < 0:
                scaled_val = float("nan")
            else:
                scaled_val = math.log1p(val)
                max_scaled = max(max_scaled, scaled_val)
            scaled_row.append(scaled_val)
        scaled.append(scaled_row)
    return scaled, max_scaled


def _generate_log_ticks(max_value: float) -> list[float]:
    if max_value <= 0:
        return [0.0, 1.0]

    ticks: set[float] = {0.0}
    upper = int(math.ceil(math.log10(max_value))) if max_value > 0 else 0
    lower = min(0, upper - 6)

    for power in range(lower, upper + 1):
        base = 10 ** power
        for multiplier in (1, 2, 5):
            tick = multiplier * base
            if tick <= max_value * 1.2 and tick > 0:
                ticks.add(round(tick, 12))

    ordered = sorted(ticks)
    if ordered[-1] < max_value:
        ordered.append(max_value)
    return ordered


def _format_tick_value(value: float) -> str:
    if value == 0:
        return "0"
    if value >= 1_000_000:
        return f"{value / 1_000_000:.1f}M"
    if value >= 1_000:
        return f"{value / 1_000:.1f}k"
    if value >= 1:
        return f"{value:.0f}"
    return f"{value:.2f}"





def _ensure_image_available(image: str) -> None:
    check = subprocess.run(["docker", "image", "inspect", image], capture_output=True)
    if check.returncode != 0:
        raise RuntimeError(f'本地未找到镜像 {image}，请先执行 docker pull {image}')


def _stop_container(name: str) -> None:
    try:
        subprocess.run(["docker", "rm", "-f", name], capture_output=True, check=False)
    except Exception:
        pass


def _ensure_database_exists_psycopg(host: str, port: int, user: str, password: str, database: str) -> None:
    import psycopg2  # type: ignore

    conn = psycopg2.connect(host=host, port=port, user=user, password=password, dbname="postgres", connect_timeout=5)
    conn.autocommit = True
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT 1 FROM pg_database WHERE datname=%s", (database,))
            if not cur.fetchone():
                cur.execute(f'CREATE DATABASE "{database}"')
    finally:
        conn.close()






def _start_opengauss_container() -> ManagedContainer:
    suffix = uuid.uuid4().hex[:8]
    container_name = f"perf-og-{suffix}"
    mapped_port = _pick_free_port()
    password = f"OpenGauss@{suffix[:4]}"
    image = os.environ.get("OPENGAUSS_IMAGE", "opengauss/opengauss:latest")
    target_db = os.environ.get("OG_BENCH_DB", "perf_opengauss")
    _ensure_image_available(image)

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
    result = subprocess.run(run_cmd, capture_output=True, text=True)
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or "无法启动 OpenGauss 容器")

    if not _wait_for_port("127.0.0.1", mapped_port, timeout=180):
        _stop_container(container_name)
        raise RuntimeError("OpenGauss 容器端口未在超时时间内开放")

    import psycopg2  # type: ignore

    start_time = time.time()
    ready = False
    while time.time() - start_time < 180:
        try:
            conn = psycopg2.connect(
                host="127.0.0.1",
                port=mapped_port,
                user="gaussdb",
                password=password,
                dbname="postgres",
                connect_timeout=5,
            )
            conn.close()
            ready = True
            break
        except Exception:
            time.sleep(3)
    if not ready:
        _stop_container(container_name)
        raise RuntimeError("OpenGauss 容器无法建立连接")

    _ensure_database_exists_psycopg("127.0.0.1", mapped_port, "gaussdb", password, target_db)
    return ManagedContainer(
        kind="opengauss",
        display_name="OpenGauss",
        container_name=container_name,
        host="127.0.0.1",
        port=mapped_port,
        user="gaussdb",
        password=password,
        database=target_db,
    )


def _start_postgres_container() -> ManagedContainer:
    suffix = uuid.uuid4().hex[:8]
    container_name = f"perf-pg-{suffix}"
    mapped_port = _pick_free_port()
    password = os.environ.get("PG_BENCH_PASSWORD", "postgres")
    image = os.environ.get("POSTGRES_IMAGE", "postgres:16")
    target_db = os.environ.get("PG_BENCH_DB", "perf_postgres")
    _ensure_image_available(image)

    run_cmd = [
        "docker",
        "run",
        "-d",
        "--name",
        container_name,
        "-e",
        f"POSTGRES_PASSWORD={password}",
        "-p",
        f"{mapped_port}:5432",
        image,
    ]
    result = subprocess.run(run_cmd, capture_output=True, text=True)
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or "无法启动 PostgreSQL 容器")

    if not _wait_for_port("127.0.0.1", mapped_port, timeout=120):
        _stop_container(container_name)
        raise RuntimeError("PostgreSQL 容器端口未在超时时间内开放")

    import psycopg2  # type: ignore

    start_time = time.time()
    ready = False
    while time.time() - start_time < 120:
        try:
            conn = psycopg2.connect(
                host="127.0.0.1",
                port=mapped_port,
                user="postgres",
                password=password,
                dbname="postgres",
                connect_timeout=5,
            )
            conn.close()
            ready = True
            break
        except Exception:
            time.sleep(2)
    if not ready:
        _stop_container(container_name)
        raise RuntimeError("PostgreSQL 容器无法建立连接")

    _ensure_database_exists_psycopg("127.0.0.1", mapped_port, "postgres", password, target_db)
    return ManagedContainer(
        kind="postgres",
        display_name="PostgreSQL",
        container_name=container_name,
        host="127.0.0.1",
        port=mapped_port,
        user="postgres",
        password=password,
        database=target_db,
    )


@contextmanager
def managed_database_containers():
    containers: List[ManagedContainer] = []
    starters = [
        ("OpenGauss", _start_opengauss_container),
        ("PostgreSQL", _start_postgres_container),
    ]
    try:
        if not _cmd_exists("docker"):
            raise RuntimeError("未检测到 docker 命令，请确认 docker 已安装并可用")
        for label, starter in starters:
            try:
                container = starter()
                containers.append(container)
            except Exception as exc:
                print(f"[warn] 无法启动 {label} 容器: {exc}")
        if not containers:
            raise RuntimeError("未能启动任何数据库容器，无法执行对比测试")
        yield containers
    finally:
        for info in containers:
            _stop_container(info.container_name)




# ---------------------------------------------------------------------------
# Quick benchmark implementation
# ---------------------------------------------------------------------------


import random
def _build_connection_kwargs(overrides: Mapping[str, object] | None = None) -> Dict[str, object]:
    """Create psycopg2 connection kwargs from defaults plus overrides."""

    config: Dict[str, object] = dict(DEFAULT_CONN)
    for key, value in (overrides or {}).items():
        if value is None:
            continue
        if key in {"database", "dbname"}:
            config["dbname"] = value
        elif key == "port":
            config[key] = int(value)
        else:
            config[key] = value
    return config


class PerformanceBenchmark:
    """Performance harness exercising typical OLTP operations."""

    def __init__(self, config: Mapping[str, object] | None = None):
        self.config = dict(config or DEFAULT_CONN)
        self.results: Dict[str, float] = {}

    @contextmanager
    def get_connection(self):
        """Yield a psycopg2 connection using the configured DSN."""

        conn = psycopg2.connect(**self.config)
        try:
            yield conn
        finally:
            conn.close()

    def ensure_schema(self) -> None:
        """Provision tables and seed baseline data if required."""

        with self.get_connection() as conn:
            cur = conn.cursor()
            cur.execute(
                """
                CREATE TABLE IF NOT EXISTS products (
                    id SERIAL PRIMARY KEY,
                    name TEXT NOT NULL,
                    price NUMERIC(10,2) NOT NULL,
                    category TEXT NOT NULL DEFAULT 'electronics'
                )
                """
            )
            cur.execute("CREATE TABLE IF NOT EXISTS bench_test (v INT)")
            conn.commit()

            cur.execute("SELECT COUNT(*) FROM products")
            count = cur.fetchone()[0]
            target = 2000
            if count < target:
                batch = [
                    (
                        f"Product {i}",
                        float(random.uniform(10, 1000)),
                        "electronics",
                    )
                    for i in range(count + 1, target + 1)
                ]
                cur.executemany(
                    "INSERT INTO products (name, price, category) VALUES (%s, %s, %s)",
                    batch,
                )
                conn.commit()

    @staticmethod
    def measure_time(func, *args, **kwargs) -> Tuple[float, object]:
        """Return execution time in milliseconds plus the function result."""

        start = time.perf_counter()
        result = func(*args, **kwargs)
        end = time.perf_counter()
        return (end - start) * 1000, result

    def test_connection_performance(self) -> None:
        print("\n1. 测试数据库连接性能...")

        single_times = []
        for _ in range(5):
            start = time.perf_counter()
            with self.get_connection():
                pass
            end = time.perf_counter()
            single_times.append((end - start) * 1000)
        avg_time = sum(single_times) / len(single_times)
        self.results["connect_ms"] = avg_time
        print(f"   ✓ 单次连接建立: {avg_time:.2f}ms")

        start = time.perf_counter()
        for _ in range(100):
            with self.get_connection():
                pass
        end = time.perf_counter()
        concurrent_time = (end - start) * 1000
        self.results["concurrent_100"] = concurrent_time
        print(f"   ✓ 并发100连接: {concurrent_time:.2f}ms")

    def test_query_performance(self) -> None:
        print("\n2. 测试读取性能...")

        with self.get_connection() as conn:
            cursor = conn.cursor()
            self.ensure_schema()

            time_ms, _ = self.measure_time(cursor.execute, "SELECT 1")
            self.results["read_select_1"] = time_ms
            print(f"   ✓ SELECT 1: {time_ms:.2f}ms")

            fetch_sql = "SELECT * FROM products WHERE category = %s LIMIT 100"
            total = 0.0
            for _ in range(10):
                elapsed, _ = self.measure_time(cursor.execute, fetch_sql, ("electronics",))
                total += elapsed
            avg_bulk = total / 10
            self.results["read_bulk"] = avg_bulk
            print(f"   ✓ 批量读取(均值): {avg_bulk:.2f}ms")

            agg_sql = "SELECT COUNT(*), AVG(price), MAX(price), MIN(price) FROM products"
            total = 0.0
            for _ in range(10):
                elapsed, _ = self.measure_time(cursor.execute, agg_sql)
                total += elapsed
            avg_agg = total / 10
            self.results["read_aggregate"] = avg_agg
            print(f"   ✓ 聚合读取(均值): {avg_agg:.2f}ms")

    def test_write_performance(self) -> None:
        print("3. 测试写入性能...")

        with self.get_connection() as conn:
            cursor = conn.cursor()
            self.ensure_schema()

            insert_sql = "INSERT INTO products (name, price) VALUES (%s, %s)"
            batch_data = [(f"Batch {i}", random.uniform(10, 1000)) for i in range(500)]
            elapsed, _ = self.measure_time(cursor.executemany, insert_sql, batch_data)
            self.results["write_batch_insert"] = elapsed
            print(f"   ✓ 批量 INSERT 500 条: {elapsed:.2f}ms")

            conn.commit()

    def test_update_delete_performance(self) -> None:
        print("4. 测试更新与删除性能...")

        with self.get_connection() as conn:
            cursor = conn.cursor()
            self.ensure_schema()

            batch_ids = [(random.uniform(10, 1000), random.randint(1, 2000)) for _ in range(200)]
            elapsed, _ = self.measure_time(cursor.executemany, "UPDATE products SET price = %s WHERE id = %s", batch_ids)
            self.results["update_batch"] = elapsed
            print(f"   ✓ 批量 UPDATE 200 条: {elapsed:.2f}ms")

            delete_sql = "DELETE FROM products WHERE id = %s"
            batch_ids = [(random.randint(1, 2000),) for _ in range(200)]
            elapsed, _ = self.measure_time(cursor.executemany, delete_sql, batch_ids)
            self.results["delete_batch"] = elapsed
            print(f"   ✓ 批量 DELETE 200 条: {elapsed:.2f}ms")

    def test_transaction_performance(self) -> None:
        print("5. 测试事务性能...")

        with self.get_connection() as conn:
            cursor = conn.cursor()
            total = 0.0
            for _ in range(5):
                start = time.perf_counter()
                cursor.execute("BEGIN")
                for _ in range(10):
                    cursor.execute("INSERT INTO bench_test VALUES (%s)", (1,))
                cursor.execute("COMMIT")
                total += (time.perf_counter() - start) * 1000
            avg_tx = total / 5
            self.results["transaction_commit"] = avg_tx
            print(f"   ✓ 事务提交 (10次写入，均值): {avg_tx:.2f}ms")

            total = 0.0
            for _ in range(5):
                start = time.perf_counter()
                cursor.execute("BEGIN")
                for _ in range(10):
                    cursor.execute("INSERT INTO bench_test VALUES (%s)", (1,))
                cursor.execute("ROLLBACK")
                total += (time.perf_counter() - start) * 1000
            avg_rollback = total / 5
            self.results["transaction_rollback"] = avg_rollback
            print(f"   ✓ 事务回滚 (10次写入，均值): {avg_rollback:.2f}ms")

    def test_concurrent_performance(self) -> None:
        print("\n6. 测试并发处理性能...")

        def worker():
            with self.get_connection() as conn:
                with conn.cursor() as cur:
                    cur.execute("SELECT 1")
                    cur.fetchone()

        for n in (10, 50, 100):
            start = time.perf_counter()
            with ThreadPoolExecutor(max_workers=n) as ex:
                futures = [ex.submit(worker) for _ in range(n * 20)]
                for future in as_completed(futures):
                    future.result()
            elapsed = time.perf_counter() - start
            total_ops = n * 20
            qps = total_ops / elapsed
            self.results[f"qps_{n}"] = qps
            print(f"   ✓ {n}并发查询QPS: {int(qps):,}")

    def generate_report(self) -> Dict[str, float]:
        print("\n" + "=" * 70)
        print("Django-OpenGauss 驱动程序性能基准测试总结")
        print("=" * 70)
        print(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print("-" * 70)
        print("\n关键性能指标（实测）：")
        summary_keys = [
            "connect_ms",
            "read_select_1",
            "write_batch_insert",
            "transaction_commit",
        ]
        for key in summary_keys:
            if key in self.results:
                label = _metric_label(key)
                print(f"  • {label}: {self.results[key]:.2f}")
        print("=" * 70)
        return self.results

    def run_all_tests(self) -> Dict[str, float]:
        print("=" * 70)
        print("Django-OpenGauss 驱动程序性能基准测试")
        print("=" * 70)
        self.test_connection_performance()
        self.test_query_performance()
        self.test_write_performance()
        self.test_update_delete_performance()
        self.test_transaction_performance()
        self.test_concurrent_performance()
        return self.generate_report()

class SQLiteBenchmark:
    """Benchmark suite for SQLite databases using sqlite3."""

    def __init__(self, path: str):
        self.path = path
        self.results: Dict[str, float] = {}

    @contextmanager
    def get_connection(self):
        import sqlite3

        conn = sqlite3.connect(self.path)
        try:
            yield conn
        finally:
            conn.close()

    def ensure_schema(self) -> None:
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute(
                """
                CREATE TABLE IF NOT EXISTS products (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    price REAL NOT NULL,
                    category TEXT NOT NULL DEFAULT 'electronics'
                )
                """
            )
            cursor.execute("CREATE TABLE IF NOT EXISTS bench_test (v INT)")
            conn.commit()

            cursor.execute("SELECT COUNT(*) FROM products")
            count = cursor.fetchone()[0]
            target = 2000
            if count < target:
                batch = []
                for i in range(count + 1, target + 1):
                    batch.append((f"Product {i}", float(random.uniform(10, 1000)), "electronics"))
                cursor.executemany("INSERT INTO products (name, price, category) VALUES (?, ?, ?)", batch)
                conn.commit()

    @staticmethod
    def measure_time(func, *args, **kwargs) -> tuple[float, object]:
        start = time.perf_counter()
        result = func(*args, **kwargs)
        end = time.perf_counter()
        return (end - start) * 1000, result

    def test_connection_performance(self) -> None:
        import sqlite3

        print("\n1. 测试数据库连接性能...")
        single_times = []
        for _ in range(5):
            start = time.perf_counter()
            conn = sqlite3.connect(self.path)
            conn.close()
            end = time.perf_counter()
            single_times.append((end - start) * 1000)
        avg_time = sum(single_times) / len(single_times)
        self.results["connect_ms"] = avg_time
        print(f"   ✓ 单次连接建立: {avg_time:.2f}ms")

    def test_query_performance(self) -> None:
        print("\n2. 测试读取性能...")

        with self.get_connection() as conn:
            cursor = conn.cursor()
            self.ensure_schema()

            time_ms, _ = self.measure_time(cursor.execute, "SELECT 1")
            self.results["read_select_1"] = time_ms
            print(f"   ✓ SELECT 1: {time_ms:.2f}ms")

            fetch_sql = "SELECT * FROM products WHERE category = ? LIMIT 100"
            total = 0.0
            for _ in range(10):
                elapsed, _ = self.measure_time(cursor.execute, fetch_sql, ("electronics",))
                total += elapsed
            avg_bulk = total / 10
            self.results["read_bulk"] = avg_bulk
            print(f"   ✓ 批量读取(均值): {avg_bulk:.2f}ms")

            agg_sql = "SELECT COUNT(*), AVG(price), MAX(price), MIN(price) FROM products"
            total = 0.0
            for _ in range(10):
                elapsed, _ = self.measure_time(cursor.execute, agg_sql)
                total += elapsed
            avg_agg = total / 10
            self.results["read_aggregate"] = avg_agg
            print(f"   ✓ 聚合读取(均值): {avg_agg:.2f}ms")

    def test_write_performance(self) -> None:
        print("3. 测试写入性能...")

        with self.get_connection() as conn:
            cursor = conn.cursor()
            self.ensure_schema()

            insert_sql = "INSERT INTO products (name, price, category) VALUES (?, ?, ?)"
            batch_data = [(f"Batch {i}", float(random.uniform(10, 1000)), "electronics") for i in range(500)]
            elapsed, _ = self.measure_time(cursor.executemany, insert_sql, batch_data)
            self.results["write_batch_insert"] = elapsed
            print(f"   ✓ 批量 INSERT 500 条: {elapsed:.2f}ms")
            conn.commit()

    def test_update_delete_performance(self) -> None:
        print("4. 测试更新与删除性能...")

        with self.get_connection() as conn:
            cursor = conn.cursor()
            self.ensure_schema()

            batch_ids = [(float(random.uniform(10, 1000)), random.randint(1, 2000)) for _ in range(200)]
            elapsed, _ = self.measure_time(cursor.executemany, "UPDATE products SET price = ? WHERE id = ?", batch_ids)
            self.results["update_batch"] = elapsed
            print(f"   ✓ 批量 UPDATE 200 条: {elapsed:.2f}ms")

            delete_sql = "DELETE FROM products WHERE id = ?"
            batch_ids = [(random.randint(1, 2000),) for _ in range(200)]
            elapsed, _ = self.measure_time(cursor.executemany, delete_sql, batch_ids)
            self.results["delete_batch"] = elapsed
            print(f"   ✓ 批量 DELETE 200 条: {elapsed:.2f}ms")

    def test_transaction_performance(self) -> None:
        print("5. 测试事务性能...")

        with self.get_connection() as conn:
            cursor = conn.cursor()
            total = 0.0
            for _ in range(5):
                start = time.perf_counter()
                cursor.execute("BEGIN")
                for _ in range(10):
                    cursor.execute("INSERT INTO bench_test VALUES (?)", (1,))
                cursor.execute("COMMIT")
                total += (time.perf_counter() - start) * 1000
            avg_commit = total / 5
            self.results["transaction_commit"] = avg_commit
            print(f"   ✓ 事务提交 (10次写入，均值): {avg_commit:.2f}ms")

            total = 0.0
            for _ in range(5):
                start = time.perf_counter()
                cursor.execute("BEGIN")
                for _ in range(10):
                    cursor.execute("INSERT INTO bench_test VALUES (?)", (1,))
                cursor.execute("ROLLBACK")
                total += (time.perf_counter() - start) * 1000
            avg_rollback = total / 5
            self.results["transaction_rollback"] = avg_rollback
            print(f"   ✓ 事务回滚 (10次写入，均值): {avg_rollback:.2f}ms")

    def run_all_tests(self) -> Dict[str, float]:
        print("=" * 70)
        print("SQLite 性能基准测试")
        print("=" * 70)
        self.test_connection_performance()
        self.test_query_performance()
        self.test_write_performance()
        self.test_update_delete_performance()
        self.test_transaction_performance()
        return self.results

# ---------------------------------------------------------------------------
# Multi-database comparison benchmark (adapted from perf_benchmark_multi.py)
# ---------------------------------------------------------------------------


def ensure_plotting_dependencies() -> None:
    if np is None or plt is None or matplotlib is None:
        print("[info] numpy/matplotlib 不可用，使用 SVG 回退绘图。")


@dataclass
class Target:
    name: str
    kind: str  # opengauss|postgres|sqlite
    params: Dict[str, object]


def add_compare_arguments(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--og-host", default=DEFAULT_CONN.get("host", "127.0.0.1"))
    parser.add_argument("--og-port", type=int, default=int(DEFAULT_CONN.get("port", 5432)))
    parser.add_argument("--og-db", default=DEFAULT_CONN.get("dbname", "postgres"))
    parser.add_argument("--og-user", default=DEFAULT_CONN.get("user", "gaussdb"))
    parser.add_argument("--og-password", default=DEFAULT_CONN.get("password", ""))
    parser.add_argument("--og-name", default="OpenGauss")

    parser.add_argument("--compare-postgres", action="store_true", default=True)
    parser.add_argument("--pg-host", default=os.environ.get("PG_HOST", "127.0.0.1"))
    parser.add_argument("--pg-port", type=int, default=int(os.environ.get("PG_PORT", 5432)))
    parser.add_argument("--pg-db", default=os.environ.get("PG_DB", "postgres"))
    parser.add_argument("--pg-user", default=os.environ.get("PG_USER", "postgres"))
    parser.add_argument("--pg-password", default=os.environ.get("PG_PASSWORD", "postgres"))
    parser.add_argument("--pg-name", default="PostgreSQL")

    parser.add_argument("--compare-sqlite", action="store_true", default=True)
    parser.add_argument("--sqlite-path", default=os.environ.get("SQLITE_PATH", ":memory:"))
    parser.add_argument("--sqlite-name", default="SQLite")

    parser.add_argument("--repeat", type=int, default=10, help="Repeats for median timing")
    parser.add_argument("--no-django", action="store_true", help="Disable Django-based measurement")
    parser.add_argument(
        "--require-min-targets",
        type=int,
        default=1,
        help="Exit non-zero if fewer targets succeed",
    )
    parser.add_argument("--out", default="tests/performance_compare.png", help="Output PNG path")
    parser.add_argument("--save-json", default="tests/performance_compare.json", help="Output JSON path")


def time_connect_postgres(host, port, db, user, password, repeat=10) -> Tuple[float, float]:
    conn_times: List[float] = []
    q_times: List[float] = []
    for _ in range(repeat):
        t0 = time.perf_counter()
        try:
            conn = psycopg2.connect(
                host=host,
                port=port,
                dbname=db,
                user=user,
                password=password,
                connect_timeout=5,
            )
        except Exception:
            return float("nan"), float("nan")
        t1 = time.perf_counter()
        conn_times.append((t1 - t0) * 1000.0)
        try:
            with conn.cursor() as cur:
                t2 = time.perf_counter()
                cur.execute("SELECT 1")
                _ = cur.fetchone()
                t3 = time.perf_counter()
                q_times.append((t3 - t2) * 1000.0)
        finally:
            conn.close()
    if not conn_times:
        return float("nan"), float("nan")
    connect_ms = _median(conn_times)
    select_ms = _median(q_times) if q_times else float("nan")
    return connect_ms, select_ms



def time_connect_sqlite(path, repeat=10) -> Tuple[float, float]:
    import sqlite3

    conn_times: List[float] = []
    q_times: List[float] = []
    for _ in range(repeat):
        t0 = time.perf_counter()
        conn = sqlite3.connect(path, timeout=5)
        t1 = time.perf_counter()
        conn_times.append((t1 - t0) * 1000.0)
        try:
            cur = conn.cursor()
            t2 = time.perf_counter()
            cur.execute("SELECT 1")
            _ = cur.fetchone()
            t3 = time.perf_counter()
            q_times.append((t3 - t2) * 1000.0)
        finally:
            conn.close()
    return _median(conn_times), _median(q_times)


def build_targets(args: argparse.Namespace) -> Dict[str, Target]:
    targets: Dict[str, Target] = {}
    targets[args.og_name] = Target(
        name=args.og_name,
        kind="opengauss",
        params=dict(
            host=args.og_host,
            port=args.og_port,
            db=args.og_db,
            user=args.og_user,
            password=args.og_password,
        ),
    )
    if args.compare_postgres:
        targets[args.pg_name] = Target(
            name=args.pg_name,
            kind="postgres",
            params=dict(
                host=args.pg_host,
                port=args.pg_port,
                db=args.pg_db,
                user=args.pg_user,
                password=args.pg_password,
            ),
        )
    if args.compare_sqlite:
        targets[args.sqlite_name] = Target(
            name=args.sqlite_name,
            kind="sqlite",
            params=dict(path=args.sqlite_path),
        )
    return targets





def setup_django_for_targets(targets: Dict[str, Target]) -> Dict[str, str]:
    from django.conf import settings as django_settings
    import django

    databases: Dict[str, Dict[str, object]] = {}
    alias_map: Dict[str, str] = {}
    default_alias = "default"
    default_configured = False

    for name, target in targets.items():
        if target.kind == "opengauss":
            alias = "og"
            config = {
                "ENGINE": "django_opengauss",
                "NAME": target.params["db"],
                "USER": target.params["user"],
                "PASSWORD": target.params["password"],
                "HOST": target.params["host"],
                "PORT": target.params["port"],
            }
            databases[alias] = config
            if not default_configured:
                databases[default_alias] = config
                alias_map[name] = default_alias
                default_configured = True
            else:
                alias_map[name] = alias
        elif target.kind == "postgres":
            alias = "pg"
            config = {
                "ENGINE": "django.db.backends.postgresql",
                "NAME": target.params["db"],
                "USER": target.params["user"],
                "PASSWORD": target.params["password"],
                "HOST": target.params["host"],
                "PORT": target.params["port"],
            }
            databases[alias] = config
            alias_map[name] = alias
        else:
            alias = "sqlite"
            config = {
                "ENGINE": "django.db.backends.sqlite3",
                "NAME": target.params["path"],
            }
            databases[alias] = config
            alias_map[name] = alias

    if not default_configured and databases:
        first_alias, config = next(iter(databases.items()))
        databases.setdefault(default_alias, config)

    if not django_settings.configured:
        django_settings.configure(
            INSTALLED_APPS=[
                "django.contrib.contenttypes",
                "django.contrib.auth",
            ],
            DATABASES=databases,
            SECRET_KEY="perf-benchmark",
            USE_TZ=True,
        )
        django.setup()
    else:
        django_settings.DATABASES.update(databases)

    return alias_map



def _placeholders_for_vendor(vendor: str) -> str:
    if vendor == "sqlite":
        return "?"
    return "%s"


def measure_workload_django(alias: str, repeat: int = 3) -> Dict[str, float]:
    from django.db import connections

    conn = connections[alias]
    vendor = getattr(conn, "vendor", "")
    ph = _placeholders_for_vendor(vendor)
    metrics: Dict[str, float] = {}

    suffix = uuid.uuid4().hex[:8]
    t_main = f"perf_bench_{suffix}"
    t_1k = f"perf_bench_1k_{suffix}"

    def exec_sql(sql: str, params: Optional[Tuple] = None) -> None:
        with conn.cursor() as cur:
            if params is None:
                cur.execute(sql)
            else:
                cur.execute(sql, params)

    def time_exec(sql: str, params: Optional[Tuple] = None) -> float:
        t0 = time.perf_counter()
        exec_sql(sql, params)
        t1 = time.perf_counter()
        return (t1 - t0) * 1000.0

    exec_sql(f"DROP TABLE IF EXISTS {t_main}")
    exec_sql(f"DROP TABLE IF EXISTS {t_1k}")

    create_sql = (
        f"CREATE TABLE {t_main} ("
        " id SERIAL PRIMARY KEY,"
        " name VARCHAR(64),"
        " price NUMERIC(10,2),"
        " created TIMESTAMP DEFAULT NOW()"
        ")"
    )
    metrics["create_table_ms"] = time_exec(create_sql)

    insert_sql = f"INSERT INTO {t_main} (name, price) VALUES ({ph}, {ph})"
    rows = [(f"Item {i}", random.uniform(10, 1000)) for i in range(1000)]
    t0 = time.perf_counter()
    with conn.cursor() as cur:
        cur.executemany(insert_sql, rows)
    metrics["insert_1k_ms"] = (time.perf_counter() - t0) * 1000.0

    more_rows = [(f"Bulk {i}", random.uniform(10, 1000)) for i in range(5000)]
    t0 = time.perf_counter()
    with conn.cursor() as cur:
        cur.executemany(insert_sql, more_rows)
    metrics["insert_5k_ms"] = (time.perf_counter() - t0) * 1000.0

    update_sql = f"UPDATE {t_main} SET price = price * 1.1"
    metrics["update_ms"] = time_exec(update_sql)

    delete_sql = f"DELETE FROM {t_main} WHERE id IN (SELECT id FROM {t_main} LIMIT 2000)"
    metrics["delete_ms"] = time_exec(delete_sql)

    tx_sql = f"INSERT INTO {t_main} (name, price) VALUES ({ph}, {ph})"
    t0 = time.perf_counter()
    with conn.cursor() as cur:
        for _ in range(100):
            cur.execute(tx_sql, ("tx", 1))
    conn.commit()
    metrics["tx_commit_ms"] = (time.perf_counter() - t0) * 1000.0

    agg_count_sql = f"SELECT COUNT(*) FROM {t_main}"
    metrics["agg_count_ms"] = time_exec(agg_count_sql)

    agg_avg_sql = f"SELECT AVG(price) FROM {t_main}"
    metrics["agg_avg_ms"] = time_exec(agg_avg_sql)

    pagination_sql = f"SELECT * FROM {t_main} ORDER BY id LIMIT 100 OFFSET 1000"
    metrics["pagination_ms"] = time_exec(pagination_sql)

    exec_sql(f"DROP TABLE IF EXISTS {t_main}")
    exec_sql(f"DROP TABLE IF EXISTS {t_1k}")

    return metrics


def _measure_native_psycopg(params: Mapping[str, object]) -> Dict[str, float]:
    import psycopg2  # type: ignore

    metrics: Dict[str, float] = {}
    table = f"perf_native_{uuid.uuid4().hex[:8]}"
    conn = psycopg2.connect(
        host=params["host"],
        port=params["port"],
        dbname=params["db"],
        user=params["user"],
        password=params["password"],
        connect_timeout=5,
    )
    conn.autocommit = False

    def _now_ms() -> float:
        return time.perf_counter() * 1000.0

    try:
        with conn.cursor() as cur:
            cur.execute(f"DROP TABLE IF EXISTS {table}")
        conn.commit()

        start = _now_ms()
        with conn.cursor() as cur:
            cur.execute(
                f"""
                CREATE TABLE {table} (
                    id SERIAL PRIMARY KEY,
                    name TEXT NOT NULL,
                    price NUMERIC(10,2) NOT NULL,
                    category TEXT NOT NULL
                )
                """
            )
        conn.commit()
        metrics["create_table_ms"] = _now_ms() - start

        insert_sql = f"INSERT INTO {table} (name, price, category) VALUES (%s, %s, %s)"
        seed_rows = [
            (f"Seed {i}", float(random.uniform(10, 1000)), "electronics") for i in range(1, 1001)
        ]
        with conn.cursor() as cur:
            cur.executemany(insert_sql, seed_rows)
        conn.commit()

        batch_payload = [
            (f"Bulk {i}", float(random.uniform(10, 1000)), "electronics") for i in range(500)
        ]
        start = _now_ms()
        with conn.cursor() as cur:
            cur.executemany(insert_sql, batch_payload)
        conn.commit()
        metrics["write_batch_insert"] = _now_ms() - start

        with conn.cursor() as cur:
            cur.execute(f"SELECT id FROM {table} ORDER BY id LIMIT 5000")
            id_pool = [row[0] for row in cur.fetchall()]

        if id_pool:
            with conn.cursor() as cur:
                start = _now_ms()
                cur.execute(
                    f"SELECT * FROM {table} WHERE category = %s ORDER BY id LIMIT 100",
                    ("electronics",),
                )
                cur.fetchall()
            metrics["read_bulk"] = _now_ms() - start

            with conn.cursor() as cur:
                start = _now_ms()
                cur.execute(f"SELECT COUNT(*), AVG(price), MAX(price), MIN(price) FROM {table}")
                cur.fetchone()
            metrics["read_aggregate"] = _now_ms() - start

            with conn.cursor() as cur:
                start = _now_ms()
                cur.execute(f"SELECT COUNT(*) FROM {table}")
                cur.fetchone()
            metrics["agg_count_ms"] = _now_ms() - start

            with conn.cursor() as cur:
                start = _now_ms()
                cur.execute(f"SELECT AVG(price) FROM {table}")
                cur.fetchone()
            metrics["agg_avg_ms"] = _now_ms() - start

            with conn.cursor() as cur:
                start = _now_ms()
                cur.execute(f"SELECT * FROM {table} ORDER BY id LIMIT 100 OFFSET 200")
                cur.fetchall()
            metrics["pagination_ms"] = _now_ms() - start

            batch_size = min(len(id_pool), 100)
            if batch_size:
                update_batch_sql = f"UPDATE {table} SET price = %s WHERE id = %s"
                updates = [
                    (float(random.uniform(10, 1000)), rid)
                    for rid in random.sample(id_pool, batch_size)
                ]
                start = _now_ms()
                with conn.cursor() as cur:
                    cur.executemany(update_batch_sql, updates)
                conn.commit()
                metrics["update_batch"] = _now_ms() - start

            delete_sql = f"DELETE FROM {table} WHERE id = %s"
            batch_delete_size = min(len(id_pool), 100)
            if batch_delete_size:
                targets = id_pool[:batch_delete_size]
                start = _now_ms()
                with conn.cursor() as cur:
                    cur.executemany(delete_sql, [(rid,) for rid in targets])
                conn.commit()
                metrics["delete_batch"] = _now_ms() - start
                id_pool = id_pool[batch_delete_size:]

        tx_sql = f"INSERT INTO {table} (name, price, category) VALUES (%s, %s, %s)"
        conn.rollback()
        start = _now_ms()
        with conn.cursor() as cur:
            for _ in range(20):
                cur.execute(tx_sql, ("tx", float(random.uniform(10, 1000)), "electronics"))
        conn.commit()
        metrics["transaction_commit"] = _now_ms() - start

        conn.rollback()
        start = _now_ms()
        with conn.cursor() as cur:
            for _ in range(20):
                cur.execute(tx_sql, ("tx", float(random.uniform(10, 1000)), "electronics"))
        conn.rollback()
        metrics["transaction_rollback"] = _now_ms() - start

    finally:
        try:
            with conn.cursor() as cur:
                cur.execute(f"DROP TABLE IF EXISTS {table}")
            conn.commit()
        except Exception:
            pass
        conn.close()

    return metrics


def _measure_native_sqlite(path: str) -> Dict[str, float]:
    import sqlite3

    metrics: Dict[str, float] = {}
    table = f"perf_native_{uuid.uuid4().hex[:8]}"
    conn = sqlite3.connect(path, timeout=5)

    def _now_ms() -> float:
        return time.perf_counter() * 1000.0

    def _safe_rollback() -> None:
        try:
            conn.rollback()
        except sqlite3.ProgrammingError:
            pass

    try:
        conn.execute(f"DROP TABLE IF EXISTS {table}")
        conn.commit()

        start = _now_ms()
        conn.execute(
            f"""
            CREATE TABLE {table} (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                price REAL NOT NULL,
                category TEXT NOT NULL
            )
            """
        )
        conn.commit()
        metrics["create_table_ms"] = _now_ms() - start

        insert_sql = f"INSERT INTO {table} (name, price, category) VALUES (?, ?, ?)"
        seed_rows = [
            (f"Seed {i}", float(random.uniform(10, 1000)), "electronics") for i in range(1, 1001)
        ]
        conn.executemany(insert_sql, seed_rows)
        conn.commit()

        batch_payload = [
            (f"Bulk {i}", float(random.uniform(10, 1000)), "electronics") for i in range(500)
        ]
        start = _now_ms()
        conn.executemany(insert_sql, batch_payload)
        conn.commit()
        metrics["write_batch_insert"] = _now_ms() - start

        id_rows = conn.execute(f"SELECT id FROM {table} ORDER BY id LIMIT 5000").fetchall()
        id_pool = [row[0] for row in id_rows]

        if id_pool:
            start = _now_ms()
            conn.execute(
                f"SELECT * FROM {table} WHERE category = ? ORDER BY id LIMIT 100",
                ("electronics",),
            ).fetchall()
            metrics["read_bulk"] = _now_ms() - start

            start = _now_ms()
            conn.execute(f"SELECT COUNT(*), AVG(price), MAX(price), MIN(price) FROM {table}").fetchone()
            metrics["read_aggregate"] = _now_ms() - start

            start = _now_ms()
            conn.execute(f"SELECT COUNT(*) FROM {table}").fetchone()
            metrics["agg_count_ms"] = _now_ms() - start

            start = _now_ms()
            conn.execute(f"SELECT AVG(price) FROM {table}").fetchone()
            metrics["agg_avg_ms"] = _now_ms() - start

            start = _now_ms()
            conn.execute(f"SELECT * FROM {table} ORDER BY id LIMIT 100 OFFSET 200").fetchall()
            metrics["pagination_ms"] = _now_ms() - start

            batch_size = min(len(id_pool), 100)
            if batch_size:
                update_batch_sql = f"UPDATE {table} SET price = ? WHERE id = ?"
                updates = [
                    (float(random.uniform(10, 1000)), rid)
                    for rid in random.sample(id_pool, batch_size)
                ]
                start = _now_ms()
                conn.executemany(update_batch_sql, updates)
                conn.commit()
                metrics["update_batch"] = _now_ms() - start

            delete_sql = f"DELETE FROM {table} WHERE id = ?"
            batch_delete_size = min(len(id_pool), 100)
            if batch_delete_size:
                targets = id_pool[:batch_delete_size]
                start = _now_ms()
                conn.executemany(delete_sql, [(rid,) for rid in targets])
                conn.commit()
                metrics["delete_batch"] = _now_ms() - start
                id_pool = id_pool[batch_delete_size:]

        tx_sql = f"INSERT INTO {table} (name, price, category) VALUES (?, ?, ?)"
        _safe_rollback()
        start = _now_ms()
        cur = conn.cursor()
        cur.execute("BEGIN")
        for _ in range(20):
            cur.execute(tx_sql, ("tx", float(random.uniform(10, 1000)), "electronics"))
        conn.commit()
        metrics["transaction_commit"] = _now_ms() - start

        _safe_rollback()
        start = _now_ms()
        cur = conn.cursor()
        cur.execute("BEGIN")
        for _ in range(20):
            cur.execute(tx_sql, ("tx", float(random.uniform(10, 1000)), "electronics"))
        conn.rollback()
        metrics["transaction_rollback"] = _now_ms() - start

    finally:
        try:
            conn.execute(f"DROP TABLE IF EXISTS {table}")
            conn.commit()
        except Exception:
            pass
        conn.close()

    return metrics


def measure_native(target: Target, repeat: int = 10) -> Dict[str, float]:
    if target.kind in {"opengauss", "postgres"}:
        connect, select = time_connect_postgres(
            target.params["host"],
            target.params["port"],
            target.params["db"],
            target.params["user"],
            target.params["password"],
            repeat,
        )
        metrics: Dict[str, float] = {"connect_ms": connect, "select_ms": select}
        if not _is_nan(connect):
            metrics["read_select_1"] = select
            try:
                metrics.update(_measure_native_psycopg(target.params))
            except Exception as exc:
                print(f"[warn] Native CRUD benchmark failed for {target.name}: {exc}")
        return metrics
    if target.kind == "sqlite":
        connect, select = time_connect_sqlite(target.params["path"], repeat)
        metrics = {"connect_ms": connect, "select_ms": select}
        if not _is_nan(connect):
            metrics["read_select_1"] = select
            try:
                metrics.update(_measure_native_sqlite(target.params["path"]))
            except Exception as exc:
                print(f"[warn] Native CRUD benchmark failed for {target.name}: {exc}")
        return metrics
    return {"connect_ms": float("nan"), "select_ms": float("nan")}


def run_multi_target(args: argparse.Namespace) -> Dict[str, Dict[str, float]]:
    ensure_plotting_dependencies()

    targets = build_targets(args)
    alias_map: Dict[str, str] = {}
    if not getattr(args, 'no_django', False):
        alias_map = setup_django_for_targets(targets)

    native_results: Dict[str, Dict[str, float]] = {}
    for name, target in targets.items():
        if target.kind in {"opengauss", "postgres"}:
            config = {
                "host": target.params["host"],
                "port": target.params["port"],
                "dbname": target.params["db"],
                "user": target.params["user"],
                "password": target.params["password"],
            }
            benchmark = PerformanceBenchmark(config=_build_connection_kwargs(config))
            native_results[name] = benchmark.run_all_tests()
        elif target.kind == "sqlite":
            sqlite_bench = SQLiteBenchmark(target.params["path"])
            native_results[name] = sqlite_bench.run_all_tests()
        else:
            native_results[name] = measure_native(target, args.repeat)

    django_results: Dict[str, Dict[str, float]] = {}
    if not args.no_django:
        for name, alias in alias_map.items():
            try:
                django_results[name] = measure_workload_django(alias, repeat=3)
            except Exception as exc:
                django_results[name] = {"error": float("nan")}
                print(f"[warn] Django workload failed for {name}: {exc}")

    success_targets = [name for name, metrics in native_results.items() if not any(_is_nan(v) for v in metrics.values())]
    if len(success_targets) < args.require_min_targets:
        raise RuntimeError(
            f"Only {len(success_targets)} targets produced results (< {args.require_min_targets})."
        )

    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    json_path = Path(args.save_json)
    json_path.parent.mkdir(parents=True, exist_ok=True)

    chart_path = render_plot(out_path, native_results, django_results)
    write_json(json_path, native_results, django_results)

    print(f"图表已保存至: {chart_path}")
    print(f"JSON结果已保存至: {json_path}")
    return {"native": native_results, "django": django_results}


def render_plot(out_path: Path, native_results: Dict[str, Dict[str, float]], django_results: Dict[str, Dict[str, float]]) -> Path:
    ensure_plotting_dependencies()

    databases, metrics, metric_labels, values = _prepare_chart_data(native_results, django_results)

    if matplotlib is None or plt is None or np is None:
        return render_svg_plot(out_path, native_results, django_results)

    x = np.arange(len(metrics))
    if len(databases) == 0:
        databases = ["Unknown"]
        values = [[float('nan')] * len(metrics)]

    scaled_values, max_scaled = _log_scale_matrix(values)
    max_scaled = max_scaled if max_scaled > 0 else 1.0
    max_actual = _max_value_from_series(values)

    bar_width = 0.8 / max(len(databases), 1)
    offsets = (np.arange(len(databases)) - (len(databases) - 1) / 2) * bar_width

    fig, ax = plt.subplots(figsize=(max(8, len(metrics) * 1.5), 6))

    for idx, db in enumerate(databases):
        series = scaled_values[idx]
        ax.bar(x + offsets[idx], series, bar_width, label=db)

    ax.set_xticks(x)
    ax.set_xticklabels(metric_labels, rotation=20, ha='right')
    ax.set_ylabel('Milliseconds (log1p scale)')
    ax.set_title('Database Performance Comparison')
    ax.legend()
    ax.grid(axis='y', linestyle='--', alpha=0.4)

    tick_values = _generate_log_ticks(max_actual)
    tick_positions = [math.log1p(val) for val in tick_values]
    ax.set_yticks(tick_positions)
    ax.set_yticklabels([_format_tick_value(val) for val in tick_values])
    ax.set_ylim(0, max_scaled * 1.05)

    fig.tight_layout()
    fig.savefig(out_path)
    plt.close(fig)
    return out_path


def render_svg_plot(out_path: Path, native_results: Dict[str, Dict[str, float]], django_results: Dict[str, Dict[str, float]]) -> Path:
    databases, metrics, metric_labels, values = _prepare_chart_data(native_results, django_results)

    width, height = 960, 540
    margin_left, margin_bottom, margin_top, margin_right = 120, 80, 50, 40
    plot_width = width - margin_left - margin_right
    plot_height = height - margin_top - margin_bottom

    if not databases:
        databases = ["Unknown"]
        values = [[float('nan')] * len(metrics)]

    max_actual = _max_value_from_series(values)
    scaled_values, max_scaled = _log_scale_matrix(values)
    if max_scaled <= 0:
        max_scaled = 1.0
    scale = plot_height / max_scaled

    per_group_width = len(databases) * 32 + max(len(databases) - 1, 0) * 14
    group_gap = 40
    plot_span = len(metrics) * per_group_width + max(len(metrics) - 1, 0) * group_gap
    plot_start = margin_left + max((plot_width - plot_span) / 2, 0)

    svg_lines = [
        f'<svg width="{width}" height="{height}" xmlns="http://www.w3.org/2000/svg">',
        '<rect width="100%" height="100%" fill="white"/>',
        f'<line x1="{margin_left}" y1="{height - margin_bottom}" x2="{width - margin_right}" y2="{height - margin_bottom}" stroke="#333"/>',
        f'<line x1="{margin_left}" y1="{margin_top}" x2="{margin_left}" y2="{height - margin_bottom}" stroke="#333"/>',
        f'<text x="{width / 2:.1f}" y="{margin_top - 15}" fill="#333" font-size="18" text-anchor="middle">Database Performance Comparison</text>',
    ]

    y_base = height - margin_bottom
    bar_width = 32
    inner_gap = 14

    label_centers: list[float] = []

    for metric_idx, metric in enumerate(metrics):
        group_start = plot_start + metric_idx * (per_group_width + group_gap)
        group_center = group_start + per_group_width / 2
        label_centers.append(group_center)
        svg_lines.append(f'<text x="{group_center:.1f}" y="{y_base + 40}" fill="#333" font-size="12" text-anchor="middle">{metric_labels[metric_idx]}</text>')

        for db_idx, db in enumerate(databases):
            value = values[db_idx][metric_idx]
            scaled_val = scaled_values[db_idx][metric_idx]
            if _is_nan(scaled_val):
                continue
            bar_height = scaled_val * scale
            x_pos = group_start + db_idx * (bar_width + inner_gap)
            svg_lines.append(f'<rect x="{x_pos:.1f}" y="{y_base - bar_height:.1f}" width="{bar_width}" height="{bar_height:.1f}" fill="{["#4F81BD", "#C0504D", "#9BBB59", "#8064A2", "#4BACC6"][db_idx % 5]}"/>')
            if not _is_nan(value):
                svg_lines.append(
                    f'<text x="{x_pos + bar_width / 2:.1f}" y="{y_base - bar_height - 6:.1f}" fill="#555" font-size="10" text-anchor="middle">{value:.1f}</text>'
                )

    tick_values = _generate_log_ticks(max_actual)
    for tick in tick_values:
        scaled_tick = math.log1p(tick) if tick >= 0 else 0.0
        y = y_base - scaled_tick * scale
        svg_lines.append(f'<line x1="{margin_left - 5}" y1="{y:.1f}" x2="{width - margin_right}" y2="{y:.1f}" stroke="#ddd" stroke-dasharray="2,3"/>')
        svg_lines.append(f'<text x="{margin_left - 10}" y="{y + 4:.1f}" fill="#333" font-size="11" text-anchor="end">{_format_tick_value(tick)}</text>')

    legend_x = width - margin_right - 160
    legend_y = margin_top + 10
    for idx, db in enumerate(databases):
        color = ["#4F81BD", "#C0504D", "#9BBB59", "#8064A2", "#4BACC6"][idx % 5]
        svg_lines.append(f'<rect x="{legend_x}" y="{legend_y + idx * 18}" width="12" height="12" fill="{color}"/>')
        svg_lines.append(f'<text x="{legend_x + 18}" y="{legend_y + idx * 18 + 10}" font-size="12" fill="#333">{db}</text>')

    svg_lines.append('</svg>')

    target_path = out_path.with_suffix('.svg') if out_path.suffix.lower() != '.svg' else out_path
    target_path.write_text('\n'.join(svg_lines), encoding='utf-8')
    print(f"[info] 已生成 SVG 图表: {target_path}")
    return target_path


def write_json(json_path: Path, native_results: Dict[str, Dict[str, float]], django_results: Dict[str, Dict[str, float]]) -> None:
    payload = {
        "generated_at": datetime.utcnow().isoformat() + "Z",
        "native": native_results,
        "django": django_results,
        "environment": {
            "host": ENV.get("OG_DB_HOST"),
            "port": ENV.get("OG_DB_PORT"),
            "database": ENV.get("OG_DB_NAME"),
        },
    }
    json_path.write_text(json.dumps(payload, indent=2, ensure_ascii=False), encoding="utf-8")

def run_auto_compare() -> int:
    out_path = Path(__file__).with_name("performance_compare.pdf")
    json_path = Path(__file__).with_name("performance_compare.json")
    sqlite_path = str(Path(__file__).with_name("performance_compare.sqlite3"))

    try:
        with managed_database_containers() as containers:
            by_kind = {container.kind: container for container in containers}
            mandatory = by_kind.get("opengauss")
            if mandatory is None:
                raise RuntimeError("OpenGauss 容器启动失败，无法执行对比测试")

            pg_container = by_kind.get("postgres")

            args = argparse.Namespace(
                command="compare",
                compare_postgres=pg_container is not None,
                compare_sqlite=True,
                no_django=True,
                repeat=10,
                require_min_targets=1,
                out=str(out_path),
                save_json=str(json_path),
                og_name=mandatory.display_name,
                og_host=mandatory.host,
                og_port=mandatory.port,
                og_db=mandatory.database,
                og_user=mandatory.user,
                og_password=mandatory.password,
                pg_name=pg_container.display_name if pg_container else "PostgreSQL",
                pg_host=pg_container.host if pg_container else "127.0.0.1",
                pg_port=pg_container.port if pg_container else 5432,
                pg_db=pg_container.database if pg_container else "postgres",
                pg_user=pg_container.user if pg_container else "postgres",
                pg_password=pg_container.password if pg_container else "postgres",
                sqlite_name="SQLite",
                sqlite_path=sqlite_path,
            )

            run_multi_target(args)
            return 0
    except Exception as exc:
        print(f"[error] 自动对比失败: {exc}")
        return 1



# ---------------------------------------------------------------------------
# CLI integration
# ---------------------------------------------------------------------------


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Performance benchmarks for django-opengauss")
    subparsers = parser.add_subparsers(dest="command")

    compare_parser = subparsers.add_parser("compare", help="Compare multiple databases and export artefacts")
    add_compare_arguments(compare_parser)
    compare_parser.set_defaults(func=run_multi_target, command="compare")

    default_out = str(Path(__file__).with_name("performance_compare.pdf"))
    default_json = str(Path(__file__).with_name("performance_compare.json"))
    parser.set_defaults(
        func=run_multi_target,
        command="compare",
        out=default_out,
        save_json=default_json,
        compare_postgres=True,
        compare_sqlite=True,
        no_django=False,
        repeat=10,
        require_min_targets=1,
        og_name="OpenGauss",
        og_host=DEFAULT_CONN.get("host", "127.0.0.1"),
        og_port=int(DEFAULT_CONN.get("port", 5432)),
        og_db=DEFAULT_CONN.get("dbname", "postgres"),
        og_user=DEFAULT_CONN.get("user", "gaussdb"),
        og_password=DEFAULT_CONN.get("password", ""),
        pg_host=os.environ.get("PG_HOST", "127.0.0.1"),
        pg_port=int(os.environ.get("PG_PORT", 5432)),
        pg_db=os.environ.get("PG_DB", "postgres"),
        pg_user=os.environ.get("PG_USER", "postgres"),
        pg_password=os.environ.get("PG_PASSWORD", "postgres"),
        pg_name="PostgreSQL",
        sqlite_path=os.environ.get("SQLITE_PATH", ":memory:"),
        sqlite_name="SQLite",
    )
    return parser


def main(argv: Optional[Iterable[str]] = None) -> int:
    provided_args = list(argv) if argv is not None else sys.argv[1:]
    if not provided_args:
        return run_auto_compare()

    parser = build_parser()
    args = parser.parse_args(provided_args)

    if not hasattr(args, "func"):
        parser.error("No command handler registered")

    args.func(args)
    return 0


if __name__ == "__main__":
    sys.exit(main())
