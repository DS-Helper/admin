package com.project.ds_helper.common.redis;

import lombok.Getter;

@Getter
public enum RedisKey {

    REFRESH_TOKEN("refreshToken");

    private final String lowerCase;

    RedisKey(String lowerCase){
        this.lowerCase = lowerCase;
    }

    public String getRedisRefreshTokenKey(String userId){
        return this.lowerCase + ":" + userId;
    }
}
