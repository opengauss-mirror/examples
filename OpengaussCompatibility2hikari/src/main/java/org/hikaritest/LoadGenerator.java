package org.hikaritest;

import org.knowm.xchart.*;
import org.knowm.xchart.style.colors.XChartSeriesColors;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class LoadGenerator {
    private static final AtomicLong totalRequestCount = new AtomicLong(0); // 总请求数
    private static final List<Long> responseTimes = new CopyOnWriteArrayList<>(); // 响应时间列表
    private static final List<Long> connectionAcquisitionTimes = new CopyOnWriteArrayList<>(); // 连接获取时间列表
    private static final List<Long> sqlExecutionTimes = new CopyOnWriteArrayList<>(); // sql执行时间列表
    private static final List<Long> timestamps = new CopyOnWriteArrayList<>(); // 时间戳列表
    private static final int THREAD_COUNT = 16; // 使用CPU核心数, 16
    private static final int OPERATION_COUNT = 1000; // 每个线程执行的操作数                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         ; // 每个线程执行的操作次数
    private static final String SQL_FILE_PATH = "sql/user_defined/test_high_concurrency/dml.sql"; // SQL 文件路径
    private static final List<Long> throughputPerSecond = new CopyOnWriteArrayList<>(); // 每秒吞吐量列表

    /**
     * 负载生成器
     */
    public static void loadGenerator() {
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        List<String> sqlStatements = loadSqlStatements(SQL_FILE_PATH);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT * OPERATION_COUNT);

        long startTime = System.currentTimeMillis(); // 记录负载生成开始时间

        // 启动性能监控和调整任务 初始延迟30s，每30s执行一次
//        ScheduledExecutorService monitorExecutor = Executors.newScheduledThreadPool(1);
//        monitorExecutor.scheduleAtFixedRate(() -> {
//            try {
//                adjustConnectionPool();
//            } catch (SQLException e) {
//                throw new RuntimeException(e);
//            }
//        }, 30, 30, TimeUnit.SECONDS);

        // 启动吞吐量统计任务
        ScheduledExecutorService statsExecutor = Executors.newScheduledThreadPool(1);
        statsExecutor.scheduleAtFixedRate(() -> {
            long requests = totalRequestCount.getAndSet(0); // 获取并重置请求数
            throughputPerSecond.add(requests);

            System.out.println("Current Throughput: " + requests + " req/s ");
        }, 1, 1, TimeUnit.SECONDS);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.submit(() -> {
                for (int j = 0; j < OPERATION_COUNT; j++) {
                    try {
                        String sql = getRandomSqlStatement(sqlStatements);
                        executeSQL(sql, startTime);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    } finally {
                        latch.countDown(); // 每个SQL完成后减少计数
                    }

                    try {
                        Thread.sleep((int) (Math.random() * 100)); // 使用随机时间间隔 ms * 100
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // 恢复中断状态
                    }
                }
            });
        }

        try {
            latch.await(); // 等待所有线程完成
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executorService.shutdown(); // 关闭线程池
            statsExecutor.shutdown(); // 关闭统计任务线程池
//            monitorExecutor.shutdown(); // 关闭监控任务线程池
            generateCharts(); // 生成图表
            HikariUtil.closeDataSource(); // 关闭数据源
        }
    }

    /**
     * 从 .sql 文件加载 SQL 语句
     *
     * @param filePath
     * @return List<String>
     */
    private static List<String> loadSqlStatements(String filePath) {
        List<String> sqlStatements = new CopyOnWriteArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                sqlBuilder.append(line.trim()).append(" ");
                if (line.trim().endsWith(";")) {
                    sqlStatements.add(sqlBuilder.toString().trim());
                    sqlBuilder.setLength(0);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (sqlBuilder.length() > 0) {
            sqlStatements.add(sqlBuilder.toString().trim());
        }

        return sqlStatements;
    }

    /**
     * 随机选择一个 SQL 语句
     *
     * @param sqlStatements
     * @return
     */
    private static String getRandomSqlStatement(List<String> sqlStatements) {
        if (sqlStatements.isEmpty()) {
            throw new IllegalStateException("没有可用的 SQL 语句");
        }
        int index = ThreadLocalRandom.current().nextInt(sqlStatements.size());
        return sqlStatements.get(index);
    }

    /**
     * SQL 语句执行逻辑
     *
     * @param sql, startTime
     * @throws SQLException
     */
    private static void executeSQL(String sql, long startTime) throws SQLException {
        long operationStartTime = System.currentTimeMillis() - startTime; // 记录相对时间
        long sqlStartTime = System.nanoTime();
        try (Connection connection = HikariUtil.getConnection()) {
            long connectionAcquiredTime = System.nanoTime();
            connection.createStatement().execute(sql);
            long sqlExecutedTime = System.nanoTime();

            // 记录时间
            responseTimes.add(TimeUnit.NANOSECONDS.toMillis(sqlExecutedTime - sqlStartTime));
            connectionAcquisitionTimes.add(TimeUnit.NANOSECONDS.toMillis(connectionAcquiredTime - sqlStartTime));
            sqlExecutionTimes.add(TimeUnit.NANOSECONDS.toMillis(sqlExecutedTime - connectionAcquiredTime));
            timestamps.add(operationStartTime);
            totalRequestCount.incrementAndGet();
//            System.out.printf("Executed SQL: %s \n Thread %s - Connection Acquire Time: %d ms, SQL Execution Time: %d ms, Response Time: %d ms%n",
//                    sql,
//                    Thread.currentThread().getName(),
//                    TimeUnit.NANOSECONDS.toMillis(connectionAcquiredTime - sqlStartTime),
//                    TimeUnit.NANOSECONDS.toMillis(sqlExecutedTime - connectionAcquiredTime),
//                    TimeUnit.NANOSECONDS.toMillis(sqlExecutedTime - sqlStartTime));
        } catch (SQLException e) {
            System.err.println("SQL execution failed: " + sql);
            e.printStackTrace();
        }
    }

    /**
     * 根据当前性能指标调整连接池大小
     */
    private static void adjustConnectionPool() throws SQLException {
        double recentAwaiting = HikariCPMetricsCollector.getAwaitingConnection().stream()
                .skip(Math.max(0, HikariCPMetricsCollector.getAwaitingConnection().size() - 5))
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double recentActiveConnections = HikariCPMetricsCollector.getActiveConnections().stream()
                .skip(Math.max(0, HikariCPMetricsCollector.getActiveConnections().size() - 5))
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        int currentMaxPoolSize = HikariUtil.getDataSource().getMaximumPoolSize();
        int currentMinIdle = HikariUtil.getDataSource().getMinimumIdle();

        System.out.println("Adjusting Connection Pool... ");
        System.out.println("Recent Average AwaitingConnect: " + (int)recentAwaiting);
        System.out.println("Recent Average activeConnect: " + (int)recentActiveConnections );

        // 定义阈值
        if (recentAwaiting > 5 || (int)recentActiveConnections > (currentMaxPoolSize * 0.8)) {
            // 增加连接池大小
            int newMaxPoolSize = Math.min(currentMaxPoolSize + 5, 100); // 最大不超过100
            int newMinIdle = Math.min(currentMinIdle + 5, 20); // 最小不超过50

            HikariUtil.getDataSource().setMaximumPoolSize(newMaxPoolSize);
            HikariUtil.getDataSource().setMinimumIdle(newMinIdle);

            System.out.println("Increased Max Pool Size to: " + newMaxPoolSize);
            System.out.println("Increased Min Idle to: " + newMinIdle);
        } else if ((int)recentActiveConnections < (currentMaxPoolSize * 0.4)) {
            // 减少连接池大小
            int newMaxPoolSize = Math.max(currentMaxPoolSize - 5, 10); // 最大不低于20
            int newMinIdle = Math.max(currentMinIdle - 5, 5); // 最小不低于5

            HikariUtil.getDataSource().setMaximumPoolSize(newMaxPoolSize);
            HikariUtil.getDataSource().setMinimumIdle(newMinIdle);

            System.out.println("Decreased Max Pool Size to: " + newMaxPoolSize);
            System.out.println("Decreased Min Idle to: " + newMinIdle);
        } else {
            System.out.println("Connection Pool size remains unchanged.");
        }

        System.out.println("----------------------------------------");
    }


    /**
     * 计算移动平均值，窗口首尾相接
     * @param data
     * @param timestamps
     * @param windowSizeInSeconds
     * @return
     */
    private static Map<String, List<Double>> calculateMovingAverage(List<Double> data, List<Long> timestamps, int windowSizeInSeconds) {
        List<Double> movingAverages = new ArrayList<>();
        List<Double> windowTimestamps = new ArrayList<>();
        int dataSize = data.size();
        int stepSize = windowSizeInSeconds * 1000; // 每个窗口的步长（毫秒）

        for (int i = 0; i < dataSize; ) {
            long windowStartTime = timestamps.get(i);
            long windowEndTime = windowStartTime + stepSize;

            double windowSum = 0;
            int count = 0;
            long windowSumTimestamps = 0;

            // 计算当前窗口的平均值
            while (i < dataSize && timestamps.get(i) < windowEndTime) {
                windowSum += data.get(i);
                windowSumTimestamps += timestamps.get(i);
                count++;
                i++;
            }

            if (count > 0) {
                movingAverages.add(windowSum / count);
                windowTimestamps.add(windowSumTimestamps / count / 1000.0); // 将时间戳从毫秒转换为秒
            } else {
                movingAverages.add(0.0); // 如果窗口为空，移动平均为0
                windowTimestamps.add(0.0); // 如果窗口为空，时间戳也设置为0
            }
        }

        Map<String, List<Double>> result = new HashMap<>();
        result.put("movingAverages", movingAverages);
        result.put("windowTimestamps", windowTimestamps);
        return result;
    }




    /**
     * 生成性能图表
     */
//    private static void generateCharts() {
//        if (responseTimes.isEmpty() || connectionAcquisitionTimes.isEmpty() || sqlExecutionTimes.isEmpty() || timestamps.isEmpty()) {
//            System.out.println("No data to display.");
//            return;
//        }
//
//        // 确保数据按时间排序
//        List<Integer> indices = new ArrayList<>();
//        for (int i = 0; i < timestamps.size(); i++) {
//            indices.add(i);
//        }
//
//        indices.sort(Comparator.comparingLong(timestamps::get));
//
//        List<Long> sortedTimestamps = indices.stream()
//                .map(timestamps::get)
//                .collect(Collectors.toList());
//
//        List<Double> sortedResponseTimes = indices.stream()
//                .map(i -> responseTimes.get(i).doubleValue())
//                .collect(Collectors.toList());
//
//        List<Double> sortedConnectionAcquisitionTimes = indices.stream()
//                .map(i -> connectionAcquisitionTimes.get(i).doubleValue())
//                .collect(Collectors.toList());
//
//        List<Double> sortedQueryExecutionTimes = indices.stream()
//                .map(i -> sqlExecutionTimes.get(i).doubleValue())
//                .collect(Collectors.toList());
//
//        // 计算移动平均
//        int windowSizeInSeconds = 10; // 窗口大小为10秒
//        Map<String, List<Double>> responseTimeResult = calculateMovingAverage(sortedResponseTimes, sortedTimestamps, windowSizeInSeconds);
//        Map<String, List<Double>> connectionAcquisitionTimeResult = calculateMovingAverage(sortedConnectionAcquisitionTimes, sortedTimestamps, windowSizeInSeconds);
//        Map<String, List<Double>> sqlExecutionTimeResult = calculateMovingAverage(sortedQueryExecutionTimes, sortedTimestamps, windowSizeInSeconds);
//
//        // 提取移动平均和时间戳
//        List<Double> responseTimeMovingAverage = responseTimeResult.get("movingAverages");
//        List<Double> responseTimeMovingAverageTimestamps = responseTimeResult.get("windowTimestamps");
//
//        List<Double> connectionAcquisitionTimeMovingAverage = connectionAcquisitionTimeResult.get("movingAverages");
//        List<Double> connectionAcquisitionTimeMovingAverageTimestamps = connectionAcquisitionTimeResult.get("windowTimestamps");
//
//        List<Double> sqlExecutionTimeMovingAverage = sqlExecutionTimeResult.get("movingAverages");
//        List<Double> sqlExecutionTimeMovingAverageTimestamps = sqlExecutionTimeResult.get("windowTimestamps");
//
//        if (responseTimeMovingAverage.isEmpty() || responseTimeMovingAverageTimestamps.isEmpty()) {
//            System.out.println("No response time data to plot.");
//            return;
//        }
//        if (connectionAcquisitionTimeMovingAverage.isEmpty() || connectionAcquisitionTimeMovingAverageTimestamps.isEmpty()) {
//            System.out.println("No connection time data to plot.");
//            return;
//        }
//        if (sqlExecutionTimeMovingAverage.isEmpty() || sqlExecutionTimeMovingAverageTimestamps.isEmpty()) {
//            System.out.println("No sqlExecution time data to plot.");
//            return;
//        }
//
//        // 生成响应时间图表
//        XYChart responseTimeChart = new XYChartBuilder()
//                .width(1000).height(800)
//                .title("Response Time Metrics (Moving Average)")
//                .xAxisTitle("Time (s)")
//                .yAxisTitle("Time (ms)")
//                .build();
//        responseTimeChart.addSeries("Response Time (Moving Average)", responseTimeMovingAverageTimestamps, responseTimeMovingAverage)
//                .setMarker(SeriesMarkers.NONE).setLineColor(XChartSeriesColors.BLUE);
//
//        // 生成连接获取时间图表
//        XYChart connectionAcquisitionTimeChart = new XYChartBuilder()
//                .width(1000).height(800)
//                .title("Connection Acquisition Time Metrics (Moving Average)")
//                .xAxisTitle("Time (s)")
//                .yAxisTitle("Time (ms)")
//                .build();
//        connectionAcquisitionTimeChart.addSeries("Connection Acquisition Time (Moving Average)", connectionAcquisitionTimeMovingAverageTimestamps, connectionAcquisitionTimeMovingAverage)
//                .setMarker(SeriesMarkers.NONE).setLineColor(XChartSeriesColors.GREEN);
//
//        // 生成查询执行时间图表
//        XYChart queryExecutionTimeChart = new XYChartBuilder()
//                .width(1000).height(800)
//                .title("Query Execution Time Metrics (Moving Average)")
//                .xAxisTitle("Time (s)")
//                .yAxisTitle("Time (ms)")
//                .build();
//        queryExecutionTimeChart.addSeries("Query Execution Time (Moving Average)", sqlExecutionTimeMovingAverageTimestamps, sqlExecutionTimeMovingAverage)
//                .setMarker(SeriesMarkers.NONE).setLineColor(XChartSeriesColors.RED);
//
//        // 显示图表
//        new SwingWrapper<>(responseTimeChart).displayChart();
//        new SwingWrapper<>(connectionAcquisitionTimeChart).displayChart();
//        new SwingWrapper<>(queryExecutionTimeChart).displayChart();
//
////    try {
////        BitmapEncoder.saveBitmap(responseTimeChart, "./ResponseTimeMetrics", BitmapEncoder.BitmapFormat.PNG);
////    } catch (IOException e) {
////        e.printStackTrace();
////    }
//    }

    /**
     * 生成合并的性能图表
     * 使用移动平均值
     */
    private static void generateCharts() {
        if (responseTimes.isEmpty() || connectionAcquisitionTimes.isEmpty() || sqlExecutionTimes.isEmpty() || timestamps.isEmpty()) {
            System.out.println("No data to display.");
            return;
        }

        // 确保数据按时间排序
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < timestamps.size(); i++) {
            indices.add(i);
        }

        indices.sort(Comparator.comparingLong(timestamps::get));

        List<Long> sortedTimestamps = indices.stream()
                .map(timestamps::get)
                .collect(Collectors.toList());

        List<Double> sortedResponseTimes = indices.stream()
                .map(i -> responseTimes.get(i).doubleValue())
                .collect(Collectors.toList());

        List<Double> sortedConnectionAcquisitionTimes = indices.stream()
                .map(i -> connectionAcquisitionTimes.get(i).doubleValue())
                .collect(Collectors.toList());

        List<Double> sortedQueryExecutionTimes = indices.stream()
                .map(i -> sqlExecutionTimes.get(i).doubleValue())
                .collect(Collectors.toList());

        // 计算移动平均
        int windowSizeInSeconds = 10; // 窗口大小为10秒
        Map<String, List<Double>> responseTimeResult = calculateMovingAverage(sortedResponseTimes, sortedTimestamps, windowSizeInSeconds);
        Map<String, List<Double>> connectionAcquisitionTimeResult = calculateMovingAverage(sortedConnectionAcquisitionTimes, sortedTimestamps, windowSizeInSeconds);
        Map<String, List<Double>> sqlExecutionTimeResult = calculateMovingAverage(sortedQueryExecutionTimes, sortedTimestamps, windowSizeInSeconds);

        // 提取移动平均和时间戳
        List<Double> responseTimeMovingAverage = responseTimeResult.get("movingAverages");
        List<Double> responseTimeMovingAverageTimestamps = responseTimeResult.get("windowTimestamps");

        List<Double> connectionAcquisitionTimeMovingAverage = connectionAcquisitionTimeResult.get("movingAverages");
        List<Double> connectionAcquisitionTimeMovingAverageTimestamps = connectionAcquisitionTimeResult.get("windowTimestamps");

        List<Double> sqlExecutionTimeMovingAverage = sqlExecutionTimeResult.get("movingAverages");
        List<Double> sqlExecutionTimeMovingAverageTimestamps = sqlExecutionTimeResult.get("windowTimestamps");

        if (responseTimeMovingAverage.isEmpty() || responseTimeMovingAverageTimestamps.isEmpty()) {
            System.out.println("No response time data to plot.");
            return;
        }
        if (connectionAcquisitionTimeMovingAverage.isEmpty() || connectionAcquisitionTimeMovingAverageTimestamps.isEmpty()) {
            System.out.println("No connection time data to plot.");
            return;
        }
        if (sqlExecutionTimeMovingAverage.isEmpty() || sqlExecutionTimeMovingAverageTimestamps.isEmpty()) {
            System.out.println("No SQL execution time data to plot.");
            return;
        }

        // 生成合并图表
        XYChart combinedChart = new XYChartBuilder()
                .width(1000).height(800)
                .title("Performance Metrics (Moving Average)")
                .xAxisTitle("Time (s)")
                .yAxisTitle("Time (ms)")
                .build();

        combinedChart.addSeries("Response Time (Moving Average)", responseTimeMovingAverageTimestamps, responseTimeMovingAverage)
                .setMarker(SeriesMarkers.NONE).setLineColor(XChartSeriesColors.BLUE);

        combinedChart.addSeries("Connection Acquisition Time (Moving Average)", connectionAcquisitionTimeMovingAverageTimestamps, connectionAcquisitionTimeMovingAverage)
                .setMarker(SeriesMarkers.NONE).setLineColor(XChartSeriesColors.GREEN);

        combinedChart.addSeries("Query Execution Time (Moving Average)", sqlExecutionTimeMovingAverageTimestamps, sqlExecutionTimeMovingAverage)
                .setMarker(SeriesMarkers.NONE).setLineColor(XChartSeriesColors.RED);

        // 显示合并图表
        new SwingWrapper<>(combinedChart).displayChart();
    }

}