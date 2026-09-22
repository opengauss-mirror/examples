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
 * 补测二：JdbcPagingItemReader 分页读。
 * 框架按 MySQL 方言生成 SELECT ... ORDER BY id LIMIT ? OFFSET ? 分页 SQL，
 * 是除游标读之外的又一条 DB 兼容路径（B 库 LIMIT/OFFSET 解析）。
 * 100 行源数据、pageSize=20、chunk=20 -> 5 页满 + 末次空读 1 次空提交。
 */
@SpringBootTest
class PagingReaderTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("pagingJob")
    private Job pagingJob;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DROP TABLE IF EXISTS src_page");
        jdbc.update("DROP TABLE IF EXISTS dst_page");
        jdbc.update("CREATE TABLE src_page (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50), age INT)");
        jdbc.update("CREATE TABLE dst_page (id INT PRIMARY KEY, name VARCHAR(50), age INT)");
        List<Object[]> rows = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            rows.add(new Object[]{"page" + i, i % 50});
        }
        jdbc.batchUpdate("INSERT INTO src_page (name, age) VALUES (?, ?)", rows);
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DROP TABLE IF EXISTS src_page");
        jdbc.update("DROP TABLE IF EXISTS dst_page");
    }

    @Test
    void pagingReaderRoundTrip() throws Exception {
        JobExecution ex = jobLauncher.run(pagingJob, new JobParametersBuilder()
                .addString("run.id", "paging-" + System.currentTimeMillis()).toJobParameters());

        assertEquals(BatchStatus.COMPLETED, ex.getStatus());
        assertEquals(100, jdbc.queryForObject("SELECT COUNT(*) FROM dst_page", Integer.class));

        Map<String, Object> m = jdbc.queryForMap(
                "SELECT READ_COUNT, WRITE_COUNT, COMMIT_COUNT FROM BATCH_STEP_EXECUTION "
                        + "WHERE STEP_EXECUTION_ID = ?",
                ex.getStepExecutions().iterator().next().getId());
        assertEquals(100, num(m.get("READ_COUNT")));
        assertEquals(100, num(m.get("WRITE_COUNT")));
        // 5 个满 chunk（20×5）+ 末次空读 1 次空提交（框架行为，同 LargeDataTest 口径）
        assertEquals(6, num(m.get("COMMIT_COUNT")));
        System.out.println("[CASE] 分页读完成: READ=" + m.get("READ_COUNT")
                + ", WRITE=" + m.get("WRITE_COUNT") + ", COMMIT=" + m.get("COMMIT_COUNT"));
    }

    private static int num(Object v) {
        return ((Number) v).intValue();
    }
}
