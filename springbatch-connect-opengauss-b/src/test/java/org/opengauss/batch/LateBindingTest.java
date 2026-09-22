package org.opengauss.batch;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
 * 补测五：@StepScope / @JobScope 晚绑定。
 * chunk size 从作业参数 chunkSize 晚绑定（=3 应切 3 个满 chunk）；
 * @JobScope tasklet 从作业参数 mark 取值写库。
 * 作业参数落库（BATCH_JOB_EXECUTION_PARAMS）同 ParamTypesTest 口径再次核对。
 */
@SpringBootTest
class LateBindingTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("lateBindingJob")
    private Job lateBindingJob;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DROP TABLE IF EXISTS src_lb");
        jdbc.update("DROP TABLE IF EXISTS dst_lb");
        jdbc.update("DROP TABLE IF EXISTS dst_lb_mark");
        jdbc.update("CREATE TABLE src_lb (id INT PRIMARY KEY, name VARCHAR(50), age INT)");
        jdbc.update("CREATE TABLE dst_lb (id INT PRIMARY KEY, name VARCHAR(50), age INT)");
        jdbc.update("CREATE TABLE dst_lb_mark (mark VARCHAR(50))");
        for (int i = 1; i <= 9; i++) {
            jdbc.update("INSERT INTO src_lb (id, name, age) VALUES (?, ?, ?)", i, "lb" + i, 20 + i);
        }
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DROP TABLE IF EXISTS src_lb");
        jdbc.update("DROP TABLE IF EXISTS dst_lb");
        jdbc.update("DROP TABLE IF EXISTS dst_lb_mark");
    }

    @Test
    void stepAndJobScopeLateBindingFromJobParameters() throws Exception {
        String runId = "lb-" + System.currentTimeMillis();
        JobExecution ex = jobLauncher.run(lateBindingJob, new JobParametersBuilder()
                .addString("run.id", runId)
                .addLong("chunkSize", 3L)
                .addString("mark", "lb-ok")
                .toJobParameters());

        assertEquals(BatchStatus.COMPLETED, ex.getStatus());
        assertEquals(9, jdbc.queryForObject("SELECT COUNT(*) FROM dst_lb", Integer.class));
        assertEquals(2, ex.getStepExecutions().size());

        // chunkSize=3 晚绑定生效：9 行切 3 个满 chunk + 末次空读 1 次空提交
        Map<String, Object> m = jdbc.queryForMap(
                "SELECT COMMIT_COUNT FROM BATCH_STEP_EXECUTION WHERE STEP_EXECUTION_ID = ?",
                ex.getStepExecutions().iterator().next().getId());
        assertEquals(4, num(m.get("COMMIT_COUNT")));
        System.out.println("[CASE] chunkSize=3 晚绑定生效: COMMIT_COUNT=" + m.get("COMMIT_COUNT"));

        // mark 参数晚绑定到 @JobScope tasklet 写库
        assertEquals("lb-ok", jdbc.queryForObject("SELECT mark FROM dst_lb_mark", String.class));

        // 参数已落库（晚绑定取值的来源）
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION_PARAMS "
                        + "WHERE PARAMETER_NAME = 'chunkSize' AND PARAMETER_VALUE = '3' "
                        + "AND JOB_EXECUTION_ID = ?",
                Integer.class, ex.getId()));
        System.out.println("[CASE] 参数 chunkSize=3 / mark=lb-ok 落库并可晚绑定");
    }

    private static int num(Object v) {
        return ((Number) v).intValue();
    }
}
