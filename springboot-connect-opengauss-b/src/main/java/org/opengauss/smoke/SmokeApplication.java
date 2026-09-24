package org.opengauss.smoke;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 冒烟程序：验证 MySQL JDBC -> dolphin 协议 -> B 库 全链路。
 * 启动前确认：openGauss 已开启 enable_dolphin_proto，B 库/schema/user 表已按 README 建好。
 */
@SpringBootApplication
public class SmokeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmokeApplication.class, args);
    }

    @Component
    public static class SmokeRunner implements CommandLineRunner {

        private final JdbcTemplate jdbc;

        public SmokeRunner(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @Override
        public void run(String... args) {
            // 1. 连接与查询
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM `user`", Integer.class);
            System.out.println("[SMOKE] 连接成功，`user` 表行数 = " + count);

            // 2. 插入
            jdbc.update("INSERT INTO `user` (`name`, `age`) VALUES (?, ?)", "smoke", 99);
            Integer age = jdbc.queryForObject(
                    "SELECT `age` FROM `user` WHERE `name` = ?", Integer.class, "smoke");
            System.out.println("[SMOKE] 插入成功，age = " + age);

            // 3. 更新
            jdbc.update("UPDATE `user` SET `age` = ? WHERE `name` = ?", 100, "smoke");
            System.out.println("[SMOKE] 更新成功");

            // 4. 删除
            jdbc.update("DELETE FROM `user` WHERE `name` = ?", "smoke");
            System.out.println("[SMOKE] 删除成功，CRUD 全链路通过");
        }
    }
}
