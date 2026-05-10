package com.aftersales.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 电商售后智能 Agent 平台启动类。
 *
 * 扫描所有模块：common、infra、domain、auth、biz、agent、rag、api
 */
@SpringBootApplication(scanBasePackages = "com.aftersales")
public class AfterSalesAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AfterSalesAgentApplication.class, args);
    }
}
