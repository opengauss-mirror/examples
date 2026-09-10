"""Reproduce two Dolphin MySQL-protocol compatibility issues.

This script uses PyMySQL directly, without Peewee, to isolate protocol
metadata and error mapping from ORM behavior. Connection settings come from
PEEWEE_TEST_* environment variables. The password is prompted when the
PEEWEE_TEST_PASSWORD variable is not set.
"""
import getpass
import os

import pymysql


METADATA_TABLE = 'diag_peewee_metadata'
ERROR_TABLE = 'diag_peewee_duplicate'


def connect():
    password = os.environ.get('PEEWEE_TEST_PASSWORD')
    if password is None:
        password = getpass.getpass('MySQL native password: ')
    return pymysql.connect(
        host=os.environ.get('PEEWEE_TEST_HOST', '127.0.0.1'),
        port=int(os.environ.get('PEEWEE_TEST_PORT', '3308')),
        user=os.environ.get('PEEWEE_TEST_USER', 'app_user'),
        password=password,
        database=os.environ.get('PEEWEE_TEST_DB', 'b_database'),
        charset='utf8mb4',
        autocommit=True,
    )


def reproduce_metadata(cursor):
    cursor.execute(f'DROP TABLE IF EXISTS `{METADATA_TABLE}`')
    cursor.execute(
        f'CREATE TABLE `{METADATA_TABLE}` ('
        'id INT AUTO_INCREMENT PRIMARY KEY, '
        'name VARCHAR(50) NOT NULL, '
        'price DECIMAL(10,2) DEFAULT 0.00)')

    cursor.execute(f'SHOW INDEX FROM `{METADATA_TABLE}`')
    index_rows = cursor.fetchall()
    cursor.execute(
        'SELECT column_name, data_type, column_type, column_key, extra '
        'FROM information_schema.columns '
        'WHERE table_schema=DATABASE() AND table_name=%s '
        'ORDER BY ordinal_position',
        (METADATA_TABLE,))
    column_rows = cursor.fetchall()

    columns_by_name = {row[0]: row for row in column_rows}
    primary_name = index_rows[0][2]
    id_extra = columns_by_name['id'][4]
    price_data_type = columns_by_name['price'][1]
    price_column_type = columns_by_name['price'][2]
    metadata_mismatch = (
        primary_name != 'PRIMARY' or
        id_extra != 'auto_increment' or
        price_data_type != 'decimal' or
        price_column_type != 'decimal(10,2)')

    print('\n[OBS-001] metadata introspection')
    print('SHOW INDEX primary key_name =', repr(primary_name))
    print('id column metadata =', repr(columns_by_name['id']))
    print('price column metadata =', repr(columns_by_name['price']))
    print('MySQL expected key_name = PRIMARY')
    print('MySQL expected extra = auto_increment')
    print('MySQL expected price type = decimal / decimal(10,2)')
    print('REPRODUCED =', metadata_mismatch)


def reproduce_duplicate_error(cursor):
    cursor.execute(f'DROP TABLE IF EXISTS `{ERROR_TABLE}`')
    cursor.execute(
        f'CREATE TABLE `{ERROR_TABLE}` ('
        'id INT AUTO_INCREMENT PRIMARY KEY, '
        'code VARCHAR(20) UNIQUE)')
    cursor.execute(
        f'INSERT INTO `{ERROR_TABLE}`(code) VALUES (%s)', ('dup',))

    print('\n[OBS-002] duplicate-key error mapping')
    try:
        cursor.execute(
            f'INSERT INTO `{ERROR_TABLE}`(code) VALUES (%s)', ('dup',))
    except pymysql.MySQLError as exc:
        code = exc.args[0] if exc.args else None
        print('exception class =', type(exc).__name__)
        print('error args =', repr(exc.args))
        print('MySQL expected = IntegrityError, code 1062, SQLSTATE 23000')
        print('REPRODUCED =',
              not isinstance(exc, pymysql.IntegrityError) or code != 1062)
    else:
        print('REPRODUCED = False (duplicate insert unexpectedly succeeded)')


def main():
    connection = connect()
    try:
        with connection.cursor() as cursor:
            reproduce_metadata(cursor)
            reproduce_duplicate_error(cursor)
    finally:
        with connection.cursor() as cursor:
            cursor.execute(f'DROP TABLE IF EXISTS `{METADATA_TABLE}`')
            cursor.execute(f'DROP TABLE IF EXISTS `{ERROR_TABLE}`')
            cursor.execute('DROP TABLE IF EXISTS `diag_obs002`')
        connection.close()
        print('\nDiagnostic tables cleaned up.')


if __name__ == '__main__':
    main()
