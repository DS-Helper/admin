package com.project.ds_helper.common.filter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Slf4j
public class UrlFilter {

    private static final String[] SECURITY_FILTER_CHAIN_PASS_URL = {
           "/auth/**",
            "/oauth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger/**",
            "/admin/inquires/un-replied/**",
            "/api/v1/mobile/oauth/**"
//            "/admin/**" // 임시 추가
    };

    private static final String[] JWT_FILTER_CHAIN_PASS_URL = {
            "/oauth",
            "/auth",
            "/v3/api-docs",
            "/swagger",
            "/swagger-ui",
            "/auth/join/organization",
            "/auth/login/organization", // 추후 삭제 2개
            "/admin/inquires/un-replied",
            "/api/v1/mobile/oauth"

//            "/admin" // 임시 추가
    };

    private static final String[] JWT_FILTER_CHAIN_PASS_GET_URL = {
        "/posts"
    };

    private static final String[] LOGIN_URL = {"/auth/login/organization"};

    /**
     * JWT FILTER PASS 경로 확인
     * **/
    public static boolean isPublicPath(String requestUri){
        for(String url : JWT_FILTER_CHAIN_PASS_URL){
            if(url.equals(requestUri)){
                return true;
            }
        }
        return false;
    }



    public static String[] getSecurityFilterPassPath(){
        return SECURITY_FILTER_CHAIN_PASS_URL;
    }

    public static boolean checkIfPublicPath(String requestUri){
        log.info("requestUri : {}", requestUri);
        for(String path : JWT_FILTER_CHAIN_PASS_URL){
            if(requestUri.startsWith(path)){
                return true;
            }
        }
        return false;
    }

    /**
     * [POST] /posts 는 JwtFilter Pass
     * **/
    public static boolean checkIfPublicPathForPosts(HttpServletRequest httpServletRequest){
        String httpMethod = httpServletRequest.getMethod();
        String requestUri = httpServletRequest.getRequestURI();
        log.info("httpMethod : {}, requestUri : {}", httpMethod, requestUri);
        return httpMethod.equalsIgnoreCase("GET") && requestUri.startsWith("/posts");
    }


    private static final String LOG_OUT_PATH = "/logout";
    public static boolean checkIfLogoutPath(String requestUri){
        if(LOG_OUT_PATH.equals(requestUri)){log.info("Is Logout Path. RequestUri : {}", requestUri); return true;};
        log.info("RequestUri : {}", requestUri);
        return false;
    }

}
