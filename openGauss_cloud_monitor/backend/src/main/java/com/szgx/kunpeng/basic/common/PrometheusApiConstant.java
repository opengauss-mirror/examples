package com.szgx.kunpeng.basic.common;


/**
 * @author zhangrb
 * @date 2022/8/11 10:47
 */
public interface PrometheusApiConstant {

    /**
     * 系统指标
     * start 开始日期
     * end
     * step 值:1小时=14
     */
    //获取CPU使用率
    String API_CPU_URL = "/api/v1/query_range?query=%281+-+sum%28rate%28node_cpu_seconds_total%7Bmode%3D%22idle%22%7D%5B1m%5D%29%29+by+%28instance%29+%2F+sum%28rate%28node_cpu_seconds_total%5B1m%5D%29%29+by+%28instance%29+%29+*+100";
    //内存使用率
    String API_RAM_URL = "/api/v1/query_range?query=%281-+%28node_memory_Buffers_bytes+%2B+node_memory_Cached_bytes+%2B+node_memory_MemFree_bytes%29+%2F+node_memory_MemTotal_bytes%29+*+100";
    //磁盘使用率
    String API_DISK_URL = "/api/v1/query_range?query=%28node_filesystem_size_bytes+%7Bmountpoint+%3D%22%2F%22%7D+-+node_filesystem_free_bytes+%7Bmountpoint+%3D%22%2F%22%7D%29+%2F%0Anode_filesystem_size_bytes+%7Bmountpoint+%3D%22%2F%22%7D+*+100%0A";
    //磁盘读写IO
    String API_DISK_R_W_IO_URL = "/api/v1/query_range?query=sum+by+%28instance%29+%28rate%28node_disk_reads_completed_total%5B1m%5D%29+%2B+rate%28node_disk_writes_completed_total%5B1m%5D%29%29";
    //磁盘读IO
    String API_DISK_R_URL = "/api/v1/query_range?query=sum+by+%28instance%29+%28rate%28node_disk_reads_completed_total%5B1m%5D%29%29";
    //磁盘写IO
    String API_DISK_W_URL = "/api/v1/query_range?query=sum+by+%28instance%29+%28rate%28node_disk_writes_completed_total%5B1m%5D%29%29";
    //上行宽带
    String API_DK_UP_URL = "/api/v1/query_range?query=sum+by+%28instance%29+%28rate%28node_network_receive_bytes_total%5B1m%5D%29%29";
    //下行宽带
    String API_DK_DOWN_URL = "/api/v1/query_range?query=sum+by+%28instance%29+%28rate%28node_network_transmit_bytes_total%5B1m%5D%29%29";

    /**
     * openGauss指标
     */
    //数据库后端连接数
    String API_GS_CONNECT_URL = "/api/v1/query_range?query=pg_stat_database_numbackends";
    //数据库使用磁盘空间
    String API_GS_DISK_URL = "/api/v1/query_range?query=pg_database_size_bytes";
    //由于锁定超时而取消此数据库中的查询数
    String API_GS_LOCK_URL = "/api/v1/query_range?query=pg_stat_database_conflicts_confl_lock";
    //数据库中由于死锁而取消的查询数
    String API_GS_DEADLOCK_URL = "/api/v1/query_range?query=pg_stat_database_conflicts_confl_deadlock";

}
