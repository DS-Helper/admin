package com.project.ds_helper.common.enums;

import lombok.Getter;

@Getter
public enum JwtTokenType {
    ACCESS_TOKEN_NAME("accessToken"),
    REFRESH_TOKEN_NAME("refreshToken");

    private final String tokenName;

    JwtTokenType(String tokenName){
        this.tokenName = tokenName;
    }


    JwtTokenType findJwtTokenTypeWithCookieName(String cookieName){
        for(JwtTokenType jwtTokenType : values()){
           if(jwtTokenType.getTokenName().equals("cookieName")){
               return jwtTokenType;
           }
        }
        throw new IllegalArgumentException("Cookie Name Is Invalid");
    }
}
