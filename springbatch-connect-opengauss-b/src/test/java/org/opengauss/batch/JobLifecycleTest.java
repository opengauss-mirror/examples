package org.opengauss.batch;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 作业生命周期用例：验证 Spring Batch 的"同参数不可重跑"语义落在 B 库的 BATCH_* 元数据表上。
 * 这组用例真正考验元数据表的读写兼容性（实例判定/参数存取/状态更新都靠这些表）。
 */
@SpringBootTest
class JobLifecycleTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("copyUserJob")
    private Job job;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void sameParametersCannotRerunAfterComplete() throws Exception {
        String runId = "lifecycle-" + System.currentTimeMillis();
        jdbc.update("DELETE FROM user_batch_out");
        JobExecution first = jobLauncher.run(job, new JobParametersBuilder()
                .addString("run.id", runId).toJobParameters());
        assertEquals(BatchStatus.COMPLETED, first.getStatus());

        // 同一 JobInstance（同参数）已完成时重跑，应被 JobRepository 拒绝
        assertThrows(JobInstanceAlreadyCompleteException.class, () -> jobLauncher.run(job,
                new JobParametersBuilder().addString("run.id", runId).toJobParameters()));
    }

    @Test
    void newParametersCreateNewInstance() throws Exception {
        int instancesBefore = count("BATCH_JOB_INSTANCE");
        jdbc.update("DELETE FROM user_batch_out");
        JobExecution ex = jobLauncher.run(job, new JobParametersBuilder()
                .addString("run.id", "new-" + System.currentTimeMillis()).toJobParameters());
        assertEquals(BatchStatus.COMPLETED, ex.getStatus());
        assertEquals(instancesBefore + 1, count("BATCH_JOB_INSTANCE"));
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
