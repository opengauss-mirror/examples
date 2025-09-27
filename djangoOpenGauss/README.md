# Django-OpenGauss

Django-OpenGauss 是针对 openGauss 数据库的 Django 后端驱动，基于 PostgreSQL 适配并提供 openGauss 专属能力。

## 核心特性

- 兼容 Django ORM 的完整功能，支持 openGauss 特性
- 内置连接池管理，便于生产环境部署
- 针对 openGauss 的类型映射与 SQL 优化
- 支持 Django 3.2 及以上版本、Python 3.8 及以上版本

## 安装

```bash
pip install django-opengauss
```

## 基础配置

在 Django 的 `settings.py` 中添加数据库配置：

```python
DATABASES = {
    'default': {
        'ENGINE': 'django_opengauss',
        'NAME': 'your_database_name',
        'USER': 'your_username',
        'PASSWORD': 'your_password',
        'HOST': 'localhost',
        'PORT': '5432',  # 默认端口
        'OPTIONS': {
            'application_name': 'Django-OpenGauss',
            'connect_timeout': 30,
            'sslmode': 'disable',  # 可选：require / verify-ca / verify-full
            'cursor_itersize': 2000,
        },
    }
}
```

### openGauss 额外选项

- `application_name`：在 openGauss 活动监控中的客户端名称
- `connect_timeout`：连接超时时间（秒）
- `sslmode`：SSL 连接策略
- `cursor_itersize`：服务端游标批量获取行数

## 环境要求

- Django >= 3.2
- Python >= 3.8
- psycopg2-binary >= 2.9
- 可访问的 openGauss 数据库实例

## 数据类型支持

驱动对常见字段提供 openGauss 优化映射，例如：

| Django 字段 | openGauss 类型 |
|-------------|----------------|
| AutoField   | serial         |
| BigAutoField| bigserial      |
| CharField   | varchar        |
| TextField   | text           |
| JSONField   | jsonb          |

更多字段映射可参考源码 `src/` 目录。

## 连接池

驱动内置连接池实现（位于 `src/pool.py`），提供基础的连接复用、健康检查能力，可通过数据库 `OPTIONS` 配置启用。

## 开发环境

```bash
git clone <repo-url>
python -m venv .venv
source .venv/bin/activate
pip install -e .
```

> 提示：源码现位于 `src/` 目录中，而对外发布的包名仍为 `django_opengauss`，保持对现有项目的兼容。

## 测试

运行测试前请确保数据库可用，可在项目根目录下直接执行 `tests/` 中的脚本：

```bash
python tests/functional_tests.py
python tests/coverage_tests.py
python tests/performance_tests.py
```

更多细节与参数说明请参考 `tests/README.md`。

## 打包

使用 setuptools 直接打包 `src/` 目录下的源码：

```bash
# 清理旧构建产物
rm -rf build dist django_opengauss.egg-info
# 生成 wheel 包（保留包名 django_opengauss）
python -m pip wheel . --no-deps --no-build-isolation -w dist
```

打包后，会在 `dist/` 目录看到 `django_opengauss-<version>.whl`，其中已包含 `src/django_opengauss/` 的全部代码。


## 许可证

MIT License

## 贡献

欢迎通过 Issue 或 Pull Request 参与贡献。如需重大改动，请先创建 Issue 讨论方案。
