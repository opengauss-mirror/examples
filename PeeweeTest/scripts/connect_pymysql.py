"""PyMySQL 直连 openGauss B 库基线验证脚本（Windows 本机运行）

用法（PowerShell）：
    $env:PEEWEE_TEST_HOST="127.0.0.1"
    $env:PEEWEE_TEST_PORT="3308"       # 使用实际的 dolphin_server_port
    $env:PEEWEE_TEST_USER="app_user"
    $env:PEEWEE_TEST_DB="b_database"
    $securePassword=Read-Host "请输入测试密码" -AsSecureString
    $env:PEEWEE_TEST_PASSWORD=[Net.NetworkCredential]::new(
        "", $securePassword).Password
    Remove-Variable securePassword
    python connect_pymysql.py
"""
import os
import sys

import pymysql

HOST = os.environ.get('PEEWEE_TEST_HOST', '127.0.0.1')
PORT = int(os.environ.get('PEEWEE_TEST_PORT', '3308'))
USER = os.environ.get('PEEWEE_TEST_USER', '')
PASSWORD = os.environ.get('PEEWEE_TEST_PASSWORD', '')
DATABASE = os.environ.get('PEEWEE_TEST_DB', 'b_database')
TABLE = 'test_pymysql_baseline'


def main():
    if not USER or not PASSWORD:
        print('请先设置环境变量 PEEWEE_TEST_USER / PEEWEE_TEST_PASSWORD')
        return 1
    try:
        conn = pymysql.connect(host=HOST, port=PORT, user=USER,
                               password=PASSWORD, database=DATABASE,
                               charset='utf8mb4')
    except Exception as exc:
        print(f'[失败] 连接 {HOST}:{PORT} 失败: {type(exc).__name__}: {exc}')
        print('检查：网络与监听地址、dolphin_server_port、pg_hba.conf、MySQL native 密码')
        return 1
    try:
        cur = conn.cursor()
        cur.execute('SELECT VERSION()')
        print('VERSION() =', cur.fetchone())
        cur.execute(f'DROP TABLE IF EXISTS `{TABLE}`')
        cur.execute(
            f'CREATE TABLE `{TABLE}`('
            'id INT AUTO_INCREMENT PRIMARY KEY, v VARCHAR(20))'
        )
        cur.execute(f'INSERT INTO `{TABLE}`(v) VALUES (%s)', ('hello',))
        print('lastrowid =', cur.lastrowid)
        conn.commit()
        cur.execute(f'SELECT COUNT(*) FROM `{TABLE}`')
        print('COUNT(*) =', cur.fetchone()[0])
        cur.execute(f'DROP TABLE `{TABLE}`')
        conn.commit()
        print('[+] PyMySQL 直连验证通过')
    except Exception as exc:
        print(f'[失败] SQL 执行出错: {type(exc).__name__}: {exc}')
        return 1
    finally:
        conn.close()
    return 0


if __name__ == '__main__':
    sys.exit(main())
