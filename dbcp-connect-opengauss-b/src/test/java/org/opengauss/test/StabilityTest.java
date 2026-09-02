package org.opengauss.test;

import org.apache.commons.dbcp2.BasicDataSource;
import org.opengauss.dao.UserDao;
import org.opengauss.datasource.BasicDataSourceFactory;
import org.opengauss.entity.User;
import org.opengauss.test.util.TestConfig;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 稳定性测试：反复建池/关池、16 线程业务事务并发、长连接保活
 */
public class StabilityTest extends BaseDbTest {

    @Test
    public void testRepeatedPoolCreateAndClose() throws Exception {
        for (int i = 0; i < 10; i++) {
            BasicDataSource ds = BasicDataSourceFactory.create(TestConfig.load());
            try {
                try (Connection conn = ds.getConnection();
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT 1")) {
                    assertTrue(rs.next());
                }
            } finally {
                closeDataSourceQuietly(ds);
            }
        }
    }

    @Test
    public void testConcurrentCrudLoad() throws Exception {
        int threads = 16;
        final long waitMillis = 10000;
        final String runId = "st_业务并发_" + System.currentTimeMillis() + "_";
        BasicDataSource ds = newPool();
        ds.setInitialSize(0);
        ds.setMinIdle(0);
        ds.setMaxTotal(threads);
        ds.setMaxWaitMillis(waitMillis);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<Boolean>> results = new ArrayList<>();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch completedBusinessFlow = new CountDownLatch(threads);
        CountDownLatch release = new CountDownLatch(1);
        Throwable failure = null;
        try {
            for (int i = 0; i < threads; i++) {
                final int idx = i;
                results.add(pool.submit(() -> {
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new RuntimeException("并发 CRUD 线程等待放行超时");
                    }
                    try (Connection conn = ds.getConnection()) {
                        executeBusinessTransaction(conn, runId + idx, 20 + idx);
                        completedBusinessFlow.countDown();
                        return release.await(waitMillis, TimeUnit.MILLISECONDS);
                    }
                }));
            }
            start.countDown();
            assertTrue("所有线程应在 20s 内完成业务事务",
                    completedBusinessFlow.await(20, TimeUnit.SECONDS));
            assertEquals("高并发业务事务执行期间 active 应等于线程数", threads, ds.getNumActive());
            release.countDown();
            for (Future<Boolean> f : results) {
                assertTrue("业务事务线程执行失败", f.get(15, TimeUnit.SECONDS));
            }
            assertEquals("高并发业务事务完成后应无连接泄漏(active=0)", 0, ds.getNumActive());
        } catch (Throwable t) {
            failure = t;
        } finally {
            // 先终止线程池，再清理测试数据，避免清理异常掩盖原始失败。
            try {
                release.countDown();
                shutdownExecutor(pool, 20000, "并发业务事务线程未在超时内终止");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (failure == null) {
                    failure = e;
                }
            } catch (Throwable t) {
                if (failure == null) {
                    failure = t;
                }
            }
            // 清理当前 runId 的数据，避免残留记录干扰后续运行。
            try {
                UserDao dao = new UserDao(ds);
                int deleted = cleanupRunData(dao, runId);
                if (failure == null) {
                    // Each successful transaction already executes DELETE and COMMIT.
                    // A nonzero cleanup count would prove that a worker left test data behind.
                    assertEquals("成功事务完成后不应残留并发测试数据", 0, deleted);
                }
            } catch (Throwable t) {
                if (failure == null) {
                    failure = t;
                }
            }
            closeDataSourceQuietly(ds);
            if (failure != null) {
                if (failure instanceof AssertionError) {
                    throw (AssertionError) failure;
                }
                if (failure instanceof RuntimeException) {
                    throw (RuntimeException) failure;
                }
                throw new RuntimeException(failure);
            }
        }
    }

    @Test
    public void testLongRunningConnectionKeepAlive() throws Exception {
        // 同一个连接复用 200 次查询，验证长连接稳定
        try (Connection conn = dataSource.getConnection()) {
            for (int i = 0; i < 200; i++) {
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT " + i)) {
                    assertTrue(rs.next());
                }
            }
        }
    }

    /**
     * 自包含的有界负载验证（不依赖 JUnit 方法执行顺序，也不依赖其他用例留下的数据）：
     * 自身执行一段有界并发插入负载，随后断言 active=0 无泄漏、池仍可借出并归还。
     */
    @Test
    public void testPoolStatsAfterLoad() throws Exception {
        int threads = 2;
        int perThread = 5;
        final String runId = "st_stats_" + System.currentTimeMillis() + "_";
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<Integer>> results = new ArrayList<>();
        CountDownLatch start = new CountDownLatch(1);
        Throwable failure = null;
        try {
            for (int i = 0; i < threads; i++) {
                final int idx = i;
                results.add(pool.submit(() -> {
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new RuntimeException("压测线程等待放行超时");
                    }
                    UserDao dao = new UserDao(dataSource);
                    int count = 0;
                    for (int j = 0; j < perThread; j++) {
                        User u = new User(runId + idx + "_" + j, 20 + (j % 10));
                        dao.insert(u);
                        count++;
                    }
                    return count;
                }));
            }
            start.countDown();
            int total = 0;
            for (Future<Integer> f : results) {
                total += f.get(30, TimeUnit.SECONDS);
            }
            assertEquals("有界负载插入数应正确", threads * perThread, total);

            // 负载结束后：无连接泄漏，池仍可借出并归还
            assertEquals("负载结束后应无连接泄漏(active=0)", 0, dataSource.getNumActive());
            try (Connection conn = dataSource.getConnection();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT 1")) {
                assertTrue(rs.next());
            }
            assertEquals("再次借出并归还后 active 应为 0", 0, dataSource.getNumActive());
        } catch (Throwable t) {
            failure = t;
        } finally {
            try {
                shutdownExecutor(pool, 20000, "压测线程未在超时内终止");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (failure == null) {
                    failure = e;
                }
            } catch (Throwable t) {
                if (failure == null) {
                    failure = t;
                }
            }
            try {
                UserDao dao = new UserDao(dataSource);
                for (User u : dao.findAll()) {
                    if (u.getName() != null && u.getName().startsWith(runId)) {
                        dao.delete(u.getId());
                    }
                }
            } catch (Throwable t) {
                if (failure == null) {
                    failure = t;
                }
            }
            if (failure != null) {
                if (failure instanceof AssertionError) {
                    throw (AssertionError) failure;
                }
                if (failure instanceof RuntimeException) {
                    throw (RuntimeException) failure;
                }
                throw new RuntimeException(failure);
            }
        }
    }

    private static void executeBusinessTransaction(Connection conn, String name, int initialAge) throws SQLException {
        conn.setAutoCommit(false);
        try {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO `user`(`name`,`age`) VALUES (?,?)")) {
                ps.setString(1, name);
                ps.setInt(2, initialAge);
                assertEquals("高并发业务事务内 INSERT 应影响一行", 1, ps.executeUpdate());
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT `age` FROM `user` WHERE `name`=?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue("高并发业务事务内 INSERT 后应可查询到数据", rs.next());
                    assertEquals(initialAge, rs.getInt(1));
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE `user` SET `age`=? WHERE `name`=?")) {
                ps.setInt(1, initialAge + 1);
                ps.setString(2, name);
                assertEquals("高并发业务事务内 UPDATE 应影响一行", 1, ps.executeUpdate());
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT `age` FROM `user` WHERE `name`=?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue("高并发业务事务内 UPDATE 后应可查询到数据", rs.next());
                    assertEquals(initialAge + 1, rs.getInt(1));
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM `user` WHERE `name`=?")) {
                ps.setString(1, name);
                assertEquals("高并发业务事务内 DELETE 应影响一行", 1, ps.executeUpdate());
            }
            conn.commit();
        } catch (SQLException | RuntimeException e) {
            conn.rollback();
            throw e;
        }
    }

    private static int cleanupRunData(UserDao dao, String runId) throws SQLException {
        int deleted = 0;
        for (User u : dao.findAll()) {
            if (u.getName() != null && u.getName().startsWith(runId)) {
                deleted += dao.delete(u.getId());
            }
        }
        return deleted;
    }
}
