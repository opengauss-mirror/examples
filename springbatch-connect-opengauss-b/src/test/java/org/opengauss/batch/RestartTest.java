package org.opengauss.batch;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 维度三补充：作业失败路径（FINDING 回归断言）。
 * Spring Batch 作业失败时把完整异常栈写入 EXIT_MESSAGE VARCHAR(2500)，超过 2500 字符被
 * B 模式严格拒绝（error 130 "value too long for type character varying(2500)"）——
 * 失败状态无法落库，BATCH_JOB_EXECUTION/BATCH_STEP_EXECUTION 停留在 STARTED
 * （框架日志：This job is now in an unknown state and should not be restarted）。
 * 行为与 MySQL 严格模式（STRICT_TRANS_TABLES，5.7+ 默认）一致；规避见 README 常见问题。
 */
@SpringBootTest
class RestartTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("failJob")
    private Job failJob;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM `user` WHERE name = 'FAIL_ME'");
    }

    @Test
    void failedJobExitMessageOverflowIsRejected() {
        String runId = "fail-" + System.currentTimeMillis();
        jdbc.update("DELETE FROM user_batch_out");
        jdbc.update("INSERT INTO `user` (name, age) VALUES ('FAIL_ME', 1)");

        UncategorizedSQLException e = assertThrows(UncategorizedSQLException.class,
                () -> jobLauncher.run(failJob, new JobParametersBuilder()
                        .addString("run.id", runId).toJobParameters()));
        // 服务端以 MySQL 语义拒绝超长 EXIT_MESSAGE（error 130，与 MySQL 严格模式一致）
        Throwable c = e.getCause();
        while (c != null && c.getMessage() == null) {
            c = c.getCause();
        }
        assertTrue(c != null && c.getMessage().contains("value too long for type character varying(2500)"),
                "期望 error 130 超长拒绝，实际：" + (c == null ? "无消息" : c.getMessage()));

        // 失败状态未能落库：job/step 元数据停留在 STARTED（unknown state 的直接证据）
        List<Long> ids = jdbc.queryForList(
                "SELECT JOB_EXECUTION_ID FROM BATCH_JOB_EXECUTION_PARAMS "
                        + "WHERE PARAMETER_NAME = 'run.id' AND PARAMETER_VALUE = ?",
                Long.class, runId);
        assertEquals(1, ids.size());
        String jobStatus = jdbc.queryForObject(
                "SELECT STATUS FROM BATCH_JOB_EXECUTION WHERE JOB_EXECUTION_ID = ?",
                String.class, ids.get(0));
        assertEquals("STARTED", jobStatus);
        List<String> stepStatuses = jdbc.queryForList(
                "SELECT STATUS FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID = ?",
                String.class, ids.get(0));
        assertEquals(1, stepStatuses.size());
        assertEquals("STARTED", stepStatuses.get(0));
    }
}
