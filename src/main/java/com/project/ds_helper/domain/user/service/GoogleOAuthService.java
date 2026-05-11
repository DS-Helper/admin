package com.project.ds_helper.domain.user.service;

import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.util.CookieUtil;
import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.user.dto.request.MobileGoogleLoginRequestDto;
import com.project.ds_helper.domain.user.dto.request.OauthWithdrawRequestDto;
import com.project.ds_helper.domain.user.dto.response.GoogleTokenResponse;
import com.project.ds_helper.domain.user.dto.response.GoogleUserInfoResponse;
import com.project.ds_helper.domain.user.entity.GoogleOauth;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.UserRole;
import com.project.ds_helper.domain.user.enums.UserType;
import com.project.ds_helper.domain.user.repository.GoogleOauthRepository;
import com.project.ds_helper.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Slf4j
public class GoogleOAuthService {

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.redirect.uri}")
    private String redirectUri;

    private final GoogleOauthRepository googleOauthRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserUtil userUtil;

    public GoogleOAuthService(GoogleOauthRepository googleOauthRepository, UserRepository userRepository,
                              @Qualifier("customRestTemplate") RestTemplate restTemplate,
                              JwtUtil jwtUtil, CookieUtil cookieUtil, @Qualifier("CustomStringRedisTemplate") StringRedisTemplate stringRedisTemplate,
                              UserUtil userUtil) {
        this.googleOauthRepository = googleOauthRepository;
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
        this.jwtUtil = jwtUtil;
        this.cookieUtil = cookieUtil;
        this.stringRedisTemplate = stringRedisTemplate;
        this.userUtil = userUtil;
    }

    public String getGoogleLoginUrl() {
        log.debug("google login-url requested");
        return UriComponentsBuilder
                .fromHttpUrl("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "email") // profile
//                .queryParam("access_type", "offline")
//                .queryParam("prompt", "consent")
                .build()
                .toUriString();
    }

    /**
     * 구글 로그인 API
     *
     * - 유저가 구글 로그인 창에서 아이디, 비밀번호를 입력하여 구글 로그인을 진행한 후 구글 측에서 본 EndPoint로 Redirect 해준다.
     * - 본 EndPoint에서는 code를 param으로 받아 Google 측에 인가 코드를 요청하여 획득하여 유저의 정보를 받아 회원가입 or 로그인을 진행한다.
     *
     * [Business Login]
     * 1. Google 측에서 받은 code를 사용하여 인가 코드 요청
     * 2. 인가코드를 사용하여 UserInfo 요청
     * 3-1. UserInfo를 바탕으로 기가입 된 유저면 로그인(AccessToken, RefreshToken 발급)
     * 3-2. 마기입 유저면 회원가입 진행 후 로그인(AccessToken, RefreshToken 발급)
     *
     * - 해당 과정에서 필요한 것은
     *      - GoogleTokenResponse(유저에 대한 Google 측의 API를 이용하기 위한 인가 코드를 받아오는 Dto)
     *      - GoogleUserInfoResponse(GoogleTokenResponse에서 받은 인가 코드를 사용하여 유저의 정보를 받아오는 Dto)
     *      - GoogleOauth(GoogleUserInfoResponse를 바탕으로 최종적으로 유저를 회원가입 할 때 DB에 저장하기 위한 Entity)
     *
     * 
     * [GoogleTokenResponse 실제 예시]
     * === Google Token RAW Response ===
     * {
     *   "access_token": "REMOVED",
     *   "expires_in": 3599,
     *   "scope": "https://www.googleapis.com/auth/userinfo.email openid",
     *   "token_type": "Bearer",
     *   "id_token": "REMOVED"
     * }
     *
     * [GoogleUserInfoResponse 실제 응답 예시]
     * == Google User Info Response ===
     * {
     *   "id": "102031241579529998737",
     *   "email": "dldbsgh00005@gmail.com",
     *   "verified_email": true,
     *   "picture": "https://lh3.googleusercontent.com/a-/ALV-UjWKitq5DPyNejoy0PZlfpv55J4FSM5yXVqC-439-nQjII-WgA=s96-c"
     * }
     * **/
    public JwtResponse googleOauthLogin(String code) throws IOException {
        log.debug("googleOauthLogin started");
        log.debug("google auth code exists: {}", code != null && !code.isBlank());

        GoogleTokenResponse token = getToken(code);
        if(token == null){
            log.debug("GoogleTokenResponse is null");
            throw new IOException("GoogleTokenResponse is null");
        }

        log.debug("google token fetched successfully");
        log.debug("GoogleTokenResponse.accessToken : {}", token.getAccessToken());

        GoogleUserInfoResponse userInfo = getUserInfo(token.getAccessToken());
        if(userInfo == null){
            log.debug("GoogleUserInfo is null");
            throw new IOException("GoogleUserInfo is null");
        }
        log.debug("google user info fetched successfully");
        log.debug("google user info response is null: {}", userInfo == null);
        log.debug("GoogleUserInfoResponse.email : {}", userInfo.getEmail());
        
        // GoogleUserInfoResponse를 사용하여 회원가잆 or 로그인 진행
        String socialOauthId = userInfo.getId();
        String email = userInfo.getEmail();
        if (email != null) {
            email = email.trim();
            if (email.isEmpty()) {
                email = null;
            }
        }
        log.debug("socialOauthId : {}, email : {}", socialOauthId, email);
        GoogleOauth googleOauth = googleOauthRepository.findBySocialOauthIdAndOauthEmail(socialOauthId, email).orElse(null);

        // 이메일 기반으로 기가입 유저는 바로 로그인 진행, 미가입 유저는 회원가입 후 로그인 진행
        if(googleOauth == null){
            log.debug("No linked google user. creating new user");
            // provider와 무관하게 User 테이블 기준 이메일 중복 가입을 차단한다.
            if (email != null && userRepository.existsByEmail(email)) {
                log.debug("google signup rejected. duplicated email found in user table. email={}", email);
                throw new IllegalArgumentException("Email Already Exist");
            }
    
            // 이메일, 유저타입, 유저롤, 구글SNS연결여부
            User newUser = User.builder()
                    .email(email)
                    .type(UserType.PERSONAL)
                    .role(UserRole.USER)
                    .googleOauthConnected(true)
                    .build();
            User user = userRepository.save(newUser);
            log.debug("User Successfully Saved");

            GoogleOauth newGoogleOauth = GoogleOauth.builder()
                    .socialOauthId(socialOauthId)
                    .oauthEmail(email)
                    .user(user)
                    .build();
            googleOauthRepository.save(newGoogleOauth);
            log.debug("GoogleOauth Successfully Saved");

            // jwt token 발급
            String userId = user.getId();
            String userRole = user.getRole().name();
            String userType = user.getType().name();
            log.debug("userId : {}. userRole : {}, userType : {}", userId, userRole, userType);

            // jwt 토큰 발급 및 쿠키 저장
            String accessToken = jwtUtil.generateAccessToken(userId, userRole, userType);
            String refreshToken = jwtUtil.generateRefreshToken(userId, userRole, userType);
            log.debug("jwt generated for new google user");
            saveRefreshTokenWithTtl(userId, refreshToken);

            return new JwtResponse(accessToken, refreshToken);
        }else{
            log.debug("Already Joined Google Oauth User");

            User user = googleOauth.getUser();
            //if (user.isDeleted()) {
                //throw new IllegalArgumentException("Deleted User");
            //}
            log.debug("User is successfully selected : {}", user.getId());

            // jwt token 발급
            String userId = user.getId();
            String userRole = user.getRole().name();
            String userType = user.getType().name();
            log.debug("userId : {}. userRole : {}, userType : {}", userId, userRole, userType);

            // jwt 토큰 발급 및 쿠키 저장
            String accessToken = jwtUtil.generateAccessToken(userId, userRole, userType);
            String refreshToken = jwtUtil.generateRefreshToken(userId, userRole, userType);
            log.debug("jwt generated for existing google user");
            saveRefreshTokenWithTtl(userId, refreshToken);

            return new JwtResponse(accessToken, refreshToken);
        }
    }

    public JwtResponse googleOauthLogin(MobileGoogleLoginRequestDto dto, HttpServletResponse httpServletResponse) throws IOException {
        log.debug("googleOauthLogin with accessToken started");
        log.debug("google accessToken exists: {}", dto.accessToken() != null && !dto.accessToken().isBlank());

        GoogleUserInfoResponse userInfo = getUserInfo(dto.accessToken());
        if (userInfo == null) {
            log.debug("GoogleUserInfo is null");
            throw new IOException("GoogleUserInfo is null");
        }

        log.debug("google user info fetched successfully");
        log.debug("google user info response is null: {}", userInfo == null);
        log.debug("GoogleUserInfoResponse.email : {}", userInfo.getEmail());

        String socialOauthId = userInfo.getId();
        String email = userInfo.getEmail();
        if (email != null) {
            email = email.trim();
            if (email.isEmpty()) {
                email = null;
            }
        }
        log.debug("socialOauthId : {}, email : {}", socialOauthId, email);
        GoogleOauth googleOauth = googleOauthRepository.findBySocialOauthIdAndOauthEmail(socialOauthId, email).orElse(null);
        log.debug("googleOauth exists: {}", googleOauth != null);
        log.debug("googleOauth exists: {}", googleOauth != null);

        if (googleOauth == null) {
            log.debug("No linked google user. creating new user");
            // provider와 무관하게 User 테이블 기준 이메일 중복 가입을 차단한다.
            if (email != null && userRepository.existsByEmail(email)) {
                log.debug("mobile google signup rejected. duplicated email found in user table. email={}", email);
                throw new IllegalArgumentException("Email Already Exist");
            }

            User newUser = User.builder()
                    .email(email)
                    .type(UserType.PERSONAL)
                    .role(UserRole.USER)
                    .googleOauthConnected(true)
                    .build();
            User user = userRepository.save(newUser);
            log.debug("User Successfully Saved");

            GoogleOauth newGoogleOauth = GoogleOauth.builder()
                    .socialOauthId(socialOauthId)
                    .oauthEmail(email)
                    .user(user)
                    .build();
            googleOauthRepository.save(newGoogleOauth);
            log.debug("GoogleOauth Successfully Saved");

            String userId = user.getId();
            String userRole = user.getRole().name();
            String userType = user.getType().name();
            log.debug("userId : {}. userRole : {}, userType : {}", userId, userRole, userType);

            String accessToken = jwtUtil.generateAccessToken(userId, userRole, userType);
            String refreshToken = jwtUtil.generateRefreshToken(userId, userRole, userType);
            log.debug("jwt generated for new google user");
            saveRefreshTokenWithTtl(userId, refreshToken);
            return new JwtResponse(accessToken, refreshToken);
        }

        log.debug("Already Joined Google Oauth User");
        User user = googleOauth.getUser();
        //if (user.isDeleted()) {
            //throw new IllegalArgumentException("Deleted User");
        //}
        log.debug("User is successfully selected : {}", user.getId());

        String userId = user.getId();
        String userRole = user.getRole().name();
        String userType = user.getType().name();
        log.debug("userId : {}. userRole : {}, userType : {}", userId, userRole, userType);

        String accessToken = jwtUtil.generateAccessToken(userId, userRole, userType);
        String refreshToken = jwtUtil.generateRefreshToken(userId, userRole, userType);
        log.debug("jwt generated for existing google user");
        saveRefreshTokenWithTtl(userId, refreshToken);
        return new JwtResponse(accessToken, refreshToken);
    }

    public GoogleTokenResponse getToken(String code) {
        log.debug("Request GoogleTokenResponse");
        log.debug("google getToken started. codeExists={}", code != null && !code.isBlank());
        log.debug("google clientId suffix: {}", maskSuffix(clientId));
        log.debug("google redirectUri: {}", redirectUri);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<?> request = new HttpEntity<>(params, headers);

//        // String으로 Raw Response 확인
//        ResponseEntity<String> rawResponse = restTemplate.postForEntity(
//                "https://oauth2.googleapis.com/token",
//                request,
//                String.class
//        );
//
//        // Google이 준 RAW 응답 출력
//        System.out.println("=== Google Token RAW Response ===");
//        System.out.println(rawResponse.getBody());

        ResponseEntity<GoogleTokenResponse> response;
        try {
            response = restTemplate.postForEntity(
                    "https://oauth2.googleapis.com/token",
                    request,
                    GoogleTokenResponse.class
            );
        } catch (HttpStatusCodeException e) {
            log.debug("google token request failed. status: {}", e.getStatusCode());
            log.debug("google token error body: {}", e.getResponseBodyAsString());
            throw e;
        }

        log.debug("Successfully Got GoogleTokenResponse");
        log.debug("google token response status: {}", response.getStatusCode());
        log.debug("GoogleTokenResponse.accessToken : {}", Objects.requireNonNull(response.getBody()).getAccessToken());

        return response.getBody();
    }

    private String maskSuffix(String value) {
        if (value == null || value.isBlank()) {
            return "null";
        }
        int visibleLength = Math.min(8, value.length());
        return value.substring(value.length() - visibleLength);
    }

    public GoogleUserInfoResponse getUserInfo(String accessToken) {
        log.debug("google getUserInfo started");
        log.debug("google accessToken exists: {}", accessToken != null && !accessToken.isBlank());
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<?> request = new HttpEntity<>(headers);

//        ResponseEntity<String> rawResponse =
//                restTemplate.exchange(
//                        "https://www.googleapis.com/oauth2/v2/userinfo",
//                        HttpMethod.GET,
//                        request,
//                        String.class
//                );
//
//        System.out.println("==============Google User Info Response================");
//        System.out.println(rawResponse);

        ResponseEntity<GoogleUserInfoResponse> response =
                restTemplate.exchange(
                        "https://www.googleapis.com/oauth2/v2/userinfo",
                        HttpMethod.GET,
                        request,
                        GoogleUserInfoResponse.class
                );
        log.debug("google user info response status: {}", response.getStatusCode());
        log.debug("google user info body exists: {}", response.getBody() != null);
        return response.getBody();
    }

    @Transactional
    public void withdraw(org.springframework.security.core.Authentication authentication, OauthWithdrawRequestDto dto) {
        String userId = userUtil.extractUserId(authentication);
        User user = userUtil.findUserById(userId);

        GoogleOauth googleOauth = googleOauthRepository.findByUser_Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("Google OAuth User Not Found"));

        if (!user.isGoogleOauthConnected()) {
            throw new IllegalArgumentException("Google OAuth Not Connected");
        }

        revokeAccessToken(dto.accessToken());
        softDeleteUser(user);
        stringRedisTemplate.delete(jwtUtil.toRedisRefreshTokenKey(userId));
    }

    void revokeAccessToken(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("token", accessToken);

        restTemplate.postForEntity(
                "https://oauth2.googleapis.com/revoke",
                new HttpEntity<>(params, headers),
                String.class
        );
    }

    private void softDeleteUser(User user) {
        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
    }


    /**
     * jwt token 발급 후 쿠키에 저장하는 메소드
     * **/
    public void generateJwtTokenAndPutInCookie(HttpServletResponse httpServletResponse, String userId, String userRole, String userType){
        // jwt 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(userId, userRole, userType);
        String refreshToken = jwtUtil.generateRefreshToken(userId, userRole, userType);
        log.debug("accessToken : {}, refreshToken : {}", accessToken, refreshToken);

        saveRefreshTokenWithTtl(userId, refreshToken);

        // 쿠키에 토큰 추가
        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.addAccessTokenCookie(accessToken).toString());
        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.addRefreshTokenCookie(refreshToken).toString());
        log.debug("jwt token is put in cookie");
    }

    private void saveRefreshTokenWithTtl(String userId, String refreshToken) {
        stringRedisTemplate.opsForValue().set(
                jwtUtil.toRedisRefreshTokenKey(userId),
                refreshToken,
                Duration.ofMillis(jwtUtil.getRefreshTokenExpirationTime())
        );
        log.debug("RefreshToken Saved. UserId : {}, key : {}", userId, jwtUtil.toRedisRefreshTokenKey(userId));
    }

}

