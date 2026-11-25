package org.opengauss.batch;

import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
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
 * 维度六：作业参数类型与实例身份。
 * String/Long/Date 三类参数的类型列落库核对；identifying=false 的参数不参与实例身份判定。
 */
@SpringBootTest
class ParamTypesTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("copyUserJob")
    private Job job;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void paramTypesStoredAndNonIdentifyingExcludedFromIdentity() throws Exception {
        jdbc.update("DELETE FROM user_batch_out");
        String runId = "pt-" + System.currentTimeMillis();
        JobParameters p = new JobParametersBuilder()
                .addString("run.id", runId)
                .addLong("run.ts", 1234567890L)
                .addDate("run.day", new Date(1700000000000L))
                .addString("run.hint", "v1", false)   // 非标识参数
                .toJobParameters();
        JobExecution ex = jobLauncher.run(job, p);
        assertEquals(BatchStatus.COMPLETED, ex.getStatus());

        // 参数类型列核对
        assertEquals("java.lang.Long", paramType(ex.getId(), "run.ts"));
        assertEquals("java.util.Date", paramType(ex.getId(), "run.day"));
        Map<String, Object> hint = jdbc.queryForMap(
                "SELECT PARAMETER_TYPE, IDENTIFYING FROM BATCH_JOB_EXECUTION_PARAMS "
                        + "WHERE JOB_EXECUTION_ID = ? AND PARAMETER_NAME = 'run.hint'",
                ex.getId());
        assertEquals("java.lang.String", hint.get("PARAMETER_TYPE"));
        assertEquals("N", hint.get("IDENTIFYING"));

        // 非标识参数不参与实例身份：3 个标识参数相同、仅 hint 变化 -> 同一实例 -> 拒绝重跑
        JobParameters p2 = new JobParametersBuilder()
                .addString("run.id", runId)
                .addLong("run.ts", 1234567890L)
                .addDate("run.day", new Date(1700000000000L))
                .addString("run.hint", "v2", false)
                .toJobParameters();
        assertThrows(JobInstanceAlreadyCompleteException.class, () -> jobLauncher.run(job, p2));
    }

    private String paramType(long executionId, String name) {
        return jdbc.queryForObject(
                "SELECT PARAMETER_TYPE FROM BATCH_JOB_EXECUTION_PARAMS "
                        + "WHERE JOB_EXECUTION_ID = ? AND PARAMETER_NAME = ?",
                String.class, executionId, name);
    }
}
