package org.opengauss.batch;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 补测六：JobExplorer / JobOperator 元数据查询 API（对 BATCH_* 表的读路径）。
 * 跑一次 copyUserJob 后经 JobExplorer 查实例/执行/步骤摘要，经 JobOperator 查作业名与步骤执行。
 */
@SpringBootTest
class ExplorerOperatorTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    @Qualifier("copyUserJob")
    private Job copyUserJob;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void explorerAndOperatorReadMetadata() throws Exception {
        jdbc.update("DELETE FROM user_batch_out");
        JobExecution ex = jobLauncher.run(copyUserJob, new JobParametersBuilder()
                .addString("run.id", "explorer-" + System.currentTimeMillis()).toJobParameters());
        assertEquals(BatchStatus.COMPLETED, ex.getStatus());

        // JobExplorer：实例列表（最新在前）与执行、步骤摘要
        List<JobInstance> instances = jobExplorer.getJobInstances("copyUserJob", 0, 10);
        assertFalse(instances.isEmpty());
        JobInstance latest = instances.get(0);
        List<JobExecution> executions = jobExplorer.getJobExecutions(latest);
        assertFalse(executions.isEmpty());
        assertTrue(executions.get(0).getStepExecutions().stream()
                .anyMatch(s -> "copyUserStep".equals(s.getStepName())));

        // JobOperator：作业名解析（JobRegistry）与步骤执行摘要
        assertTrue(jobOperator.getJobNames().contains("copyUserJob"));
        var summaries = jobOperator.getStepExecutionSummaries(ex.getId());
        assertTrue(summaries.values().stream().anyMatch(s -> s.contains("copyUserStep")));

        System.out.println("[CASE] JobExplorer 实例 " + instances.size() + " 个 / JobOperator 作业名与步骤摘要可查");
    }
}
