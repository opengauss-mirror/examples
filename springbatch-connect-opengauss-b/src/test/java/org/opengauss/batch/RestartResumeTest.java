package org.opengauss.batch;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 补测一：失败重启与断点恢复（Spring Batch 招牌特性；失败状态落库受 EXIT_MESSAGE 列长度限制）。
 * 前置：@BeforeEach 放大 EXIT_MESSAGE 列（ALTER 到 VARCHAR(10000)），
 * 使失败栈可落库——本用例同时验证该方式在 B 模式实测可行。
 * 流程：首轮第 4 行（RESTART_FAIL）抛错 -> 作业 FAILED；翻 off 故障开关后
 * JobOperator.restart(execId) 复用原参数重启 -> 从最后提交的 chunk 后断点续跑，作业 COMPLETED。
 */
@SpringBootTest
class RestartResumeTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    @Qualifier("restartJob")
    private Job restartJob;

    @Autowired
    private AtomicBoolean restartFailArmed;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        // 放大失败栈落库列（job/step 两张表都要放大）
        enlargeExitMessages("BATCH_JOB_EXECUTION");
        enlargeExitMessages("BATCH_STEP_EXECUTION");

        jdbc.update("DROP TABLE IF EXISTS user_restart");
        jdbc.update("DROP TABLE IF EXISTS dst_restart");
        jdbc.update("CREATE TABLE user_restart (id INT PRIMARY KEY, name VARCHAR(50), age INT)");
        jdbc.update("CREATE TABLE dst_restart (id INT PRIMARY KEY, name VARCHAR(50), age INT)");
        for (int i = 1; i <= 6; i++) {
            jdbc.update("INSERT INTO user_restart (id, name, age) VALUES (?, ?, ?)",
                    i, i == 4 ? "RESTART_FAIL" : "r" + i, 20 + i);
        }
        restartFailArmed.set(true);
    }

    @AfterEach
    void tearDown() {
        restartFailArmed.set(false);
        jdbc.update("DROP TABLE IF EXISTS user_restart");
        jdbc.update("DROP TABLE IF EXISTS dst_restart");
        // 清理本作业元数据后把 EXIT_MESSAGE 列恢复 2500，
        // 避免影响依赖默认列宽的 RestartTest（测试间共享同一套 BATCH_* 表）
        String execIds = "SELECT JOB_EXECUTION_ID FROM BATCH_JOB_EXECUTION WHERE JOB_INSTANCE_ID IN "
                + "(SELECT JOB_INSTANCE_ID FROM BATCH_JOB_INSTANCE WHERE JOB_NAME = 'restartJob')";
        String stepIds = "SELECT STEP_EXECUTION_ID FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID IN (" + execIds + ")";
        // 按 FK 依赖逆序：CONTEXT -> STEP_EXECUTION -> PARAMS -> JOB_EXECUTION_CONTEXT -> EXECUTION -> INSTANCE
        jdbc.update("DELETE FROM BATCH_STEP_EXECUTION_CONTEXT WHERE STEP_EXECUTION_ID IN (" + stepIds + ")");
        jdbc.update("DELETE FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID IN (" + execIds + ")");
        jdbc.update("DELETE FROM BATCH_JOB_EXECUTION_PARAMS WHERE JOB_EXECUTION_ID IN (" + execIds + ")");
        jdbc.update("DELETE FROM BATCH_JOB_EXECUTION_CONTEXT WHERE JOB_EXECUTION_ID IN (" + execIds + ")");
        jdbc.update("DELETE FROM BATCH_JOB_EXECUTION WHERE JOB_EXECUTION_ID IN (" + execIds + ")");
        jdbc.update("DELETE FROM BATCH_JOB_INSTANCE WHERE JOB_NAME = 'restartJob'");
        resizeExitMessages("BATCH_JOB_EXECUTION", 2500);
        resizeExitMessages("BATCH_STEP_EXECUTION", 2500);
    }

    @Test
    void failThenRestartResumesFromLastCommittedChunk() throws Exception {
        String runId = "restart-" + System.currentTimeMillis();

        // 首轮：第 4 行失败，作业 FAILED；chunk=2，仅 chunk1（前 2 行）已提交。
        // 注意：TaskExecutorJobLauncher.run 对作业执行失败不抛异常（只对启动期错误抛），
        // 而是返回 FAILED 状态的 JobExecution——此处按框架语义断言状态。
        JobExecution first = jobLauncher.run(restartJob,
                new JobParametersBuilder().addString("run.id", runId).toJobParameters());
        assertEquals(BatchStatus.FAILED, first.getStatus());

        long firstExecId = execIdByRunId(runId);
        Map<String, Object> exec = jdbc.queryForMap(
                "SELECT STATUS, EXIT_MESSAGE FROM BATCH_JOB_EXECUTION WHERE JOB_EXECUTION_ID = ?",
                firstExecId);
        assertEquals("FAILED", exec.get("STATUS"));
        String exitMessage = (String) exec.get("EXIT_MESSAGE");
        // 放大列宽生效：完整异常栈成功落库（未放大前此处写入即报 error 130）
        assertNotNull(exitMessage);
        assertTrue(exitMessage.length() > 2000, "EXIT_MESSAGE 应存下完整异常栈，实际长度 " + exitMessage.length());
        assertEquals("FAILED", jdbc.queryForObject(
                "SELECT STATUS FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID = ?",
                String.class, firstExecId));
        System.out.println("[CASE] 首轮失败落库: FAILED, EXIT_MESSAGE 长度 = " + exitMessage.length()
                + "（ALTER 放大列后写入成功）");

        // 关掉故障开关，JobOperator.restart 复用原参数重启（返回新执行 ID）
        restartFailArmed.set(false);
        long reId = jobOperator.restart(firstExecId);
        JobExecution re = jobExplorer.getJobExecution(reId);
        assertNotNull(re);
        assertEquals(BatchStatus.COMPLETED, re.getStatus());
        assertEquals(6, jdbc.queryForObject("SELECT COUNT(*) FROM dst_restart", Integer.class));

        // 断点恢复：chunk1（2 行）已提交不重跑；重启从第 3 行续读 4 行、2 个 chunk
        Map<String, Object> counts = jdbc.queryForMap(
                "SELECT READ_COUNT, WRITE_COUNT, COMMIT_COUNT FROM BATCH_STEP_EXECUTION "
                        + "WHERE STEP_EXECUTION_ID = ?",
                re.getStepExecutions().iterator().next().getId());
        assertEquals(4, num(counts.get("READ_COUNT")), "重启后应跳过已提交的 2 行、续读 4 行");
        assertEquals(4, num(counts.get("WRITE_COUNT")));
        System.out.println("[CASE] 重启续跑完成: READ=" + counts.get("READ_COUNT")
                + ", WRITE=" + counts.get("WRITE_COUNT") + ", COMMIT=" + counts.get("COMMIT_COUNT"));

        // 同一 JobInstance 复用（同参数）：实例 1 个、执行 2 个
        Integer instanceId = jdbc.queryForObject(
                "SELECT JOB_INSTANCE_ID FROM BATCH_JOB_EXECUTION WHERE JOB_EXECUTION_ID = ?",
                Integer.class, firstExecId);
        assertEquals(2, jdbc.queryForObject(
                "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION WHERE JOB_INSTANCE_ID = ?",
                Integer.class, instanceId));
        assertFalse(re.getId().equals(firstExecId));
        System.out.println("[CASE] 同一 JobInstance 下执行 2 次（首败 + 重启成功）");
    }

    private void enlargeExitMessages(String table) {
        resizeExitMessages(table, 10000);
    }

    private void resizeExitMessages(String table, int size) {
        try {
            jdbc.execute("ALTER TABLE " + table + " MODIFY EXIT_MESSAGE VARCHAR(" + size + ")");
        } catch (Exception e1) {
            jdbc.execute("ALTER TABLE " + table + " ALTER COLUMN EXIT_MESSAGE TYPE VARCHAR(" + size + ")");
        }
    }

    private long execIdByRunId(String runId) {
        return jdbc.queryForObject(
                "SELECT JOB_EXECUTION_ID FROM BATCH_JOB_EXECUTION_PARAMS "
                        + "WHERE PARAMETER_NAME = 'run.id' AND PARAMETER_VALUE = ?",
                Long.class, runId);
    }

    private static int num(Object v) {
        return ((Number) v).intValue();
    }
}
