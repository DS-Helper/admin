package com.project.ds_helper.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.common.enums.ErrorCode;
import com.project.ds_helper.common.enums.JwtTokenType;
import com.project.ds_helper.common.enums.SuccessCode;
import com.project.ds_helper.common.util.CookieUtil;
import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.domain.user.dto.CustomUserDetails;
import com.project.ds_helper.domain.user.dto.request.OrganizationLoginReqDto;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.repository.UserRepository;
import com.project.ds_helper.domain.user.service.UserLoginHistoryService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import java.io.IOException;

@Slf4j
public class CustomLoginFilter extends AbstractAuthenticationProcessingFilter {

    private boolean postOnly = true;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final BCryptPasswordEncoder encoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;
    private final UserLoginHistoryService userLoginHistoryService;
    private final ObjectMapper objectMapper;

    public CustomLoginFilter(
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            CookieUtil cookieUtil,
            BCryptPasswordEncoder encoder,
            @Qualifier(value = "CustomStringRedisTemplate") StringRedisTemplate redisTemplate,
            UserRepository userRepository,
            UserLoginHistoryService userLoginHistoryService,
            ObjectMapper objectMapper
    ) {
        super("/auth/login/organization");
        setAuthenticationManager(authenticationManager);
        this.setAuthenticationManager(authenticationManager);
        this.jwtUtil = jwtUtil;
        this.cookieUtil = cookieUtil;
        this.authenticationManager = authenticationManager;
        this.encoder = encoder;
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.userLoginHistoryService = userLoginHistoryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException {
        if (this.postOnly && !request.getMethod().equalsIgnoreCase(HttpMethod.POST.name())) {
            throw new AuthenticationServiceException("Unsupported Method : " + request.getMethod());
        }

        OrganizationLoginReqDto loginRequestDTO;
        try {
            loginRequestDTO = objectMapper.readValue(request.getInputStream(), OrganizationLoginReqDto.class);
            log.debug("loginRequestDTO successfully parsed");
        } catch (IOException e) {
            throw new RuntimeException("[Login Filter] Parsing Failed : ", e);
        }

        String email = loginRequestDTO.getEmail();
        String password = loginRequestDTO.getPassword();
        log.debug("email : {}, password : {}", email, password);

        UsernamePasswordAuthenticationToken token =
                UsernamePasswordAuthenticationToken.unauthenticated(email, password);
        log.debug("Login Requested email : {}", email);

        return this.authenticationManager.authenticate(token);
    }

    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult)
            throws IOException, ServletException {
        log.debug("Login Success Logic Start");

        CustomUserDetails customUserDetails = (CustomUserDetails) authResult.getPrincipal();
        String userId = customUserDetails.getId();
        String role = customUserDetails.getRole().name();

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User Not Found"));
        String type = user.getType().name();
        userLoginHistoryService.recordSuccessfulLogin(user);

        String accessToken = jwtUtil.generateAccessToken(userId, role, type);
        String refreshToken = jwtUtil.generateRefreshToken(userId, role, type);

        redisTemplate.opsForValue().set(jwtUtil.toRedisRefreshTokenKey(userId), refreshToken);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.addHeader(HttpHeaders.AUTHORIZATION, accessToken);
        response.addHeader(JwtTokenType.REFRESH_TOKEN_NAME.getTokenName(), refreshToken);
        response.setStatus(200);

        objectMapper.writeValue(
                response.getWriter(),
                new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), new JwtResponse(accessToken, refreshToken))
        );
    }

    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed)
            throws IOException, ServletException {
        log.debug("Login Fail Logic Start");

        response.setStatus(ErrorCode.UNAUTHORIZED.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getWriter(),
                ResponseVo.error(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getMessage())
        );
    }
}
