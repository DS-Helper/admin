package com.project.ds_helper.common.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class RedisConfig {

        @Value("${spring.data.redis.host:172.26.15.107}")
        private String redisHost;

        @Value("${spring.data.redis.port:6379}")
        private int redisPort;

        // Redis Connection Configuration
        @Bean
        LettuceConnectionFactory redisConnectionFactory() {
            return new LettuceConnectionFactory(new RedisStandaloneConfiguration(redisHost, redisPort));
        }

        @Bean(name = "CustomStringRedisTemplate")
        StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory lettuceConnectionFactory){
            StringRedisTemplate stringRedisTemplate = new StringRedisTemplate();
            stringRedisTemplate.setConnectionFactory(lettuceConnectionFactory);

            return stringRedisTemplate;
        }
}
