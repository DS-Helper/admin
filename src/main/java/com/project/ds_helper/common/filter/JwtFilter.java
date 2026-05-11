package com.project.ds_helper.common.filter;

import com.project.ds_helper.common.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;


@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    /** 필드 **/
    private final JwtUtil jwtUtil;

    /** 생성자 **/
    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /** 필더 검증 **/
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 공개 경로 여부 확인(추후 header만 검사하는 방식으로 수정)
//        if(UrlFilter.checkIfPublicPath(request.getRequestURI()) || UrlFilter.checkIfPublicPathForPosts(request)){
//            log.info("Is Public Endpoint");
//            filterChain.doFilter(request, response);
//            return ;
//        }

        // accessToken 검증
        String authToken = jwtUtil.getAccessTokenFromRequestHeader(request);

        if(authToken == null){
            log.debug("[JwtFilter] bearerToken is Null" ); // or bearerToken Not Start With Bearer
            filterChain.doFilter(request, response);
            return;
        }
        if(!jwtUtil.isAccessToken(authToken)){
            log.debug("[JwtFilter] token type is not accessToken");
            filterChain.doFilter(request, response);
            return;
        }
        if(jwtUtil.isExpired(authToken)){
            log.debug("[JwtFilter] JwtToken Is Expired");
            filterChain.doFilter(request, response);
            return;
        }

        /** 인증 객체 저장 **/
        try{
            String email = jwtUtil.getId(authToken);
            log.debug("[JwtFilter] email : {}", email);
            // 추후 권한 추가
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(email, authToken, Collections.singletonList(new SimpleGrantedAuthority(jwtUtil.getRole(authToken))));
            log.debug("usernamePasswordAuthenticationToken built successfully");

            // IP 등 추가적인 세부 정보를 저장
            usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            log.debug("usernamePasswordAuthenticationToken set Details successfully");

            // 컨텍스트에 인증정보 저장, 이후 필터들에서는 이미 인증된 객체로 인식
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            }
            log.debug("securityHolder set authentication successfully");

        }catch (JwtException jwtException){
            log.debug("JwtException Caused");
            SecurityContextHolder.clearContext();
        }

        /** 다음 필터 호출 **/
        log.debug("[JwtFilter] Next Filter");
        filterChain.doFilter(request, response);
    }
}

