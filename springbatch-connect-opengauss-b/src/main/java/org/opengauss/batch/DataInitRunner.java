package org.opengauss.batch;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 每次应用启动时重建作业输出表，保证用例可重复执行。
 * 输入表 `user` 复用 #27 任务建好的表（数据不在此改动）。
 * DROP IF EXISTS + CREATE 的 MySQL DDL 在 B 模式下执行，本身也是 DDL 兼容性验证点。
 */
@Component
public class DataInitRunner implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    public DataInitRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        jdbc.update("DROP TABLE IF EXISTS user_batch_out");
        jdbc.update("CREATE TABLE user_batch_out (id INT PRIMARY KEY, name VARCHAR(50), age INT)");
        System.out.println("[CASE] user_batch_out 表已重建");
    }
}
