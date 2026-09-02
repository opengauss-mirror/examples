package org.opengauss.test;

import org.apache.commons.dbcp2.BasicDataSource;
import org.opengauss.datasource.BasicDataSourceFactory;
import org.opengauss.test.util.TestConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

/**
 * 测试基类：
 * 1. 统一创建 / 关闭连接池（每个测试类各自建池，互不影响）
 * 2. 提供把连接参数应用到临时 BasicDataSource 的工具方法
 * 3. 提供数据库连通性预检（失败时给出明确的排查提示）
 * 4. 提供测试共用的连接池/物理连接/会话断言工具方法
 */
public abstract class BaseDbTest {

    protected static BasicDataSource dataSource;

    @BeforeClass
    public static void initPool() throws Exception {
        closeIfNeeded();
        dataSource = BasicDataSourceFactory.create(TestConfig.load());
        // 连通性预检：能借出连接并执行 SELECT 1 才继续，否则给出明确提示
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("SELECT 1");
        } catch (Exception e) {
            closeIfNeeded();
            throw new IllegalStateException(
                    "数据库不可达，请检查：① openGauss 实例是否已启动并开启 enable_dolphin_proto；"
                            + "② dolphin_server_port 与 dbcp.properties 中 url 端口是否一致；"
                            + "③ 用户/密码/权限是否正确。原始异常: " + e.getMessage(), e);
        }
    }

    @AfterClass
    public static void closePool() throws Exception {
        closeIfNeeded();
    }

    private static void closeIfNeeded() {
        if (dataSource != null) {
            try {
                dataSource.close();
            } catch (SQLException ignored) {
                // 关闭失败不影响用例结果
            }
        }
    }

    /**
     * 把 dbcp.properties（含系统属性覆盖）的连接参数应用到临时连接池
     */
    protected static void applyUrl(BasicDataSource ds) {
        Properties p = TestConfig.load();
        ds.setDriverClassName(p.getProperty("driverClassName"));
        ds.setUrl(p.getProperty("url"));
        ds.setUsername(p.getProperty("username"));
        ds.setPassword(p.getProperty("password"));
    }

    /**
     * 新建一个仅带连接参数（驱动/URL/账号/密码）的测试专用连接池。
     * 其余池参数由各用例显式设置，避免依赖 dbcp.properties 的全局取值。
     */
    protected static BasicDataSource newPool() {
        BasicDataSource ds = new BasicDataSource();
        applyUrl(ds);
        return ds;
    }

    /**
     * 获取当前 MySQL 会话的服务端会话标识（SELECT CONNECTION_ID()）。
     * 注意：dolphin 会在会话结束后复用该数值，故只用于“同一会话复用”判断，
     * 不能用于区分“重建后的新会话”是否不同。
     */
    protected static long connectionId(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT CONNECTION_ID()")) {
            if (!rs.next()) {
                throw new SQLException("SELECT CONNECTION_ID() 无返回结果");
            }
            return rs.getLong(1);
        }
    }

    /**
     * 获取被 DBCP 包装的真实 MySQL 物理连接。
     * 不使用 unwrap(Connection.class)——它只会返回 DBCP 包装对象本身。
     */
    protected static com.mysql.cj.jdbc.ConnectionImpl physicalConnection(Connection conn) throws SQLException {
        com.mysql.cj.jdbc.ConnectionImpl phys = conn.unwrap(com.mysql.cj.jdbc.ConnectionImpl.class);
        if (phys == null) {
            throw new SQLException("无法取得真实 MySQL 物理连接（unwrap(ConnectionImpl.class) 返回 null）");
        }
        return phys;
    }

    /**
     * 服务端断连：从独立管理连接（共享池 dataSource 的另一个会话）执行 KILL <CONNECTION_ID>。
     * 不调用被测连接的 ConnectionImpl.close()，被测 JDBC/物理连接本地仍报告 isClosed()==false，
     * 从而证明是服务端终止会话，而非客户端关闭。
     * 说明：dolphin 的 MySQL 协议实测支持 KILL（本环境已验证：执行后服务端会话从
     * SHOW PROCESSLIST 消失，客户端 isClosed() 仍为 false，下次查询才报断连）。
     */
    protected static void killServerSession(long id) throws SQLException {
        try (Connection mgmt = dataSource.getConnection();
             Statement st = mgmt.createStatement()) {
            st.execute("KILL " + id);
        }
    }

    /**
     * 通过 SHOW PROCESSLIST 确认某个服务端会话是否仍存在（用于核验服务端断连是否真正生效）。
     */
    protected static boolean serverSessionExists(long id) throws SQLException {
        try (Connection c = dataSource.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SHOW PROCESSLIST")) {
            while (rs.next()) {
                if (rs.getLong(1) == id) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 停止并等待线程池终止（带超时）：shutdown 后 awaitTermination 指定时长，
     * 超时仍未终止则 shutdownNow 再补等一次；仍无法终止视为测试失败。
     * 保证所有并发用例的线程终止路径都有明确超时，不会无限挂起。
     */
    protected static void shutdownExecutor(ExecutorService pool, long timeoutMillis, String message)
            throws InterruptedException {
        if (pool == null) {
            return;
        }
        pool.shutdown();
        if (!pool.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS)) {
            pool.shutdownNow();
            if (!pool.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS)) {
                fail(message + "（线程池在超时内未终止）");
            }
        }
    }

    /**
     * 先带超时停止线程池，随后无论线程池终止是否失败都关闭连接池；
     * 线程池终止失败/被中断不会阻止 ds.close()，其失败会在连接池关闭后重新抛出。
     */
    protected static void shutdownExecutorAndClosePool(ExecutorService pool, BasicDataSource ds,
            long timeoutMillis, String message) {
        Throwable failure = null;
        try {
            shutdownExecutor(pool, timeoutMillis, message);
        } catch (Throwable t) {
            failure = t;
        }
        closeDataSourceQuietly(ds);
        if (failure != null) {
            if (failure instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            if (failure instanceof RuntimeException) {
                throw (RuntimeException) failure;
            }
            if (failure instanceof Error) {
                throw (Error) failure;
            }
            throw new RuntimeException(failure);
        }
    }

    /**
     * 关闭测试专用连接池（忽略关闭异常，确保 finally 中不因关闭失败掩盖原断言失败）。
     */
    protected static void closeDataSourceQuietly(BasicDataSource ds) {
        if (ds != null) {
            try {
                ds.close();
            } catch (SQLException ignored) {
            }
        }
    }

    /**
     * 关闭单个连接（忽略关闭异常）。finally 中逐个调用：某个连接关闭失败不影响其余连接与连接池关闭。
     */
    protected static void closeConnectionQuietly(Connection c) {
        if (c != null) {
            try {
                c.close();
            } catch (SQLException ignored) {
            }
        }
    }

    /**
     * 轮询等待条件成立（带超时，避免测试无限阻塞）。
     */
    protected static void waitForCondition(Condition cond, long timeoutMillis, String message) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (cond.eval()) {
                return;
            }
            Thread.sleep(200);
        }
        fail(message);
    }

    @FunctionalInterface
    protected interface Condition {
        boolean eval();
    }
}
