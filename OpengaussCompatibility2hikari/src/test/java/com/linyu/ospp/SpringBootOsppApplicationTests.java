package com.linyu.ospp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.hikari.initialization-fail-timeout=-1",
        "ospp.verify.enabled=false"
})
class SpringBootOsppApplicationTests {

    @Test
    void contextLoads() {
        // 仅验证 Spring 上下文可以正常加载，不依赖外部数据库。
        // HikariCP 设置 initialization-fail-timeout=-1 后，连接池不会在启动时 fail-fast。
        // 同时关闭 ospp.verify.enabled，避免启动验证 Runner 在无数据库环境下执行 SQL 校验。
    }

}