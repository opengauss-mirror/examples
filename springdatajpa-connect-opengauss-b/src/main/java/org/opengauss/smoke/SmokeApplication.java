package org.opengauss.smoke;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * JPA 冒烟程序：验证 Hibernate 6 在 MySQL 协议链路上的行为。
 * 重点观察：启动元数据查询（show-sql 可见）、IDENTITY 主键回填、分页 SQL。
 */
@SpringBootApplication
public class SmokeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmokeApplication.class, args);
    }

    @Component
    public static class SmokeRunner implements CommandLineRunner {

        private final UserRepository repo;

        public SmokeRunner(UserRepository repo) {
            this.repo = repo;
        }

        @Override
        public void run(String... args) {
            // 1. 查询
            long count = repo.count();
            System.out.println("[SMOKE] 连接成功，User 总数 = " + count);

            // 2. 保存：验证 IDENTITY 主键回填（getGeneratedKeys）
            User saved = repo.save(new User("smoke", 99));
            System.out.println("[SMOKE] 保存成功，主键回填 id = " + saved.getId());

            // 3. 分页：Hibernate 生成 LIMIT/OFFSET
            Page<User> page = repo.findAll(PageRequest.of(0, 2));
            System.out.println("[SMOKE] 分页成功，本页 " + page.getContent().size()
                    + " 条，共 " + page.getTotalElements() + " 条");

            // 4. 模糊查询（显式 escape '!' 规避 B 模式反斜杠转义问题）+ 删除
            Page<User> found = repo.searchByName("%smoke%", PageRequest.of(0, 5));
            System.out.println("[SMOKE] 模糊查询成功，命中 " + found.getTotalElements() + " 条");
            repo.delete(saved);
            System.out.println("[SMOKE] 删除成功，JPA 全链路通过");
        }
    }
}
