package org.opengauss.test;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 连接校验与自愈测试：
 * testOnBorrow 校验、testWhileIdle 后台校验、连接池对失效连接（服务端断连）的自愈能力。
 * 服务端断连通过**独立管理连接执行 KILL &lt;CONNECTION_ID&gt;** 实现（不再调用被测
 * ConnectionImpl.close()），并先用 SHOW PROCESSLIST 复核服务端会话确实已失效，
 * 再验证 testOnBorrow / testWhileIdle 丢弃死连接并重建。
 * 均显式 testOnReturn=false，避免归还路径的校验干扰对“死连接已进入空闲池”的判定。
 */
public class ConnectionValidationTest extends BaseDbTest {

    @Test
    public void testValidationQueryOnBorrow() throws Exception {
        // 基类连接池 testOnBorrow=true：借出即执行 SELECT 1 校验
        try (Connection conn = dataSource.getConnection()) {
            assertFalse(conn.isClosed());
            assertTrue("testOnBorrow=true 时借出的连接应通过校验并可用", conn.isValid(3));
        }
    }

    /**
     * 服务端断连后，testOnBorrow 应丢弃死连接并重建新物理连接。
     * 流程：KILL 会话 → SHOW PROCESSLIST 确认消失 → 本地 isClosed() 仍为 false →
     * 逻辑归还（testOnReturn=false）后 idle==1 证明死连接进入空闲池 → 再次借出时
     * testOnBorrow 校验失败才丢弃并重建。
     */
    @Test
    public void testOnBorrowDiscardsKilledConnectionAndRebuilds() throws Exception {
        BasicDataSource ds = newPool();
        ds.setMaxTotal(1);
        ds.setMaxWaitMillis(5000);
        ds.setTestOnBorrow(true);
        ds.setTestWhileIdle(false);
        ds.setTestOnReturn(false);
        ds.setValidationQuery("SELECT 1");

        Connection c1 = null;
        try {
            c1 = ds.getConnection();
            com.mysql.cj.jdbc.ConnectionImpl phys1 = physicalConnection(c1);
            long killedId = connectionId(c1);
            // 服务端断连：独立管理连接执行 KILL，不调用被测 ConnectionImpl.close()
            killServerSession(killedId);
            // 复核：服务端会话确实消失（而非仅客户端标记关闭）
            waitForCondition(() -> {
                try {
                    return !serverSessionExists(killedId);
                } catch (SQLException e) {
                    return false; // 查询失败视作会话尚未消失，继续轮询
                }
            }, 5000, "KILL 后服务端会话应消失，id=" + killedId);
            // 本地视图：被测 JDBC/物理连接仍报告未关闭，证明是服务端终止而非客户端 close
            assertFalse("服务端 KILL 后本地 isClosed() 仍应为 false", c1.isClosed());

            c1.close(); // 逻辑归还（testOnReturn=false，归还不做校验）
            c1 = null;
            assertEquals("死连接应进入空闲池（testOnReturn=false 归还未校验）", 1, ds.getNumIdle());

            // 再次借出：testOnBorrow 校验失败 → 丢弃死连接 → 新建可用连接（自愈）
            try (Connection c2 = ds.getConnection();
                 Statement st = c2.createStatement();
                 ResultSet rs = st.executeQuery("SELECT 1")) {
                assertTrue("重建后的连接应可执行查询", rs.next() && rs.getInt(1) == 1);
                assertFalse("池应自愈并提供可用连接", c2.isClosed());
                com.mysql.cj.jdbc.ConnectionImpl phys2 = physicalConnection(c2);
                assertTrue("testOnBorrow 应丢弃死连接并创建新物理连接（对象应不同）",
                        phys2 != phys1);
            }
        } finally {
            if (c1 != null) {
                try {
                    c1.close();
                } catch (SQLException ignored) {
                }
            }
            closeDataSourceQuietly(ds);
        }
    }

    /**
     * 服务端断连后，testWhileIdle 应通过后台校验清除死连接。
     * 流程：KILL 会话 → SHOW PROCESSLIST 确认消失 → 本地 isClosed() 仍为 false →
     * 逻辑归还（testOnReturn=false）后、维护线程首轮前 idle==1 → 后台校验轮询到 idle==0
     * → 再次借出得到新物理对象。
     */
    @Test
    public void testWhileIdleEvictsKilledConnection() throws Exception {
        BasicDataSource ds = newPool();
        ds.setMaxTotal(2);
        ds.setMaxIdle(1);
        ds.setMinIdle(0);
        ds.setInitialSize(1);
        ds.setTestOnBorrow(false);
        ds.setTestWhileIdle(true);
        ds.setTestOnReturn(false);
        ds.setValidationQuery("SELECT 1");
        // 首轮维护跑批在 10s 后，保证有足够时间完成“KILL→归还→断言 idle==1”
        ds.setTimeBetweenEvictionRunsMillis(10000);
        // 调大空闲时长阈值：确保只有 testWhileIdle 的校验路径会移除死连接（而非按空闲时长驱逐）
        ds.setMinEvictableIdleTimeMillis(60000);

        Connection c1 = null;
        try {
            c1 = ds.getConnection();
            com.mysql.cj.jdbc.ConnectionImpl phys1 = physicalConnection(c1);
            long killedId = connectionId(c1);
            killServerSession(killedId);
            waitForCondition(() -> {
                try {
                    return !serverSessionExists(killedId);
                } catch (SQLException e) {
                    return false;
                }
            }, 5000, "KILL 后服务端会话应消失，id=" + killedId);
            assertFalse("服务端 KILL 后本地 isClosed() 仍应为 false", c1.isClosed());

            c1.close(); // 逻辑归还（testOnReturn=false）
            c1 = null;
            // 维护线程首轮（10s）之前，死连接应已在空闲池中
            assertEquals("维护线程首轮前死连接应进入空闲池（idle==1）", 1, ds.getNumIdle());

            // 后台维护线程通过 testWhileIdle 校验清除死连接：空闲数应回到 0
            waitForCondition(() -> ds.getNumIdle() == 0, 15000,
                    "testWhileIdle 应清除死连接（minIdle=0 且不会按时间驱逐）");

            // 再次借出应得到全新的物理会话
            try (Connection c2 = ds.getConnection()) {
                assertFalse("重新借出的连接应可用", c2.isClosed());
                com.mysql.cj.jdbc.ConnectionImpl phys2 = physicalConnection(c2);
                assertTrue("testWhileIdle 清除死连接后应创建新物理连接（对象应不同）",
                        phys2 != phys1);
            }
        } finally {
            if (c1 != null) {
                try {
                    c1.close();
                } catch (SQLException ignored) {
                }
            }
            closeDataSourceQuietly(ds);
        }
    }
}
