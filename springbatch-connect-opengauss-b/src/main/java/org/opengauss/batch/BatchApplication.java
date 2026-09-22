package org.opengauss.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Batch 适配工程入口。
 * Boot 3 自动装配 BatchAutoConfiguration（JobLauncher/JobRepository/JobExplorer/事务管理器），无需 @EnableBatchProcessing。
 * 启动前确认：openGauss 已开启 enable_dolphin_proto，B 库/schema/user 表已按 README 建好。
 */
@SpringBootApplication
public class BatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }
}
