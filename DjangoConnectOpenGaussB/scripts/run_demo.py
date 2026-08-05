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
    print("=== 查询所有用户 ===")
    for u in User.objects.all().order_by("id"):
        print(u)

    print("=== 新增用户 ===")
    u = User(name="zhaoliu", age=18)
    u.save()
    print(f"新增 id={u.id}")

    print("=== 模糊查询用户名包含 zhao 的用户 ===")
    zhao_users = list(User.objects.filter(name__icontains="zhao").order_by("id"))
    for x in zhao_users:
        print(x)

    if zhao_users:
        target = zhao_users[0]
        target.name = "zhaoliuliuliu"
        target.age = 28
        print("=== 更新用户信息 ===")
        target.save()
        print(f"更新行: id={target.id}")

        print("=== 根据ID查询用户 ===")
        print(User.objects.get(id=target.id))

        print("=== 删除用户 ===")
        deleted, _ = User.objects.filter(id=target.id).delete()
        print(f"删除行数：{deleted}")

    print("=== 查询所有用户 ===")
    for u in User.objects.all().order_by("id"):
        print(u)

    print("DEMO_OK")


if __name__ == "__main__":
    main()
