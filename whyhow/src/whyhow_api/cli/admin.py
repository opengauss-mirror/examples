# whyhow_api/cli/admin.py
"""Admin CLI for managing users and the database (Postgres/openGauss)."""

import asyncio
import json
import secrets
import string
from typing import Sequence

import typer
import sqlalchemy as sa
from sqlalchemy.ext.asyncio import create_async_engine, AsyncEngine

from whyhow_api.config import Settings
from whyhow_api.services.crud.user_pg import users as T_USERS
from whyhow_api.services.crud.workspace_pg import workspaces as T_WORKSPACES
from whyhow_api.services.crud.document_pg import documents as T_DOCUMENTS
from whyhow_api.services.crud.chunks_pg import chunks as T_CHUNKS
from whyhow_api.services.crud.graph_pg import graphs as T_GRAPHS
from whyhow_api.services.crud.node_pg import nodes as T_NODES
from whyhow_api.services.crud.triple_pg import triples as T_TRIPLES
from whyhow_api.services.crud.schema_pg import schemas as T_SCHEMAS
from whyhow_api.services.crud.rule_pg import rules as T_RULES
from whyhow_api.services.crud.task_pg import tasks as T_TASKS

app = typer.Typer(help="WhyHow Admin CLI (Postgres/openGauss)")

def _tables() -> Sequence[sa.Table]:
    return [
        T_USERS,
        T_WORKSPACES,
        T_DOCUMENTS,
        T_CHUNKS,
        T_GRAPHS,
        T_NODES,
        T_TRIPLES,
        T_SCHEMAS,
        T_RULES,
        T_TASKS,
    ]

def _gen_api_key(length: int = 40) -> str:
    chars = string.ascii_letters + string.digits
    return "".join(secrets.choice(chars) for _ in range(length))

async def _run_ddl(engine: AsyncEngine) -> None:
    async with engine.begin() as conn:
        for tbl in _tables():
            await conn.run_sync(tbl.create, checkfirst=True)

@app.command("init-db")
def init_db() -> None:
    """Create tables if not exist (idempotent)."""
    settings = Settings()
    engine = create_async_engine(settings.opengauss.dsn, echo=settings.opengauss.echo_sql, pool_pre_ping=True)

    asyncio.run(_run_ddl(engine))
    typer.echo("Tables ensured (IF NOT EXISTS).")

@app.command("create-user")
def create_user(
    email: str = typer.Option(..., help="User email (unique)."),
    username: str = typer.Option("user", help="Username"),
    firstname: str = typer.Option("first", help="First name"),
    lastname: str = typer.Option("last", help="Last name"),
    openai_key: str = typer.Option("", help="(Optional) BYO OpenAI API key to store in providers JSON."),
) -> None:
    """Insert a user row and generate WhyHow API key."""
    settings = Settings()
    engine = create_async_engine(settings.opengauss.dsn, echo=settings.opengauss.echo_sql, pool_pre_ping=True)

    providers_json = None
    if openai_key:
        providers_json = json.dumps({
            "providers": [
                {
                    "type": "llm",
                    "value": "byo-openai",
                    "api_key": openai_key,
                    "metadata": {
                        "byo-openai": {
                            "language_model_name": "gpt-4o",
                            "embedding_name": "text-embedding-3-small",
                        }
                    },
                }
            ]
        }, ensure_ascii=False)

    async def _insert() -> None:
        async with engine.begin() as conn:
            for tbl in _tables():
                await conn.run_sync(tbl.create, checkfirst=True)
            api_key = _gen_api_key()
            stmt = sa.insert(T_USERS).values(
                email=email,
                username=username,
                firstname=firstname,
                lastname=lastname,
                api_key=api_key,
                active=True,
                providers=providers_json,
            ).returning(T_USERS.c.id, T_USERS.c.api_key)
            row = (await conn.execute(stmt)).mappings().one()
            typer.echo(f"✅ User created. id={row['id']}  whyhow_api_key={row['api_key']}")

    asyncio.run(_insert())

if __name__ == "__main__":
    app()
