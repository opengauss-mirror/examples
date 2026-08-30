<map version="1.0.1">
<node TEXT="Spring Boot + MySQL JDBC 连接 openGauss B 库 测试（37 用例；BOM 默认 9.7.0 基线全通过）">
<node TEXT="测试环境" POSITION="right">
<node TEXT="openGauss 7.0.0-RC3（fe6f6e4b）B 库 proto_test_db / schema mysql_test_db" POSITION="right"/>
<node TEXT="MySQL 协议端口 3308（enable_dolphin_proto = on）" POSITION="right"/>
<node TEXT="Spring Boot 3.5.16 + JDK 17 + mysql-connector-j（BOM 默认 9.7.0）" POSITION="right"/>
</node>
<node TEXT="一、连接与认证（4 用例，全部通过）" POSITION="right">
<node TEXT="正常连接：productName=MySQL，productVersion=8.0.28-dophin-server" POSITION="right"/>
<node TEXT="错误密码拒绝（failed in auth check）" POSITION="right"/>
<node TEXT="错误用户名拒绝（no pg_hba.conf entry）" POSITION="right"/>
<node TEXT="连 5432 原生口拒绝（Communications link failure）" POSITION="right"/>
<node TEXT="FINDING A：getTables 报错 information_schema.TABLES 不存在（默认配置 lower_case_table_names=0 下大小写敏感；=1 持久化后实测恢复）" POSITION="right"/>
</node>
<node TEXT="二、类型映射 + 6 个文档参数（8 用例，全部通过）" POSITION="right">
<node TEXT="9 种类型往返：TINYINT / TINYINT(1)→Boolean / BIT(1)→Boolean / DECIMAL→BigDecimal / DATE / DATETIME / TIMESTAMP / VARCHAR / BLOB（服务端将 TINYINT(1) 与 BIT(1) 均上报为 typeName=BIT）" POSITION="right"/>
<node TEXT="tinyInt1isBit=false → Integer（参数生效）" POSITION="right"/>
<node TEXT="transformedBitIsBoolean=false/true → getObject 均映射 Boolean（与官方文档备注一致）" POSITION="right"/>
<node TEXT="FINDING B：零日期由 dolphin.sql_mode 的 no_zero_date 控制（默认拒绝 → 去掉可存储 → zeroDateTimeBehavior 三档读取均生效：convertToNull→null / exception→抛错 / round→0001-01-01）" POSITION="right"/>
<node TEXT="原 FINDING C 改写为正向确认：useOldAliasMetadataBehavior 参数生效（表列别名 true→返回别名 n，默认→原始列名 name；表达式别名两种设置均返回别名，符合文档备注）" POSITION="right"/>
<node TEXT="useaffectedrows=true/默认：无变化 UPDATE 均返回 1（与文档备注「目前不支持，始终返回找到行数」一致）" POSITION="right"/>
<node TEXT="allowMultiQueries=true：两条语句两个结果集均返回（参数生效）；默认：服务端明确拒绝" POSITION="right"/>
</node>
<node TEXT="三、DML 批量（3 用例，全部通过）" POSITION="right">
<node TEXT="PreparedStatement.addBatch 1000 行" POSITION="right"/>
<node TEXT="JdbcTemplate.batchUpdate" POSITION="right"/>
<node TEXT="rewriteBatchedStatements=true 提速约 17 倍（59ms vs 1020ms）" POSITION="right"/>
</node>
<node TEXT="四、事务（5 用例，全部通过）" POSITION="right">
<node TEXT="TransactionTemplate 提交可见 / 回滚不可见" POSITION="right"/>
<node TEXT="手动 Connection commit / rollback" POSITION="right"/>
<node TEXT="SAVEPOINT 保存点 + 部分回滚" POSITION="right"/>
<node TEXT="FINDING D：SERIALIZABLE 读回 4（REPEATABLE_READ）" POSITION="right"/>
</node>
<node TEXT="五、连接池（2 用例，全部通过）" POSITION="right">
<node TEXT="HikariCP maximumPoolSize=10 配置生效" POSITION="right"/>
<node TEXT="CONNECTION_ID() 两次取连接同 id → 池复用确认" POSITION="right"/>
</node>
<node TEXT="六、已知限制专项（3 用例）" POSITION="right">
<node TEXT="游标：驱动层无报错、2000 行完整读回；协议层是否真游标待确认" POSITION="right"/>
<node TEXT="Statement.setFetchSize(50) 无报错（可能被静默忽略）" POSITION="right"/>
<node TEXT="存储过程 CallableStatement 报错 information_schema.PARAMETERS 不存在（默认配置大小写敏感所致；lower_case_table_names=1 后大写可查）" POSITION="right"/>
</node>
<node TEXT="七、协议命令专项（9 用例，评审意见落地）" POSITION="right">
<node TEXT="7.1 COM_QUERY / 7.2 COM_INIT_DB / 7.3 COM_PING" POSITION="right"/>
<node TEXT="7.4 COM_STMT_PREPARE / EXECUTE / CLOSE（useServerPrepStmts=true 全生命周期）" POSITION="right"/>
<node TEXT="7.5 COM_STMT_SEND_LONG_DATA：无零字节 1MB 完整往返（MD5 一致，9.7.0 基线）" POSITION="right"/>
<node TEXT="FINDING H：8.0.28~8.4.0 档读回仅 1040384 字节（= 127×8192，丢末块 8KB）；9.7.0/26.7.0 档完整" POSITION="right"/>
<node TEXT="FINDING F：含 0x00 二进制被按 C 字符串截断为前缀（200 字节读回 150；客户端预编译路径无此问题）" POSITION="right"/>
<node TEXT="7.6 COM_STMT_RESET（clearParameters 后复用，8.0.33 及以上档正常）" POSITION="right"/>
<node TEXT="FINDING I：8.0.28 档 COM_STMT_RESET 直接崩溃 openGauss 服务端进程（EOFException 断连、3308 不可达、可复现 3 次）" POSITION="right"/>
<node TEXT="7.7 COM_STMT_FETCH：驱动层可用、协议层待确认" POSITION="right"/>
<node TEXT="7.8 COM_SET_OPTION（allowMultiQueries）" POSITION="right"/>
<node TEXT="FINDING G：多语句后续 SELECT 结果集 getMoreResults() 不可得（DML 均落库）" POSITION="right"/>
<node TEXT="7.9 COM_QUIT；无 JDBC 触发路径的命令在报告 4.1.8 说明" POSITION="right"/>
</node>
<node TEXT="驱动版本矩阵（8 档 × 37 用例）" POSITION="right">
<node TEXT="8.0.28（文档基准，旧坐标）：FINDING I 崩溃 + FINDING H 丢 8KB，其余 30 通过" POSITION="right"/>
<node TEXT="8.0.33 / 8.1.0 / 8.3.0 / 8.4.0：36/37，仅 FINDING H（RESET 正常、服务端不崩）" POSITION="right"/>
<node TEXT="9.7.0（Boot 默认）/ 26.7.0（最新）：37/37 全部通过" POSITION="right"/>
<node TEXT="三档行为：8.0.28 崩 / 8.0.33~8.4.0 丢数据 / 9.7.0+ 正常 → 基础功能与驱动版本无关，协议命令专项与驱动版本相关（缺陷在服务端）" POSITION="right"/>
</node>
<node TEXT="结论" POSITION="right">
<node TEXT="Boot 3.x + 新驱动基础 CRUD / 事务 / 批量 / 类型映射 / 连接池可用" POSITION="right"/>
<node TEXT="服务端预编译 + SEND_LONG_DATA/RESET 场景建议 9.7.0 及以上驱动（FINDING H/I）" POSITION="right"/>
<node TEXT="支撑 docs 仓版本范围表述更新：8.0.28 及以上均可正常工作" POSITION="right"/>
</node>
</node>
</map>
