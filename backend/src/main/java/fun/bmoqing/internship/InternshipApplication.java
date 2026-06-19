/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship; // ⚠️必须是这个

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("fun.bmoqing.internship.mapper")
@EnableScheduling
public class InternshipApplication {
    public static void main(String[] args) {
        SpringApplication.run(InternshipApplication.class, args);
    }
}