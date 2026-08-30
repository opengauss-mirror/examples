package org.opengauss.smoke.jpacases;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengauss.smoke.jpacases.JpaDbUtil.*;

/**
 * 协议命令专项（评审意见：COM_STMT_PREPARE / COM_STMT_SEND_LONG_DATA /
 * COM_STMT_EXECUTE 最好覆盖到每个 command）。
 * 与 #27 报告（springboot-connect-opengauss-b）7.1-7.9 同一套设计，按"JDBC 是否有触发路径"逐条实测：
 * C1 COM_QUERY（普通 Statement，文本协议）
 * C2 COM_INIT_DB（连接建立携带 database；setCatalog + useLocalSessionState=false）
 * C3 COM_PING（isValid）
 * C4 COM_STMT_PREPARE / COM_STMT_EXECUTE / COM_STMT_CLOSE（服务端预编译）
 * C5 COM_STMT_SEND_LONG_DATA（服务端预编译 + 1MB LONGBLOB 流式写入）
 * C6 COM_STMT_RESET（clearParameters 后复用同一预编译语句）
 * C7 COM_STMT_FETCH（useCursorFetch + setFetchSize）
 * C8 COM_SET_OPTION（allowMultiQueries 多语句）
 * C9 COM_QUIT（close）
 * COM_FIELD_LIST / COM_PROCESS_INFO / COM_STATISTICS / COM_PROCESS_KILL / COM_CHANGE_USER
 * 等命令 JDBC 驱动无触发路径（不提供对应 API），在自测报告 4.1.6 说明，不在此处用例化。
 */
class ProtocolCommandTest {

    @BeforeAll
    static void createData() throws SQLException {
        try (Connection c = raw()) {
            exec(c,
                    "DROP TABLE IF EXISTS jpa_cmd",
                    "CREATE TABLE jpa_cmd (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50), data LONGBLOB)");
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO jpa_cmd (name) VALUES (?)")) {
                for (int i = 1; i <= 500; i++) {
                    ps.setString(1, "cmd_" + i);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            log("C0 准备协议命令测试数据（jpa_cmd 500 行，含 LONGBLOB 列）");
        }
    }

    private static int count(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM jpa_cmd")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Test
    @DisplayName("C1 COM_QUERY：普通 Statement 执行 SQL（文本协议）")
    void comQuery() throws SQLException {
        try (Connection c = raw()) {
            int before = count(c);
            try (Statement st = c.createStatement()) {
                st.executeUpdate("INSERT INTO jpa_cmd (name) VALUES ('query_a')");
                st.executeUpdate("INSERT INTO jpa_cmd (name) VALUES ('query_b')");
            }
            int after = count(c);
            log("C1 COM_QUERY 文本协议插入/查询正常：%d -> %d 行", before, after);
            assertEquals(before + 2, after);
        }
    }

    @Test
    @DisplayName("C2 COM_INIT_DB：连接建立携带 database + setCatalog(useLocalSessionState=false)")
    void comInitDb() throws SQLException {
        // 连接建立阶段：url 指定 database，驱动握手时即发出 COM_INIT_DB；
        // useLocalSessionState=false 关闭本地状态缓存，setCatalog 按协议重新确认 database。
        try (Connection c = raw("useLocalSessionState=false")) {
            assertEquals(DB, c.getCatalog());
            c.setCatalog(DB);
            assertTrue(count(c) >= 500, "切换 catalog 后查询应正常");
            log("C2 COM_INIT_DB 连接初始化/切换 database 正常：catalog=%s", c.getCatalog());
        }
    }

    @Test
    @DisplayName("C3 COM_PING：Connection.isValid")
    void comPing() throws SQLException {
        try (Connection c = raw()) {
            boolean ok = c.isValid(2);
            log("C3 COM_PING 心跳正常：isValid=%s", ok);
            assertTrue(ok);
        }
    }

    @Test
    @DisplayName("C4 COM_STMT_PREPARE/EXECUTE/CLOSE：服务端预编译语句全生命周期")
    void comStmtPrepareExecuteClose() throws SQLException {
        // cachePrepStmts=false 确保每次 prepareStatement 真实发出 COM_STMT_PREPARE
        try (Connection c = raw("useServerPrepStmts=true&cachePrepStmts=false")) {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO jpa_cmd (name) VALUES (?)")) {
                ps.setString(1, "prep_c4");
                assertEquals(1, ps.executeUpdate());
            } // 语句 close -> COM_STMT_CLOSE
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT name FROM jpa_cmd WHERE name = ?")) {
                ps.setString(1, "prep_c4");
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next(), "服务端预编译语句执行后应查回已插入行");
                    assertEquals("prep_c4", rs.getString(1));
                }
            }
            log("C4 COM_STMT_PREPARE/EXECUTE/CLOSE 服务端预编译插入+查询往返正常");
        }
    }

    @Test
    @DisplayName("C5 COM_STMT_SEND_LONG_DATA：服务端预编译 + 1MB LONGBLOB 流式写入")
    void comStmtSendLongData() throws SQLException {
        try (Connection c = raw("useServerPrepStmts=true")) {
            // ① 链路基线：无零字节 1MB（全 0x41）应完整往返（MD5 一致）。
            byte[] blob = new byte[1024 * 1024];
            Arrays.fill(blob, (byte) 0x41);
            String expectMd5 = md5(blob);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO jpa_cmd (name, data) VALUES (?, ?)")) {
                ps.setString(1, "long_c5");
                ps.setBlob(2, new ByteArrayInputStream(blob));
                assertEquals(1, ps.executeUpdate());
            }
            byte[] read;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT data FROM jpa_cmd WHERE name = ?")) {
                ps.setString(1, "long_c5");
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    read = rs.getBytes(1);
                }
            }
            log("C5a COM_STMT_SEND_LONG_DATA 1MB 无零字节 LONGBLOB 流式写入并读回：%d -> %d 字节，MD5 %s",
                    blob.length, read.length, expectMd5.equals(md5(read)) ? "一致" : "不一致");
            assertEquals(blob.length, read.length);
            assertEquals(expectMd5, md5(read));

            // ② FINDING F 回归断言：二进制内容遇 0x00 被服务端按 C 字符串截断为前缀
            //    （与 #27 报告 FINDING F 同源；MySQL 原生完整存储）。
            //    服务端修复后本断言自动失败，提醒移除规避说明并更新自测报告。
            byte[] mixed = new byte[200];
            Arrays.fill(mixed, (byte) 0x41);
            mixed[150] = 0x00;
            Arrays.fill(mixed, 151, mixed.length, (byte) 0x42);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO jpa_cmd (name, data) VALUES (?, ?)")) {
                ps.setString(1, "long_c5_mixed");
                ps.setBlob(2, new ByteArrayInputStream(mixed));
                assertEquals(1, ps.executeUpdate());
            }
            byte[] mixedRead;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT data FROM jpa_cmd WHERE name = ?")) {
                ps.setString(1, "long_c5_mixed");
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    mixedRead = rs.getBytes(1);
                }
            }
            log("C5b FINDING F：含 0x00 的 200 字节读回 %d 字节（=第一个 0x00 前的前缀长度，MySQL 原生应回 200）",
                    mixedRead.length);
            assertEquals(150, mixedRead.length,
                    "FINDING F 回归断言：dolphin 遇 0x00 截断为前缀；服务端修复后应回 200 字节");
        }
    }

    @Test
    @DisplayName("C6 COM_STMT_RESET：clearParameters 后复用同一预编译语句")
    void comStmtReset() throws SQLException {
        try (Connection c = raw("useServerPrepStmts=true")) {
            int before = count(c);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO jpa_cmd (name) VALUES (?)")) {
                ps.setString(1, "reset_a");
                assertEquals(1, ps.executeUpdate());
                ps.clearParameters(); // 参数置空，下次执行时驱动发 COM_STMT_RESET
                ps.setString(1, "reset_b");
                assertEquals(1, ps.executeUpdate());
            }
            int after = count(c);
            log("C6 COM_STMT_RESET clearParameters 后复用预编译语句正常：%d -> %d 行", before, after);
            assertEquals(before + 2, after);
        }
    }

    @Test
    @DisplayName("C7 COM_STMT_FETCH：useCursorFetch + setFetchSize")
    void comStmtFetch() throws SQLException {
        try (Connection c = raw("useCursorFetch=true&useServerPrepStmts=true")) {
            int n;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, name FROM jpa_cmd ORDER BY id")) {
                ps.setFetchSize(100);
                try (ResultSet rs = ps.executeQuery()) {
                    n = 0;
                    while (rs.next()) {
                        n++;
                    }
                }
            }
            log("C7 COM_STMT_FETCH 游标拉取无报错，读取 %d 行。协议层是否真逐批返回需服务端日志佐证"
                    + "（同 #27 报告 6.1 口径，报告中按'驱动层可用、协议层待确认'记录）", n);
            assertTrue(n >= 500, "应至少读回准备阶段写入的 500 行");
        }
    }

    @Test
    @DisplayName("C8 COM_SET_OPTION：allowMultiQueries=true 多语句执行")
    void comSetOption() throws SQLException {
        try (Connection c = raw("allowMultiQueries=true")) {
            int before = count(c);
            int cnt = -1;
            boolean hasResults;
            try (Statement st = c.createStatement()) {
                hasResults = st.execute("INSERT INTO jpa_cmd (name) VALUES ('mq_1'); "
                        + "INSERT INTO jpa_cmd (name) VALUES ('mq_2'); "
                        + "SELECT COUNT(*) FROM jpa_cmd");
                // FINDING G：dolphin 多语句结果返回结构不完整——execute() 只反映首语句
                // 结果，getMoreResults() 取不到后续语句的结果集（与 #27 报告 FINDING G 同源；
                // MySQL 原生按语句逐个返回全部结果）。服务端修复后 more=true 且 cnt 能读到
                // COUNT(*)，本断言自动失败提醒更新报告。
                hasResults = st.getMoreResults();
                if (hasResults) {
                    try (ResultSet rs = st.getResultSet()) {
                        if (rs.next()) {
                            cnt = rs.getInt(1);
                        }
                    }
                }
            }
            int after = count(c);
            log("C8 COM_SET_OPTION 多语句执行生效：%d -> %d 行（后续 SELECT 结果集经 getMoreResults "
                    + "不可得，见 FINDING G）", before, after);
            assertEquals(before + 2, after, "多语句中的 DML 应全部生效");
            assertEquals(-1, cnt,
                    "FINDING G 回归断言：dolphin 当前不回传后续 SELECT 结果集；服务端修复后 cnt 应=落库行数");
        }
    }

    @Test
    @DisplayName("C9 COM_QUIT：Connection.close")
    void comQuit() throws SQLException {
        Connection c = raw();
        c.close();
        assertTrue(c.isClosed());
        log("C9 COM_QUIT 连接关闭正常（isClosed=true）");
    }

    private static String md5(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
