package com.sky.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus配置类
 * 主要用于配置分页插件等拦截器
 */
@Configuration
@Slf4j
public class MybatisPlusConfiguration {

    /**
     * 注册MyBatis-Plus分页拦截器
     * @return MybatisPlusInterceptor
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        log.info("注册MyBatis-Plus分页拦截器");
        
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 创建分页插件
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        
        // 设置最大单页限制数量,默认500条,-1不受限制
        paginationInnerInterceptor.setMaxLimit(500L);
        
        // 溢出总页数后是否进行处理(默认不处理)
        paginationInnerInterceptor.setOverflow(false);
        
        // 添加分页插件
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        
        return interceptor;
    }
}
