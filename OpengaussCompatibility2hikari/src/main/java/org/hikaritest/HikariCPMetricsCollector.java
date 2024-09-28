package org.hikaritest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.zaxxer.hikari.HikariPoolMXBean;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;

public class HikariCPMetricsCollector {
    private static final List<Double> idleConnections = new ArrayList<>();
    private static final List<Double> activeConnections = new ArrayList<>();
    private static final List<Double> totalConnections = new ArrayList<>();
    private static final List<Double> threadsAwaitingConnection = new ArrayList<>();
    private static final List<Double> timestamps = new ArrayList<>();
    private static long startTime;
    private static ScheduledExecutorService scheduler;

    public static void main(String[] args) throws SQLException, IOException {
        loadExecute();
        plotMetrics();
        HikariUtil.closeDataSource();
        shutdownScheduler();
    }

    /**
     * 返回连接获取等待时间列表
     * @return List<Double> threadsAwaitingConnection
     */
    public static List<Double> getAwaitingConnection(){
        return threadsAwaitingConnection;
    }

    /**
     * 返回活跃连接数量列表
     * @return List<Double> activeConnections
     */
    public static List<Double> getActiveConnections(){
        return activeConnections;
    }

    /**
     * 调用模拟负载生成器，执行负载
     * 启动/关闭指标数据收集器
     */
    public static void loadExecute(){
        startCollectingMetrics();
        LoadGenerator.loadGenerator();
        stopCollectingMetrics();
    }

    /**
     * 每秒收集一次指标数据
     */
    public static void startCollectingMetrics() {
        startTime = System.currentTimeMillis();
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                collectMetrics();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * 停止指标数据收集
     */
    public static void stopCollectingMetrics() {
        // Ensure metrics collection is stopped
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    /**
     * 关闭调度器
     */
    private static void shutdownScheduler() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                // 等待现有任务终止
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                    // 再等待一会儿，让任务对取消操作做出响应
                    if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                        System.err.println("ScheduledExecutorService未能终止。");
                    }
                }
            } catch (InterruptedException ie) {
                // 如果当前线程也被中断，重新取消
                scheduler.shutdownNow();
                // 保留中断状态
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 将指标数据存储到相应列表
     * @throws SQLException
     */
    public static void collectMetrics() throws SQLException {
        HikariPoolMXBean poolMXBean = HikariUtil.getHikariPoolMXBean();
        double currentTime = (System.currentTimeMillis() - startTime) / 1000.0; // 计算相对时间（秒）

        // 收集指标数据
        idleConnections.add((double) poolMXBean.getIdleConnections());
        activeConnections.add((double) poolMXBean.getActiveConnections());
        totalConnections.add((double) poolMXBean.getTotalConnections());
        threadsAwaitingConnection.add((double) poolMXBean.getThreadsAwaitingConnection());

        // 记录当前时间戳（秒）
        timestamps.add(currentTime);
    }

    /**
     * 读取指标数据列表，绘制图表
     */
    public static void plotMetrics() {
        XYChart chart = new XYChartBuilder().width(800).height(600).title("HikariCP Metrics").xAxisTitle("Time (s)").yAxisTitle("Value").build();
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);

        Color[] colors = new Color[] {
                new Color(0, 0, 255),     // Blue
                new Color(255, 0, 0),     // Red
                new Color(0, 255, 0),     // Green
                new Color(255, 0, 255),   // Magenta
                new Color(0, 255, 255),   // Cyan
        };
        chart.getStyler().setSeriesColors(colors);

        // 添加数据列到图表
        addSeriesToChart(chart, "Idle Connections", idleConnections);
        addSeriesToChart(chart, "Active Connections", activeConnections);
        addSeriesToChart(chart, "Total Connections", totalConnections);
        addSeriesToChart(chart, "Thread Awaiting Connection", threadsAwaitingConnection);

        // 展示图表
        new SwingWrapper<>(chart).displayChart();
    }

    /**
     * 将数据列添加到图表中
     * @param chart 图表对象
     * @param seriesName 数据列的名称
     * @param data 数据列表
     */
    private static void addSeriesToChart(XYChart chart, String seriesName, List<Double> data) {
        if (data.isEmpty()) {
            System.out.println("Series " + seriesName + " is empty!");
            return;
        }

        double[] xData = new double[data.size()];
        double[] yData = new double[data.size()];
        for (int i = 0; i < data.size(); i++) {
            xData[i] = timestamps.get(i); // 使用时间戳作为 x 轴数据，单位为秒
            yData[i] = data.get(i);
        }
        XYSeries series = chart.addSeries(seriesName, xData, yData);
        series.setMarker(SeriesMarkers.NONE);
    }
}
