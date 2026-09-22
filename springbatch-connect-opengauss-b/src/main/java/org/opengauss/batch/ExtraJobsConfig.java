package org.opengauss.batch;

import static org.opengauss.batch.CopyUserJobConfig.UserRow;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 各维度附加作业：skip / retry / 失败重启 / 监听器与执行上下文 / 多步骤 / 大数据分块。
 * 除大数据作业外均复用 CopyUserJobConfig 的 userReader/userOutWriter 链路（读 `user` 写 user_batch_out），
 * 测试串行执行、每个用例运行前自行清空输出表，共享 bean 无并发问题。
 */
@Configuration
public class ExtraJobsConfig {

    // ===== 一、skipJob：坏行跳过（faultTolerant + skip） =====

    @Bean
    public Job skipJob(JobRepository jobRepository, Step skipStep) {
        return new JobBuilder("skipJob", jobRepository).start(skipStep).build();
    }

    @Bean
    public Step skipStep(JobRepository jobRepository, PlatformTransactionManager txManager,
                         JdbcCursorItemReader<UserRow> userReader,
                         JdbcBatchItemWriter<UserRow> userOutWriter) {
        return new StepBuilder("skipStep", jobRepository)
                .<UserRow, UserRow>chunk(2, txManager)
                .reader(userReader)
                .processor((ItemProcessor<UserRow, UserRow>) row -> {
                    if ("SKIP_ME".equals(row.name())) {
                        throw new IllegalArgumentException("坏数据行：" + row.name());
                    }
                    return row;
                })
                .writer(userOutWriter)
                .faultTolerant()
                .skip(IllegalArgumentException.class)
                .skipLimit(100)
                .build();
    }

    // ===== 二、retryJob：瞬时故障重试（faultTolerant + retry） =====

    /** 各行的处理尝试次数，测试断言用。 */
    @Bean
    public ConcurrentHashMap<String, Integer> retryAttempts() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public Job retryJob(JobRepository jobRepository, Step retryStep) {
        return new JobBuilder("retryJob", jobRepository).start(retryStep).build();
    }

    @Bean
    public Step retryStep(JobRepository jobRepository, PlatformTransactionManager txManager,
                          JdbcCursorItemReader<UserRow> userReader,
                          JdbcBatchItemWriter<UserRow> userOutWriter,
                          ConcurrentHashMap<String, Integer> retryAttempts) {
        return new StepBuilder("retryStep", jobRepository)
                .<UserRow, UserRow>chunk(2, txManager)
                .reader(userReader)
                .processor((ItemProcessor<UserRow, UserRow>) row -> {
                    if ("RETRY_ME".equals(row.name())) {
                        int n = retryAttempts.merge(row.name(), 1, Integer::sum);
                        // retryLimit(2) = 含初试共 2 次尝试：第 1 次失败、第 2 次成功
                        if (n < 2) {
                            throw new IllegalStateException("模拟瞬时故障，第 " + n + " 次处理");
                        }
                    }
                    return row;
                })
                .writer(userOutWriter)
                .faultTolerant()
                .retry(IllegalStateException.class)
                .retryLimit(2)
                .build();
    }

    // ===== 三、failJob：无容错配置，坏行直接导致作业失败 =====

    @Bean
    public Job failJob(JobRepository jobRepository, Step failStep) {
        return new JobBuilder("failJob", jobRepository).start(failStep).build();
    }

    @Bean
    public Step failStep(JobRepository jobRepository, PlatformTransactionManager txManager,
                         JdbcCursorItemReader<UserRow> userReader,
                         JdbcBatchItemWriter<UserRow> userOutWriter) {
        return new StepBuilder("failStep", jobRepository)
                .<UserRow, UserRow>chunk(2, txManager)
                .reader(userReader)
                .processor((ItemProcessor<UserRow, UserRow>) row -> {
                    if ("FAIL_ME".equals(row.name())) {
                        throw new RuntimeException("模拟业务失败行：" + row.name());
                    }
                    return row;
                })
                .writer(userOutWriter)
                .build();
    }

    // ===== 四、listenerJob：JobExecutionListener + 执行上下文持久化 =====

    /** beforeJob 写入、afterJob 读出的标记值，测试断言用。 */
    @Bean
    public AtomicReference<String> listenerSeen() {
        return new AtomicReference<>();
    }

    @Bean
    public Job listenerJob(JobRepository jobRepository, Step listenerStep,
                           AtomicReference<String> listenerSeen) {
        return new JobBuilder("listenerJob", jobRepository)
                .listener(new JobExecutionListener() {
                    @Override
                    public void beforeJob(JobExecution jobExecution) {
                        jobExecution.getExecutionContext().putString("listener.marker", "hello-batch");
                    }

                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        listenerSeen.set(jobExecution.getExecutionContext().getString("listener.marker"));
                    }
                })
                .start(listenerStep)
                .build();
    }

    @Bean
    public Step listenerStep(JobRepository jobRepository, PlatformTransactionManager txManager,
                             JdbcCursorItemReader<UserRow> userReader,
                             JdbcBatchItemWriter<UserRow> userOutWriter) {
        return new StepBuilder("listenerStep", jobRepository)
                .<UserRow, UserRow>chunk(2, txManager)
                .reader(userReader)
                .processor((ItemProcessor<UserRow, UserRow>) row -> row)
                .writer(userOutWriter)
                .build();
    }

    // ===== 五、twoStepJob：多步骤作业（step1 chunk -> step2 tasklet，验证顺序与每步上下文） =====

    @Bean
    public Job twoStepJob(JobRepository jobRepository, Step step1Copy, Step step2CountTasklet) {
        return new JobBuilder("twoStepJob", jobRepository)
                .start(step1Copy)
                .next(step2CountTasklet)
                .build();
    }

    @Bean
    public Step step1Copy(JobRepository jobRepository, PlatformTransactionManager txManager,
                          JdbcCursorItemReader<UserRow> userReader,
                          JdbcBatchItemWriter<UserRow> userOutWriter) {
        return new StepBuilder("step1Copy", jobRepository)
                .<UserRow, UserRow>chunk(2, txManager)
                .reader(userReader)
                .processor((ItemProcessor<UserRow, UserRow>) row -> row)
                .writer(userOutWriter)
                .build();
    }

    @Bean
    public Step step2CountTasklet(JobRepository jobRepository, PlatformTransactionManager txManager,
                                  JdbcTemplate jdbc) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            Integer out = jdbc.queryForObject("SELECT COUNT(*) FROM user_batch_out", Integer.class);
            // step1 必须先于 step2 完成：此时输出表应有数据
            if (out == null || out == 0) {
                throw new IllegalStateException("step1 未先执行：user_batch_out 为空");
            }
            chunkContext.getStepContext().getStepExecution()
                    .getExecutionContext().putInt("step2.saw.rows", out);
            System.out.println("[CASE] step2 观测 user_batch_out 行数 = " + out);
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("step2CountTasklet", jobRepository)
                .tasklet(tasklet, txManager)
                .build();
    }

    // ===== 六、largeJob：大数据分块（1000 行，chunk=200 -> 5 次提交） =====

    @Bean
    public Job largeJob(JobRepository jobRepository, Step largeStep) {
        return new JobBuilder("largeJob", jobRepository).start(largeStep).build();
    }

    @Bean
    public Step largeStep(JobRepository jobRepository, PlatformTransactionManager txManager,
                          DataSource dataSource) {
        // 独立 reader/writer（不复用共享 bean），表由 LargeDataTest 建
        JdbcCursorItemReader<UserRow> reader = new JdbcCursorItemReader<>();
        reader.setDataSource(dataSource);
        reader.setSql("SELECT id, name, age FROM src_large ORDER BY id");
        reader.setRowMapper((rs, n) -> new UserRow(rs.getInt("id"), rs.getString("name"), rs.getInt("age")));

        JdbcBatchItemWriter<UserRow> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setSql("INSERT INTO dst_large (id, name, age) VALUES (?, ?, ?)");
        writer.setItemPreparedStatementSetter((row, ps) -> {
            ps.setInt(1, row.id());
            ps.setString(2, row.name());
            ps.setInt(3, row.age());
        });
        try {
            writer.afterPropertiesSet();
        } catch (Exception e) {
            throw new IllegalStateException("largeStep writer 初始化失败", e);
        }

        return new StepBuilder("largeStep", jobRepository)
                .<UserRow, UserRow>chunk(200, txManager)
                .reader(reader)
                .processor((ItemProcessor<UserRow, UserRow>) row -> row)
                .writer(writer)
                .build();
    }
}
