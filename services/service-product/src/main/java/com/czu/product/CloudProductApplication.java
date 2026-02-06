package com.czu.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class CloudProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudProductApplication.class, args);
    }
}
