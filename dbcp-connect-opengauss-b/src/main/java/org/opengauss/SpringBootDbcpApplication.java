package org.opengauss;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.UUID;

/**
 * Spring Boot + DBCP2 + MySQL Connector/J 连接 openGauss B 兼容库的可运行示例。
 *
 * <p>启动时会输出实际数据源类型，并通过 JdbcTemplate 执行查询、提交和回滚，
 * 用完连接后由 Spring/DBCP 自动归还连接池。</p>
 */
@SpringBootApplication
public class SpringBootDbcpApplication {

    private static final Logger log = LoggerFactory.getLogger(SpringBootDbcpApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringBootDbcpApplication.class, args);
    }

    @Bean
    public CommandLineRunner verifyDbcp(DataSource dataSource, JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager) {
        return args -> {
            if (!(dataSource instanceof BasicDataSource)) {
                throw new IllegalStateException("实际数据源不是 DBCP BasicDataSource: "
                        + dataSource.getClass().getName());
            }
            BasicDataSource dbcp = (BasicDataSource) dataSource;
            if (dbcp.getTimeBetweenEvictionRunsMillis() <= 0) {
                throw new IllegalStateException(
                        "DBCP 空闲连接维护线程未启用，请配置 spring.datasource.dbcp2.time-between-eviction-runs-millis");
            }
            log.info("Spring 注入数据源: {}", dataSource.getClass().getName());
            log.info("DBCP 参数: initialSize={}, maxTotal={}, maxWaitMillis={}, "
                            + "testWhileIdle={}, timeBetweenEvictionRunsMillis={}, "
                            + "minEvictableIdleTimeMillis={}",
                    dbcp.getInitialSize(), dbcp.getMaxTotal(), dbcp.getMaxWaitMillis(),
                    dbcp.getTestWhileIdle(), dbcp.getTimeBetweenEvictionRunsMillis(),
                    dbcp.getMinEvictableIdleTimeMillis());

            Integer initialRows = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM `user`", Integer.class);
            log.info("JdbcTemplate 查询 user 表: {} 行", initialRows);

            String marker = "spring-" + UUID.randomUUID().toString().replace("-", "");
            String rollbackMarker = marker + "-rollback";
            TransactionTemplate transactions = new TransactionTemplate(transactionManager);
            try {
                // 提交事务：插入后由另一个连接可见，证明 Spring 事务管理器已生效。
                transactions.execute(status -> {
                    jdbcTemplate.update("INSERT INTO `user`(`name`,`age`) VALUES (?,?)", marker, 30);
                    return null;
                });
                Integer committed = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM `user` WHERE `name` = ?", Integer.class, marker);
                if (!Integer.valueOf(1).equals(committed)) {
                    throw new IllegalStateException("提交事务后的数据行数应为 1，实际为 " + committed);
                }
                log.info("TransactionTemplate COMMIT 后查询到 {} 行", committed);

                // 回滚事务：标记回滚后不得留下数据。正常情况下 execute 不会抛出异常；
                // 兼容事务管理器将 rollback-only 结果报告为 UnexpectedRollbackException 的实现。
                try {
                    transactions.execute(status -> {
                        jdbcTemplate.update("INSERT INTO `user`(`name`,`age`) VALUES (?,?)",
                                rollbackMarker, 31);
                        status.setRollbackOnly();
                        return null;
                    });
                } catch (UnexpectedRollbackException expected) {
                    log.info("TransactionTemplate 已按 rollback-only 标记回滚: {}", expected.getMessage());
                }
                Integer rolledBack = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM `user` WHERE `name` = ?", Integer.class, rollbackMarker);
                if (!Integer.valueOf(0).equals(rolledBack)) {
                    throw new IllegalStateException("回滚事务后的数据行数应为 0，实际为 " + rolledBack);
                }
                log.info("TransactionTemplate ROLLBACK 后查询到 {} 行", rolledBack);
            } finally {
                jdbcTemplate.update("DELETE FROM `user` WHERE `name` IN (?, ?)", marker, rollbackMarker);
                log.info("Spring Boot DBCP 链路验证完成，临时数据已清理");
            }
        };
    }
}
