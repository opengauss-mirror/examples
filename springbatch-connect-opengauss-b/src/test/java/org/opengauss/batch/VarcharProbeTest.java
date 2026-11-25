package org.opengauss.batch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 诊断探针（FINDING 佐证）：B 模式 VARCHAR(n) 的长度语义。
 * 结论：按字符计（与 MySQL 一致）——2500 字符纯 ASCII 可插入；
 * 2496 字符/2502 字节（含多字节汉字）同样可插入，证明不是按字节截断。
 * 由此排除"EXIT_MESSAGE 超长系字节语义所致"，确认超长原因在框架写入值本身（见 RestartTest）。
 */
@SpringBootTest
class VarcharProbeTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void varcharLengthSemantics() {
        jdbc.update("DROP TABLE IF EXISTS probe_vc");
        jdbc.update("CREATE TABLE probe_vc (s VARCHAR(2500))");

        // 2500 字符纯 ASCII：字符/字节语义下均恰好 2500，应通过
        jdbc.update("INSERT INTO probe_vc VALUES (?)", "x".repeat(2500));
        System.out.println("[CASE] 2500 字符纯 ASCII 插入 VARCHAR(2500) 成功");

        // 2496 字符 = 2502 字节（6 个汉字多 12 字节）：字符语义应通过，字节语义应报错
        String s = "x".repeat(2490) + "中中中中中中";
        jdbc.update("DELETE FROM probe_vc");
        jdbc.update("INSERT INTO probe_vc VALUES (?)", s);
        System.out.println("[CASE] 2496 字符/2502 字节插入 VARCHAR(2500) 成功 -> 按字符计（与 MySQL 一致）");

        jdbc.update("DROP TABLE probe_vc");
    }
}
