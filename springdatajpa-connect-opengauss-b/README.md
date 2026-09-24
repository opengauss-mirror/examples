# Spring Data JPA 连接 openGauss B 兼容库

对应任务：Issue #28 — openGauss B 库 B 协议适配 MySQL Java 框架 SpringDataJPA。

使用 Spring Boot 3.x + Spring Data JPA（Hibernate 6）配合 MySQL JDBC 驱动连接 openGauss B 兼容模式数据库的示例工程，包含 19 个测试用例、7 档驱动版本矩阵，以及测试思维导图与自测报告。

## 目录结构

```
springdatajpa-connect-opengauss-b
├── pom.xml                    # 含 7 档驱动 profile（版本矩阵用）
├── README.md
├── docs
│   ├── 测试思维导图-SpringDataJPA连接openGaussB库.{mm,md,html}
│   └── 基于SpringDataJPA框架的openGauss数据库测试.md   # 自测报告
└── src
    ├── main/java/org/opengauss/smoke   # 实体/仓储/方言/示例应用
    └── test/java/org/opengauss/smoke/jpacases  # 测试套件（19 用例）
```

## 前置条件

与 `springboot-connect-opengauss-b`（Issue #27 工程）相同：JDK 17+、openGauss MySQL 协议已开（3308）、B 库/schema/`` `user` `` 表/用户已就绪。环境搭建步骤详见 docs 仓《[SpringDataJPA 连接示例](https://docs.opengauss.org/zh/docs/latest/mysql_compatibility_developer_guide/springdatajpa_development.html)》。

## 配置

| 变量 | 默认值 | 说明 |
|---|---|---|
| DB_HOST | 127.0.0.1 | openGauss 所在机器的 IP |
| DB_PORT | 3308 | dolphin 协议端口 |
| DB_NAME | mysql_test_db | B 库下 schema（等价 MySQL database） |
| DB_USER / DB_PASSWORD | mysql_test_db / ChangeMe_123 | MySQL 认证用户 |

## 运行

```bash
# 运行全部测试（19 用例）
mvn test

# 运行示例主程序
mvn spring-boot:run
```

## 测试内容（19 用例）

| 类别 | 用例 | 覆盖点 |
|---|---|---|
| 已知差异回归 | K1 / K2 | K1 复现转义语义差异（派生 LIKE escape 报错，回归断言，与 Plugin 仓 issue #504 同源）；K2 验证 workaround（JPQL 显式 `escape '!'`） |
| ddl-auto 专项 | D1 / D2 | D1 update 模式（FINDING E 静默失效）；D2 validate 模式 |
| 类型映射 | M1 | BigDecimal/LocalDate/LocalDateTime/Boolean/String 往返 + IDENTITY 主键回填 |
| 分页与批量 | P1 / P2 | PageRequest 多页遍历 / saveAll(50) 批量回填 |
| 冒烟 | 3 | saveAndFind / pagination / transactionRollback |
| 协议命令 | C1-C9 | COM_QUERY/INIT_DB/PING/STMT_PREPARE/STMT_EXECUTE/STMT_CLOSE/STMT_SEND_LONG_DATA/STMT_RESET/STMT_FETCH/SET_OPTION/QUIT（评审意见落地，与 #27 报告 7.1-7.9 同设计） |

## 驱动版本矩阵（7 档 × 19 用例，三档行为）

| 驱动版本 | 结果 |
|---|---|
| 8.0.28（文档基准） | 协议类 1 失败 + 6 错误（FINDING I 崩溃 + FINDING H 丢 8KB），其余 12 通过（含 K1/D1 复现） |
| 8.0.33 / 8.1.0 / 8.3.0 / 8.4.0 | 18/19（仅 FINDING H；RESET 正常、服务端不崩） |
| 9.7.0（Boot 默认）/ 26.7.0（最新） | 19/19 全部通过 |

切换档位重跑：

```bash
mvn test -Pdriver-8.0.28
mvn test -Pdriver-8.0.33
mvn test -Pdriver-8.1.0
mvn test -Pdriver-8.3.0
mvn test -Pdriver-8.4.0
mvn test -Pdriver-9.7.0
mvn test -Pdriver-26.7.0
```

> 三档行为：8.0.28 崩 / 8.0.33~8.4.0 丢数据 / 9.7.0+ 正常。核心链路与 K1/D1/FINDING E/F/G 均与驱动版本无关；FINDING H/I 为服务端缺陷、触发条件依赖驱动版本。
>
> **注意：8.0.28 档跑协议命令测试会打崩 openGauss 服务端进程（FINDING I），需重启数据库后才可继续。**

## 已知要点与限制

## 已知要点与限制

| 项 | 说明 |
|---|---|
| 转义语义差异（issue #504） | Hibernate 6.6 默认给 LIKE 追加 `escape '\\'`（两个反斜杠），B 模式视 `'\\'` 为 2 字符 → `invalid escape string`。根源是 B 模式不支持反斜杠作为字符串转义字符（与社区已记录的已知差异同源，参见 Plugin 仓 issue #504）。规避：JPQL 显式 `escape '!'`（见 UserRepository.searchByName）。与驱动版本无关 |
| FINDING E | ddl-auto=update 静默失效（HHH000339：information_schema.KEYWORDS 不存在（默认配置 `dolphin.lower_case_table_names=0` 下大小写敏感：大写报错、小写可查）→ 不建表也不报错）。**设置 `dolphin.lower_case_table_names=1` 并持久化后实测 Hibernate 元数据探测恢复**；未配置时建议手工建表 + `ddl-auto: none` |
| FINDING F | COM_STMT_SEND_LONG_DATA（服务端预编译）写入含 0x00 的二进制被按 C 字符串截断为前缀（无零字节 1MB 完整往返 MD5 一致；客户端预编译路径无此问题） |
| FINDING G | allowMultiQueries 多语句后续 SELECT 结果集经 getMoreResults() 不可得（仅首语句结果 + 末 DML update count；DML 均落库） |
| FINDING H | COM_STMT_SEND_LONG_DATA 无零字节 1MB：8.0.28~8.4.0 档读回仅 1040384 字节（= 127×8192，丢末块 8KB）；9.7.0/26.7.0 档完整 |
| FINDING I | COM_STMT_RESET（clearParameters 后复用）：仅 8.0.28 档，第二次 executeUpdate 直接崩溃 openGauss 服务端进程（EOFException 断连、3308 不可达、可复现 3 次）；8.0.33 及以上档正常 |
| 方言 | `database-platform: org.opengauss.smoke.OpenGaussBMySQLDialect`（继承 MySQLDialect，声明无反斜杠转义语义，作为适配点） |
| jakarta | Spring Boot 3.x 实体类 import 用 `jakarta.persistence.*`（不是 javax） |

完整分析见 `docs/基于SpringDataJPA框架的openGauss数据库测试.md`。
