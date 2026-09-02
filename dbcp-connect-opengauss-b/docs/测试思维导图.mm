<map version="1.0.1">
<!-- DBCP 适配 openGauss B 兼容模式 —— 测试思维导图（FreeMind/XMind 兼容格式） -->
<!-- 测试思维导图：与 docs/自测报告.md 保持一致，共 46 个 JUnit 用例。 -->
<node TEXT="DBCP 适配 openGauss B 兼容模式 测试（46 用例）" FOLDED="false">
  <node TEXT="1. 环境配置" FOLDED="true">
    <node TEXT="openGauss 安装与启动（gs_initdb/gs_ctl，不复制 gaussdb.state）"/>
    <node TEXT="B 兼容库创建（create_b_database.sql → init.sql 顺序）"/>
    <node TEXT="dolphin 协议参数（enable_dolphin_proto / dolphin_server_port / default_database_name）"/>
    <node TEXT="安装包或转测包已配套提供 openGauss-server 与 Dolphin，直接初始化并启动"/>
    <node TEXT="业务用户与权限（set_native_password / schema owner / GRANT）"/>
    <node TEXT="pg_hba 客户端接入认证（仅本机/受限网段，禁止 0.0.0.0/0）"/>
    <node TEXT="JDK / Maven / DBCP2 / Pool2 / Connector-J 依赖版本"/>
  </node>
  <node TEXT="2. 连接池初始化（DbcpDataSourceTest 5）" FOLDED="true">
    <node TEXT="驱动加载（显式 driverClassName vs JDBC4 SPI）"/>
    <node TEXT="initialSize 初始连接建立"/>
    <node TEXT="native 认证建连（opengauss/opengauss123）"/>
    <node TEXT="错误配置容错（密码错误 / 端口不通）"/>
  </node>
  <node TEXT="3. 连接获取与归还（ConnectionPoolTest 10）" FOLDED="true">
    <node TEXT="TC-01 单连接借出 / 归还（active 计数）"/>
    <node TEXT="TC-02 物理会话复用：maxTotal=1，close 后重新借出复用同一 CONNECTION_ID 与物理对象"/>
    <node TEXT="TC-03 连接泄漏回收（removeAbandonedOnMaintenance）"/>
    <node TEXT="TC-04 空闲驱逐（minEvictableIdleTimeMillis + 驱逐线程）"/>
    <node TEXT="TC-05 池参数：maxTotal/maxWaitMillis：等待时长上下界 [1500,5000]ms + 池耗尽异常（Timeout waiting for idle object / NoSuchElementException）"/>
    <node TEXT="TC-06 池参数：maxIdle：并发创建 8 个物理连接，归还后 idle ≤ maxIdle=2"/>
    <node TEXT="TC-07 池参数：minIdle：维护线程轮询补足空闲连接到 minIdle=2（被借走期间仍补足）"/>
    <node TEXT="TC-08 池参数：状态复位（补充）：归还前改 autoCommit/隔离级别，重新借出恢复默认值（dolphin 不支持 readOnly，不声明 readOnly 复位）"/>
    <node TEXT="连接池参数生效：汇总 maxTotal/maxWait、maxIdle、minIdle 和状态复位断言"/>
    <node TEXT="并发获取连接：多线程同时借出并持有连接，断言 active 不超过 maxTotal，结束后 active==0"/>
  </node>
  <node TEXT="4. 连接校验与断连自愈（ConnectionValidationTest 3）" FOLDED="true">
    <node TEXT="testOnBorrow（SELECT 1 校验，借出可用）"/>
    <node TEXT="服务端断连：独立管理连接执行 KILL &lt;CONNECTION_ID&gt;（不调用被测 ConnectionImpl.close()），SHOW PROCESSLIST 核验会话消失，本地 isClosed() 仍为 false，testOnReturn=false 下归还后先证 idle==1 进入空闲池"/>
    <node TEXT="testOnBorrow 自愈（testOnBorrow=true、testWhileIdle=false）：再次借出校验失败才丢弃死连接并创建新物理连接"/>
    <node TEXT="testWhileIdle 自愈（testOnBorrow=false、testWhileIdle=true）：后台校验清除死连接到 idle==0（排除按时间驱逐），再借出为新物理连接"/>
  </node>
  <node TEXT="5. CRUD 功能（CrudTest 8）" FOLDED="true">
    <node TEXT="查询（全表 / 按 id / 模糊 LIKE / 分页 LIMIT OFFSET）"/>
    <node TEXT="新增（AUTO_INCREMENT 自增主键回填）"/>
    <node TEXT="更新"/>
    <node TEXT="删除"/>
    <node TEXT="批量插入（executeBatch + 主键回填）"/>
  </node>
  <node TEXT="6. 事务（TransactionTest 5）" FOLDED="true">
    <node TEXT="自动提交默认值（autocommit=true）"/>
    <node TEXT="手动提交（commit 后其他连接可见）"/>
    <node TEXT="回滚（rollback 后不可见）"/>
    <node TEXT="隔离级别设置（READ_COMMITTED / REPEATABLE_READ）"/>
    <node TEXT="DBCP 包装连接事务透传"/>
  </node>
  <node TEXT="7. 数据类型与字符集（DataTypeCharsetTest 3）" FOLDED="true">
    <node TEXT="中文 / emoji（utf8mb4 无损读写）"/>
    <node TEXT="INT / BIGINT / DECIMAL / DATE / DATETIME / TEXT 往返"/>
    <node TEXT="NULL 处理（wasNull 识别）"/>
  </node>
  <node TEXT="8. 约束与受限行为（ConstraintTest 3）" FOLDED="true">
    <node TEXT="allowMultiQueries 默认关闭：分号多查询按预期拒绝"/>
    <node TEXT="useCursorFetch 参数兼容性：设置 fetchSize 后查询结果正确"/>
    <node TEXT="useSSL=false：非 SSL JDBC 连接可用"/>
  </node>
  <node TEXT="9. 稳定性（StabilityTest 4）" FOLDED="true">
    <node TEXT="反复建池 / 关池（10 次循环，每轮 finally 关闭）"/>
    <node TEXT="长连接保活（单连接复用 200 次查询）"/>
    <node TEXT="自包含有界负载后池状态（并发负载后 active=0 且仍可借出）"/>
    <node TEXT="并发 CRUD 压测（多线程事务 CRUD，失败清理，结束后无连接泄漏）"/>
  </node>
  <node TEXT="10. 结论输出" FOLDED="true">
    <node TEXT="用例统计：46 通过，0 失败，0 错误，0 跳过（详见自测报告）"/>
    <node TEXT="报告映射：测试类 → 校验点 → 关键断言（详见自测报告）"/>
    <node TEXT="Spring Boot 链路：Spring Boot → DBCP BasicDataSource → Connector/J → Dolphin → openGauss B 兼容库"/>
    <node TEXT="测试环境：openGauss 7.0.0 master 分支转测包，服务端与 Dolphin 配套提供；Dolphin 127.0.0.1:36443"/>
    <node TEXT="推荐配置（URL 参数 / 池参数取值表）"/>
  </node>
</node>
</map>
