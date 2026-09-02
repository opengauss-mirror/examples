#!/usr/bin/env python
"""CRUD demo for Django ORM against openGauss B-mode MySQL protocol."""
from __future__ import annotations

import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "config.settings")
os.environ.setdefault("OG_MYSQL_HOST", "127.0.0.1")
os.environ.setdefault("OG_MYSQL_PORT", "3308")
os.environ.setdefault("OG_MYSQL_DB", "mysql_test_db")
os.environ.setdefault("OG_MYSQL_USER", "omm")
# OG_MYSQL_PASSWORD must be provided by the environment (do not hardcode secrets)

if not os.environ.get("OG_MYSQL_PASSWORD"):
    print("ERROR: please export OG_MYSQL_PASSWORD before running this demo", file=sys.stderr)
    raise SystemExit(2)

import pymysql_bootstrap  # noqa: F401
import django

django.setup()

from demo_app.models import User


def main():
    created_ids: list[int] = []
    try:
        print("=== 查询所有用户 ===")
        for row in User.objects.all().order_by("id"):
            print(row)

        print("=== 新增用户 ===")
        created = User(name="zhaoliu", age=18)
        created.save()
        created_ids.append(created.pk)
        print(f"新增 id={created.pk}")

        print("=== 特殊字符读写 ===")
        special = User(name="O'Brien\\test", age=99)
        special.save()
        created_ids.append(special.pk)
        loaded = User.objects.get(pk=special.pk)
        if loaded.name != special.name:
            raise RuntimeError(
                f"特殊字符读写失败: expected {special.name!r}, got {loaded.name!r}"
            )
        print(f"特殊字符 OK: id={special.pk} name={loaded.name!r}")

        print("=== 模糊查询用户名包含 zhao 的用户 ===")
        zhao_users = list(User.objects.filter(name__icontains="zhao").order_by("id"))
        if not zhao_users:
            raise RuntimeError("icontains 查询返回空集，兼容性可能存在问题")
        if created.pk not in {row.pk for row in zhao_users}:
            raise RuntimeError(f"本次创建 id={created.pk} 未出现在 icontains 结果中")
        for row in zhao_users:
            print(row)

        print("=== 更新用户信息 ===")
        created.name = "zhaoliuliuliu"
        created.age = 28
        created.save()
        updated = User.objects.get(pk=created.pk)
        if updated.name != "zhaoliuliuliu" or updated.age != 28:
            raise RuntimeError(
                f"更新断言失败: id={created.pk} name={updated.name!r} age={updated.age}"
            )
        print(f"更新行: id={created.pk}")

        print("=== 根据ID查询用户 ===")
        print(User.objects.get(pk=created.pk))

        print("=== 删除用户 ===")
        deleted, _ = User.objects.filter(pk=created.pk).delete()
        if deleted != 1:
            raise RuntimeError(f"删除断言失败: expected 1 row, got {deleted}")
        if User.objects.filter(pk=created.pk).exists():
            raise RuntimeError(f"删除后 id={created.pk} 仍存在")
        created_ids.remove(created.pk)
        print(f"删除行数：{deleted}")

        print("=== 查询所有用户 ===")
        for row in User.objects.all().order_by("id"):
            print(row)

        print("DEMO_OK")
    finally:
        if created_ids:
            User.objects.filter(pk__in=created_ids).delete()


if __name__ == "__main__":
    main()
