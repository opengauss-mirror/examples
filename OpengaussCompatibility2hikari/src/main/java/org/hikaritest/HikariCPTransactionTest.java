package org.hikaritest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.*;

public class HikariCPTransactionTest {
    @Before
    public void startTest() {
        System.out.println("start the test ...");
    }

    @After
    public void tearDown () {
        // 关闭数据源
        HikariUtil.closeDataSource();
    }

    /**
     * 测试connection.setTransactionIsolation()的有效性
     *
     * @throws SQLException
     */
    @Test
    public void TransactionIsolationSettingTest () throws SQLException, InterruptedException {
        Connection conn1 = HikariUtil.getConnection();
        Connection conn2 = HikariUtil.getConnection();

        // 设置连接的事务隔离级别
        conn1.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        conn2.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

        // 测试事务隔离级别——脏读
        DirtyReadTest(conn1, conn2);
        // 测试事务隔离级别——不可重复读
        NonRepeatableReadTest(conn1, conn2);
        // 测试事务隔离级别——幻读
        PhantomReadTest(conn1, conn2);
    }

    /**
     * 测试事务隔离级别——脏读
     *
     * @throws SQLException
     * @throws InterruptedException
     */
    public void DirtyReadTest(Connection conn1, Connection conn2) throws SQLException, InterruptedException {

        conn1.setAutoCommit(false);
        conn2.setAutoCommit(false);

        System.out.println("Transaction isolation level of conn1: " + getTransactionIsolation(conn1));
        System.out.println("Transaction isolation level of conn2: " + getTransactionIsolation(conn2));

        // CREATE TABLE account (id INT PRIMARY KEY, balance DECIMAL(10, 2));
        // INSERT INTO account (id, balance) VALUES (1, 1.00);
        Statement stmt1 = conn1.createStatement();
        Statement stmt2 = conn2.createStatement();

        stmt1.executeUpdate("UPDATE account SET balance = balance + 100 WHERE id = 1");

        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(3000);
                System.out.println("Start rollback conn1...");
                conn1.rollback();
                System.out.println("Rollback done.");

            } catch (SQLException | InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.start();

        ResultSet rs = stmt2.executeQuery("SELECT balance FROM account WHERE id = 1");
        if (rs.next()) {
            System.out.println("Balance: " + rs.getInt("balance"));
        }

        conn2.commit();

        thread.join();
    }

    /**
     * 测试事务隔离级别——不可重复读
     *
     * @throws SQLException
     * @throws InterruptedException
     */
    public void NonRepeatableReadTest(Connection conn1, Connection conn2) throws SQLException, InterruptedException {

        conn1.setAutoCommit(false);
        conn2.setAutoCommit(false);

        System.out.println("Transaction isolation level of conn1: " + getTransactionIsolation(conn1));
        System.out.println("Transaction isolation level of conn2: " + getTransactionIsolation(conn2));

        // CREATE TABLE account (id INT PRIMARY KEY, balance DECIMAL(10, 2));
        // INSERT INTO account (id, balance) VALUES (1, 1.00);
        Statement stmt1 = conn1.createStatement();
        Statement stmt2 = conn2.createStatement();

        ResultSet rs1 = stmt2.executeQuery("SELECT balance FROM account WHERE id = 1");
        if (rs1.next()) {
            System.out.println("Initial Balance: " + rs1.getInt("balance"));
        }

        Thread thread = new Thread(() -> {
            try {
                System.out.println("Start update...");
                stmt1.executeUpdate("UPDATE account SET balance = balance + 100 WHERE id = 1");
                conn1.commit();
                System.out.println("Update done.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        thread.start();

        Thread.sleep(2000);

        ResultSet rs2 = stmt2.executeQuery("SELECT balance FROM account WHERE id = 1");
        if (rs2.next()) {
            System.out.println("Updated Balance: " + rs2.getInt("balance"));
        }

        conn2.commit();

        thread.join();
    }

    /**
     * 测试事务隔离级别——幻读
     *
     * @throws SQLException
     * @throws InterruptedException
     */
    public void PhantomReadTest (Connection conn1, Connection conn2) throws SQLException, InterruptedException {

        conn1.setAutoCommit(false);
        conn2.setAutoCommit(false);

        System.out.println("Transaction isolation level of conn1: " + getTransactionIsolation(conn1));
        System.out.println("Transaction isolation level of conn2: " + getTransactionIsolation(conn2));

        // CREATE TABLE account (id INT PRIMARY KEY, balance DECIMAL(10, 2));
        // INSERT INTO account (id, balance) VALUES (1, 1.00);
        Statement stmt1 = conn1.createStatement();
        Statement stmt2 = conn2.createStatement();

        ResultSet rs1 = stmt2.executeQuery("SELECT COUNT(*) AS count FROM account");
        if (rs1.next()) {
            System.out.println("Initial Count: " + rs1.getInt("count"));
        }

        Thread thread = new Thread(() -> {
            try {
                System.out.println("Start insert...");
                stmt1.executeUpdate("INSERT INTO account (id, balance) VALUES (2, 2.00)");
                System.out.println("Insert done.");
                conn1.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        thread.start();

        Thread.sleep(2000);

        ResultSet rs2 = stmt2.executeQuery("SELECT COUNT(*) AS count FROM account");
        if (rs2.next()) {
            System.out.println("Updated Count: " + rs2.getInt("count"));
        }

        conn2.commit();

        thread.join();
    }


    /**
     * 获取连接对应的事务隔离级别
     * @param conn
     * @return String
     * @throws SQLException
     */
    public static String getTransactionIsolation(Connection conn) throws SQLException {
        int isolationLevel = conn.getTransactionIsolation();

        switch (isolationLevel) {
            case Connection.TRANSACTION_NONE:
                return "TRANSACTION_NONE";
            case Connection.TRANSACTION_READ_UNCOMMITTED:
                return "TRANSACTION_READ_UNCOMMITTED";
            case Connection.TRANSACTION_READ_COMMITTED:
                return "TRANSACTION_READ_COMMITTED";
            case Connection.TRANSACTION_REPEATABLE_READ:
                return "TRANSACTION_REPEATABLE_READ";
            case Connection.TRANSACTION_SERIALIZABLE:
                return "TRANSACTION_SERIALIZABLE";
            default:
                return "UNKNOWN";
        }
    }

}
