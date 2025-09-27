"""
OpenGauss 数据库客户端配置
提供命令行客户端集成
"""
from django.db.backends.postgresql import client as postgresql_client
import os


class DatabaseClient(postgresql_client.DatabaseClient):
    """
    OpenGauss 数据库客户端
    使用 gsql 命令行工具（OpenGauss 的交互式终端）
    """
    executable_name = 'gsql'  # OpenGauss 命令行工具
    
    def settings_to_cmd_args(self, settings_dict):
        """
        将 Django 设置转换为命令行参数
        """
        args = []
        
        # 数据库名称
        if settings_dict['NAME']:
            args.extend(['-d', settings_dict['NAME']])
        
        # 主机地址
        if settings_dict['HOST']:
            args.extend(['-h', settings_dict['HOST']])
        
        # 端口
        if settings_dict['PORT']:
            args.extend(['-p', str(settings_dict['PORT'])])
        
        # 用户名
        if settings_dict['USER']:
            args.extend(['-U', settings_dict['USER']])
        
        # 其他选项
        if settings_dict.get('OPTIONS'):
            options = settings_dict['OPTIONS']
            
            # SSL 模式
            if 'sslmode' in options:
                args.extend(['-c', 'sslmode=%s' % options['sslmode']])
            
            # 连接超时
            if 'connect_timeout' in options:
                args.extend(['-c', 'connect_timeout=%s' % options['connect_timeout']])
            
            # 客户端编码
            if 'client_encoding' in options:
                args.extend(['-c', 'client_encoding=%s' % options['client_encoding']])
        
        # 默认设置为扩展显示模式
        args.append('-x')  # 扩展显示
        
        return args
    
    def runshell(self):
        """
        运行交互式 shell
        """
        settings_dict = self.connection.settings_dict
        args = self.settings_to_cmd_args(settings_dict)
        
        # 设置密码环境变量
        if settings_dict['PASSWORD']:
            env = os.environ.copy()
            env['PGPASSWORD'] = settings_dict['PASSWORD']
        else:
            env = None
        
        # 执行 gsql 命令
        if os.name == 'nt':
            # Windows 系统
            import subprocess
            subprocess.run([self.executable_name] + args, env=env)
        else:
            # Unix 系统
            if env:
                os.execvpe(self.executable_name, [self.executable_name] + args, env)
            else:
                os.execvp(self.executable_name, [self.executable_name] + args)