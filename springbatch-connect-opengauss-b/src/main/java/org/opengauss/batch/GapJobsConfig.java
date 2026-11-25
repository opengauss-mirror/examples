package org.opengauss.batch;

import static org.opengauss.batch.CopyUserJobConfig.UserRow;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.batch.item.database.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 覆盖度补测作业（对话评审新增）：
 * restartJob 失败重启与断点恢复 / pagingJob 分页读（LIMIT/OFFSET）/ moreListenersJob 监听器族
 * （Step/Chunk/Skip/ItemProcess）/ conditionalFlowJob 条件流 / lateBindingJob StepScope+JobScope 晚绑定。
 * 各作业的表均由对应测试类自建自删，与其他用例互不干扰。
 */
@Configuration
public class GapJobsConfig {

    // ===== 一、restartJob：中途失败 -> 重启从断点恢复（放大 EXIT_MESSAGE 列宽后全链路） =====

    /** 首轮运行是否触发失败（测试在重启前翻为 false）。 */
    @Bean
    public AtomicBoolean restartFailArmed() {
        return new AtomicBoolean(true);
    }

    @Bean
    public Job restartJob(JobRepository jobRepository, Step restartStep) {
        // 不加 RunIdIncrementer：重启必须复用首轮的 JobParameters
        return new JobBuilder("restartJob", jobRepository).start(restartStep).build();
    }

    @Bean
    public Step restartStep(JobRepository jobRepository, PlatformTransactionManager txManager,
                            DataSource dataSource, AtomicBoolean restartFailArmed) {
        JdbcCursorItemReader<UserRow> reader = new JdbcCursorItemReader<>();
        reader.setDataSource(dataSource);
        reader.setSql("SELECT id, name, age FROM user_restart ORDER BY id");
        reader.setRowMapper((rs, n) -> new UserRow(rs.getInt("id"), rs.getString("name"), rs.getInt("age")));

        JdbcBatchItemWriter<UserRow> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setSql("INSERT INTO dst_restart (id, name, age) VALUES (?, ?, ?)");
        writer.setItemPreparedStatementSetter((row, ps) -> {
            ps.setInt(1, row.id());
            ps.setString(2, row.name());
            ps.setInt(3, row.age());
        });
        try {
            writer.afterPropertiesSet();
        } catch (Exception e) {
            throw new IllegalStateException("restartStep writer 初始化失败", e);
        }

        return new StepBuilder("restartStep", jobRepository)
                .<UserRow, UserRow>chunk(2, txManager)
                .reader(reader)
                .processor((ItemProcessor<UserRow, UserRow>) row -> {
                    if ("RESTART_FAIL".equals(row.name()) && restartFailArmed.get()) {
                        throw new IllegalStateException("模拟第 4 行处理中途失败");
                    }
                    return row;
                })
                .writer(writer)
                .build();
    }

    // ===== 二、pagingJob：JdbcPagingItemReader 分页读（LIMIT ? OFFSET ?） =====

    @Bean
    public Job pagingJob(JobRepository jobRepository, Step pagingStep) {
        return new JobBuilder("pagingJob", jobRepository).start(pagingStep).build();
    }

    @Bean
    public Step pagingStep(JobRepository jobRepository, PlatformTransactionManager txManager,
                           DataSource dataSource) {
        // 框架按 MySQL 方言生成 SELECT ... ORDER BY id LIMIT ? OFFSET ?（B 库解析兼容点）。
        // 用 OpenGaussPagingItemReader 规避 FINDING B：基类初始化会 setMaxRows(pageSize)，
        // Connector/J 以 SET sql_select_limit 实现该语义，B 库不识别（error 28804）。
        MySqlPagingQueryProvider provider = new MySqlPagingQueryProvider();
        provider.setSelectClause("id, name, age");
        provider.setFromClause("src_page");
        provider.setSortKeys(Map.of("id", Order.ASCENDING));

        OpenGaussPagingItemReader<UserRow> reader = new OpenGaussPagingItemReader<>();
        reader.setName("pagingReader");
        reader.setDataSource(dataSource);
        reader.setQueryProvider(provider);
        reader.setPageSize(20);
        reader.setRowMapper((rs, n) -> new UserRow(rs.getInt("id"), rs.getString("name"), rs.getInt("age")));
        // 工厂内 new 的 reader 非 bean，无生命周期回调，需手动初始化
        try {
            reader.afterPropertiesSet();
        } catch (Exception e) {
            throw new IllegalStateException("pagingStep reader 初始化失败", e);
        }

        JdbcBatchItemWriter<UserRow> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setSql("INSERT INTO dst_page (id, name, age) VALUES (?, ?, ?)");
        writer.setItemPreparedStatementSetter((row, ps) -> {
            ps.setInt(1, row.id());
            ps.setString(2, row.name());
            ps.setInt(3, row.age());
        });
        try {
            writer.afterPropertiesSet();
        } catch (Exception e) {
            throw new IllegalStateException("pagingStep writer 初始化失败", e);
        }

        return new StepBuilder("pagingStep", jobRepository)
                .<UserRow, UserRow>chunk(20, txManager)
                .reader(reader)
                .processor((ItemProcessor<UserRow, UserRow>) row -> row)
                .writer(writer)
                .build();
    }

    // ===== 三、moreListenersJob：StepExecution / Chunk / Skip / ItemProcess 监听器 =====

    @Bean
    public AtomicInteger stepBeforeCount() {
        return new AtomicInteger();
    }

    @Bean
    public AtomicInteger stepAfterCount() {
        return new AtomicInteger();
    }

    @Bean
    public AtomicInteger chunkBeforeCount() {
        return new AtomicInteger();
    }

    @Bean
    public AtomicInteger chunkAfterCount() {
        return new AtomicInteger();
    }

    @Bean
    public AtomicInteger processAfterCount() {
        return new AtomicInteger();
    }

    /** 触发 afterProcess 的行名序列（排查重跑/复用行为用，按触发顺序记录）。 */
    @Bean
    public CopyOnWriteArrayList<String> processedNames() {
        return new CopyOnWriteArrayList<>();
    }

    @Bean
    public CopyOnWriteArrayList<String> skippedNames() {
        return new CopyOnWriteArrayList<>();
    }

    @Bean
    public CopyOnWriteArrayList<String> stepNamesSeen() {
        return new CopyOnWriteArrayList<>();
    }

    @Bean
    public Job moreListenersJob(JobRepository jobRepository, Step moreListenersStep) {
        return new JobBuilder("moreListenersJob", jobRepository).start(moreListenersStep).build();
    }

    @Bean
    public Step moreListenersStep(JobRepository jobRepository, PlatformTransactionManager txManager,
                                  DataSource dataSource,
                                  AtomicInteger stepBeforeCount, AtomicInteger stepAfterCount,
                                  AtomicInteger chunkBeforeCount, AtomicInteger chunkAfterCount,
                                  AtomicInteger processAfterCount,
                                  CopyOnWriteArrayList<String> processedNames,
                                  CopyOnWriteArrayList<String> skippedNames,
                                  CopyOnWriteArrayList<String> stepNamesSeen) {
        JdbcCursorItemReader<UserRow> reader = new JdbcCursorItemReader<>();
        reader.setDataSource(dataSource);
        reader.setSql("SELECT id, name, age FROM src_listen ORDER BY id");
        reader.setRowMapper((rs, n) -> new UserRow(rs.getInt("id"), rs.getString("name"), rs.getInt("age")));

        JdbcBatchItemWriter<UserRow> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setSql("INSERT INTO dst_listen (id, name, age) VALUES (?, ?, ?)");
        writer.setItemPreparedStatementSetter((row, ps) -> {
            ps.setInt(1, row.id());
            ps.setString(2, row.name());
            ps.setInt(3, row.age());
        });
        try {
            writer.afterPropertiesSet();
        } catch (Exception e) {
            throw new IllegalStateException("moreListenersStep writer 初始化失败", e);
        }

        StepExecutionListener stepListener = new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                stepBeforeCount.incrementAndGet();
                stepNamesSeen.add(stepExecution.getStepName());
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                stepAfterCount.incrementAndGet();
                return stepExecution.getExitStatus();
            }
        };

        ChunkListener chunkListener = new ChunkListener() {
            @Override
            public void beforeChunk(ChunkContext context) {
                chunkBeforeCount.incrementAndGet();
            }

            @Override
            public void afterChunk(ChunkContext context) {
                chunkAfterCount.incrementAndGet();
            }
        };

        return new StepBuilder("moreListenersStep", jobRepository)
                .<UserRow, UserRow>chunk(2, txManager)
                .reader(reader)
                .processor((ItemProcessor<UserRow, UserRow>) row -> {
                    if ("SKIP_ME".equals(row.name())) {
                        throw new IllegalArgumentException("坏数据行：" + row.name());
                    }
                    return row;
                })
                .writer(writer)
                .listener(stepListener)
                .listener(chunkListener)
                .listener(new ItemProcessListener<UserRow, UserRow>() {
                    @Override
                    public void afterProcess(UserRow item, UserRow result) {
                        processAfterCount.incrementAndGet();
                        processedNames.add(item.name());
                    }
                })
                .faultTolerant()
                .skip(IllegalArgumentException.class)
                .skipLimit(100)
                .listener(new org.springframework.batch.core.SkipListener<UserRow, UserRow>() {
                    @Override
                    public void onSkipInProcess(UserRow item, Throwable t) {
                        skippedNames.add(item.name());
                    }
                })
                .build();
    }

    // ===== 四、conditionalFlowJob：按 ExitStatus 条件流分支 =====

    @Bean
    public AtomicBoolean flowFailArmed() {
        return new AtomicBoolean(true);
    }

    @Bean
    public CopyOnWriteArrayList<String> flowBranch() {
        return new CopyOnWriteArrayList<>();
    }

    @Bean
    public Job conditionalFlowJob(JobRepository jobRepository, Step flowStepA,
                                  Step flowFallbackStep, Step flowSuccessStep) {
        return new JobBuilder("conditionalFlowJob", jobRepository)
                .start(flowStepA)
                .on("FAILED").to(flowFallbackStep)
                .from(flowStepA).on("*").to(flowSuccessStep)
                .end()
                .build();
    }

    @Bean
    public Step flowStepA(JobRepository jobRepository, PlatformTransactionManager txManager,
                          AtomicBoolean flowFailArmed) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            if (flowFailArmed.get()) {
                contribution.setExitStatus(ExitStatus.FAILED);
            }
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("flowStepA", jobRepository).tasklet(tasklet, txManager).build();
    }

    @Bean
    public Step flowFallbackStep(JobRepository jobRepository, PlatformTransactionManager txManager,
                                 CopyOnWriteArrayList<String> flowBranch) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            flowBranch.add("fallback");
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("flowFallbackStep", jobRepository).tasklet(tasklet, txManager).build();
    }

    @Bean
    public Step flowSuccessStep(JobRepository jobRepository, PlatformTransactionManager txManager,
                                CopyOnWriteArrayList<String> flowBranch) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            flowBranch.add("success");
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("flowSuccessStep", jobRepository).tasklet(tasklet, txManager).build();
    }

    // ===== 五、lateBindingJob：@JobScope Step 动态 chunk size + @StepScope reader + @JobScope tasklet =====

    @Bean
    public Job lateBindingJob(JobRepository jobRepository, Step lbStep, Step lbMarkStep) {
        return new JobBuilder("lateBindingJob", jobRepository)
                .start(lbStep)
                .next(lbMarkStep)
                .build();
    }

    /** @StepScope reader：每个 step 执行时创建（游标每次打开），标准晚绑定作用域用法。 */
    @Bean
    @StepScope
    public JdbcCursorItemReader<UserRow> lbReader(DataSource dataSource) {
        JdbcCursorItemReader<UserRow> reader = new JdbcCursorItemReader<>();
        reader.setDataSource(dataSource);
        reader.setSql("SELECT id, name, age FROM src_lb ORDER BY id");
        reader.setRowMapper((rs, n) -> new UserRow(rs.getInt("id"), rs.getString("name"), rs.getInt("age")));
        return reader;
    }

    /** @JobScope Step：job 启动时才创建，chunk size 由作业参数 chunkSize 晚绑定（默认 2）。 */
    @Bean
    @JobScope
    public Step lbStep(JobRepository jobRepository, PlatformTransactionManager txManager,
                       DataSource dataSource, JdbcCursorItemReader<UserRow> lbReader,
                       @Value("#{jobParameters['chunkSize']}") Integer chunkSize) {

        JdbcBatchItemWriter<UserRow> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setSql("INSERT INTO dst_lb (id, name, age) VALUES (?, ?, ?)");
        writer.setItemPreparedStatementSetter((row, ps) -> {
            ps.setInt(1, row.id());
            ps.setString(2, row.name());
            ps.setInt(3, row.age());
        });
        try {
            writer.afterPropertiesSet();
        } catch (Exception e) {
            throw new IllegalStateException("lbStep writer 初始化失败", e);
        }

        return new StepBuilder("lbStep", jobRepository)
                .<UserRow, UserRow>chunk(chunkSize == null ? 2 : chunkSize, txManager)
                .reader(lbReader)
                .processor((ItemProcessor<UserRow, UserRow>) row -> row)
                .writer(writer)
                .build();
    }

    @Bean
    @JobScope
    public Tasklet lbMarkTasklet(@Value("#{jobParameters['mark']}") String mark, JdbcTemplate jdbc) {
        return (contribution, chunkContext) -> {
            jdbc.update("INSERT INTO dst_lb_mark (mark) VALUES (?)", mark == null ? "(null)" : mark);
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step lbMarkStep(JobRepository jobRepository, PlatformTransactionManager txManager,
                           Tasklet lbMarkTasklet) {
        return new StepBuilder("lbMarkStep", jobRepository).tasklet(lbMarkTasklet, txManager).build();
    }

    // ===== 六、explorer 断言用的公开 reader/writer 不需要：ExplorerOperatorTest 复用 copyUserJob =====
}
