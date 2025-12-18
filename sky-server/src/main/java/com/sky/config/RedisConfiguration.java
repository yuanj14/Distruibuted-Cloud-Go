package com.sky.config;

import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfiguration {

    /**
     * 创建并配置 RedisTemplate Bean
     * 
     * 此方法创建一个 RedisTemplate 实例用于操作 Redis 数据库。
     * 
     * @param redisConnectionFactory Redis 连接工厂，由 Spring Boot 自动配置提供
     * @return 配置好的 RedisTemplate 实例
     * 
     * 注解说明：
     * - @Bean: 将此方法的返回值注册为 Spring 容器中的 Bean
     * - @ConditionalOnMissingBean: 仅当容器中不存在 RedisTemplate 类型的 Bean 时才创建
     * 
     * 配置说明：
     * - Key 使用 StringRedisSerializer：所有 key 以可读字符串存储
     * - Value 使用 GenericJackson2JsonRedisSerializer：自动适配各种类型（Integer、String、Object等）
     * - Hash 的 field 使用 StringRedisSerializer：字段名以可读字符串存储
     * - Hash 的 value 使用 GenericJackson2JsonRedisSerializer：字段值自动适配各种类型
     * 
     * 优点：在 Redis 中存储的数据可读性强，支持多种数据类型，无需手动转换
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("开始创建 Redis 模板对象...");
        RedisTemplate redisTemplate = new RedisTemplate();
        // 设置 Redis 连接工厂
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 创建序列化器
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
        
        // key 使用 String 序列化器
        redisTemplate.setKeySerializer(stringRedisSerializer);

//        // 这个配置会应用到：String、List、Set、ZSet 的 value
//        redisTemplate.setValueSerializer(jsonRedisSerializer);
//
//        // 这个配置只应用到：Hash 的 field
//        redisTemplate.setHashKeySerializer(stringRedisSerializer);
//
//        // 这个配置只应用到：Hash 的 field value
//        redisTemplate.setHashValueSerializer(jsonRedisSerializer);

        return redisTemplate;
    }

}
