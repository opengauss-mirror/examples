package org.opengauss.batch;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 维度四：容错（faultTolerant）。
 * skip：坏行跳过继续、计数器落 BATCH_STEP_EXECUTION；retry：瞬时故障行重试后成功。
 */
@SpringBootTest
class SkipRetryTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("skipJob")
    private Job skipJob;

    @Autowired
    @Qualifier("retryJob")
    private Job retryJob;

    @Autowired
    private ConcurrentHashMap<String, Integer> retryAttempts;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM `user` WHERE name IN ('SKIP_ME', 'RETRY_ME')");
    }

    @Test
    void skipBadRowsCompletesJob() throws Exception {
        jdbc.update("DELETE FROM user_batch_out");
        jdbc.update("INSERT INTO `user` (name, age) VALUES ('SKIP_ME', 1)");
        jdbc.update("INSERT INTO `user` (name, age) VALUES ('SKIP_ME', 2)");
        int good = jdbc.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE name <> 'SKIP_ME'", Integer.class);

        JobExecution ex = jobLauncher.run(skipJob, new JobParametersBuilder()
                .addString("run.id", "skip-" + System.currentTimeMillis()).toJobParameters());

        assertEquals(BatchStatus.COMPLETED, ex.getStatus());
        // 输出表只含好行
        assertEquals(good, count("user_batch_out"));
        // 计数器：读满（含坏行）、写只含好行、处理跳过 2
        Map<String, Object> m = jdbc.queryForMap(
                "SELECT READ_COUNT, WRITE_COUNT, PROCESS_SKIP_COUNT FROM BATCH_STEP_EXECUTION "
                        + "WHERE STEP_EXECUTION_ID = ?",
                ex.getStepExecutions().iterator().next().getId());
        assertEquals(good + 2, num(m.get("READ_COUNT")));
        assertEquals(good, num(m.get("WRITE_COUNT")));
        assertEquals(2, num(m.get("PROCESS_SKIP_COUNT")));
    }

    @Test
    void retryTransientErrorSucceeds() throws Exception {
        jdbc.update("DELETE FROM user_batch_out");
        jdbc.update("INSERT INTO `user` (name, age) VALUES ('RETRY_ME', 7)");
        retryAttempts.clear();

        JobExecution ex = jobLauncher.run(retryJob, new JobParametersBuilder()
                .addString("run.id", "retry-" + System.currentTimeMillis()).toJobParameters());

        assertEquals(BatchStatus.COMPLETED, ex.getStatus());
        // retryLimit(2) = 含初试共 2 次尝试：第 1 次失败、第 2 次成功
        assertEquals(2, retryAttempts.get("RETRY_ME"));
        assertEquals(1, count("user_batch_out WHERE name = 'RETRY_ME'"));
    }

    private int count(String fromWhere) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + fromWhere, Integer.class);
    }

    private static int num(Object v) {
        return ((Number) v).intValue();
    }
}
