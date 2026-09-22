package org.opengauss.batch;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 示例作业 copyUserJob：chunk 模式读 `user` 表 -> 名字转大写、age+1 -> 写 user_batch_out 表。
 * chunk=2 使 3 行源数据切成 2 个事务提交，覆盖 JobRepository 的 step 执行/上下文元数据读写。
 */
@Configuration
public class CopyUserJobConfig {

    /** 作业中流转的行对象。 */
    public record UserRow(int id, String name, int age) {
    }

    @Bean
    public Job copyUserJob(JobRepository jobRepository, Step copyUserStep) {
        return new JobBuilder("copyUserJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(copyUserStep)
                .build();
    }

    @Bean
    public Step copyUserStep(JobRepository jobRepository, PlatformTransactionManager txManager,
                             JdbcCursorItemReader<UserRow> userReader,
                             ItemProcessor<UserRow, UserRow> nameUpper,
                             JdbcBatchItemWriter<UserRow> userOutWriter) {
        return new StepBuilder("copyUserStep", jobRepository)
                .<UserRow, UserRow>chunk(2, txManager)
                .reader(userReader)
                .processor(nameUpper)
                .writer(userOutWriter)
                .build();
    }

    @Bean
    public JdbcCursorItemReader<UserRow> userReader(DataSource dataSource) {
        JdbcCursorItemReader<UserRow> reader = new JdbcCursorItemReader<>();
        reader.setDataSource(dataSource);
        reader.setSql("SELECT id, name, age FROM `user` ORDER BY id");
        reader.setRowMapper((rs, rowNum) ->
                new UserRow(rs.getInt("id"), rs.getString("name"), rs.getInt("age")));
        return reader;
    }

    @Bean
    public ItemProcessor<UserRow, UserRow> nameUpper() {
        return row -> new UserRow(row.id(), row.name().toUpperCase(), row.age() + 1);
    }

    @Bean
    public JdbcBatchItemWriter<UserRow> userOutWriter(DataSource dataSource) throws Exception {
        JdbcBatchItemWriter<UserRow> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setSql("INSERT INTO user_batch_out (id, name, age) VALUES (?, ?, ?)");
        writer.setItemPreparedStatementSetter((row, ps) -> {
            ps.setInt(1, row.id());
            ps.setString(2, row.name());
            ps.setInt(3, row.age());
        });
        writer.afterPropertiesSet();
        return writer;
    }
}
