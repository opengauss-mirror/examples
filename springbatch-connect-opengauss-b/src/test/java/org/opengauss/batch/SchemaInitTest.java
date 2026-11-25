package org.opengauss.batch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 维度一：元数据表初始化。
 * 应用启动时 Spring Batch 用自带 schema-mysql.sql 在 B 库上建 9 张 BATCH_* 表
 * （DDL 含 ENGINE=InnoDB 子句与 TIMESTAMP DEFAULT '0000-00-00 00:00:00'），
 * 本用例逐表核对存在性——任一表缺失或 DDL 不被 B 模式接受都会在启动阶段失败。
 */
@SpringBootTest
class SchemaInitTest {

    private static final String[] TABLES = {
            "BATCH_JOB_INSTANCE", "BATCH_JOB_EXECUTION", "BATCH_JOB_EXECUTION_PARAMS",
            "BATCH_JOB_EXECUTION_CONTEXT", "BATCH_JOB_EXECUTION_SEQ",
            "BATCH_STEP_EXECUTION", "BATCH_STEP_EXECUTION_CONTEXT", "BATCH_STEP_EXECUTION_SEQ",
            "BATCH_JOB_SEQ"};

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void allMetaTablesPresent() {
        for (String table : TABLES) {
            Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
            System.out.println("[CASE] 元数据表 " + table + " 存在，当前行数 = " + n);
        }
    }
}
