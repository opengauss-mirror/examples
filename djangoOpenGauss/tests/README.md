# Django OpenGauss 测试说明

本目录包含三类测试，用于验证驱动在不同维度的质量与表现。

## 1. 功能测试 `functional_tests.py`
- **目的**：集成校验 OpenGauss Django 后端的 ORM、Schema、连接池等核心功能。
- **运行**：`python functional_tests.py`
- **输出**：终端展示详细日志，同时产出 `functional_test_results.json` 记录执行摘要及各用例状态。

## 2. 覆盖率测试 `coverage_tests.py`
- **目的**：聚合关键模块的单元/集成测试，便于统计代码覆盖率。
- **运行**：`python coverage_tests.py`
- **输出**：遵循 pytest/coverage 配置，可结合 `coverage run` 与 `coverage html` 生成报告。

## 3. 性能对比测试 `performance_tests.py`
- **目的**：对比 OpenGauss、PostgreSQL、SQLite 等数据库在批量 CRUD、事务、统计等场景下的性能差异。
- **运行**：`python performance_tests.py`
- **输出**：生成性能图表（PNG/SVG）与 JSON 数据，默认存放于 `tests/` 目录。

> 运行性能测试前请确保相关数据库/容器已经启动，并根据实际环境调整连接参数。
