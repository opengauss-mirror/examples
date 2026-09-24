<map version="1.0.1">
<node TEXT="SpringDataJPA + MySQL JDBC 连接 openGauss B 库 测试（19 用例；BOM 默认 9.7.0 基线全通过）">
<node TEXT="测试环境" POSITION="right">
<node TEXT="openGauss 7.0.0-RC3（fe6f6e4b）B 库 proto_test_db / schema mysql_test_db" POSITION="right"/>
<node TEXT="MySQL 协议端口 3308（enable_dolphin_proto = on）" POSITION="right"/>
<node TEXT="Spring Boot 3.5.16 + Hibernate 6.6.53 + mysql-connector-j 9.7.0" POSITION="right"/>
<node TEXT="方言 MySQLDialect（自定义 OpenGaussBMySQLDialect 保留适配点）" POSITION="right"/>
</node>
<node TEXT="一、已知差异回归（2 用例，符合预期）" POSITION="right">
<node TEXT="K1：派生 LIKE 复现转义语义差异 —— invalid escape string（Hibernate 6.6 默认 escape '\\'（两个反斜杠），B 模式视 '\\' 为 2 字符；与 Plugin 仓 issue #504 同源）" POSITION="right"/>
<node TEXT="K2：JPQL 显式 escape '!'（searchByName）可用，workaround 稳定" POSITION="right"/>
<node TEXT="方法论：K1 写成回归断言 —— 服务端行为调整后自动失败，提醒更新文档" POSITION="right"/>
</node>
<node TEXT="二、ddl-auto 专项（2 用例）" POSITION="right">
<node TEXT="D1 FINDING E：ddl-auto=update 容器启动成功但表未创建（静默失效）" POSITION="right"/>
<node TEXT="根因：默认配置 lower_case_table_names=0 下 information_schema 表名大小写敏感（KEYWORDS 大写报不存在，小写可查）→ schema 对比失败" POSITION="right"/>
<node TEXT="解法：dolphin.lower_case_table_names=1 并持久化后实测大写 KEYWORDS 可查、Hibernate 元数据探测恢复" POSITION="right"/>
<node TEXT="实际影响：启动正常，运行时才报 relation does not exist" POSITION="right"/>
<node TEXT="D2：ddl-auto=validate 未报错（校验深度存疑，如实表述）" POSITION="right"/>
</node>
<node TEXT="三、类型映射（1 用例，通过）" POSITION="right">
<node TEXT="BigDecimal / LocalDate / LocalDateTime / Boolean↔TINYINT(1) / String 往返一致" POSITION="right"/>
<node TEXT="IDENTITY 主键回填（getGeneratedKeys 链路正常）" POSITION="right"/>
</node>
<node TEXT="四、分页与批量（2 用例，通过）" POSITION="right">
<node TEXT="P1：PageRequest 多页遍历（LIMIT/OFFSET 与 count 查询正常）" POSITION="right"/>
<node TEXT="P2：saveAll(50) 批量主键逐个回填" POSITION="right"/>
</node>
<node TEXT="五、冒烟（3 用例，通过）" POSITION="right">
<node TEXT="saveAndFind / pagination / transactionRollback（@Transactional 回滚正常）" POSITION="right"/>
</node>
<node TEXT="六、协议命令专项（9 用例，评审意见落地，与 #27 报告 7.1-7.9 同设计）" POSITION="right">
<node TEXT="C1 COM_QUERY / C2 COM_INIT_DB / C3 COM_PING" POSITION="right"/>
<node TEXT="C4 COM_STMT_PREPARE / EXECUTE / CLOSE（useServerPrepStmts=true 全生命周期）" POSITION="right"/>
<node TEXT="C5 COM_STMT_SEND_LONG_DATA：无零字节 1MB 完整往返（MD5 一致，9.7.0 基线）" POSITION="right"/>
<node TEXT="FINDING H：8.0.28~8.4.0 档读回仅 1040384 字节（= 127×8192，丢末块 8KB）；9.7.0/26.7.0 档完整" POSITION="right"/>
<node TEXT="FINDING F：含 0x00 二进制被按 C 字符串截断为前缀（200 字节读回 150；客户端预编译路径无此问题）" POSITION="right"/>
<node TEXT="C6 COM_STMT_RESET（clearParameters 后复用，8.0.33 及以上档正常）" POSITION="right"/>
<node TEXT="FINDING I：8.0.28 档 COM_STMT_RESET 直接崩溃 openGauss 服务端进程（EOFException 断连、可复现 3 次）" POSITION="right"/>
<node TEXT="C7 COM_STMT_FETCH：驱动层可用、协议层待确认" POSITION="right"/>
<node TEXT="C8 COM_SET_OPTION（allowMultiQueries）" POSITION="right"/>
<node TEXT="FINDING G：多语句后续 SELECT 结果集 getMoreResults() 不可得（DML 均落库）" POSITION="right"/>
<node TEXT="C9 COM_QUIT；无 JDBC 触发路径的命令在报告 4.1.6 说明" POSITION="right"/>
</node>
<node TEXT="驱动版本矩阵（7 档 × 19 用例）" POSITION="right">
<node TEXT="8.0.28（文档基准）：FINDING I 崩溃 + FINDING H 丢 8KB，其余 12 通过（含 K1/D1 复现）" POSITION="right"/>
<node TEXT="8.0.33 / 8.1.0 / 8.3.0 / 8.4.0：18/19，仅 FINDING H（RESET 正常、服务端不崩）" POSITION="right"/>
<node TEXT="9.7.0（Boot 默认）/ 26.7.0（最新）：19/19 全部通过" POSITION="right"/>
<node TEXT="三档行为：8.0.28 崩 / 8.0.33~8.4.0 丢数据 / 9.7.0+ 正常 → 核心链路与 K1/D1/FINDING E/F/G 与驱动版本无关；FINDING H/I 为服务端缺陷、触发条件依赖驱动版本" POSITION="right"/>
</node>
<node TEXT="结论" POSITION="right">
<node TEXT="JPA 核心链路（类型映射 / 主键回填 / 分页 / 批量 / 事务）可用" POSITION="right"/>
<node TEXT="问题/差异根源在服务端：反斜杠转义语义差异（已知差异，与 #504 同源）/ FINDING E 大小写敏感（配置可恢复）/ FINDING F 二进制 0x00 截断 / FINDING G 多语句结果结构 / FINDING H 丢末块 8KB（8.0.28~8.4.0 档）/ FINDING I 崩溃（仅 8.0.28 档）" POSITION="right"/>
<node TEXT="指导文档建议：LIKE 显式 escape '!'；ddl-auto 建议 lower_case_table_names=1（或 none + 手工建表）；服务端预编译 + SEND_LONG_DATA/RESET 场景建议 9.7.0 及以上驱动" POSITION="right"/>
</node>
</node>
</map>
