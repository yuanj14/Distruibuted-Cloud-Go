package com.itheima.mp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @mapper *
// 该包及其子包下的所有接口都会被自动识别为 Mapper 接口
@MapperScan("com.itheima.mp.mapper")
@SpringBootApplication
public class MpDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MpDemoApplication.class, args);
    }
}

