package org.opengauss.test;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 连接池核心行为测试：
 * 并发借出（两阶段屏障 + 全部同时持有）、maxTotal/maxWaitMillis 上下界与池耗尽异常、
 * maxIdle 对空闲连接数上限（精确收敛）、minIdle 由维护线程补足、归还后物理会话复用、
 * 连接状态复位（autoCommit/隔离级别）、removeAbandoned 泄漏回收、按空闲时长驱逐。
 */
public class ConnectionPoolTest extends BaseDbTest {

    @Test
    public void testBorrowAndReturn() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            assertFalse(conn.isClosed());
            assertEquals(1, dataSource.getNumActive());
        }
        assertEquals("归还后 active 应为 0", 0, dataSource.getNumActive());
    }

    /**
     * 并发借出：使用测试专用连接池 + 两阶段屏障。
     * 所有线程先各自借到连接并一直持有，主线程确认 active 达到线程数（同时持有的峰值），
     * 再统一放行执行查询。所有 await/Future 均带超时。
     */
    @Test
    public void testConcurrentBorrowAllThreadsHoldSimultaneously() throws Exception {
        int threads = 6;
        BasicDataSource ds = newPool();
        ds.setMaxTotal(threads);
        ds.setMaxWaitMillis(5000);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch held = new CountDownLatch(threads); // 阶段1：所有线程已借到连接
            CountDownLatch release = new CountDownLatch(1);     // 阶段2：放行执行查询
            CountDownLatch done = new CountDownLatch(threads);
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(pool.submit(() -> {
                    try (Connection conn = ds.getConnection()) {
                        held.countDown();
                        if (!release.await(15, TimeUnit.SECONDS)) {
                            return false;
                        }
                        try (Statement st = conn.createStatement();
                             ResultSet rs = st.executeQuery("SELECT 1")) {
                            return rs.next() && rs.getInt(1) == 1;
                        }
                    } finally {
                        done.countDown();
                    }
                }));
            }
            assertTrue("所有线程应在 15s 内借到连接", held.await(15, TimeUnit.SECONDS));

            // 所有线程此刻都持有连接且阻塞在屏障上，active 应正好等于线程数（即并发峰值）
            int peakActive = 0;
            for (int i = 0; i < 50; i++) {
                peakActive = Math.max(peakActive, ds.getNumActive());
                if (ds.getNumActive() >= threads) {
                    break;
                }
                Thread.sleep(20);
            }
            assertEquals("所有线程同时持有连接时 peak active 应等于线程数", threads, peakActive);
            assertEquals("屏障阶段 active 应等于线程数（全部同时持有）", threads, ds.getNumActive());

            release.countDown();
            assertTrue("所有线程应在 15s 内完成查询并归还", done.await(15, TimeUnit.SECONDS));
            for (Future<Boolean> f : results) {
                assertTrue("并发借出执行失败", f.get(5, TimeUnit.SECONDS));
            }
            assertEquals("全部归还后 active 应为 0", 0, ds.getNumActive());
        } finally {
            shutdownExecutorAndClosePool(pool, ds, 15000, "并发借出线程未在超时内终止");
        }
    }

    /**
     * maxTotal/maxWaitMillis：验证等待时长上下界与明确的池耗尽异常。
     * 若第二次借出意外成功，必须先关闭该连接再 fail；连接池在 finally 中可靠关闭。
     */
    @Test
    public void testMaxTotalAndMaxWaitTimeout() throws Exception {
        BasicDataSource ds = newPool();
        ds.setMaxTotal(1);
        ds.setMaxWaitMillis(2000);
        Connection c2 = null;
        try {
            try (Connection c1 = ds.getConnection()) {
                long begin = System.currentTimeMillis();
                try {
                    c2 = ds.getConnection();
                    fail("超过 maxTotal 且超过 maxWaitMillis 应抛出 SQLException");
                } catch (SQLException e) {
                    long cost = System.currentTimeMillis() - begin;
                    assertTrue("等待时长应接近 maxWaitMillis(2000ms)，实际: " + cost + "ms", cost >= 1500);
                    assertTrue("等待时长不应远超 maxWaitMillis(2000ms)，实际: " + cost + "ms", cost <= 5000);
                    assertTrue("应给出明确的池耗尽异常，实际: " + e.getMessage(),
                            e.getMessage() != null
                                    && e.getMessage().contains("Timeout waiting for idle object"));
                    assertTrue("池耗尽异常应链式包含 NoSuchElementException，实际 cause: "
                                    + (e.getCause() == null ? null : e.getCause()),
                            e.getCause() instanceof NoSuchElementException
                                    && e.getCause().getMessage() != null
                                    && e.getCause().getMessage().contains("Timeout waiting for idle object"));
                }
            }
        } finally {
            closeConnectionQuietly(c2);
            closeDataSourceQuietly(ds);
        }
    }

    /**
     * 满池真实业务行为：8 条连接分别在事务中完成 CRUD 后继续持有；
     * 第 9 条请求先在无释放时超时，再验证释放任一连接后的恢复执行。
     */
    @Test
    public void testMaxTotalConcurrentCrudAndQueuedBorrow() throws Exception {
        final int maxTotal = 8;
        final long waitMillis = 5000;
        final long workerHoldMillis = 15000;
        BasicDataSource ds = newPool();
        ds.setInitialSize(0);
        ds.setMinIdle(0);
        ds.setMaxTotal(maxTotal);
        ds.setMaxWaitMillis(waitMillis);

        ExecutorService pool = Executors.newFixedThreadPool(maxTotal + 1);
        CountDownLatch workersHoldingConnections = new CountDownLatch(maxTotal);
        CountDownLatch releaseWorkers = new CountDownLatch(1);
        List<Future<Boolean>> workers = new ArrayList<>();
        String runId = "pool_max_" + System.nanoTime();
        try {
            for (int i = 0; i < maxTotal; i++) {
                final String name = runId + "_" + i;
                workers.add(pool.submit(() -> {
                    boolean completedCrud = false;
                    try (Connection conn = ds.getConnection()) {
                        executeCrudInTransaction(conn, name, 20);
                        completedCrud = true;
                        workersHoldingConnections.countDown();
                        return releaseWorkers.await(workerHoldMillis, TimeUnit.MILLISECONDS);
                    } finally {
                        // 失败路径同样唤醒主线程，后续 Future.get 会保留原始失败原因。
                        if (!completedCrud) {
                            workersHoldingConnections.countDown();
                        }
                    }
                }));
            }

            assertTrue("所有 worker 应在 20s 内完成事务并保持借出连接",
                    workersHoldingConnections.await(20, TimeUnit.SECONDS));
            assertEquals("8 条连接均应处于 active，不能只验证建连成功",
                    maxTotal, ds.getNumActive());

            Future<Boolean> timedOutOverflow = pool.submit(() -> {
                try (Connection ignored = ds.getConnection()) {
                    return false;
                } catch (SQLException e) {
                    return e.getMessage() != null
                            && e.getMessage().contains("Timeout waiting for idle object");
                }
            });
            assertTrue("8 条连接均不释放时，第 9 条请求应在 maxWaitMillis 后超时",
                    timedOutOverflow.get(waitMillis + 3000, TimeUnit.MILLISECONDS));
            assertEquals("第 9 条请求超时后，8 条 worker 仍应处于 active",
                    maxTotal, ds.getNumActive());

            CountDownLatch overflowRequested = new CountDownLatch(1);
            long overflowBegin = System.nanoTime();
            Future<Boolean> overflow = pool.submit(() -> {
                overflowRequested.countDown();
                try (Connection conn = ds.getConnection();
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT 1")) {
                    return rs.next() && rs.getInt(1) == 1;
                }
            });

            assertTrue("第 9 条连接请求未发起", overflowRequested.await(2, TimeUnit.SECONDS));
            Thread.sleep(300);
            assertFalse("达到 maxTotal 后，第 9 条请求应等待空闲连接而不能立即成功", overflow.isDone());
            assertEquals("排队期间 active 不得超过 maxTotal", maxTotal, ds.getNumActive());

            releaseWorkers.countDown();
            for (Future<Boolean> worker : workers) {
                assertTrue("worker 未在满池释放后正常结束", worker.get(10, TimeUnit.SECONDS));
            }
            assertTrue("释放连接后，第 9 条请求应恢复并执行 SQL", overflow.get(10, TimeUnit.SECONDS));
            long overflowCost = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - overflowBegin);
            assertTrue("第 9 条请求应实际经历排队等待，耗时: " + overflowCost + "ms",
                    overflowCost >= 250);
            assertEquals("全部归还后不应遗留 active 连接", 0, ds.getNumActive());
        } finally {
            releaseWorkers.countDown();
            shutdownExecutorAndClosePool(pool, ds, 15000, "满池并发 CRUD 线程未在超时内终止");
        }
    }

    /**
     * maxIdle：并发创建 maxTotal 个物理连接，全部归还后空闲连接数应精确收敛到 maxIdle。
     * 采用 held/release 两阶段屏障：工作线程内部 try-with-resources 自行归还，
     * 主线程确认 8 个连接同时 active 后统一放行；Future/await 全部带超时，
     * 任何失败路径都由 finally 停止线程池并关闭 ds，不会丢失借出的连接。
     */
    @Test
    public void testMaxIdleLimitsIdleConnections() throws Exception {
        int connCount = 8;
        int maxIdle = 2;
        BasicDataSource ds = newPool();
        ds.setMaxTotal(connCount);
        ds.setMaxIdle(maxIdle);
        ds.setMaxWaitMillis(10000);
        ExecutorService pool = Executors.newFixedThreadPool(connCount);
        try {
            CountDownLatch held = new CountDownLatch(connCount); // 阶段1：全部线程已借到连接
            CountDownLatch release = new CountDownLatch(1);       // 阶段2：放行归还
            CountDownLatch done = new CountDownLatch(connCount);
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < connCount; i++) {
                results.add(pool.submit(() -> {
                    try (Connection c = ds.getConnection()) {
                        held.countDown();
                        if (!release.await(15, TimeUnit.SECONDS)) {
                            return false;
                        }
                        return true;
                    } finally {
                        done.countDown();
                    }
                }));
            }
            assertTrue("8 个线程应在 15s 内全部借到连接", held.await(15, TimeUnit.SECONDS));
            assertEquals("8 个连接应同时 active（全部持有）", connCount, ds.getNumActive());

            release.countDown();
            assertTrue("8 个线程应在 15s 内全部归还", done.await(15, TimeUnit.SECONDS));
            for (Future<Boolean> f : results) {
                assertTrue("借出/归还执行失败", f.get(10, TimeUnit.SECONDS));
            }

            // 全部归还后：active 必须为 0（无泄漏），空闲数必须精确收敛到 maxIdle=2
            waitForCondition(() -> ds.getNumIdle() == maxIdle, 10000,
                    "全部归还后空闲连接数应精确收敛到 maxIdle=" + maxIdle);
            assertEquals("全部归还后 active 应为 0", 0, ds.getNumActive());
            assertEquals("归还后空闲连接数应恰好等于 maxIdle，实际: " + ds.getNumIdle(),
                    maxIdle, ds.getNumIdle());
        } finally {
            shutdownExecutorAndClosePool(pool, ds, 15000, "maxIdle 借出线程未在超时内终止");
        }
    }

    /**
     * minIdle：启动维护线程，轮询断言空闲连接被补足到 minIdle；
     * 且在被借走期间维护线程仍继续补足。
     */
    @Test
    public void testMinIdleReplenishedByMaintenanceThread() throws Exception {
        int minIdle = 2;
        BasicDataSource ds = newPool();
        ds.setInitialSize(0);
        ds.setMinIdle(minIdle);
        ds.setMaxIdle(5);
        ds.setMaxTotal(5);
        ds.setTimeBetweenEvictionRunsMillis(300);
        Connection a = null;
        Connection b = null;
        try {
            try (Connection c = ds.getConnection()) {
                // 首次借出触发建池
            }
            waitForCondition(() -> ds.getNumIdle() >= minIdle, 15000,
                    "维护线程应在池建成后把空闲连接补足到 minIdle=" + minIdle);
            assertEquals("空闲补足期间不应有借出的连接", 0, ds.getNumActive());
            assertTrue("minIdle 补足后空闲连接数应 >= " + minIdle + "，实际: " + ds.getNumIdle(),
                    ds.getNumIdle() >= minIdle);

            // 借走全部空闲连接后，维护线程仍应把空闲补足回 minIdle（active 保持被借出状态）
            a = ds.getConnection();
            b = ds.getConnection();
            assertEquals("借走 2 个空闲连接后 idle 应为 0", 0, ds.getNumIdle());
            waitForCondition(() -> ds.getNumIdle() >= minIdle, 15000,
                    "持有连接期间维护线程仍应补足空闲连接到 minIdle");
            assertEquals("补足期间被借出的连接不应被释放", 2, ds.getNumActive());
        } finally {
            if (a != null) {
                try {
                    a.close();
                } catch (SQLException ignored) {
                }
            }
            if (b != null) {
                try {
                    b.close();
                } catch (SQLException ignored) {
                }
            }
            closeDataSourceQuietly(ds);
        }
    }

    /**
     * 连接归还复用：maxTotal=1，close 归还后重新借出应复用同一物理连接（同一会话、同一对象）。
     */
    @Test
    public void testReturnAndReborrowReusesPhysicalSession() throws Exception {
        BasicDataSource ds = newPool();
        ds.setMaxTotal(1);
        ds.setMaxWaitMillis(3000);
        try {
            com.mysql.cj.jdbc.ConnectionImpl phys1;
            long id1;
            try (Connection c = ds.getConnection()) {
                phys1 = physicalConnection(c);
                id1 = connectionId(c);
            }
            com.mysql.cj.jdbc.ConnectionImpl phys2;
            long id2;
            try (Connection c = ds.getConnection()) {
                phys2 = physicalConnection(c);
                id2 = connectionId(c);
            }
            assertTrue("close 归还后重新借出应复用同一物理连接对象", phys1 == phys2);
            assertEquals("close 归还后重新借出应复用同一服务端会话(CONNECTION_ID 相同)", id1, id2);
        } finally {
            closeDataSourceQuietly(ds);
        }
    }

    /**
     * 连接状态复位：归还前修改 autoCommit/isolation，重新借出后应恢复默认值。
     * 注：dolphin 的 MySQL 协议实测不支持 readOnly（setReadOnly(true) 后 isReadOnly()
     * 仍为 false），故本用例只断言 autoCommit 与隔离级别的复位，不声明 readOnly 复位。
     */
    @Test
    public void testStateResetOnReborrow() throws Exception {
        BasicDataSource ds = newPool();
        ds.setMaxTotal(1);
        ds.setMaxWaitMillis(3000);
        ds.setDefaultAutoCommit(true);
        ds.setDefaultTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

        Connection c1 = null;
        try {
            c1 = ds.getConnection();
            c1.setAutoCommit(false);
            c1.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            assertEquals("归还前 autoCommit 应已被修改为 false", false, c1.getAutoCommit());
            assertEquals("归还前隔离级别应已被修改为 READ_COMMITTED",
                    Connection.TRANSACTION_READ_COMMITTED, c1.getTransactionIsolation());
            c1.close();
            c1 = null;

            try (Connection c2 = ds.getConnection()) {
                assertEquals("重新借出后 autoCommit 应复位为默认 true", true, c2.getAutoCommit());
                assertEquals("重新借出后隔离级别应复位为默认 REPEATABLE_READ",
                        Connection.TRANSACTION_REPEATABLE_READ, c2.getTransactionIsolation());
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
     * 连接泄漏回收：removeAbandoned 通过维护线程在等待期间周期回收废弃连接。
     * leak/second/third 均在 try 内获取，finally 逐个 quiet close（单个失败不阻断其余清理），
     * ds 使用 closeDataSourceQuietly；等待上界按 maxWaitMillis + 合理调度容差断言。
     */
    @Test
    public void testLeakReclamationWithRemoveAbandoned() throws Exception {
        BasicDataSource ds = newPool();
        ds.setMaxTotal(2);
        ds.setMaxWaitMillis(8000);
        ds.setRemoveAbandonedOnBorrow(true);
        ds.setRemoveAbandonedTimeout(5); // 借出超过 5 秒未使用视为泄漏
        ds.setRemoveAbandonedOnMaintenance(true);
        ds.setTimeBetweenEvictionRunsMillis(1000);
        Connection leak = null;  // 故意不归还（模拟泄漏）
        Connection second = null;
        Connection third = null;
        try {
            leak = ds.getConnection();
            second = ds.getConnection();
            long begin = System.currentTimeMillis();
            try {
                third = ds.getConnection();
            } catch (SQLException e) {
                fail("等待 removeAbandonedTimeout 后应能借出第三个连接: " + e.getMessage());
            }
            assertFalse("回收后借出的连接应可用", third.isClosed());
            long cost = System.currentTimeMillis() - begin;
            assertTrue("回收应等待 removeAbandonedTimeout(5s) 而非立即成功，实际: " + cost + "ms",
                    cost >= 4000);
            assertTrue("回收等待不应超过 maxWaitMillis(8000ms) + 调度容差，实际: " + cost + "ms",
                    cost <= 8000 + 3000);
        } finally {
            closeConnectionQuietly(third);
            closeConnectionQuietly(second);
            closeConnectionQuietly(leak); // 已被回收，close 幂等
            closeDataSourceQuietly(ds);
        }
    }

    /**
     * 空闲驱逐：空闲连接超过 minEvictableIdleTimeMillis 后被维护线程驱逐到 minIdle。
     */
    @Test
    public void testIdleEvictionByMinEvictableIdleTime() throws Exception {
        BasicDataSource ds = newPool();
        ds.setInitialSize(2);
        ds.setMinIdle(0);
        ds.setMinEvictableIdleTimeMillis(3000);
        ds.setTimeBetweenEvictionRunsMillis(1000);
        try {
            try (Connection c = ds.getConnection()) {
                // 首次借出触发建池，归还后 2 个空闲
            }
            waitForCondition(() -> ds.getNumIdle() < 2, 15000,
                    "空闲连接应在超过 minEvictableIdleTimeMillis 后被驱逐到 minIdle=0");
            assertEquals(0, ds.getNumActive());
        } finally {
            closeDataSourceQuietly(ds);
        }
    }

    private static void executeCrudInTransaction(Connection conn, String name, int initialAge) throws SQLException {
        conn.setAutoCommit(false);
        try {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO `user`(`name`,`age`) VALUES (?,?)")) {
                ps.setString(1, name);
                ps.setInt(2, initialAge);
                assertEquals("事务内 INSERT 应影响一行", 1, ps.executeUpdate());
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT `age` FROM `user` WHERE `name`=?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue("事务内 INSERT 后应可查询到数据", rs.next());
                    assertEquals(initialAge, rs.getInt(1));
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE `user` SET `age`=? WHERE `name`=?")) {
                ps.setInt(1, initialAge + 1);
                ps.setString(2, name);
                assertEquals("事务内 UPDATE 应影响一行", 1, ps.executeUpdate());
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT `age` FROM `user` WHERE `name`=?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue("事务内 UPDATE 后应可查询到数据", rs.next());
                    assertEquals(initialAge + 1, rs.getInt(1));
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM `user` WHERE `name`=?")) {
                ps.setString(1, name);
                assertEquals("事务内 DELETE 应影响一行", 1, ps.executeUpdate());
            }
            conn.commit();
        } catch (SQLException | RuntimeException e) {
            conn.rollback();
            throw e;
        }
    }
}
