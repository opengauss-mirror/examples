"""
OpenGauss 连接池管理器
提供高效的数据库连接池管理、连接健康检查和性能监控
"""
import threading
import time
import logging
from queue import Queue, Empty, Full
from contextlib import contextmanager
from django.db import connection
from django.conf import settings
import psycopg2
from psycopg2 import pool


logger = logging.getLogger('django.db.backends.opengauss')


class OpenGaussConnectionPool:
    """
    OpenGauss 连接池管理器
    提供连接复用、健康检查、自动重连等功能
    """
    
    def __init__(self, database_settings, min_conn=1, max_conn=20, 
                 max_idle_time=300, health_check_interval=60):
        """
        初始化连接池
        
        Args:
            database_settings: Django 数据库配置
            min_conn: 最小连接数
            max_conn: 最大连接数
            max_idle_time: 最大空闲时间（秒）
            health_check_interval: 健康检查间隔（秒）
        """
        self.database_settings = database_settings
        self.min_conn = min_conn
        self.max_conn = max_conn
        self.max_idle_time = max_idle_time
        self.health_check_interval = health_check_interval
        
        # 连接池状态
        self._pool = None
        self._lock = threading.RLock()
        self._connection_count = 0
        self._created_connections = 0
        self._closed_connections = 0
        self._health_check_failures = 0
        
        # 性能统计
        self.stats = {
            'total_requests': 0,
            'active_connections': 0,
            'idle_connections': 0,
            'failed_connections': 0,
            'average_response_time': 0.0,
            'last_health_check': None,
        }
        
        # 健康检查线程
        self._health_check_thread = None
        self._stop_health_check = threading.Event()
        
        self._initialize_pool()
    
    def _initialize_pool(self):
        """初始化连接池"""
        try:
            # 构建连接参数
            conn_params = self._build_connection_params()
            
            # 创建 psycopg2 连接池
            self._pool = psycopg2.pool.ThreadedConnectionPool(
                minconn=self.min_conn,
                maxconn=self.max_conn,
                **conn_params
            )
            
            logger.info(f"OpenGauss 连接池初始化成功: min={self.min_conn}, max={self.max_conn}")
            
            # 启动健康检查线程
            self._start_health_check()
            
        except Exception as e:
            logger.error(f"连接池初始化失败: {e}")
            raise
    
    def _build_connection_params(self):
        """构建连接参数"""
        params = {
            'host': self.database_settings.get('HOST', 'localhost'),
            'port': self.database_settings.get('PORT', 5432),
            'database': self.database_settings.get('NAME'),
            'user': self.database_settings.get('USER'),
            'password': self.database_settings.get('PASSWORD'),
        }
        
        # 添加选项参数
        options = self.database_settings.get('OPTIONS', {})
        for key, value in options.items():
            if key in ['sslmode', 'connect_timeout', 'application_name']:
                params[key] = value
        
        # 默认设置
        params.setdefault('application_name', 'Django-OpenGauss-Pool')
        params.setdefault('connect_timeout', 10)
        
        return params
    
    @contextmanager
    def get_connection(self):
        """
        获取数据库连接的上下文管理器
        """
        conn = None
        start_time = time.time()
        
        try:
            # 更新统计
            self.stats['total_requests'] += 1
            
            # 从池中获取连接
            with self._lock:
                if self._pool is None:
                    raise RuntimeError("连接池未初始化或已关闭")
                
                conn = self._pool.getconn()
                self._connection_count += 1
                self.stats['active_connections'] += 1
            
            # 验证连接健康性
            if not self._is_connection_healthy(conn):
                self._pool.putconn(conn, close=True)
                conn = self._pool.getconn()
            
            logger.debug(f"获取连接成功, 当前活跃连接数: {self.stats['active_connections']}")
            
            yield conn
            
        except Exception as e:
            self.stats['failed_connections'] += 1
            logger.error(f"连接获取失败: {e}")
            
            if conn:
                # 连接有问题，关闭它
                try:
                    self._pool.putconn(conn, close=True)
                except Exception:
                    pass
                conn = None
            
            raise
            
        finally:
            # 归还连接到池
            if conn:
                try:
                    with self._lock:
                        self._pool.putconn(conn)
                        self._connection_count -= 1
                        self.stats['active_connections'] -= 1
                except Exception as e:
                    logger.error(f"连接归还失败: {e}")
            
            # 更新响应时间统计
            response_time = time.time() - start_time
            self.stats['average_response_time'] = (
                (self.stats['average_response_time'] * (self.stats['total_requests'] - 1) + response_time) /
                self.stats['total_requests']
            )
    
    def _is_connection_healthy(self, conn):
        """检查连接健康性"""
        try:
            with conn.cursor() as cursor:
                cursor.execute("SELECT 1")
                cursor.fetchone()
            return True
        except Exception:
            return False
    
    def _start_health_check(self):
        """启动健康检查线程"""
        if self._health_check_thread and self._health_check_thread.is_alive():
            return
        
        self._stop_health_check.clear()
        self._health_check_thread = threading.Thread(
            target=self._health_check_worker,
            daemon=True,
            name='OpenGauss-HealthCheck'
        )
        self._health_check_thread.start()
        logger.info("健康检查线程已启动")
    
    def _health_check_worker(self):
        """健康检查工作线程"""
        while not self._stop_health_check.wait(self.health_check_interval):
            try:
                self._perform_health_check()
            except Exception as e:
                logger.error(f"健康检查异常: {e}")
    
    def _perform_health_check(self):
        """执行健康检查"""
        start_time = time.time()
        
        try:
            with self.get_connection() as conn:
                with conn.cursor() as cursor:
                    cursor.execute("SELECT version(), current_timestamp")
                    result = cursor.fetchone()
                    
            logger.debug(f"健康检查通过, 数据库版本: {result[0]}")
            self.stats['last_health_check'] = time.time()
            
        except Exception as e:
            self._health_check_failures += 1
            logger.warning(f"健康检查失败 (第{self._health_check_failures}次): {e}")
            
            # 如果连续失败多次，尝试重建连接池
            if self._health_check_failures >= 3:
                logger.error("连续健康检查失败，尝试重建连接池")
                self._rebuild_pool()
        
        # 更新空闲连接统计
        with self._lock:
            if self._pool:
                # 粗略估算空闲连接数
                self.stats['idle_connections'] = max(0, self.min_conn - self.stats['active_connections'])
    
    def _rebuild_pool(self):
        """重建连接池"""
        try:
            with self._lock:
                if self._pool:
                    self._pool.closeall()
                
                self._initialize_pool()
                self._health_check_failures = 0
                logger.info("连接池重建成功")
                
        except Exception as e:
            logger.error(f"连接池重建失败: {e}")
    
    def get_pool_stats(self):
        """获取连接池统计信息"""
        with self._lock:
            return {
                **self.stats,
                'total_created_connections': self._created_connections,
                'total_closed_connections': self._closed_connections,
                'current_connection_count': self._connection_count,
                'health_check_failures': self._health_check_failures,
                'pool_size_min': self.min_conn,
                'pool_size_max': self.max_conn,
            }
    
    def close(self):
        """关闭连接池"""
        logger.info("开始关闭连接池...")
        
        # 停止健康检查
        if self._health_check_thread:
            self._stop_health_check.set()
            self._health_check_thread.join(timeout=5)
        
        # 关闭所有连接
        with self._lock:
            if self._pool:
                self._pool.closeall()
                self._pool = None
                
        logger.info("连接池已关闭")
    
    def __enter__(self):
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()


class PooledDatabaseWrapper:
    """
    使用连接池的数据库包装器
    集成到 Django 数据库后端中
    """
    
    _pool = None
    _pool_lock = threading.RLock()
    
    @classmethod
    def get_pool(cls, database_settings):
        """获取或创建连接池"""
        with cls._pool_lock:
            if cls._pool is None:
                # 从设置中获取连接池参数
                pool_options = database_settings.get('OPTIONS', {}).get('CONNECTION_POOL', {})
                
                cls._pool = OpenGaussConnectionPool(
                    database_settings=database_settings,
                    min_conn=pool_options.get('MIN_CONN', 1),
                    max_conn=pool_options.get('MAX_CONN', 20),
                    max_idle_time=pool_options.get('MAX_IDLE_TIME', 300),
                    health_check_interval=pool_options.get('HEALTH_CHECK_INTERVAL', 60),
                )
                
                logger.info("全局连接池已创建")
            
            return cls._pool
    
    @classmethod
    def close_pool(cls):
        """关闭全局连接池"""
        with cls._pool_lock:
            if cls._pool:
                cls._pool.close()
                cls._pool = None


def get_pooled_connection(database_settings):
    """
    获取池化连接的便捷函数
    可在 Django 数据库后端中使用
    """
    pool = PooledDatabaseWrapper.get_pool(database_settings)
    return pool.get_connection()


# Django 管理命令集成
class Command:
    """
    Django 管理命令：查看连接池状态
    python manage.py pool_status
    """
    
    help = '显示 OpenGauss 连接池状态'
    
    def handle(self, *args, **options):
        from django.conf import settings
        
        db_settings = settings.DATABASES['default']
        
        try:
            pool = PooledDatabaseWrapper.get_pool(db_settings)
            stats = pool.get_pool_stats()
            
            print("=== OpenGauss 连接池状态 ===")
            print(f"总请求数: {stats['total_requests']}")
            print(f"活跃连接数: {stats['active_connections']}")
            print(f"空闲连接数: {stats['idle_connections']}")
            print(f"失败连接数: {stats['failed_connections']}")
            print(f"平均响应时间: {stats['average_response_time']:.3f}秒")
            print(f"连接池大小: {stats['pool_size_min']}-{stats['pool_size_max']}")
            print(f"健康检查失败次数: {stats['health_check_failures']}")
            
            if stats['last_health_check']:
                last_check = time.time() - stats['last_health_check']
                print(f"上次健康检查: {last_check:.1f}秒前")
            
        except Exception as e:
            print(f"获取连接池状态失败: {e}")


# 连接池装饰器
def with_pooled_connection(func):
    """
    装饰器：为函数提供池化连接
    """
    def wrapper(*args, **kwargs):
        from django.conf import settings
        
        db_settings = settings.DATABASES['default']
        pool = PooledDatabaseWrapper.get_pool(db_settings)
        
        with pool.get_connection() as conn:
            return func(conn, *args, **kwargs)
    
    return wrapper


# 示例使用
@with_pooled_connection
def execute_query(conn, sql, params=None):
    """
    使用连接池执行查询的示例函数
    """
    with conn.cursor() as cursor:
        cursor.execute(sql, params or [])
        return cursor.fetchall()


# 在应用关闭时清理连接池
import atexit
atexit.register(PooledDatabaseWrapper.close_pool)