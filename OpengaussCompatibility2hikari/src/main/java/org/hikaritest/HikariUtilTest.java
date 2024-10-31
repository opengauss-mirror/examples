package org.hikaritest;

import com.zaxxer.hikari.HikariPoolMXBean;

import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.*;
import java.util.concurrent.*;


public class HikariUtilTest {
    private static final Logger logger = LoggerFactory.getLogger(HikariUtilTest.class);

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
     * 测试HikariCP连接池在数据库连接中断时的处理机制
     * 测试HikariCP连接池对超时查询连接的处理机制
     */
    @Test
    public void error_test() {
        logger.info("error test ...");
        String sql = "select * from store_sales";
        try (Connection conn = HikariUtil.getConnection()){
            logger.info("get connect");
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setQueryTimeout(2);   // 2s
                pstmt.execute();
            }
        } catch (SQLException e) {
            String sqlState = e.getSQLState();
            int errorCode = e.getErrorCode();
            logger.error("SQL Exception occurred. SQLState: {}, ErrorCode: {}, SQL: {}, Thread: {}",
                    sqlState, errorCode, sql, Thread.currentThread().getName(), e);

            e.printStackTrace();
        }

        try (Connection conn = HikariUtil.getConnection()){
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                logger.info("start to execute the sql2");
//                pstmt.setQueryTimeout(2);   // 2s
                pstmt.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 并行执行多个用例
     */
    @Test
    public void executeSqlFilesInParallel() {
        String dirPath = "src/main/resources/test_cases/";
        File directory = new File(dirPath);

        if(directory.exists() && directory.isDirectory()) {
            File[] sqlFiles = directory.listFiles((dir, name) -> name.endsWith(".sql"));
            if(sqlFiles != null){
                ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                CountDownLatch latch = new CountDownLatch(sqlFiles.length);

                for(File sqlFile : sqlFiles){
                    executorService.submit(() -> {
                       try{
                            executeSqlFile(dirPath,sqlFile.getName());
                       } finally {
                           latch.countDown();
                       }
                    });
                }

                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                } finally {
                    executorService.shutdown();
                }
            }
        } else {
            logger.error("The directory {} does not exist or is not a directory.", dirPath);
        }
    }

    /**
     * 测试（一个事务块）SQL语句执行
     * 非并行
     * 部分ddl语句不支持在一个事务块运行
     */
    @Test
    public void SQLinTransactionTest(){
//        String dirPath = "src/main/resources/test_cases/";
        String dirPath = "sql/user_defined/test_high_concurrency/";
        String sqlFilePath = "ddl.sql";
        String outputFileName = dirPath + "output/" + sqlFilePath.replace(".sql", ".txt");

        try (Connection conn = HikariUtil.getConnection();
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName))) {
            // 关闭自动提交
//            conn.setAutoCommit(false);
            String sqlQueries = readSqlFile(dirPath + sqlFilePath);
            String[] queries = sqlQueries.split("。");

            for (String query : queries) {
                System.out.println(query);
                query = query.trim();
                if (!query.isEmpty()) {
                    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                        boolean hasResultSet = pstmt.execute();
                        if (hasResultSet) {
                            try (ResultSet rs = pstmt.getResultSet()) {
                                ResultSetMetaData metaData = rs.getMetaData();
                                int columnCount = metaData.getColumnCount();
                                for (int i = 1; i <= columnCount; i++) {
                                    writer.write(metaData.getColumnName(i) + "\t");
                                }
                                writer.newLine();

                                while (rs.next()) {
                                    for (int i = 1; i <= columnCount; i++) {
                                        writer.write(rs.getString(i) + "\t");
                                    }
                                    writer.newLine();
                                }
                                writer.newLine();
                            }
                        }
                    } catch (SQLException e) {
                        // 回滚事务
//                        conn.rollback();
                        e.printStackTrace();
                        throw e;
                    }
                }
            }

            // 提交事务
//            conn.commit();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 多线程测试（自定义）SQL语句执行
     * @throws IOException
     */
    @Test
    public void SQLExecuteTest() throws IOException {
        String sqlFilePath = "sql/user_defined/test_high_concurrency/ddl.sql";
        String sqlQueries = readSqlFile(sqlFilePath);
        String[] queries = sqlQueries.split("。");
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        CountDownLatch latch = new CountDownLatch(queries.length);
        for (String query : queries) {
            executorService.submit(() -> {
                try (Connection conn = HikariUtil.getConnection()) {
                    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                        pstmt.execute();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(); // 等待所有线程完成
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        executorService.shutdown();
    }

    /**
     * 多线程测试TPCH查询语句
     */
    @Test
    public void TPCHTest() {
        String sqlDirectoryPath = "src/main/resources/tpch_sql/";
        File directory = new File(sqlDirectoryPath);
        if (directory.exists() && directory.isDirectory()) {
            File[] sqlFiles = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".sql"));
            // 多线程执行sql
            if (sqlFiles != null) {
                ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                CountDownLatch latch = new CountDownLatch(sqlFiles.length);

                for (File sqlFile : sqlFiles) {
                    executorService.submit(() -> {
                        try (Connection conn = HikariUtil.getConnection()) {
                            String tmpFileName = sqlFile.getName();
                            String sqlPath = sqlDirectoryPath + tmpFileName;
                            String query = readSqlFile(sqlPath);
                            System.out.println("Executing SQL from file: " + sqlPath);
                            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                                ResultSet rs = pstmt.executeQuery();
                                String outputFileName = sqlDirectoryPath + "output/" + tmpFileName.replace(".sql", ".txt");
                                writeResultSetToFile(rs, outputFileName);
                                System.out.println("SQL execution completed and result written to: " + outputFileName);
                            }
                        } catch (IOException | SQLException e) {
                            System.err.println("Error executing SQL from file: " + sqlFile.getName());
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                try {
                    latch.await(); // 等待所有线程完成
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                executorService.shutdown();
            }
        } else {
            System.out.println("Invalid directory path or directory does not exist.");
        }
    }

    /**
     * 多线程测试TPCDS查询语句
     */
    @Test
    public void TPCDSTest() {
        String sqlDirectoryPath = "src/main/resources/tpcds_sql/";
        File directory = new File(sqlDirectoryPath);
        if (directory.exists() && directory.isDirectory()) {
            File[] sqlFiles = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".sql"));
            // 多线程执行sql
            if (sqlFiles != null) {
                ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                CountDownLatch latch = new CountDownLatch(sqlFiles.length);

                for (File sqlFile : sqlFiles) {
                    executorService.submit(() -> {
                        try (Connection conn = HikariUtil.getConnection()) {
                            String tmpFileName = sqlFile.getName();
                            String sqlPath = sqlDirectoryPath + tmpFileName;
                            String query = readSqlFile(sqlPath);
                            System.out.println("Executing SQL from file: " + sqlPath);
                            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                                ResultSet rs = pstmt.executeQuery();
                                String outputFileName = sqlDirectoryPath + "output/" + tmpFileName.replace(".sql", ".txt");
                                writeResultSetToFile(rs, outputFileName);
                                System.out.println("SQL execution completed and result written to: " + outputFileName);
                            }
                        } catch (IOException | SQLException e) {
                            System.err.println("Error executing SQL from file: " + sqlFile.getName());
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                try {
                    latch.await(); // 等待所有线程完成
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                executorService.shutdown();
            }
        } else {
            System.out.println("Invalid directory path or directory does not exist.");
        }
    }

    /**
     * 单用例执行逻辑
     * 结果输出到文件
     * @param dirPath
     * @param sqlFilePath
     */
    private void executeSqlFile(String dirPath, String sqlFilePath) {
        String outputFileName = dirPath + "output/" + sqlFilePath.replace(".sql", ".txt");

        try (Connection conn = HikariUtil.getConnection();
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName))) {
            String sqlQueries = readSqlFile(dirPath + sqlFilePath);
            String[] queries = sqlQueries.split("。");

            for (String query : queries) {
                System.out.println(query);
                query = query.trim();
                if (!query.isEmpty()) {
                    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                        boolean hasResultSet = pstmt.execute();
                        if (hasResultSet) {
                            try (ResultSet rs = pstmt.getResultSet()) {
                                ResultSetMetaData metaData = rs.getMetaData();
                                int columnCount = metaData.getColumnCount();
                                for (int i = 1; i <= columnCount; i++) {
                                    writer.write(metaData.getColumnName(i) + "\t");
                                }
                                writer.newLine();

                                while (rs.next()) {
                                    for (int i = 1; i <= columnCount; i++) {
                                        writer.write(rs.getString(i) + "\t");
                                    }
                                    writer.newLine();
                                }
                                writer.newLine();
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                        throw e;
                    }
                }
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 读取指定路径的SQL文件，并返回其内容
     * @param sqlFilePath 要读取的SQL文件路径
     * @return SQL文件内容
     * @throws IOException 读取文件时发生的I/0错误
     */
    private @NotNull String readSqlFile(String sqlFilePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(sqlFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    /**
     * 打印HikariCP连接池的状态
     * @param poolMXBean HikariPoolMXBean实例，用户获取连接池状态
     * @param stateDescription 状态描述，用于标识打印输出的状态信息
     */
    private static void printPoolStatus(@NotNull HikariPoolMXBean poolMXBean, String stateDescription) {
        System.out.println(stateDescription + " - Active Connections: " + poolMXBean.getActiveConnections());
        System.out.println(stateDescription + " - Idle Connections: " + poolMXBean.getIdleConnections());
        System.out.println(stateDescription + " - Total Connections: " + poolMXBean.getTotalConnections());
        System.out.println(stateDescription + " - Threads Awaiting Connection: " + poolMXBean.getThreadsAwaitingConnection());
    }

    /**
     *  将sql运行结果写入对应outfile
     * @param rs
     * @param outputFileName
     * @throws IOException
     * @throws SQLException
     */
    public void writeResultSetToFile(@NotNull ResultSet rs, String outputFileName) throws IOException, SQLException {
        File outputFile = new File(outputFileName);
        try (PrintWriter writer = new PrintWriter(outputFile)) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // 写入列名
            for (int i = 1; i <= columnCount; i++) {
                writer.print(metaData.getColumnName(i));
                if (i < columnCount) {
                    writer.print(",");
                }
            }
            writer.println();

            // 写入数据
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    writer.print(rs.getString(i));
                    if (i < columnCount) {
                        writer.print(",");
                    }
                }
                writer.println();
            }
        }
    }

}