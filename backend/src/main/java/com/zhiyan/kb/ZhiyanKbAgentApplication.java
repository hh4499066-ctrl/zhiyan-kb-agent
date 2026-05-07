package com.zhiyan.kb;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.zhiyan.kb.mapper")
@SpringBootApplication
public class ZhiyanKbAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZhiyanKbAgentApplication.class, args);
    }
}
