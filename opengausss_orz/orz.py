import inspect
import subprocess
import os
from prettytable import PrettyTable
import sys
import configparser

# 读取配置文件
config = configparser.ConfigParser()
current_folder_path = os.path.dirname(os.path.abspath(__file__))
ini_path = os.path.join(current_folder_path, "database.ini")
config.read(ini_path)
# 获取数据库参数
database = config.get('connect', 'database')
port = config.getint('connect', 'port')


def execute_database_command(command):
    """
    连接到OpenGauss数据库并执行命令.

    :param command: string, SQL命令
    :return:
        string, 执行命令的结果
    """
    # 构建连接命令
    connect_command = f'gsql -d {database} -p {port}'

    # 构建完整命令
    full_command = f'{connect_command} -c "{command};"'

    # 执行命令并获得输出
    output = subprocess.getoutput(full_command)
    return output


def execute_cluster_command(command):
    """
    执行集群相关操作。

    :param command:string, 命令
    :return:
        string, 执行命令的结果
    """
    output = subprocess.getoutput(command)
    return output


def pretty_print(str):
    """
    使用prettytable格式化输出结果.

    :param str: string, 执行命令获取的结果
    :return:
        string,输出结果
    """
    rows = []
    for line in str.splitlines():
        if "|" in line:
            line = "dummy_clo | " + line
            rows.append([val.strip() for val in line.split("|")[1:]])

    if len(rows) == 0:
        print("\033[31m" + str + "\033[0m")
        return

    table = PrettyTable()

    i = 1
    for row in rows:
        if i == 1:
            row = [s.upper() for s in row]
            table.field_names = row
        else:
            table.add_row(row)
        i = i + 1

    table.align = "l"  # 设置左对齐
    print(table)


def data_log(limitnum=None, show_help=False):
    """
    显示指定数据节点日志limitnum行

    :param limitnum: int，行数
    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "\n" + "命令说明：".rjust(30) + "查看数据节点日志的后limitnum行"

    if limitnum is None:
        # 如果限制数未提供，则提示错误信息
        return "Please provide a limit number."

    # 获取文件夹路径
    command = f"SELECT current_setting('log_directory');"
    folder_path = execute_cluster_command(command)
    # 获取文件夹中的所有文件
    files = os.listdir(folder_path)
    # 排序文件列表按照修改时间
    sorted_files = sorted(files, key=lambda x: os.path.getmtime(os.path.join(folder_path, x)))
    # 获取最新的文件名
    newest_file = sorted_files[-1]
    # 构建最新文件的完整路径
    newest_file_path = os.path.join(folder_path, newest_file)
    command = f"tail -{limitnum} {newest_file_path}"
    output = execute_cluster_command(command)
    pretty_print(output)


def wal_log(limitnum=None, show_help=False):
    """
    显示指定WAL日志limitnum行

    :param limitnum: int，行数
    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "\n"+"命令说明：".rjust(30)+"查看WAL节点日志的后limitnum行"

    if limitnum is None:
        # 如果限制数未提供，则提示错误信息
        return "Please provide a limit number."

    command = f"show data_directory;"
    output = execute_database_command(command)

    # 分割文字为行
    lines = output.split('\n')
    # 提取路径
    path = lines[2].strip()

    folder_path = os.path.join(path, "pg_xlog")
    # 获取文件夹中的所有文件
    files = os.listdir(folder_path)
    # 排序文件列表按照修改时间
    sorted_files = sorted(files, key=lambda x: os.path.getmtime(os.path.join(folder_path, x)))
    # 获取前limitnum个文件
    top_10_files = sorted_files[:int(limitnum)]
    # 显示前limitnum个文件
    for file in top_10_files:
        print(file)

def all_clusters_status(show_help=False):
    """
    查看所有集群节点的状态, 并通过prettyTable格式化输出.

    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "\n" + "命令说明：".rjust(30) + "查看所有集群节点的状态"

    command = f"gs_om -t status --detail"

    output = execute_cluster_command(command)
    pretty_print(output)


def cluster_status(nodename=None, show_help=False):
    """
    查看指定集群节点的状态, 并通过prettyTable格式化输出.

    :param nodename: string，节点名称
    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "nodename\n" + "命令说明：".rjust(30) + "查看指定集群节点的状态"

    if nodename is None:
        # 如果节点名未提供，则提示错误信息
        return "Please provide a nodename."

    command = f"gs_om -t status -h {nodename}"

    output = execute_cluster_command(command)
    pretty_print(output)


def start_cluster(show_help=False):
    """
    启动集群。

    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "\n" + "命令说明：".rjust(30) + "启动集群"

    command = f"gs_om -t start"
    output = execute_cluster_command(command)
    print(output)


def stop_cluster(show_help=False):
    """
    停止集群。

    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "\n" + "命令说明：".rjust(30) + "停止集群"

    command = f"gs_om -t stop"
    output = execute_cluster_command(command)
    print(output)


def switchover_cluster(show_help=False):
    """
    switchover切换集群

    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "\n" + "命令说明：".rjust(30) + "switchover切换主库"
    all_clusters_status()
    addr = input("请输入需要切换库的地址")
    command = f"gs_ctl switchover -D {addr}"
    output = execute_cluster_command(command)
    print(output)
    command = f"gs_om -t refreshconf"
    output = execute_cluster_command(command)
    print(output)
    all_clusters_status()


def failover_cluster(show_help=False):
    """
    failover切换集群

    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "\n" + "命令说明：".rjust(30) + "failover切换主库"
    all_clusters_status()
    addr = input("请输入需要切换库的地址")
    command = f"gs_ctl failover -D {addr}"
    output = execute_cluster_command(command)
    print(output)
    command = f"gs_om -t refreshconf"
    output = execute_cluster_command(command)
    print(output)
    all_clusters_status()


def table_statistics(tablename=None, show_help=False):
    """
    查看表的统计信息, 并通过prettyTable格式化输出.

    :param tablename: string，表名
    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "table_name\n" + "命令说明：".rjust(30) + "查询指定指定表的统计信息"

    if tablename is None:
        # 如果表名未提供，则提示错误信息
        return "Please provide a table name."

    command = f"ANALYZE verbose {tablename};" \
              f"SELECT relowner,relname,relkind,relpages,reltuples FROM pg_class WHERE tablename='{tablename}';" \
              f"SELECT * FROM pg_stats WHERE tablename='{tablename}';"

    output = execute_database_command(command)
    pretty_print(output)


def table_vacuum(tablename=None, show_help=False):
    """
       VACUUM回收表, 并通过prettyTable格式化输出.

       :param tablename: string，表名
       :param show_help: boolean, 是否显示帮助信息
       :return:
           若show_help为true，返回命令帮助
       """

    if show_help:
        return "table_name\n" + "命令说明：".rjust(30) + "回收指定表空间"

    if tablename is None:
        # 如果表名未提供，则提示错误信息
        return "Please provide a table name."

    command = f"VACUUM (VERBOSE ANALYZE) {tablename};"

    output = execute_database_command(command)
    pretty_print(output)


def object_ddl(objecttype=None, objectname=None, show_help=False):
    """
    查看对象的DDL语句, 并通过prettyTable格式化输出.

    :param objecttype:string, 数据库对象
    :param objectname:string，对象名称
    :param show_help:boolean, 是否显示帮助信息
    :return:
           若show_help为true，返回命令帮助
    """
    if show_help:
        return "objecttype\tobjectname\n" + "命令说明：".rjust(30) + "查看数据库对象的DDL语句"

    if objecttype is None or objectname is None:
        return "Please provide objecttype and objectname."

    command = f"SELECT * FROM pg_get_{objecttype}def('{objectname}'::regclass);"

    output = execute_database_command(command)
    pretty_print(output)


def all_users(show_help=False):
    """
    查看所有用户，并通过prettyTable格式化输出.

    :param show_help: boolean, 是否显示帮助信息
    :return:
           若show_help为true，返回命令帮助
    """
    if show_help:
        return "\n" + "命令说明：".rjust(30) + "查看所有用户"

    command = f"SELECT * FROM pg_user; "
    output = execute_database_command(command)
    pretty_print(output)


def indices_table(tablename=None, show_help=False):
    """
    查看指定表上的索引，并通过prettyTable格式化输出.

    :param tablename: string, 表名
    :param show_help: boolean, 是否显示帮助信息
    :return:
           若show_help为true，返回命令帮助
    """
    if show_help:
        return "tablename\n" + "命令说明：".rjust(30) + "查看指定表上的索引"

    if tablename is None:
        # 如果表名未提供，则提示错误信息
        return "Please provide a table name."

    command = f"SELECT * FROM pg_index WHERE indrelid=('{tablename}'::regclass);"
    output = execute_database_command(command)
    pretty_print(output)


def stat_activity(show_help=False):
    """
    显示当前所有会话，并通过prettyTable格式化输出.

    :param show_help: boolean, 是否显示帮助信息
    :return:
           若show_help为true，返回命令帮助
    """
    if show_help:
        return "\n" + "命令说明：".rjust(30) + "显示当前所有会话"

    command = f"SELECT * FROM pg_stat_activity;"
    output = execute_database_command(command)
    pretty_print(output)


def table_EXPLAIN(tablename=None, show_help=False):
    """
    查看表的执行计划，并通过prettyTable格式化输出.

    :param tablename: string, 表名
    :param show_help: boolean, 是否显示帮助信息
    :return:
           若show_help为true，返回命令帮助
    """
    if show_help:
        return "tablename\n" + "命令说明：".rjust(30) + "查看表的执行计划"

    if tablename is None:
        # 如果表名未提供，则提示错误信息
        return "Please provide a table name."

    command = f"EXPLAIN SELECT * FROM {tablename};"
    output = execute_database_command(command)
    pretty_print(output)


def longtime_sql(show_help=False):
    """
    查看长时间运行的SQL，并通过prettyTable格式化输出.

    :param show_help:boolean, 是否显示帮助信息
    :return:
           若show_help为true，返回命令帮助
    """
    if show_help:
        return "\n" + "命令说明：".rjust(30) + "查看长时间运行的SQL"

    command = f"SELECT datname,pid, usename, query_start, STATE, left(query, 40) as query,now()-query_start " \
              f"FROM pg_stat_activity " \
              f"WHERE STATE <>'idle';"
    output = execute_database_command(command)
    pretty_print(output)


def tablesize(limit=None, show_help=False):
    """
    查看膨胀高的表TOP limit(碎片)， limit由用户参数输入，并通过prettyTable格式化输出.

    :param limit:int,显示的数量
    :param show_help:show_help:boolean, 是否显示帮助信息
    :return:
           若show_help为true，返回命令帮助
    """
    if show_help:
        return "limit\n" + "命令说明：".rjust(30) + " 查看膨胀高的表TOP limit(碎片)， limit由用户参数输入"

    if limit is None:
        # 如果限制数未提供，则提示错误信息
        return "Please provide a limit number."

    command = f"SELECT relname AS TABLE_NAME," \
              f"pg_size_pretty(pg_relation_size(schemaname||'.'||relname)) AS table_size," \
              f"n_dead_tup,n_live_tup,(n_dead_tup * 100 / (n_live_tup + n_dead_tup))AS dead_tup_ratio " \
              f"FROM pg_stat_user_tables " \
              f"WHERE n_dead_tup<>0  " \
              f"ORDER BY 5  DESC LIMIT {limit};"
    output = execute_database_command(command)
    pretty_print(output)


def num_connection_db(show_help=False):
    """
    查看数据库用户连接数，并通过prettyTable格式化输出.

    :param show_help: boolean, 是否显示帮助信息
    :return:
           若show_help为true，返回命令帮助
    """
    if show_help:
        return "\n" + "命令说明：".rjust(30) + "查看数据库用户连接数"

    command = f"SELECT datname,usename,state,count(*) " \
              f"FROM pg_stat_activity " \
              f"GROUP BY datname,usename,state " \
              f"ORDER BY 1,2,3,4;"
    output = execute_database_command(command)
    pretty_print(output)


def kill_limittime_sql(limittime=None, show_help=False):
    """
    查杀超过limittime分钟的SQL， limittime由用户参数输入，并通过prettyTable格式化输出.

    :param limittime: int, 限制的时间
    :param show_help: boolean, 是否显示帮助信息
    :return:
           若show_help为true，返回命令帮助
    """
    if show_help:
        return "limittime\n" + "命令说明：".rjust(30) + "查杀超过limittime分钟的sql， limittime由用户参数输入"

    if limittime is None:
        # 如果限制数未提供，则提示错误信息
        return "Please provide a limit number."

    command = f"SELECT pg_terminate_backend(pid) " \
              f"FROM pg_stat_activity " \
              f"WHERE clock_timestamp()-query_start >  '{limittime} min';"
    output = execute_database_command(command)
    pretty_print(output)


def kill_limittime_transaction(limittime=None, show_help=False):
    """
    查杀超过limittime分钟的事务， limittime由用户参数输入，并通过prettyTable格式化输出.

    :param limittime: int, 限制的时间
    :param show_help: boolean, 是否显示帮助信息
    :return:
           若show_help为true，返回命令帮助
    """
    if show_help:
        return "limittime\n" + "命令说明：".rjust(30) + "查杀超过limittime分钟的事务， limittime由用户参数输入"

    if limittime is None:
        # 如果限制数未提供，则提示错误信息
        return "Please provide a limit number."

    command = f"SELECT pg_terminate_backend(pid) " \
              f"FROM pg_stat_activity " \
              f"WHERE clock_timestamp()-xact_start >  '{limittime} min';"
    output = execute_database_command(command)
    pretty_print(output)


def order_datatable(limitnum=None, show_help=False):
    """
    limitnum个数据表大小排序，limitnum由用户参数输入，并通过prettyTable格式化输出.

    :param limitnum:int, 限制个数
    :param show_help: boolean, 是否显示帮助信息
    :return:
           若show_help为true，返回命令帮助
    """
    if show_help:
        return "limitnum\n" + "命令说明：".rjust(30) + "数据表大小排序"

    if limitnum is None:
        # 如果限制数未提供，则提示错误信息
        return "Please provide a limit number."

    command = f"SELECT current_catalog AS datname, " \
              f"nsp.nspname, rel.relname,pg_total_relation_size(rel.oid) AS bytes, " \
              f"pg_relation_size(rel.oid) AS relsize," \
              f"pg_indexes_size(rel.oid) AS indexsize," \
              f"pg_total_relation_size(reltoastrelid) AS toastsize " \
              f"FROM pg_namespace nsp " \
              f"JOIN pg_class rel ON nsp.oid = rel.relnamespace " \
              f"WHERE nspname not in ('pg_catalog', 'information_schema', 'snapshot') and rel.relkind = 'r'	" \
              f"ORDER BY 4 DESC limit {limitnum};"
    output = execute_database_command(command)
    pretty_print(output)


def order_index(limitnum=None, show_help=False):
    """
        limitnum个索引大小排序，limitnum由用户参数输入，并通过prettyTable格式化输出.

        :param limitnum:int, 限制个数
        :param show_help: boolean, 是否显示帮助信息
        :return:
               若show_help为true，返回命令帮助
        """
    if show_help:
        return "limitnum\n" + "命令说明：".rjust(30) + "索引大小排序"

    if limitnum is None:
        # 如果限制数未提供，则提示错误信息
        return "Please provide a limit number."

    command = f"SELECT current_catalog AS datname, schemaname schema_name, relname table_name, indexrelname index_name, pg_table_size(indexrelid) as index_size " \
              f"FROM pg_stat_user_indexes WHERE schemaname not in('pg_catalog', 'information_schema', 'snapshot') " \
              f"ORDER BY 4 DESC limit {limitnum};"
    output = execute_database_command(command)
    pretty_print(output)


def database_size(show_help=False):
    """
    查询数据库大小，并通过prettyTable格式化输出.

    :param show_help:boolean, 是否显示帮助信息
    :return:
           若show_help为true，返回命令帮助
    """
    if show_help:
        return "\n" + "命令说明：".rjust(30) + "查询数据库大小"

    command = f"SELECT pg_database.datname AS 'database_name',	" \
              f"pg_size_pretty(pg_database_size (pg_database.datname)) AS size_in_mb " \
              f"FROM pg_database ORDER BY size_in_mb DESC;"
    output = execute_database_command(command)
    pretty_print(output)


def table_location(tablename=None, show_help=False):
    """
    查询指定表的存储位置，并通过prettyTable格式化输出.

    :param tablename: string, 表名
    :param show_help: boolean, 是否显示帮助信息
    :return:
           若show_help为true，返回命令帮助
    """
    if show_help:
        return "tablename\n" + "命令说明：".rjust(30) + "查询指定表的存储位置"

    if tablename is None:
        # 如果表名未提供，则提示错误信息
        return "Please provide a table name."

    command = f"SELECT pg_relation_filepath('{tablename}');"
    output = execute_database_command(command)
    pretty_print(output)


def schema_tablesize(show_help=False):
    """
    查看schema下各表数据量，并通过prettyTable格式化输出.

    :param show_help: boolean, 是否显示帮助信息
    :return:
           若show_help为true，返回命令帮助
    """
    if show_help:
        return "tablename\n" + "命令说明：".rjust(30) + "查询指定表的存储位置"
    command = f"SELECT relname,pg_size_pretty(pg_total_relation_size(relid)) " \
              f"FROM pg_stat_user_tables " \
              f"WHERE schemaname ='public' " \
              f"ORDER BY pg_total_relation_size(relid) DESC ;"
    output = execute_database_command(command)
    pretty_print(output)


def lockblock_information(show_help=False):
    """
    显示锁阻塞详情，并通过prettyTable格式化输出.

    :param show_help: boolean, 是否显示帮助信息
    :return:
           若show_help为true，返回命令帮助
    """
    if show_help:
        return "\n" + "命令说明：".rjust(30) + "锁阻塞详情"
    command = f"WITH tl AS (" \
              f"SELECT usename, granted, locktag, query_start, query " \
              f"FROM pg_locks l, pg_stat_activity AS a " \
              f"WHERE l.pid = a.pid	AND locktag " \
              f"IN(" \
              f"SELECT locktag	" \
              f"FROM pg_locks	" \
              f"WHERE granted = 'f'))" \
              f"SELECT ts.usename locker_user, ts.query_start locker_query_start, ts.granted locker_granted, " \
              f"ts.query locker_query, tt.query locked_query, tt.query_start locked_query_start, " \
              f"tt.granted locked_granted, tt.usename locked_user, " \
              f"extract(epoch FROM now() - tt.query_start) AS locked_times FROM (" \
              f"SELECT * FROM tl WHERE granted = 't') AS ts," \
              f"(SELECT * FROM tl WHERE granted = 'f') tt WHERE ts.locktag = tt.locktag " \
              f"ORDER BY 1;"
    output = execute_database_command(command)
    pretty_print(output)


def lockblock_source(show_help=False):
    """
    阻塞源统计，并通过prettyTable格式化输出.

    :param show_help: boolean, 是否显示帮助信息
    :return:
           若show_help为true，返回命令帮助
    """
    if show_help:
        return "\n" + "命令说明：".rjust(30) + "锁阻塞源统计"
    command = f"WITH tl AS (" \
              f"SELECT usename,	granted, locktag, query_start, query " \
              f"FROM pg_locks AS l, pg_stat_activity AS a " \
              f"WHERE l.pid = a.pid	and locktag IN (" \
              f"SELECT locktag	FROM pg_locks WHERE granted = 'f')) " \
              f"SELECT usename,	query_start, granted, query, count(query) " \
              f"COUNT FROM tl " \
              f"WHERE granted = 't'	GROUP BY usename, query_start, granted,	query ORDER BY 5 DESC;"
    output = execute_database_command(command)
    pretty_print(output)


def all_tables_information(show_help=False):
    """
    查询所有表信息：表名称、所属模式、创建时间、表空间等信息, 并通过prettyTable格式化输出.

    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "\n" + "命令说明：".rjust(30) + "查询所有表信息"
    command = f"SELECT * FROM pg_tables WHERE schemaname='public';"
    output = execute_database_command(command)
    pretty_print(output)


def table_information(tablename=None, show_help=False):
    """
    查询指定表信息, 并通过prettyTable格式化输出.

    :param tablename:  string, 表名
    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "table_name\n" + "命令说明：".rjust(30) + "查询指定表信息"

    if tablename is None:
        # 如果表名未提供，则提示错误信息
        return "Please provide a table name."

    command = f"SELECT * FROM pg_tables WHERE tablename='{tablename}' AND schemaname='public';"
    output = execute_database_command(command)
    pretty_print(output)


def table_structure(tablename=None, show_help=False):
    """
    查询指定表详细信息：列名、数据类型、默认值、是否允许为NULL以及列的注释信息, 并通过prettyTable格式化输出.

    :param tablename:  string, 表名
    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "table_name\n" + "命令说明：".rjust(30) + "查询指定表详细信息"

    if tablename is None:
        # 如果表名未提供，则提示错误信息
        return "Please provide a table name."

    command = f"SELECT " \
              f"a.attname AS column_name, pg_catalog.format_type(a.atttypid, a.atttypmod) " \
              f"AS " \
              f"data_type, " \
              f"(SELECT pg_catalog.pg_get_expr(d.adbin, d.adrelid) " \
              f"FROM " \
              f"pg_catalog.pg_attrdef d " \
              f"WHERE d.adrelid = a.attrelid AND d.adnum = a.attnum AND a.atthasdef) " \
              f"AS " \
              f"column_default, a.attnotnull AS not_null, " \
              f"(SELECT pg_catalog.col_description(a.attrelid, a.attnum)::text FROM pg_catalog.pg_class c WHERE c.oid = a.attrelid) " \
              f"AS " \
              f"column_comment FROM pg_catalog.pg_attribute a " \
              f"WHERE " \
              f"a.attrelid = 'public.{tablename}'::regclass AND a.attnum > 0 AND NOT a.attisdropped " \
              f"ORDER BY a.attnum;"
    output = execute_database_command(command)
    pretty_print(output)


def all_tables_stat(show_help=False):
    """
    查看所有表的统计信息：表的名称、扫描次数、插入次数、更新次数、删除次数、序列扫描次数, 并通过prettyTable格式化输出.

    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "\n" + "命令说明：".rjust(30) + "查看所有表的统计信息"
    command = f"SELECT * FROM pg_stat_all_tables;"
    output = execute_database_command(command)
    pretty_print(output)


#
def table_stat(tablename=None, show_help=False):
    """
    查看指定表的统计信息, 并通过prettyTable格式化输出.

    :param tablename:  string, 表名
    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "table_name\n" + "命令说明：".rjust(30) + "查看指定表的统计信息"

    if tablename is None:
        # 如果表名未提供，则提示错误信息
        return "Please provide a table name."

    command = f"SELECT * FROM pg_stat_all_tables WHERE relname = '{tablename}';"
    output = execute_database_command(command)
    pretty_print(output)


def table_stat_all(tablename=None, show_help=False):
    """
    查看指定表的详细统计信息（IO情况）：表名称、磁盘读取次数、磁盘写入次数、块读取次数、块写入次数、磁盘读取时间、磁盘写入时间,
    并通过prettyTable格式化输出.

    :param tablename:  string, 表名
    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "table_name\n" + "命令说明：".rjust(30) + "查看指定表的详细统计信息（IO情况）"

    if tablename is None:
        # 如果表名未提供，则提示错误信息
        return "Please provide a table name."

    command = f"SELECT * FROM pg_statio_user_tables WHERE relname = '{tablename}';"
    output = execute_database_command(command)
    pretty_print(output)


def all_tables_size(show_help=False):
    """
    查看所有表的大小, 并通过prettyTable格式化输出.

    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "\n" + "命令说明：".rjust(30) + "查看所有表的大小"
    command = f"SELECT relname,pg_size_pretty(pg_relation_size(relid)) as data_size " \
              f"FROM pg_catalog.pg_statio_all_tables " \
              f"ORDER BY pg_total_relation_size(relid) DESC;"
    output = execute_database_command(command)
    pretty_print(output)


#
def table_size(tablename=None, show_help=False):
    """
    查看指定表的大小, 并通过prettyTable格式化输出.

    :param tablename:  string, 表名
    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "table_name\n" + "命令说明：".rjust(30) + "查看指定表的大小"

    if tablename is None:
        # 如果表名未提供，则提示错误信息
        return "Please provide a table name."

    command = f"SELECT pg_size_pretty(pg_relation_size('{tablename}'));"
    output = execute_database_command(command)
    pretty_print(output)
    table_structure("user_tbl")


#
def all_tables_indexes(show_help=False):
    """
    查看所有表的的索引大小, 并通过prettyTable格式化输出.

    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "\n" + "命令说明：".rjust(30) + "查看所有表的的索引大小"
    command = f"SELECT relname,pg_size_pretty(pg_indexes_size(relid)) as index_size " \
              f"FROM pg_catalog.pg_statio_all_tables " \
              f"ORDER BY pg_total_indexes_size(relid) DESC;"
    output = execute_database_command(command)
    pretty_print(output)


# :
def table_indexes(tablename=None, show_help=False):
    """
    查看指定表的索引大小, 并通过prettyTable格式化输出.

    :param tablename:  string, 表名
    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "table_name\n" + "命令说明：".rjust(30) + "查看指定表的索引大小"

    if tablename is None:
        # 如果表名未提供，则提示错误信息
        return "Please provide a table name."

    command = f"SELECT pg_size_pretty(pg_indexes_size('{tablename}'));"
    output = execute_database_command(command)
    pretty_print(output)


#
def all_tables_def(show_help=False):
    """
    查看所有表的定义语句, 并通过prettyTable格式化输出.

    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "\n" + "命令说明：".rjust(30) + "查看所有表的定义语句"
    command = f"SELECT pg_get_tabledef(c.oid) " \
              f"FROM pg_catalog.pg_class c " \
              f"WHERE c.relkind='r' AND c.relname NOT LIKE 'pg_%' AND c.relname NOT LIKE 'sql_%' " \
              f"ORDER BY c.relname;"
    output = execute_database_command(command)
    pretty_print(output)


#
def table_def(tablename=None, show_help=False):
    """
    查看指定表的定义语句, 并通过prettyTable格式化输出.

    :param tablename:  string, 表名
    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "table_name\n" + "命令说明：".rjust(30) + "查看指定表的定义语句"

    if tablename is None:
        # 如果表名未提供，则提示错误信息
        return "Please provide a table name."

    command = f"\\d {tablename};"
    output = execute_database_command(command)
    pretty_print(output)


#
def all_indexes_status(show_help=False):
    """
    查看所有索引的状态, 并通过prettyTable格式化输出.

    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "\n" + "命令说明：".rjust(30) + "查看所有索引的状态"
    command = f"SELECT * FROM pg_stat_user_indexes;"
    output = execute_database_command(command)
    pretty_print(output)


#
def table_indexes_status(tablename=None, show_help=False):
    """
    查看指定表的索引状态：索引名称、所属表的名称、扫描次数、读取次数、写入次数、块读取次数、块写入次数, 并通过prettyTable格式化输出.

    :param tablename:  string, 表名
    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "table_name\n" + "命令说明：".rjust(30) + "查看指定表的索引状态"

    if tablename is None:
        # 如果表名未提供，则提示错误信息
        return "Please provide a table name."

    command = f"SELECT * FROM pg_stat_user_indexes WHERE relname = '{tablename}';"
    output = execute_database_command(command)
    pretty_print(output)


#
def table_indexes_def(tablename=None, show_help=False):
    """
    查看指定表的所有索引定义语句, 并通过prettyTable格式化输出.

    :param tablename:  string, 表名
    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "table_name\n" + "命令说明：".rjust(30) + "查看指定表的所有索引定义语句"

    if tablename is None:
        # 如果表名未提供，则提示错误信息
        return "Please provide a table name."

    command = f"SELECT pg_get_indexdef(indexrelid) FROM pg_index WHERE indrelid = '{tablename}'::regclass;"
    output = execute_database_command(command)
    pretty_print(output)


#
def index_def(indexname=None, show_help=False):
    """
    查看指定索引的定义语句, 并通过prettyTable格式化输出.

    :param indexname: string, 索引名
    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "index_name\n" + "命令说明：".rjust(30) + "查看指定索引的定义语句"

    if indexname is None:
        # 如果索引名未提供，则提示错误信息
        return "Please provide an index name."

    command = f"SELECT pg_get_indexdef('{indexname}');"
    output = execute_database_command(command)
    pretty_print(output)


#
def database_status(database_name=None, show_help=False):
    """
    查看指定数据库的状态：数据库名称、连接数、提交次数、回滚次数、磁盘读取次数、磁盘写入次数、磁盘读取时间、磁盘写入时间,
    并通过prettyTable格式化输出.

    :param database_name: string, 数据库名
    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "database_name\n" + "命令说明：".rjust(30) + "查看指定数据库的状态"

    if database_name is None:
        # 如果数据库名未提供，则提示错误信息
        return "Please provide a database name."

    command = f"FROM pg_stat_database WHERE datname = '{database_name}';"
    output = execute_database_command(command)
    pretty_print(output)


#
def database_conflicts(database_name=None, show_help=False):
    """
    查看指定数据库中发生的冲突信息：数据库名称、锁冲突次数、唯一约束冲突次数、外键约束冲突次数, 并通过prettyTable格式化输出.

    :param database_name: string, 数据库名
    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "database_name\n" + "命令说明：".rjust(30) + "查看指定数据库中发生的冲突信息"

    if database_name is None:
        # 如果数据库名未提供，则提示错误信息
        return "Please provide a database name."

    command = f"SELECT * FROM pg_stat_database_conflicts WHERE datname = '{database_name}';"
    output = execute_database_command(command)
    pretty_print(output)


#
def parameter(parameter_name=None, show_help=False):
    """
    查看指定参数的值, 并通过prettyTable格式化输出.

    :param parameter_name: string, 参数名
    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "parameter_name\n" + "命令说明：".rjust(30) + "查看指定参数的值"

    if parameter_name is None:
        # 如果参数名未提供，则提示错误信息
        return "Please provide a parameter name."

    command = f"SHOW {parameter_name};"
    output = execute_database_command(command)
    pretty_print(output)


def all_parameters(show_help=False):
    """
    查看参数的值, 并通过prettyTable格式化输出.
    :param show_help:
    :return:
    """
    if show_help:
        return "parameter_name\n" + "命令说明：".rjust(30) + "查看all参数的值"
    command = f"SHOW ALL;"
    output = execute_database_command(command)
    pretty_print(output)


#
def parameter_workmem(show_help=False):
    """
    查看工作内存池的参数值, 并通过prettyTable格式化输出.

    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "\n" + "命令说明：".rjust(30) + "查看工作内存池的参数值"
    command = f"SELECT * FROM pg_settings WHERE name LIKE '%work_mem%';"
    output = execute_database_command(command)
    pretty_print(output)


#
def parameter_log(show_help=False):
    """
    查看日志的参数值, 并通过prettyTable格式化输出.

    :param show_help: boolean, 是否显示帮助信息
    :return:
        若show_help为true，返回命令帮助
    """
    if show_help:
        return "\n" + "命令说明：".rjust(30) + "查看日志的参数值"
    command = f"SELECT * FROM pg_settings WHERE name LIKE '%log%';"
    output = execute_database_command(command)
    pretty_print(output)


all_options = {
    "all_tables_information": all_tables_information,
    "all_tables_stat": all_tables_stat,
    "all_tables_size": all_tables_size,
    "all_tables_indexes": all_tables_indexes,
    "all_tables_def": all_tables_def,
    "all_indexes_status": all_indexes_status,
    "parameter_workmem": parameter_workmem,
    "parameter_log": parameter_log,
    "table_information": table_information,
    "table_structure": table_structure,
    "table_stat": table_stat,
    "table_stat_all": table_stat_all,
    "table_size": table_size,
    "table_indexes": table_indexes,
    "table_def": table_def,
    "table_indexes_status": table_indexes_status,
    "table_indexes_def": table_indexes_def,
    "index_def": index_def,
    "database_status": database_status,
    "database_conflicts": database_conflicts,
    "parameter": parameter,
    "data_log": data_log,
    "wal_log": wal_log,
    "all_clusters_status": all_clusters_status,
    "cluster_status": cluster_status,
    "start_cluster": start_cluster,
    "stop_cluster": stop_cluster,
    "switchover_cluster": switchover_cluster,
    "failover_cluster": failover_cluster,
    "table_vacuum": table_vacuum,
    "object_ddl": object_ddl,
    "all_users": all_users,
    "indices_table": indices_table,
    "stat_activity": stat_activity,
    "table_EXPLAIN": table_EXPLAIN,
    "longtime_sql": longtime_sql,
    "tablesize": tablesize,
    "num_connection_db": num_connection_db,
    "kill_limittime_sql": kill_limittime_sql,
    "kill_limittime_transaction": kill_limittime_transaction,
    "order_datatable": order_datatable,
    "order_index": order_index,
    "database_size": database_size,
    "table_location": table_location,
    "schema_tablesize": schema_tablesize,
    "lockblock_information": lockblock_information,
    "lockblock_source": lockblock_source
}


def show_one_help(command):
    """
    输出单个命令的帮助信息

    :param command: string, 命令
    :return:
        若command非已有命令，则返回”Wrong Command"
    """
    function = all_options.get(command)

    if function is None:
        print("\033[31m" + "Wrong Command" + "\033[0m")
        return

    str = command.ljust(25) + "命令格式：orz " + f"{command} " + function(show_help=True) + "\n"
    print(str)


def show_all_help():
    """
    输出所有命令的帮助信息

    :return:
        无
    """
    print("FUNCTIONS".ljust(25) + "NOTES\n")
    for command, function in all_options.items():
        show_one_help(command)


#
def main():
    """
    main函数，根据命令行参数调用不同的函数.

    :return:
    """
    if len(sys.argv) == 1:
        """
        只输入orz，输出所有命令帮助
        """
        show_all_help()
        return

    if len(sys.argv) == 3 and sys.argv[2] == "help":
        """
        获取某个命令帮助
        """
        show_one_help(sys.argv[1])
        return

    """
    输入两个及以上参数情况： orz command [para1 [para2 para3]]
    判断函数是否存在：
    否：
        输出”Wrong Command“
    是：
        判断输入参数与命令函数所需参数是否相同：
            是：执行命令函数
            否：输出”wrong number of parameters“
    """
    command = all_options.get(sys.argv[1])
    if command is None:
        print("\033[31m" + "Wrong Command" + "\033[0m")
        return

    # 获取命令行参数列表
    args = []
    for arg in sys.argv[2:]:
        args.append(arg)

    # 获取要执行函数的所需参数个数
    num_params = len(inspect.signature(command).parameters) - 1

    if num_params == len(args):
        command(*args)
    else:
        print("\033[31m" + "Wrong Number Of Parameters" + "\033[0m")
    return


if __name__ == '__main__':
    main()


