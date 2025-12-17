package com.easymeeting;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Arrays;

@SpringBootApplication(scanBasePackages = ("com.easymeeting"))
@MapperScan(basePackages = {"com.easymeeting.mappers"})
@EnableScheduling
@EnableAsync
@EnableTransactionManagement
public class EasyMeetingApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyMeetingApplication.class, args);
    }
}

