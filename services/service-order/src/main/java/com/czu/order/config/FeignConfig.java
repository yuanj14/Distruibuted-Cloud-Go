package com.czu.order.config;

import feign.Logger;
import feign.Retryer;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {
//  RPC日志监听
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
//  重试机制 next = last * 1.5^n
    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(100, 1000, 3);
    }
//  拦截器
    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            template.header("X-Request-Source", "service-order");
            template.header("X-Request-Time", String.valueOf(System.currentTimeMillis()));
        };
    }
}
