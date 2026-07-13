package com.project.ds_helper.common.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedisKeyTest {

    @Test
    @DisplayName("Redis 키는 userId를 붙여 생성한다")
    void createsKey() {
        assertThat(RedisKey.REFRESH_TOKEN.getRedisRefreshTokenKey("user-1")).isEqualTo("refreshToken:user-1");
    }
}
