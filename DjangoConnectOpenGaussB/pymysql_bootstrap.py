"""
Bootstrap PyMySQL as MySQLdb so Django's mysql backend can talk to
openGauss dolphin MySQL protocol port.
"""
import pymysql

pymysql.install_as_MySQLdb()
