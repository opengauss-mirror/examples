package com.linyu.ospp;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 大并发业务场景验证：模拟多用户并发下单（账户扣减 + 订单写入，事务化）。
 *
 * 设计要点（回应"用大并发测一下、构建业务场景"）：
 *  1. 业务场景：每个 worker 代表一个用户，在单个事务内完成"扣减自己账户余额 + 写入订单"，
 *     比单纯建连/单条 CRUD 更贴近真实业务。
 *  2. 大并发：worker 数远大于连接池 maximumPoolSize，制造连接池竞争，验证
 *     (a) 连接池上限不被突破（峰值活跃连接 <= maximumPoolSize）
 *     (b) 高并发下事务全部正确提交、数据一致
 *     (c) 吞吐（QPS）与总耗时
 *
 * 用法: java ...HighConcurrencyBusinessDemo [host] [workers] [db] [user]
 *   说明: 密码必须通过环境变量 DB_PASSWORD 设置
 *   默认: host=127.0.0.1 workers=50 db=mysql_db user=mysqluser
 */
public class HighConcurrencyBusinessDemo {

    private static final int MAX_POOL = 10;          // 故意小于并发数，制造连接池竞争
    private static final int AMOUNT = 10;            // 每笔订单金额
    private static final long BIZ_SLEEP_MS = 20;     // 模拟业务处理耗时（持连接期间），制造连接池排队

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int workers = args.length > 1 ? Integer.parseInt(args[1]) : 50;
        String db = args.length > 2 ? args[2] : "mysql_db";
        String user = args.length > 3 ? args[3] : "mysqluser";
        String pwd = System.getenv("DB_PASSWORD");
        if (pwd == null || pwd.isEmpty()) {
            throw new IllegalStateException("请设置环境变量 DB_PASSWORD");
        }

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:mysql://" + host + ":3306/" + db
                + "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC&rewriteBatchedStatements=true");
        cfg.setUsername(user);
        cfg.setPassword(pwd);
        cfg.setMaximumPoolSize(MAX_POOL);
        cfg.setMinimumIdle(2);
        cfg.setConnectionTimeout(30000);
        cfg.setMaxLifetime(540000);
        cfg.setPoolName("HikariCP-HC-Stress");
        final HikariDataSource ds;
        try {
            ds = new HikariDataSource(cfg);
        } catch (RuntimeException e) {
            System.err.println("连接池初始化失败，请检查配置文件: " + e.getMessage());
            throw new IllegalStateException("连接池初始化失败", e);
            }

        if (workers <= 0) {
            throw new IllegalArgumentException("并发线程数必须大于 0，当前值：" + workers);
        }

        setup(ds, workers);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();
        AtomicInteger peakActive = new AtomicInteger();
        AtomicLong totalBalance = new AtomicLong();

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(workers);

        ExecutorService es = Executors.newFixedThreadPool(workers);

        long t0 = System.currentTimeMillis();
        for (int i = 0; i < workers; i++) {
            final int wid = i + 1;
            es.submit(() -> {
                try {
                    startGate.await();
                } catch (InterruptedException ignored) {
                }
                try (Connection c = ds.getConnection()) {
                    HikariPoolMXBean mx = ds.getHikariPoolMXBean();
                    if (mx != null) {
                        int cur = mx.getActiveConnections();
                        int prev;
                        do {
                            prev = peakActive.get();
                        } while (cur > prev && !peakActive.compareAndSet(prev, cur));
                    }
                    businessTx(c, wid);
                    success.incrementAndGet();
                } catch (Exception e) {
                    fail.incrementAndGet();
                    System.err.println("worker " + wid + " FAILED: " + e.getMessage());
                } finally {
                    doneGate.countDown();
                }
            });
        }
        startGate.countDown();
        boolean finished = doneGate.await(180, TimeUnit.SECONDS);
        long t1 = System.currentTimeMillis();

        boolean verified = false;
        if (finished) {
            // 正常完成：优雅关闭线程池（此时所有任务已结束），再做一致性校验
            es.shutdown();
            verified = verify(ds, workers, success.get(), totalBalance);
        } else {
            // 超时：180s 内未全部完成，先打印中间结果，再强制中断仍在运行的 worker，
            // 并等待其释放连接，避免粗暴 close 数据源导致线程被突然打断而产生难以排查的异常。
            System.err.println("超时：180s 内仍有 worker 未完成，强制中断剩余任务。");
            es.shutdownNow();
            try {
                es.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println();
        System.out.println("==================== 大并发业务场景 验证结果 ====================");
        System.out.println("目标实例      : " + host + ":3306/" + db);
        System.out.println("并发 worker  : " + workers);
        System.out.println("连接池上限    : " + MAX_POOL + " (maximumPoolSize)");
        System.out.println("完成状态      : " + (finished ? "全部在 180s 内完成" : "超时未完成!"));
        System.out.println("成功/失败     : " + success.get() + " / " + fail.get());
        System.out.println("峰值活跃连接  : " + peakActive.get() + "  (必须 <= 连接池上限 " + MAX_POOL + ")");
        System.out.println("总耗时        : " + (t1 - t0) + " ms");
        System.out.println("吞吐 QPS      : " + String.format("%.1f", workers * 1000.0 / (t1 - t0)));
        if (finished) {
            System.out.println("账户总余额    : " + totalBalance.get() + " (期望=" + (workers * 1000L - (long) success.get() * AMOUNT) + ")");
        } else {
            System.out.println("账户总余额    : 超时未完成，余额校验已跳过");
        }
        System.out.println("===============================================================");

        try {
            ds.close();
        } catch (Exception ignored) {
        }

        // 退出码综合判定：任一条件不满足即视为验证失败。
        //  - finished=false：超时未完成（连接池竞争下出现永久阻塞或任务卡死）
        //  - fail>0：存在失败的事务
        //  - peakActive>MAX_POOL：峰值活跃连接突破连接池上限
        //  - verified=false：一致性校验未通过（订单数/账户余额与预期不符）
        boolean ok = finished && fail.get() == 0 && peakActive.get() <= MAX_POOL && verified;
        if (!ok) {
            System.err.println("== 大并发业务场景验证 FAILED ==");
            System.exit(1);
        }
        System.out.println("== 大并发业务场景验证 PASSED ==");
        System.exit(0);
    }

    /** 建表 + 初始化账户（每个 worker 一个账户，余额 1000）。 */
    private static void setup(DataSource ds, int workers) throws Exception {
        try (Connection c = ds.getConnection(); java.sql.Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS hc_accounts (id INT PRIMARY KEY, balance INT)");
            st.execute("CREATE TABLE IF NOT EXISTS hc_orders (id INT PRIMARY KEY, user_id INT, amount INT)");
            st.execute("TRUNCATE TABLE hc_orders");
            st.execute("DELETE FROM hc_accounts");
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO hc_accounts(id,balance) VALUES(?,?)")) {
                for (int i = 1; i <= workers; i++) {
                    ps.setInt(1, i);
                    ps.setInt(2, 1000);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    /** 单笔业务事务：扣减自己账户 + 写入订单，提交。 */
    private static void businessTx(Connection c, int wid) throws Exception {
        c.setAutoCommit(false);
        try {
            try (PreparedStatement u = c.prepareStatement("UPDATE hc_accounts SET balance=balance-? WHERE id=?")) {
                u.setInt(1, AMOUNT);
                u.setInt(2, wid);
                if (u.executeUpdate() != 1) {
                    throw new RuntimeException("账户更新失败 wid=" + wid);
                }
            }
            try (PreparedStatement io = c.prepareStatement("INSERT INTO hc_orders(id,user_id,amount) VALUES(?,?,?)")) {
                io.setInt(1, wid);
                io.setInt(2, wid);
                io.setInt(3, AMOUNT);
                io.executeUpdate();
            }
            Thread.sleep(BIZ_SLEEP_MS); // 模拟业务处理（持连接期间），拉长占用以制造连接池排队
            c.commit();
        } catch (Exception e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
    }

    /** 一致性校验：订单数=worker 数；账户总余额=初始-扣减。返回 true 表示两项校验均通过。 */
    private static boolean verify(DataSource ds, int workers, int success, AtomicLong outBalance) throws Exception {
        boolean allPass = true;
        try (Connection c = ds.getConnection(); java.sql.Statement st = c.createStatement()) {
            try (ResultSet r1 = st.executeQuery("SELECT COUNT(*) FROM hc_orders")) {
                r1.next();
                long orders = r1.getLong(1);
                boolean pass = orders == success;
                allPass &= pass;
                System.out.println("[校验] 订单数=" + orders + " (期望=" + success + ") -> "
                        + (pass ? "PASS" : "FAIL"));
            }
            try (ResultSet r2 = st.executeQuery("SELECT SUM(balance) FROM hc_accounts")) {
                r2.next();
                long sum = r2.getLong(1);
                outBalance.set(sum);
                long expect = workers * 1000L - (long) success * AMOUNT;
                boolean pass = sum == expect;
                allPass &= pass;
                System.out.println("[校验] 账户总余额=" + sum + " (期望=" + expect + ") -> "
                        + (pass ? "PASS" : "FAIL"));
            }
        }
        return allPass;
    }
}
