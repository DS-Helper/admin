package com.project.ds_helper.common.util;

import com.project.ds_helper.common.enums.JwtTokenType;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CookieUtil {

    public ResponseCookie addAccessTokenCookie(String accessToken) {
        ResponseCookie cookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)                    // JS에서 접근 불가
                .secure(true)                      // HTTPS에서만 전송
                .path("/")                         // 모든 경로에 적용
                .maxAge(3600 * 24)                 // 1일
                .sameSite("None")                  // ✅ 크로스도메인 허용
                .domain(".dshelper.kr")         // 서브도메인 공유 가능
                .build();

        return cookie;
    }

    public ResponseCookie addRefreshTokenCookie(String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)                    // JS에서 접근 불가
                .secure(true)                      // HTTPS에서만 전송
                .path("/")                         // 모든 경로에 적용
                .maxAge(3600 * 24)                 // 1일
                .sameSite("None")                  // ✅ 크로스도메인 허용
                .domain(".dshelper.kr")         // 서브도메인 공유 가능
                .build();

        return cookie;
    }


//    public Cookie generateAccessTokenCookie(String accessToken){
//        Cookie cookie = new Cookie(JwtTokenType.ACCESS_TOKEN_NAME.getTokenName(), accessToken);
//        cookie.setHttpOnly(true);
//        cookie.setSecure(true);
////        cookie.setSecure(false);
//        cookie.setPath("/");
//        cookie.setMaxAge(3600 * 24);
//        cookie.setDomain("dshelper.kro.kr");
//        return cookie;
//    }
//
//    public Cookie generateRefreshTokenCookie(String refreshToken){
//        Cookie cookie = new Cookie(JwtTokenType.REFRESH_TOKEN_NAME.getTokenName(), refreshToken);
//        cookie.setHttpOnly(true);
//        cookie.setSecure(true);
//        cookie.setMaxAge(3600 * 24);
//        //        cookie.setSecure(false);
//        cookie.setPath("/");
//        cookie.setDomain("dshelper.kro.kr");
//        cookie.setAttribute("samesite", "none");
//        return cookie;
//    }

    public String getAuthToken(HttpServletRequest httpServletRequest){
        Cookie[] cookies = httpServletRequest.getCookies();
        if(cookies != null){
            for(Cookie cookie : cookies){
                if(cookie.getName().equals(JwtTokenType.ACCESS_TOKEN_NAME.getTokenName())){
                    log.info("accessToken : {}", cookie.getValue());
                    return cookie.getValue();
                }
            }
        }
        log.info("accessToken is not in cookie");
        return null;
    }

    /**
     * @Param HttpServletRequest httpServletRequest
     * **/
    public String getRefreshTokenFromCookie(HttpServletRequest httpServletRequest){
        Cookie[] cookies = httpServletRequest.getCookies();
        if(cookies != null){
            for(Cookie cookie : cookies){
                if(cookie.getName().equals(JwtTokenType.REFRESH_TOKEN_NAME.getTokenName())){
                    log.info("refreshToken : {}", cookie.getValue());
                    return cookie.getValue();
                }
            }
        }
        log.info("refreshToken is not in cookie");
        return null;
    }

    /**
     * @Param String cookieName
     * @Return Cookie (MaxAge = 0)
     * **/
    public ResponseCookie expireCookieByCookieName(String cookieName){
         return ResponseCookie.from(cookieName, "")
                .domain(".dshelper.kr")
                .path("/")
                .build();
    }


    public ResponseCookie addAccessTokenCookieForLocalhost(String accessToken) {
        ResponseCookie cookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(false)                    // JS에서 접근 불가
                .secure(false)                      // HTTPS에서만 전송
                .path("/")                         // 모든 경로에 적용
                .maxAge(3600 * 24)                 // 1일
                .sameSite("None")                  // ✅ 크로스도메인 허용
                .domain("localhost")         // 서브도메인 공유 가능
                .build();

        return cookie;
    }

    public ResponseCookie addRefreshTokenCookieForLocalhost(String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(false)                    // JS에서 접근 불가
                .secure(false)                      // HTTPS에서만 전송
                .path("/")                         // 모든 경로에 적용
                .maxAge(3600 * 24)                 // 1일
                .sameSite("None")                  // ✅ 크로스도메인 허용
                .domain("localhost")         // 서브도메인 공유 가능
                .build();

        return cookie;
    }


}
