package com.bmdst.services.starter.redis.config;

import com.bmdst.services.starter.redis.client.RedisClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Kang Weibing
 * @description Redis spring boot 自动装配文件
 * @since 2021-06-22 19:44
 */

@Configuration
public class RedisAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(RedisClient.class)
    public RedisClient redisClient() {
        return new RedisClient();
    }
}