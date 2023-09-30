# openGauss运维辅助工具

基于Python的封装工具可以提供方便快捷的管理和运维操作，通过简单的命令就能够执行常见的openGauss管理任务。

## 安装和使用

### 安装

1. 确保安装python3

2. `pip install -r requirements.txt`

3. 将orz.py和database.ini文件下载到自己电脑

4. 修改database.ini文件中数据库相关配置

5. 修改omm用户下.bashrc文件，设置命令别称

   `vim ~/.bashrc`

   `alias orz = 'python3 /文件所在路径/orz.py'`

6. `source ~/.bashrc`

7. 运行requests文件安装所需包

### 使用

在omm用户下输入orz命令查看相关操作内容，根据提示进行使用。