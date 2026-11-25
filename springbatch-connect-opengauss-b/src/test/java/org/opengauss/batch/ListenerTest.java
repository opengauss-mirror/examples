package org.opengauss.batch;

import java.util.concurrent.atomic.AtomicReference;

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
 * 维度五：JobExecutionListener 与执行上下文持久化。
 * beforeJob 写入 ExecutionContext、afterJob 读回；上下文随作业结束序列化进
 * BATCH_JOB_EXECUTION_CONTEXT / BATCH_STEP_EXECUTION_CONTEXT（TEXT 列）。
 */
@SpringBootTest
class ListenerTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("listenerJob")
    private Job listenerJob;

    @Autowired
    private AtomicReference<String> listenerSeen;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void listenerFiresAndExecutionContextPersists() throws Exception {
        jdbc.update("DELETE FROM user_batch_out");
        JobExecution ex = jobLauncher.run(listenerJob, new JobParametersBuilder()
                .addString("run.id", "listener-" + System.currentTimeMillis()).toJobParameters());

        assertEquals(BatchStatus.COMPLETED, ex.getStatus());
        // 监听器 beforeJob 写入的值在 afterJob 读回
        assertEquals("hello-batch", listenerSeen.get());

        // 作业级上下文落库，且包含监听器写入的键。
        // 观测（5.2.6 实测）：上下文以 base64(Java 序列化) 存于 SHORT_CONTEXT，SERIALIZED_CONTEXT 列为 NULL。
        Integer jobCtxRows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION_CONTEXT WHERE JOB_EXECUTION_ID = ?",
                Integer.class, ex.getId());
        assertEquals(1, jobCtxRows);
        String shortCtx = jdbc.queryForObject(
                "SELECT SHORT_CONTEXT FROM BATCH_JOB_EXECUTION_CONTEXT WHERE JOB_EXECUTION_ID = ?",
                String.class, ex.getId());
        assertTrue(decodeCtx(shortCtx).contains("listener.marker"));

        // 步骤级上下文落库（同上，base64 二进制串）
        Integer stepCtxRows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM BATCH_STEP_EXECUTION_CONTEXT WHERE STEP_EXECUTION_ID = ?",
                Integer.class, ex.getStepExecutions().iterator().next().getId());
        assertEquals(1, stepCtxRows);
        String stepShort = jdbc.queryForObject(
                "SELECT SHORT_CONTEXT FROM BATCH_STEP_EXECUTION_CONTEXT WHERE STEP_EXECUTION_ID = ?",
                String.class, ex.getStepExecutions().iterator().next().getId());
        assertTrue(decodeCtx(stepShort).contains("batch.stepType"));
    }

    /** base64(Java 序列化流) -> 字符串（ASCII 键名在序列化流中保持原样可见）。 */
    private static String decodeCtx(String base64) {
        return new String(java.util.Base64.getDecoder().decode(base64),
                java.nio.charset.StandardCharsets.ISO_8859_1);
    }
}
