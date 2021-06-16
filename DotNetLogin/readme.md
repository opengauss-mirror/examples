# Asp.Net用户登录认证示例

## 引用框架或类库

1. Asp.Net Core
2. Dapper
3. Npgsql.dll（自己移植的3.2.7版本的Npgsql [PR地址](https://gitee.com/opengauss/openGauss-connector-adonet/pulls/5)）

## 说明
1. 请自行创建登录数据库的账号
2. 请使用DataStudio(OpenGauss官方提供客户端)或者gsql创建test数据库
3. 请修改appsettings.json中的ConnectionStrings配置链接
4. 程序运行时不会自动创建用户表和用户数据，请打开项目目录中Scripts，依次执行 schema.sql,data.sql用来初始化用户表和用户数据（默认用户和密码均为test）
5. 可以在vs中运行本示例
