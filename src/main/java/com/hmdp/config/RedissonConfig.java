package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Program : hm-dianping
 * Author : llj
 * Create : 2024-05-15 21:50
 **/

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.75.3:6379").setPassword("123456");
        return Redisson.create(config);
    }

    @Bean
    public RedissonClient redissonClient2(){
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.75.4:6379").setPassword("123456");
        return Redisson.create(config);
    }

    @Bean
    public RedissonClient redissonClient3(){
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.75.5:6379").setPassword("123456");
        return Redisson.create(config);
    }



}
