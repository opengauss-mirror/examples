# whyhow_api/utilities/routers.py
"""Routers utilities (Postgres/SQLAlchemy)."""

from __future__ import annotations
import logging
from typing import Any, Mapping

from fastapi import Query
import sqlalchemy as sa
from sqlalchemy.sql import Select

logger = logging.getLogger(__name__)


def clean_url(url: str) -> str:
    """Clean URL (去掉尾部斜杠但保留根路径)."""
    return url.rstrip("/") if len(url) > 1 else url


def order_query(
    order: str = Query(default="descending", enum=["ascending", "descending"])
):
    """
    返回一个“排序方向函数”，供具体列使用：
        dir_fn = order_query(order)
        stmt = select(tbl).order_by(dir_fn(tbl.c.created_at), dir_fn(tbl.c.id))
    """
    def _dir(column: sa.ColumnElement) -> sa.sql.elements.UnaryExpression:
        return column.asc() if order == "ascending" else column.desc()
    return _dir


def apply_pagination(stmt: Select, *, skip: int | None, limit: int | None) -> Select:
    """
    为 SQLAlchemy Select 应用分页；limit=-1 表示不限制。
    """
    if skip is not None:
        stmt = stmt.offset(max(skip, 0))
    if limit is not None and limit >= 0:
        stmt = stmt.limit(limit)
    return stmt


def apply_filters(stmt: Select, table: sa.Table, filters: Mapping[str, Any] | None) -> Select:
    """
    将简单的等值条件拼到 where 中：
        stmt = apply_filters(select(tbl), tbl, {"created_by": user_id, "workspace": ws_id})
    """
    if not filters:
        return stmt
    conds: list[sa.ColumnElement[bool]] = []
    for name, val in filters.items():
        col = getattr(table.c, name, None)
        if col is not None and val is not None:
            conds.append(col == val)
    if conds:
        stmt = stmt.where(sa.and_(*conds))
    return stmt
