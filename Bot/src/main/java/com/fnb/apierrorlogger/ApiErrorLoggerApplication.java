package com.fnb.apierrorlogger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ApiErrorLoggerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiErrorLoggerApplication.class, args);
    }
}
