package com.yzj.airouter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.yzj.airouter.mapper") //扫描 Mapper 文件
@EnableAsync
public class AiRouterApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiRouterApplication.class, args);
    }

}
