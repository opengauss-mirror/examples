package org.opengauss.batch;

import java.util.ArrayList;
import java.util.List;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 维度七：多步骤作业。
 * step1（chunk 拷贝）先于 step2（tasklet 校验）执行：step2 内断言输出表非空；
 * BATCH_STEP_EXECUTION 按执行顺序落两行，step2 的上下文持久化到 BATCH_STEP_EXECUTION_CONTEXT。
 */
@SpringBootTest
class MultiStepTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("twoStepJob")
    private Job twoStepJob;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void twoStepsRunInOrder() throws Exception {
        jdbc.update("DELETE FROM user_batch_out");
        JobExecution ex = jobLauncher.run(twoStepJob, new JobParametersBuilder()
                .addString("run.id", "multi-" + System.currentTimeMillis()).toJobParameters());

        assertEquals(BatchStatus.COMPLETED, ex.getStatus());
        assertEquals(2, ex.getStepExecutions().size());

        // 两步按顺序落库（step2 tasklet 内部已断言 step1 先行）
        List<String> names = jdbc.queryForList(
                "SELECT STEP_NAME FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID = ? "
                        + "ORDER BY STEP_EXECUTION_ID",
                String.class, ex.getId());
        assertEquals(List.of("step1Copy", "step2CountTasklet"), names);

        // step2 写入的执行上下文已持久化（SHORT_CONTEXT 存 base64(Java 序列化)，解码后核对键）
        String ctx = jdbc.queryForObject(
                "SELECT SHORT_CONTEXT FROM BATCH_STEP_EXECUTION_CONTEXT WHERE STEP_EXECUTION_ID = ?",
                String.class, new ArrayList<>(ex.getStepExecutions()).get(1).getId());
        String decoded = new String(java.util.Base64.getDecoder().decode(ctx),
                java.nio.charset.StandardCharsets.ISO_8859_1);
        assertTrue(decoded.contains("step2.saw.rows"));
    }
}
