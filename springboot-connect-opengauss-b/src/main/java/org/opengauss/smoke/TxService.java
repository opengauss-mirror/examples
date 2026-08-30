package org.opengauss.smoke;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 事务行为验证用服务：@Transactional 提交/回滚由 Spring 事务管理驱动。 */
@Service
public class TxService {

    private final JdbcTemplate jdbc;

    public TxService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void insertThenThrow(String name, int age) {
        jdbc.update("INSERT INTO `user` (`name`, `age`) VALUES (?, ?)", name, age);
        throw new RuntimeException("事务回滚验证");
    }
}
