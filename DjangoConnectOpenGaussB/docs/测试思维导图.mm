<?xml version="1.0" encoding="UTF-8"?>
<map version="1.0.1">
  <node TEXT="Django × openGauss B协议适配测试" FOLDED="false" ID="root">
    <node TEXT="1.环境准备" POSITION="right" ID="env">
      <node TEXT="操作系统" ID="env-os">
        <node TEXT="openEuler 20.03 LTS" ID="env-os-1"/>
        <node TEXT="架构 x86_64 / aarch64" ID="env-os-2"/>
      </node>
      <node TEXT="数据库" ID="env-db">
        <node TEXT="openGauss 7.0.x Server（非 Lite）" ID="env-db-1"/>
        <node TEXT="任务要求 7.0.0-LTS / 实测 7.0.0-RC3" ID="env-db-2"/>
        <node TEXT="安装路径 /opt/software/openGauss" ID="env-db-3"/>
      </node>
      <node TEXT="dolphin MySQL协议" ID="env-dolphin">
        <node TEXT="enable_dolphin_proto=on" ID="env-dolphin-1"/>
        <node TEXT="dolphin_server_port=3308" ID="env-dolphin-2"/>
        <node TEXT="listen_addresses=*" ID="env-dolphin-3"/>
        <node TEXT="原生端口 5432" ID="env-dolphin-4"/>
      </node>
      <node TEXT="业务库" ID="env-biz">
        <node TEXT="B库 proto_test_db" ID="env-biz-1"/>
        <node TEXT="业务库 mysql_test_db + user表" ID="env-biz-2"/>
      </node>
    </node>

    <node TEXT="2.驱动与适配层" POSITION="right" ID="driver">
      <node TEXT="Python 驱动" ID="driver-py">
        <node TEXT="主路径 PyMySQL" ID="driver-py-1"/>
        <node TEXT="可选 mysqlclient" ID="driver-py-2"/>
        <node TEXT="pymysql_bootstrap 注册 MySQLdb" ID="driver-py-3"/>
      </node>
      <node TEXT="Django Backend" ID="driver-backend">
        <node TEXT="ENGINE=opengauss_mysql" ID="driver-backend-1"/>
        <node TEXT="基于 django.db.backends.mysql" ID="driver-backend-2"/>
        <node TEXT="软探测 version/sql_mode" ID="driver-backend-3"/>
        <node TEXT="规避 @@default_storage_engine → 28804" ID="driver-backend-4"/>
      </node>
      <node TEXT="连接参数" ID="driver-conn">
        <node TEXT="HOST / PORT=3308" ID="driver-conn-1"/>
        <node TEXT="NAME=mysql_test_db" ID="driver-conn-2"/>
        <node TEXT="USER/PASSWORD（native_password）" ID="driver-conn-3"/>
      </node>
    </node>

    <node TEXT="3.连接认证测试" POSITION="right" ID="auth">
      <node TEXT="T1 端口连通" ID="auth-t1">
        <node TEXT="TCP 5432 通过" ID="auth-t1-1"/>
        <node TEXT="TCP 3308 通过" ID="auth-t1-2"/>
        <node TEXT="安全组放行" ID="auth-t1-3"/>
      </node>
      <node TEXT="T2 握手认证" ID="auth-t2">
        <node TEXT="set_native_password" ID="auth-t2-1"/>
        <node TEXT="PyMySQL 认证通过" ID="auth-t2-2"/>
      </node>
      <node TEXT="T3 Schema/表就绪" ID="auth-t3">
        <node TEXT="CREATE DATABASE/TABLE 通过" ID="auth-t3-1"/>
        <node TEXT="预置 user 样例数据" ID="auth-t3-2"/>
      </node>
    </node>

    <node TEXT="4.ORM功能测试" POSITION="left" ID="orm">
      <node TEXT="T4 查询全部" ID="orm-t4">
        <node TEXT="User.objects.all() 通过" ID="orm-t4-1"/>
      </node>
      <node TEXT="T5 CRUD闭环" ID="orm-t5">
        <node TEXT="新增 create 通过" ID="orm-t5-1"/>
        <node TEXT="模糊查询 filter(name__icontains) 通过" ID="orm-t5-2"/>
        <node TEXT="更新 save/update 通过" ID="orm-t5-3"/>
        <node TEXT="按ID查询 get 通过" ID="orm-t5-4"/>
        <node TEXT="删除 delete 通过" ID="orm-t5-5"/>
        <node TEXT="DEMO_OK" ID="orm-t5-6"/>
      </node>
      <node TEXT="入口脚本" ID="orm-script">
        <node TEXT="scripts/run_demo.py" ID="orm-script-1"/>
        <node TEXT="云上 /opt/django-opengauss-b" ID="orm-script-2"/>
      </node>
    </node>

    <node TEXT="5.协议限制" POSITION="left" ID="limit">
      <node TEXT="T6 存储过程 跳过" ID="limit-t6"/>
      <node TEXT="server-side cursor 不支持" ID="limit-cursor"/>
      <node TEXT="对照 dolphin 官方兼容说明" ID="limit-doc"/>
    </node>

    <node TEXT="6.问题与修复" POSITION="left" ID="bugs">
      <node TEXT="缺 libopenblas → yum install openblas" ID="bugs-1"/>
      <node TEXT="OOM → overcommit=0 + 调小内存参数" ID="bugs-2"/>
      <node TEXT="缺 unixODBC → yum install unixODBC" ID="bugs-3"/>
      <node TEXT="Lite 无 dolphin → 改用 Server" ID="bugs-4"/>
      <node TEXT="28804 → opengauss_mysql 适配层" ID="bugs-5"/>
    </node>

    <node TEXT="7.交付物" POSITION="left" ID="deliver">
      <node TEXT="examples 示例工程 + 自测报告 + 本思维导图" ID="deliver-1"/>
      <node TEXT="docs django_development.md" ID="deliver-2"/>
      <node TEXT="Plugin 本期无服务端补丁" ID="deliver-3"/>
    </node>
  </node>
</map>
