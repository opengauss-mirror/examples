package org.opengauss.batch;

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
 * 冒烟用例：需真实 openGauss B 库环境（README 前置条件满足后才能运行）。
 * 同一套用例配合 pom 里不同 driver profile 执行，即构成驱动版本扫测矩阵的用例组。
 * 断言全部用"前后差值"写法：BATCH_* 元数据表在同一次测试会话内累计，不依赖绝对值。
 */
@SpringBootTest
class SmokeTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("copyUserJob")
    private Job job;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void runJobCopiesRowsAndWritesMetadata() throws Exception {
        jdbc.update("DELETE FROM user_batch_out");
        int src = count("`user`");
        int instancesBefore = count("BATCH_JOB_INSTANCE");
        int executionsBefore = count("BATCH_JOB_EXECUTION");
        int stepsBefore = count("BATCH_STEP_EXECUTION");

        JobExecution ex = jobLauncher.run(job, new JobParametersBuilder()
                .addString("run.id", "smoke-" + System.currentTimeMillis()).toJobParameters());

        assertEquals(BatchStatus.COMPLETED, ex.getStatus());
        // 源表全部行落输出表，且处理逻辑生效（name=UPPER(name)、age=age+1）
        assertEquals(src, count("user_batch_out"));
        Integer transformed = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_batch_out o JOIN `user` u ON o.id = u.id "
                        + "WHERE o.age = u.age + 1 AND o.name = UPPER(u.name)", Integer.class);
        assertEquals(src, transformed);
        // JobRepository 元数据各表 +1：实例/执行/步骤/参数
        assertEquals(instancesBefore + 1, count("BATCH_JOB_INSTANCE"));
        assertEquals(executionsBefore + 1, count("BATCH_JOB_EXECUTION"));
        assertEquals(stepsBefore + 1, count("BATCH_STEP_EXECUTION"));
        Integer params = jdbc.queryForObject(
                "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION_PARAMS WHERE PARAMETER_NAME = 'run.id'", Integer.class);
        assertEquals(executionsBefore + 1, params);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
