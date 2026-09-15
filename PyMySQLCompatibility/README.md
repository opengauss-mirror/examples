# PyMySQL x openGauss(dolphin) 兼容性测试

2025 开源之夏课题：PyMySQL 兼容 openGauss B兼容数据库。

## 环境
- openGauss 7.0.0-RC3 + dolphin 插件
- PyMySQL 1.2.0 / Python 3.9 / MySQL 协议端口 3308

## 运行测试
pip install pymysql
python test_pymysql_opengauss.py

## 测试覆盖
18 个测试项：连接认证、CRUD、事务、游标、二进制、日期时间、Unicode、重连稳定性等。

详见 self_test_report.md