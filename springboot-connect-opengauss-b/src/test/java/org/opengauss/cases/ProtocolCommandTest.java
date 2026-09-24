package org.opengauss.cases;

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
import static org.opengauss.cases.DbUtil.*;

/**
 * 维度七：MySQL 协议命令基础功能专项（评审意见：COM_STMT_PREPARE / COM_STMT_SEND_LONG_DATA /
 * COM_STMT_EXECUTE 最好覆盖到每个 command）。
 * 按"JDBC 是否有触发路径"逐条实测驱动可触发的全部命令：
 * 7.1 COM_QUERY（普通 Statement，文本协议）
 * 7.2 COM_INIT_DB（连接建立携带 database；setCatalog + useLocalSessionState=false）
 * 7.3 COM_PING（isValid）
 * 7.4 COM_STMT_PREPARE / COM_STMT_EXECUTE / COM_STMT_CLOSE（服务端预编译）
 * 7.5 COM_STMT_SEND_LONG_DATA（服务端预编译 + 1MB LONGBLOB 流式写入）
 * 7.6 COM_STMT_RESET（clearParameters 后复用同一预编译语句）
 * 7.7 COM_STMT_FETCH（useCursorFetch + setFetchSize）
 * 7.8 COM_SET_OPTION（allowMultiQueries 多语句）
 * 7.9 COM_QUIT（close）
 * COM_FIELD_LIST / COM_PROCESS_INFO / COM_STATISTICS / COM_PROCESS_KILL / COM_CHANGE_USER
 * 等命令 JDBC 驱动无触发路径（不提供对应 API），在自测报告 4.1.8 说明，不在此处用例化。
 */
class ProtocolCommandTest {

    @BeforeAll
    static void createData() throws SQLException {
        try (Connection c = raw(null)) {
            exec(c,
                    "DROP TABLE IF EXISTS t_cmd",
                    "CREATE TABLE t_cmd (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50), data LONGBLOB)");
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO t_cmd (name) VALUES (?)")) {
                for (int i = 1; i <= 500; i++) {
                    ps.setString(1, "cmd_" + i);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            log("7.0 准备协议命令测试数据（t_cmd 500 行，含 LONGBLOB 列）");
        }
    }

    private static int count(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM t_cmd")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Test
    @DisplayName("7.1 COM_QUERY：普通 Statement 执行 SQL（文本协议）")
    void comQuery() throws SQLException {
        try (Connection c = raw(null)) {
            int before = count(c);
            try (Statement st = c.createStatement()) {
                st.executeUpdate("INSERT INTO t_cmd (name) VALUES ('query_a')");
                st.executeUpdate("INSERT INTO t_cmd (name) VALUES ('query_b')");
            }
            int after = count(c);
            log("7.1 COM_QUERY 文本协议插入/查询正常：%d -> %d 行", before, after);
            assertEquals(before + 2, after);
        }
    }

    @Test
    @DisplayName("7.2 COM_INIT_DB：连接建立携带 database + setCatalog(useLocalSessionState=false)")
    void comInitDb() throws SQLException {
        // 连接建立阶段：url 指定 database，驱动握手时即发出 COM_INIT_DB；
        // useLocalSessionState=false 关闭本地状态缓存，setCatalog 按协议重新确认 database。
        try (Connection c = raw("useLocalSessionState=false")) {
            assertEquals(DB, c.getCatalog());
            c.setCatalog(DB);
            assertTrue(count(c) >= 500, "切换 catalog 后查询应正常");
            log("7.2 COM_INIT_DB 连接初始化/切换 database 正常：catalog=%s", c.getCatalog());
        }
    }

    @Test
    @DisplayName("7.3 COM_PING：Connection.isValid")
    void comPing() throws SQLException {
        try (Connection c = raw(null)) {
            boolean ok = c.isValid(2);
            log("7.3 COM_PING 心跳正常：isValid=%s", ok);
            assertTrue(ok);
        }
    }

    @Test
    @DisplayName("7.4 COM_STMT_PREPARE/EXECUTE/CLOSE：服务端预编译语句全生命周期")
    void comStmtPrepareExecuteClose() throws SQLException {
        // cachePrepStmts=false 确保每次 prepareStatement 真实发出 COM_STMT_PREPARE
        try (Connection c = raw("useServerPrepStmts=true&cachePrepStmts=false")) {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO t_cmd (name) VALUES (?)")) {
                ps.setString(1, "prep_7_4");
                assertEquals(1, ps.executeUpdate());
            } // 语句 close -> COM_STMT_CLOSE
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT name FROM t_cmd WHERE name = ?")) {
                ps.setString(1, "prep_7_4");
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next(), "服务端预编译语句执行后应查回已插入行");
                    assertEquals("prep_7_4", rs.getString(1));
                }
            }
            log("7.4 COM_STMT_PREPARE/EXECUTE/CLOSE 服务端预编译插入+查询往返正常");
        }
    }

    @Test
    @DisplayName("7.5 COM_STMT_SEND_LONG_DATA：服务端预编译 + 1MB LONGBLOB 流式写入")
    void comStmtSendLongData() throws SQLException {
        try (Connection c = raw("useServerPrepStmts=true")) {
            // ① 链路基线：无零字节 1MB（全 0x41）应完整往返（MD5 一致）。
            byte[] blob = new byte[1024 * 1024];
            Arrays.fill(blob, (byte) 0x41);
            String expectMd5 = md5(blob);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO t_cmd (name, data) VALUES (?, ?)")) {
                ps.setString(1, "long_7_5");
                ps.setBlob(2, new ByteArrayInputStream(blob));
                assertEquals(1, ps.executeUpdate());
            }
            byte[] read;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT data FROM t_cmd WHERE name = ?")) {
                ps.setString(1, "long_7_5");
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    read = rs.getBytes(1);
                }
            }
            log("7.5a COM_STMT_SEND_LONG_DATA 1MB 无零字节 LONGBLOB 流式写入并读回：%d -> %d 字节，MD5 %s",
                    blob.length, read.length, expectMd5.equals(md5(read)) ? "一致" : "不一致");
            assertEquals(blob.length, read.length);
            assertEquals(expectMd5, md5(read));

            // ② FINDING F 回归断言：二进制内容遇 0x00 被服务端按 C 字符串截断为前缀
            //    （MySQL 原生完整存储；dolphin 在 SEND_LONG_DATA 二进制参数处理中遇 0x00 截断，
            //    客户端预编译文本协议路径无此问题）。服务端修复后本断言自动失败，
            //    提醒移除规避说明并更新自测报告。
            byte[] mixed = new byte[200];
            Arrays.fill(mixed, (byte) 0x41);
            mixed[150] = 0x00;
            Arrays.fill(mixed, 151, mixed.length, (byte) 0x42);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO t_cmd (name, data) VALUES (?, ?)")) {
                ps.setString(1, "long_7_5_mixed");
                ps.setBlob(2, new ByteArrayInputStream(mixed));
                assertEquals(1, ps.executeUpdate());
            }
            byte[] mixedRead;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT data FROM t_cmd WHERE name = ?")) {
                ps.setString(1, "long_7_5_mixed");
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    mixedRead = rs.getBytes(1);
                }
            }
            log("7.5b FINDING F：含 0x00 的 200 字节读回 %d 字节（=第一个 0x00 前的前缀长度，MySQL 原生应回 200）",
                    mixedRead.length);
            assertEquals(150, mixedRead.length,
                    "FINDING F 回归断言：dolphin 遇 0x00 截断为前缀；服务端修复后应回 200 字节");
        }
    }

    @Test
    @DisplayName("7.6 COM_STMT_RESET：clearParameters 后复用同一预编译语句")
    void comStmtReset() throws SQLException {
        try (Connection c = raw("useServerPrepStmts=true")) {
            int before = count(c);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO t_cmd (name) VALUES (?)")) {
                ps.setString(1, "reset_a");
                assertEquals(1, ps.executeUpdate());
                ps.clearParameters(); // 参数置空，下次执行时驱动发 COM_STMT_RESET
                ps.setString(1, "reset_b");
                assertEquals(1, ps.executeUpdate());
            }
            int after = count(c);
            log("7.6 COM_STMT_RESET clearParameters 后复用预编译语句正常：%d -> %d 行", before, after);
            assertEquals(before + 2, after);
        }
    }

    @Test
    @DisplayName("7.7 COM_STMT_FETCH：useCursorFetch + setFetchSize")
    void comStmtFetch() throws SQLException {
        try (Connection c = raw("useCursorFetch=true&useServerPrepStmts=true")) {
            int n;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, name FROM t_cmd ORDER BY id")) {
                ps.setFetchSize(100);
                try (ResultSet rs = ps.executeQuery()) {
                    n = 0;
                    while (rs.next()) {
                        n++;
                    }
                }
            }
            log("7.7 COM_STMT_FETCH 游标拉取无报错，读取 %d 行。协议层是否真逐批返回需服务端日志佐证"
                    + "（同 6.1 口径，报告中按'驱动层可用、协议层待确认'记录）", n);
            assertTrue(n >= 500, "应至少读回准备阶段写入的 500 行");
        }
    }

    @Test
    @DisplayName("7.8 COM_SET_OPTION：allowMultiQueries=true 多语句执行")
    void comSetOption() throws SQLException {
        try (Connection c = raw("allowMultiQueries=true")) {
            int before = count(c);
            int cnt = -1;
            boolean hasResults;
            try (Statement st = c.createStatement()) {
                hasResults = st.execute("INSERT INTO t_cmd (name) VALUES ('mq_1'); "
                        + "INSERT INTO t_cmd (name) VALUES ('mq_2'); "
                        + "SELECT COUNT(*) FROM t_cmd");
                // FINDING G：dolphin 多语句结果返回结构不完整——execute() 只反映首语句
                // 结果（首语句为 DML 时返回 false + 首语句 update count），getMoreResults()
                // 取不到后续语句的结果集（B 组诊断：首结果 + 末 update count 之外全部丢弃；
                // 纯 SELECT 多结果集则正常逐个返回）。MySQL 原生按语句逐个返回全部结果。
                // 服务端修复后 more=true 且 cnt 能读到 COUNT(*)，本断言自动失败提醒更新报告。
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
            log("7.8 COM_SET_OPTION 多语句执行生效：%d -> %d 行（后续 SELECT 结果集经 getMoreResults "
                    + "不可得，见 FINDING G）", before, after);
            assertEquals(before + 2, after, "多语句中的 DML 应全部生效");
            assertEquals(-1, cnt,
                    "FINDING G 回归断言：dolphin 当前不回传后续 SELECT 结果集；服务端修复后 cnt 应=落库行数");
        }
    }

    @Test
    @DisplayName("7.9 COM_QUIT：Connection.close")
    void comQuit() throws SQLException {
        Connection c = raw(null);
        c.close();
        assertTrue(c.isClosed());
        log("7.9 COM_QUIT 连接关闭正常（isClosed=true）");
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
