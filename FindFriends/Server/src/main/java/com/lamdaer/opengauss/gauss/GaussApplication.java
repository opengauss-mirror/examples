package com.lamdaer.opengauss.gauss;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author lamdaer
 * @createTime 2020/10/23
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.lamdaer")
@MapperScan(basePackages = "com.lamdaer.opengauss.gauss.mapper")
public class GaussApplication {
    public static void main(String[] args) {
        SpringApplication.run(GaussApplication.class, args);
        
    }
}
