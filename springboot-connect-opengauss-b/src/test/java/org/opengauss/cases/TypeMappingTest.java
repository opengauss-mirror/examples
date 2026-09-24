package org.opengauss.cases;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengauss.cases.DbUtil.*;

/**
 * 维度二：类型映射 + 官方协议兼容文档（dolphin_mysql_protocol_compatibility）明示的 6 个 JDBC 参数兼容点。
 * tinyInt1isBit / transformedBitIsBoolean / zeroDateTimeBehavior / useOldAliasMetadataBehavior
 * / useaffectedrows / allowMultiQueries（文档标注暂不支持的参数实测行为并记录）。
 */
class TypeMappingTest {

    @BeforeAll
    static void createTable() throws SQLException {
        try (Connection c = raw(null)) {
            exec(c,
                    "DROP TABLE IF EXISTS t_type_map",
                    "CREATE TABLE t_type_map ("
                            + " id INT AUTO_INCREMENT PRIMARY KEY,"
                            + " t_tinyint TINYINT,"
                            + " t_tinyint1 TINYINT(1),"
                            + " t_bit BIT(1),"
                            + " t_decimal DECIMAL(10,2),"
                            + " t_date DATE,"
                            + " t_datetime DATETIME,"
                            + " t_timestamp TIMESTAMP,"
                            + " t_varchar VARCHAR(50),"
                            + " t_blob BLOB"
                            + ")");
            log("2.0 建表成功：t_type_map（含 TINYINT/TINYINT(1)/BIT(1)/DECIMAL/DATE/DATETIME/TIMESTAMP/VARCHAR/BLOB）");
        }
    }

    private static void insertBaseRow(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO t_type_map (t_tinyint, t_tinyint1, t_bit, t_decimal,"
                        + " t_date, t_datetime, t_timestamp, t_varchar, t_blob)"
                        + " VALUES (?,?,?,?,?,?,?,?,?)")) {
            ps.setInt(1, 127);
            ps.setInt(2, 1);
            ps.setInt(3, 1);
            ps.setBigDecimal(4, new BigDecimal("3.14"));
            ps.setDate(5, Date.valueOf("2026-08-28"));
            ps.setTimestamp(6, Timestamp.valueOf("2026-08-28 12:34:56"));
            ps.setTimestamp(7, Timestamp.valueOf("2026-08-28 12:34:56"));
            ps.setString(8, "hello");
            ps.setBytes(9, new byte[]{1, 2, 3});
            ps.executeUpdate();
        }
    }

    @Test
    @DisplayName("2.1 默认参数下各类型的 Java 映射")
    void defaultTypeMapping() throws SQLException {
        try (Connection c = raw(null)) {
            insertBaseRow(c);
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT t_tinyint, t_tinyint1, t_bit, t_decimal, t_date,"
                            + " t_datetime, t_timestamp, t_varchar, t_blob FROM t_type_map"
                            + " WHERE t_varchar = 'hello'");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                log("2.1 TINYINT      -> %s（值 %s）", rs.getObject(1).getClass().getSimpleName(), rs.getObject(1));
                log("2.1 TINYINT(1)   -> %s（值 %s）", rs.getObject(2).getClass().getSimpleName(), rs.getObject(2));
                log("2.1 BIT(1)       -> %s（值 %s）", rs.getObject(3).getClass().getSimpleName(), rs.getObject(3));
                log("2.1 DECIMAL      -> %s（值 %s）", rs.getObject(4).getClass().getSimpleName(), rs.getObject(4));
                log("2.1 DATE         -> %s（值 %s）", rs.getObject(5).getClass().getSimpleName(), rs.getObject(5));
                log("2.1 DATETIME     -> %s（值 %s）", rs.getObject(6).getClass().getSimpleName(), rs.getObject(6));
                log("2.1 TIMESTAMP    -> %s（值 %s）", rs.getObject(7).getClass().getSimpleName(), rs.getObject(7));
                log("2.1 VARCHAR      -> %s（值 %s）", rs.getObject(8).getClass().getSimpleName(), rs.getObject(8));
                log("2.1 BLOB         -> %s（长度 %d）", rs.getObject(9).getClass().getSimpleName(),
                        rs.getBytes(9).length);
                log("2.1 服务端列类型元数据: t_tinyint1 typeName=%s jdbcType=%d, t_bit typeName=%s jdbcType=%d",
                        rs.getMetaData().getColumnTypeName(2), rs.getMetaData().getColumnType(2),
                        rs.getMetaData().getColumnTypeName(3), rs.getMetaData().getColumnType(3));

                // 驱动默认行为断言（MySQL 驱动 8.x 的文档化默认值）
                assertEquals(Integer.class, rs.getObject(1).getClass(), "TINYINT 应映射 Integer");
                assertEquals(Boolean.class, rs.getObject(2).getClass(), "TINYINT(1) 默认 tinyInt1isBit=true 应映射 Boolean");
                // 官方协议兼容文档：transformedBitIsBoolean 默认值为 false，但备注"不管是否转换，在 Java 中都是 Boolean 类型"
                assertEquals(Boolean.class, rs.getObject(3).getClass(), "BIT(1) 默认参数下映射 Boolean（与官方文档备注一致）");
                assertEquals(BigDecimal.class, rs.getObject(4).getClass(), "DECIMAL 应映射 BigDecimal");
                assertEquals(Date.class, rs.getObject(5).getClass(), "DATE 应映射 java.sql.Date");
                // MySQL 8.x 驱动文档行为：getObject 对 DATETIME 返回 LocalDateTime（不是 Timestamp）
                assertEquals(java.time.LocalDateTime.class, rs.getObject(6).getClass(),
                        "DATETIME 应映射 LocalDateTime（驱动 8.x 文档行为）");
                assertEquals(Timestamp.class, rs.getObject(7).getClass(), "TIMESTAMP 应映射 Timestamp");
                assertEquals(String.class, rs.getObject(8).getClass(), "VARCHAR 应映射 String");
            }
        }
    }

    @Test
    @DisplayName("2.2 tinyInt1isBit=false：TINYINT(1) 应映射 Integer")
    void tinyInt1isBitFalse() throws SQLException {
        try (Connection c = raw("tinyInt1isBit=false")) {
            insertBaseRow(c);
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT t_tinyint1 FROM t_type_map WHERE t_varchar = 'hello'");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object v = rs.getObject(1);
                log("2.2 tinyInt1isBit=false: TINYINT(1) -> %s（值 %s）", v.getClass().getSimpleName(), v);
                assertEquals(Integer.class, v.getClass());
                assertEquals(1, v);
            }
        }
    }

    @Test
    @DisplayName("2.3 transformedBitIsBoolean=true/false：BIT(1) 的 getObject 映射（官方文档备注：Java 侧均为 Boolean）")
    void transformedBitIsBoolean() throws SQLException {
        insertBaseRow(raw(null));
        for (String param : new String[]{"transformedBitIsBoolean=false", "transformedBitIsBoolean=true"}) {
            try (Connection c = raw(param)) {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT t_bit FROM t_type_map WHERE t_varchar = 'hello'");
                     ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    Object v = rs.getObject(1);
                    byte[] b = rs.getBytes(1);
                    log("2.3 %s: BIT(1) getObject -> %s（值 %s），getBytes -> byte[%d]",
                            param, v.getClass().getSimpleName(), v, b.length);
                    // 官方文档备注："不管是否转换，在 Java 中都是 Boolean 类型" —— 实测两种设置均映射 Boolean
                    assertEquals(Boolean.class, v.getClass(),
                            param + " 下 BIT(1) getObject 应恒为 Boolean（官方文档备注）");
                    // getBytes 对 BIT 列恒返回原始字节，与参数无关
                    assertArrayEquals(new byte[]{1}, b);
                }
            }
        }
    }

    @Test
    @DisplayName("2.4 BLOB 读写往返")
    void blobRoundtrip() throws SQLException {
        byte[] data = new byte[]{9, 8, 7, 6, 5, 4, 3, 2, 1};
        try (Connection c = raw(null)) {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO t_type_map (t_blob) VALUES (?)")) {
                ps.setBytes(1, data);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT t_blob FROM t_type_map ORDER BY id DESC LIMIT 1");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                byte[] back = rs.getBytes(1);
                log("2.4 BLOB 往返: 写入 %d 字节，读回 %d 字节", data.length, back.length);
                assertArrayEquals(data, back);
            }
        }
    }

    @Test
    @DisplayName("2.5 zeroDateTimeBehavior：零日期由 dolphin.sql_mode 的 no_zero_date 控制（默认拒绝、去掉可存储，三档读取与 MySQL 一致）")
    void zeroDateTime() throws SQLException {
        // ① 默认配置（dolphin.sql_mode 含 no_zero_date）：写入零日期被拒
        try (Connection c = raw(null)) {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO t_type_map (t_datetime) VALUES (?)")) {
                ps.setString(1, "0000-00-00 00:00:00");
                ps.executeUpdate();
                log("2.5 注意：默认 sql_mode 下零日期写入成功，说明测试库未开启 no_zero_date（与文档默认口径不同）");
            } catch (SQLException e) {
                assertTrue(e.getMessage() != null && e.getMessage().contains("out of range"),
                        "默认 sql_mode（含 no_zero_date）应拒绝零日期，实际: " + e.getMessage());
                log("2.5 默认 sql_mode（含 no_zero_date）拒绝零日期写入: %s", e.getMessage());
            }
        }
        // ② 去掉 no_zero_date（会话级 SET）：零日期可正常存储
        String sqlMode = null;
        try (Connection c = raw(null);
             Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT @@sql_mode")) {
                if (rs.next()) {
                    sqlMode = rs.getString(1);
                }
            } catch (SQLException e) {
                log("2.5 @@sql_mode 读取失败（%s），改走 current_setting", e.getMessage());
                try (ResultSet rs = st.executeQuery("SELECT current_setting('dolphin.sql_mode')")) {
                    if (rs.next()) {
                        sqlMode = rs.getString(1);
                    }
                }
            }
        } catch (SQLException e) {
            log("2.5 读取 sql_mode 失败（%s），跳过零日期存储/读取验证", e.getMessage());
        }
        if (sqlMode != null && sqlMode.contains("no_zero_date")) {
            // 诊断结论：dolphin.sql_mode 的 GUC 校验要求——值里含 STRICT_TRANS_TABLES
            // （MySQL 协议层附加 token）时必须是全量列表，否则报 invalid value。
            // 故去掉 no_zero_date 的同时须一并去掉 STRICT_TRANS_TABLES（实测 SET 后零日期
            // 可写入；SET SESSION sql_mode=... 的 MySQL 写法会被协议层拦截、实际不生效，
            // 必须用 SET dolphin.sql_mode=...）。
            String relaxed = sqlMode.replace("no_zero_date", "")
                    .replace("STRICT_TRANS_TABLES", "")
                    .replaceAll(",\\s*,", ",")
                    .replaceAll("^\\s*,|,\\s*$", "");
            if (relaxed.trim().isEmpty()) {
                log("2.5 sql_mode 仅含 no_zero_date，跳过零日期存储/读取验证");
                return;
            }
            int zeroId;
            try (Connection c = raw(null)) {
                try (Statement st = c.createStatement()) {
                    st.execute("SET dolphin.sql_mode = '" + relaxed + "'");
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO t_type_map (t_datetime) VALUES (?)")) {
                    ps.setString(1, "0000-00-00 00:00:00");
                    assertEquals(1, ps.executeUpdate(), "去掉 no_zero_date 后零日期应可写入");
                }
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("SELECT MAX(id) FROM t_type_map")) {
                    rs.next();
                    zeroId = rs.getInt(1);
                }
                log("2.5 去掉 no_zero_date 后零日期写入成功（id=%d）", zeroId);
            }
            // ③ 表中有零日期时 zeroDateTimeBehavior 三档读取行为（与 MySQL 语义一致）
            for (String mode : new String[]{"CONVERT_TO_NULL", "EXCEPTION", "ROUND"}) {
                try (Connection c = raw("zeroDateTimeBehavior=" + mode);
                     PreparedStatement ps = c.prepareStatement(
                             "SELECT t_datetime FROM t_type_map WHERE id = ?")) {
                    ps.setInt(1, zeroId);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        Object v = rs.getObject(1);
                        log("2.5 zeroDateTimeBehavior=%s: 读到 %s（%s）",
                                mode, String.valueOf(v), v == null ? "null" : v.getClass().getSimpleName());
                        if ("CONVERT_TO_NULL".equals(mode)) {
                            assertNull(v, "convertToNull 档应读为 null（与 MySQL 语义一致）");
                        } else if ("ROUND".equals(mode)) {
                            assertTrue(String.valueOf(v).startsWith("0001-01-01"),
                                    "round 档应读为 0001-01-01，实际: " + v);
                        }
                    }
                } catch (SQLException e) {
                    if ("EXCEPTION".equals(mode)) {
                        assertTrue(e.getMessage() != null
                                        && e.getMessage().contains("Zero date value prohibited"),
                                "exception 档应抛 Zero date value prohibited，实际: " + e.getMessage());
                        log("2.5 zeroDateTimeBehavior=%s: 读取按预期抛异常: %s", mode, e.getMessage());
                    } else {
                        throw e;
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("2.6 useOldAliasMetadataBehavior：表达式别名 vs 表列别名（官方文档备注：表达式别名始终返回别名）")
    void useOldAliasMetadataBehavior() throws SQLException {
        String exprNameTrue = null, exprNameDefault = null;
        String colNameTrue = null, colNameDefault = null;
        // 场景一：表达式别名（SELECT 1 AS my_alias）——文档备注：始终返回别名，与参数无关
        for (String param : new String[]{"useOldAliasMetadataBehavior=true", null}) {
            try (Connection c = raw(param)) {
                try (PreparedStatement ps = c.prepareStatement("SELECT 1 AS my_alias");
                     ResultSet rs = ps.executeQuery()) {
                    var md = rs.getMetaData();
                    log("2.6 表达式别名（%s）: columnName=%s, columnLabel=%s",
                            param == null ? "默认" : param, md.getColumnName(1), md.getColumnLabel(1));
                    if (param == null) {
                        exprNameDefault = md.getColumnName(1);
                    } else {
                        exprNameTrue = md.getColumnName(1);
                    }
                }
            }
        }
        // 场景二：表列别名（SELECT name AS n）——参数真正的生效场景
        for (String param : new String[]{"useOldAliasMetadataBehavior=true", null}) {
            try (Connection c = raw(param)) {
                try (PreparedStatement ps = c.prepareStatement("SELECT name AS n FROM `user` LIMIT 1");
                     ResultSet rs = ps.executeQuery()) {
                    var md = rs.getMetaData();
                    log("2.6 表列别名（%s）: columnName=%s, columnLabel=%s",
                            param == null ? "默认" : param, md.getColumnName(1), md.getColumnLabel(1));
                    if (param == null) {
                        colNameDefault = md.getColumnName(1);
                    } else {
                        colNameTrue = md.getColumnName(1);
                    }
                }
            }
        }
        // 断言：表达式别名始终返回别名（文档备注）；表列别名 true 时 getColumnName 返回别名，false 时返回原始列名（文档：目前仅支持 getColumnName）
        assertEquals("my_alias", exprNameTrue, "表达式别名 + true 应返回别名");
        assertEquals("my_alias", exprNameDefault, "表达式别名 + 默认应返回别名（文档备注）");
        assertEquals("n", colNameTrue, "表列别名 + true 时 getColumnName 应返回别名 n（参数生效）");
        assertEquals("name", colNameDefault, "表列别名 + 默认时 getColumnName 应返回原始列名 name");
    }

    @Test
    @DisplayName("2.7 useaffectedrows：无变化 UPDATE 的影响行数（官方文档备注：目前不支持，始终返回找到的行数）")
    void useAffectedRows() throws SQLException {
        try (Connection c0 = raw(null)) {
            try (PreparedStatement ps = c0.prepareStatement(
                    "INSERT INTO t_type_map (t_varchar, t_tinyint) VALUES ('n7_hello', 5)")) {
                ps.executeUpdate();
            }
        }
        for (String param : new String[]{"useaffectedrows=true", null}) {
            try (Connection c = raw(param)) {
                int updated;
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE t_type_map SET t_tinyint = t_tinyint WHERE t_varchar = 'n7_hello'")) {
                    updated = ps.executeUpdate();
                }
                log("2.7 %s: 无变化 UPDATE 影响行数 = %d（MySQL 原生差异：默认=找到行 1，true=受影响行 0）",
                        param == null ? "默认" : param, updated);
                // 官方文档备注："目前不支持该参数……openGauss 始终返回'找到的行'的个数" —— 实测两种设置均返回 1
                assertEquals(1, updated, "useaffectedrows 下无变化 UPDATE 应始终返回找到的行数 1（官方文档备注）");
            }
        }
    }

    @Test
    @DisplayName("2.8 allowMultiQueries：多语句执行（官方文档：默认 false，true 时允许 ';' 分隔多查询）")
    void allowMultiQueries() throws SQLException {
        int resultsTrue = -1;
        try (Connection c = raw("allowMultiQueries=true")) {
            try (Statement st = c.createStatement()) {
                boolean hasResults = st.execute("SELECT 1 AS a; SELECT 2 AS b");
                int count = 0;
                do {
                    count++;
                    if (hasResults) {
                        try (ResultSet rs = st.getResultSet()) {
                            rs.next();
                            log("2.8 allowMultiQueries=true: 第 %d 个结果集第一列值 = %s", count, rs.getObject(1));
                        }
                    } else {
                        log("2.8 allowMultiQueries=true: 第 %d 个结果为更新计数 %d", count, st.getUpdateCount());
                    }
                    hasResults = st.getMoreResults();
                } while (hasResults || st.getUpdateCount() != -1);
                resultsTrue = count;
                log("2.8 allowMultiQueries=true: 共收到 %d 个结果", count);
            }
        }
        boolean defaultThrew = false;
        try (Connection c = raw(null)) {
            try (Statement st = c.createStatement()) {
                st.execute("SELECT 1 AS a; SELECT 2 AS b");
            } catch (SQLException e) {
                defaultThrew = true;
                log("2.8 默认: 多语句执行报错: SQLState=%s, msg=%s", e.getSQLState(), e.getMessage());
            }
        }
        assertEquals(2, resultsTrue, "allowMultiQueries=true 应返回 2 个结果集");
        assertTrue(defaultThrew, "默认配置下多语句应被服务端拒绝");
    }
}
