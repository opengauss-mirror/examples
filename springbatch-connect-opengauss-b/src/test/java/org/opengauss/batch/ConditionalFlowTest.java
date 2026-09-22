package org.opengauss.batch;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

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
 * 补测四：条件流。stepA 按开关把 ExitStatus 置为 FAILED / COMPLETED，
 * 流程按 on("FAILED") / on("*") 分支到 fallback / success（此前仅覆盖顺序 .next()）。
 * 步骤执行顺序从 BATCH_STEP_EXECUTION 按 STEP_EXECUTION_ID 核对。
 */
@SpringBootTest
class ConditionalFlowTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("conditionalFlowJob")
    private Job conditionalFlowJob;

    @Autowired
    private AtomicBoolean flowFailArmed;

    @Autowired
    private CopyOnWriteArrayList<String> flowBranch;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        flowBranch.clear();
        flowFailArmed.set(true);
    }

    @AfterEach
    void tearDown() {
        flowFailArmed.set(false);
    }

    @Test
    void exitStatusRoutesToFallbackThenSuccess() throws Exception {
        // 分支一：stepA FAILED -> flowFallbackStep
        JobExecution run1 = jobLauncher.run(conditionalFlowJob, new JobParametersBuilder()
                .addString("run.id", "flow-fail-" + System.currentTimeMillis()).toJobParameters());
        assertEquals(BatchStatus.COMPLETED, run1.getStatus());
        assertEquals(List.of("fallback"), flowBranch);
        assertEquals(List.of("flowStepA", "flowFallbackStep"), stepNames(run1.getId()));
        System.out.println("[CASE] FAILED 分支 -> fallback: " + stepNames(run1.getId()));

        // 分支二：stepA 正常 -> flowSuccessStep
        flowFailArmed.set(false);
        flowBranch.clear();
        JobExecution run2 = jobLauncher.run(conditionalFlowJob, new JobParametersBuilder()
                .addString("run.id", "flow-ok-" + System.currentTimeMillis()).toJobParameters());
        assertEquals(BatchStatus.COMPLETED, run2.getStatus());
        assertEquals(List.of("success"), flowBranch);
        assertEquals(List.of("flowStepA", "flowSuccessStep"), stepNames(run2.getId()));
        System.out.println("[CASE] * 分支 -> success: " + stepNames(run2.getId()));
    }

    private List<String> stepNames(long jobExecutionId) {
        return jdbc.queryForList(
                "SELECT STEP_NAME FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID = ? "
                        + "ORDER BY STEP_EXECUTION_ID",
                String.class, jobExecutionId);
    }
}
