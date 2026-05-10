package com.anti;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.anti.mapper")
public class AntiFraudPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AntiFraudPlatformApplication.class, args);
    }
}
