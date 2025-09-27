"""
OpenGauss 数据库创建工具
处理测试数据库的创建和销毁
"""
from django.db.backends.postgresql import creation as postgresql_creation
from django.db.backends.utils import truncate_name
import sys


class DatabaseCreation(postgresql_creation.DatabaseCreation):
    """
    OpenGauss 数据库创建类
    处理数据库和测试数据库的创建、克隆和销毁
    """
    
    @property
    def _nodb_connection(self):
        """
        返回一个不连接到特定数据库的连接
        用于创建和删除数据库
        """
        from django.db import connections
        
        nodb_connection = connections.create_connection(self.connection.alias)
        try:
            nodb_connection.settings_dict = self.connection.settings_dict.copy()
            nodb_connection.settings_dict['NAME'] = 'postgres'  # 连接到默认的postgres数据库
            nodb_connection.features = self.connection.features
            nodb_connection.ops = self.connection.ops
            nodb_connection.creation = self
            nodb_connection.introspection = self.connection.introspection
            nodb_connection.validation = self.connection.validation
        except AttributeError:
            # 如果某些属性不存在，使用父类的方法
            pass
        
        return nodb_connection
    
    def _create_test_db(self, verbosity, autoclobber, keepdb=False):
        """
        创建测试数据库
        """
        # 使用父类的实现，它已经处理了大部分逻辑
        try:
            return super()._create_test_db(verbosity, autoclobber, keepdb)
        except Exception as e:
            # 如果父类方法失败，尝试简化的创建方法
            test_database_name = self._get_test_db_name()
            
            if keepdb:
                return test_database_name
            
            if verbosity >= 1:
                print(f"Creating test database '{test_database_name}'...")
            
            with self._nodb_connection.cursor() as cursor:
                try:
                    # 尝试创建数据库
                    cursor.execute(
                        "CREATE DATABASE %s" % self.connection.ops.quote_name(test_database_name)
                    )
                except Exception as create_error:
                    if verbosity >= 1:
                        print("Got an error creating the test database: %s" % create_error)
                    if not autoclobber:
                        confirm = input(
                            "Type 'yes' if you would like to try deleting the test "
                            "database '%s', or 'no' to cancel: " % test_database_name
                        )
                    if autoclobber or confirm == 'yes':
                        try:
                            if verbosity >= 1:
                                print("Destroying old test database for alias %s..." % (
                                    self._get_database_display_str(verbosity, test_database_name),
                                ))
                            cursor.execute(
                                "DROP DATABASE %s" % self.connection.ops.quote_name(test_database_name)
                            )
                            # 重新创建数据库
                            cursor.execute(
                                "CREATE DATABASE %s" % self.connection.ops.quote_name(test_database_name)
                            )
                        except Exception as recreate_error:
                            sys.stderr.write(
                                "Got an error recreating the test database: %s\n" % recreate_error
                            )
                            sys.exit(2)
                    else:
                        print("Tests cancelled.")
                        sys.exit(1)
            
            return test_database_name
    
    def _clone_test_db(self, suffix, verbosity, keepdb=False):
        """
        克隆测试数据库
        OpenGauss 支持使用 CREATE DATABASE ... TEMPLATE 语法
        """
        source_database_name = self.connection.settings_dict['NAME']
        target_database_name = self.get_test_db_clone_settings(suffix)['NAME']
        
        if keepdb:
            return target_database_name
        
        if verbosity >= 1:
            print("Cloning test database for alias %s..." % (
                self._get_database_display_str(verbosity, target_database_name),
            ))
        
        # 使用 WITH TEMPLATE 克隆数据库
        with self._nodb_connection.cursor() as cursor:
            try:
                # 确保没有连接到源数据库
                cursor.execute(
                    "SELECT pg_terminate_backend(pg_stat_activity.pid) "
                    "FROM pg_stat_activity "
                    "WHERE pg_stat_activity.datname = %s "
                    "AND pid <> pg_backend_pid()",
                    [source_database_name]
                )
                
                # 克隆数据库
                cursor.execute(
                    "CREATE DATABASE %s WITH TEMPLATE %s" % (
                        self.connection.ops.quote_name(target_database_name),
                        self.connection.ops.quote_name(source_database_name),
                    )
                )
            except Exception as e:
                if verbosity >= 1:
                    print("Got an error cloning the test database: %s" % e)
                sys.exit(2)
        
        return target_database_name
    
    def _destroy_test_db(self, test_database_name, verbosity):
        """
        销毁测试数据库
        """
        if verbosity >= 1:
            print("Destroying test database for alias %s..." % (
                self._get_database_display_str(verbosity, test_database_name),
            ))
        
        with self._nodb_connection.cursor() as cursor:
            # 强制断开所有连接
            cursor.execute(
                "SELECT pg_terminate_backend(pg_stat_activity.pid) "
                "FROM pg_stat_activity "
                "WHERE pg_stat_activity.datname = %s "
                "AND pid <> pg_backend_pid()",
                [test_database_name]
            )
            
            # 删除数据库
            cursor.execute(
                "DROP DATABASE %s" % self.connection.ops.quote_name(test_database_name)
            )
    
    def sql_table_creation_suffix(self):
        """
        返回创建表时的后缀 SQL
        可用于设置表空间等
        """
        suffix = []
        
        # 表空间设置
        if self.connection.settings_dict.get('TEST', {}).get('TABLESPACE'):
            suffix.append(
                "TABLESPACE %s" % self.connection.ops.quote_name(
                    self.connection.settings_dict['TEST']['TABLESPACE']
                )
            )
        
        # 其他 OpenGauss 特定设置
        test_settings = self.connection.settings_dict.get('TEST', {})
        if test_settings.get('STORAGE_PARAMETERS'):
            storage_params = test_settings['STORAGE_PARAMETERS']
            param_sql = ', '.join(
                '%s = %s' % (k, v) for k, v in storage_params.items()
            )
            suffix.append("WITH (%s)" % param_sql)
        
        return ' '.join(suffix) if suffix else ''