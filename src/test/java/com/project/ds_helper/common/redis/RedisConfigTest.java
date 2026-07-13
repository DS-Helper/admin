package com.project.ds_helper.common.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class RedisConfigTest {

    @Test
    @DisplayName("Redis 설정은 연결 팩토리와 템플릿을 만든다")
    void createsBeans() {
        RedisConfig config = new RedisConfig();
        ReflectionTestUtils.setField(config, "redisHost", "localhost");
        ReflectionTestUtils.setField(config, "redisPort", 6379);

        LettuceConnectionFactory factory = config.redisConnectionFactory();
        assertThat(factory).isNotNull();
        assertThat(config.stringRedisTemplate(factory)).isNotNull();
    }
}
