package org.opengauss.batch;

import java.util.ArrayList;
import java.util.List;
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
 * 维度二补充：大数据量分块处理。
 * 1000 行源数据、chunk=200 -> 5 次提交；验证游标逐行读 + 批量写 + 提交计数的完整链路
 * （表 DDL 含 AUTO_INCREMENT，是 B 模式 DDL 兼容性的又一验证点）。
 */
@SpringBootTest
class LargeDataTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("largeJob")
    private Job largeJob;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DROP TABLE IF EXISTS src_large");
        jdbc.update("DROP TABLE IF EXISTS dst_large");
        jdbc.update("CREATE TABLE src_large (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50), age INT)");
        jdbc.update("CREATE TABLE dst_large (id INT PRIMARY KEY, name VARCHAR(50), age INT)");
        List<Object[]> rows = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            rows.add(new Object[]{"row" + i, i % 100});
        }
        jdbc.batchUpdate("INSERT INTO src_large (name, age) VALUES (?, ?)", rows);
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DROP TABLE IF EXISTS src_large");
        jdbc.update("DROP TABLE IF EXISTS dst_large");
    }

    @Test
    void chunkedLargeTableRoundTrip() throws Exception {
        JobExecution ex = jobLauncher.run(largeJob, new JobParametersBuilder()
                .addString("run.id", "large-" + System.currentTimeMillis()).toJobParameters());

        assertEquals(BatchStatus.COMPLETED, ex.getStatus());
        assertEquals(1000, count("dst_large"));

        Map<String, Object> m = jdbc.queryForMap(
                "SELECT READ_COUNT, WRITE_COUNT, COMMIT_COUNT FROM BATCH_STEP_EXECUTION "
                        + "WHERE STEP_EXECUTION_ID = ?",
                ex.getStepExecutions().iterator().next().getId());
        assertEquals(1000, num(m.get("READ_COUNT")));
        assertEquals(1000, num(m.get("WRITE_COUNT")));
        // 5 个满 chunk + 末次空读触发 1 次空提交（框架行为）
        assertEquals(6, num(m.get("COMMIT_COUNT")));
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private static int num(Object v) {
        return ((Number) v).intValue();
    }
}
