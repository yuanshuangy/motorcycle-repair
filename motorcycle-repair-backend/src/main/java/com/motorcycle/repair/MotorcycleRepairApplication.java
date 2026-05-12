package com.motorcycle.repair;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.motorcycle.repair.mapper")
public class MotorcycleRepairApplication {
    public static void main(String[] args) {
        SpringApplication.run(MotorcycleRepairApplication.class, args);
    }
}
