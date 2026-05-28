package com.campus.recruitment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.campus.recruitment.mapper")
@EnableScheduling
public class CampusRecruitmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusRecruitmentApplication.class, args);
    }
}
