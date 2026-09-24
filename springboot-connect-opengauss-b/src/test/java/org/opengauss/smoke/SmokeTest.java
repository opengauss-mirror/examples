package org.opengauss.smoke;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 冒烟用例：需真实 openGauss B 库环境（README 前置条件满足后才能运行）。
 * 同一套用例配合 pom 里不同 driver profile 执行，即构成驱动版本扫测矩阵的用例组。
 */
@SpringBootTest
class SmokeTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TxService txService;

    @Test
    void crud() {
        jdbc.update("INSERT INTO `user` (`name`, `age`) VALUES (?, ?)", "t_crud", 1);
        Integer age = jdbc.queryForObject(
                "SELECT `age` FROM `user` WHERE `name` = ?", Integer.class, "t_crud");
        assertEquals(1, age);
        jdbc.update("UPDATE `user` SET `age` = ? WHERE `name` = ?", 2, "t_crud");
        jdbc.update("DELETE FROM `user` WHERE `name` = ?", "t_crud");
    }

    @Test
    void batch() {
        List<Object[]> args = List.of(
                new Object[]{"t_batch1", 10},
                new Object[]{"t_batch2", 11});
        int[] rows = jdbc.batchUpdate(
                "INSERT INTO `user` (`name`, `age`) VALUES (?, ?)", args);
        assertEquals(2, rows.length);
        jdbc.update("DELETE FROM `user` WHERE `name` LIKE 't_batch%'");
    }

    @Test
    void transactionRollback() {
        try {
            txService.insertThenThrow("t_tx", 1);
        } catch (RuntimeException expected) {
            // 预期异常：触发回滚
        }
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE `name` = 't_tx'", Integer.class);
        assertEquals(0, count, "事务应已回滚，表中不应存在 t_tx");
    }
}
