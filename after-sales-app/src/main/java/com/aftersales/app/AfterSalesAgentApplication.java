package com.aftersales.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 电商售后智能 Agent 平台启动类。
 */
@SpringBootApplication(scanBasePackages = "com.aftersales")
@MapperScan("com.aftersales.infra.mapper")
public class AfterSalesAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AfterSalesAgentApplication.class, args);
    }
}
