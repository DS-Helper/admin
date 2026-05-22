package com.project.ds_helper.domain.user.service;

import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.util.CookieUtil;
import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.domain.user.dto.request.MobileGoogleLoginRequestDto;
import com.project.ds_helper.domain.user.dto.response.GoogleTokenResponse;
import com.project.ds_helper.domain.user.dto.response.GoogleUserInfoResponse;
import com.project.ds_helper.domain.user.entity.GoogleOauth;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.UserRole;
import com.project.ds_helper.domain.user.enums.UserType;
import com.project.ds_helper.domain.user.repository.GoogleOauthRepository;
import com.project.ds_helper.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

@Service
@Slf4j
public class MobileGoogleOauthService {

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    private final GoogleOauthRepository googleOauthRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserLoginHistoryService userLoginHistoryService;

    public MobileGoogleOauthService(GoogleOauthRepository googleOauthRepository, UserRepository userRepository,
                                    @Qualifier("customRestTemplate") RestTemplate restTemplate,
                                    JwtUtil jwtUtil, CookieUtil cookieUtil, @Qualifier("CustomStringRedisTemplate") StringRedisTemplate stringRedisTemplate,
                                    UserLoginHistoryService userLoginHistoryService) {
        this.googleOauthRepository = googleOauthRepository;
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
        this.jwtUtil = jwtUtil;
        this.cookieUtil = cookieUtil;
        this.stringRedisTemplate = stringRedisTemplate;
        this.userLoginHistoryService = userLoginHistoryService;
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
    public JwtResponse mobileGoogleLogin(MobileGoogleLoginRequestDto dto, HttpServletResponse httpServletResponse) throws IOException {

        log.debug("GoogleTokenResponse.accessToken : {}", dto.accessToken());

        GoogleUserInfoResponse userInfo = getUserInfo(dto.accessToken());
        if(userInfo == null){
            log.debug("GoogleUserInfo is null");
            throw new IOException("GoogleUserInfo is null");
        }
//
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
            // provider와 무관하게 User 테이블 기준 이메일 중복 가입을 차단한다.
            if (email != null && userRepository.existsByEmail(email)) {
                log.debug("mobile google signup rejected. duplicated email found in user table. email={}", email);
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
                    .refreshToken(dto.refreshToken())
                    .build();
            googleOauthRepository.save(newGoogleOauth);
            log.debug("GoogleOauth Successfully Saved");

            // jwt token 발급
            String userId = user.getId();
            String userRole = user.getRole().name();
            String userType = user.getType().name();
            log.debug("userId : {}. userRole : {}, userType : {}", userId, userRole, userType);

            String accessToken = jwtUtil.generateAccessToken(userId, userRole, userType);
            String refreshToken = jwtUtil.generateRefreshToken(userId, userRole, userType);
            userLoginHistoryService.recordSuccessfulLogin(user);
            saveRefreshTokenWithTtl(userId, refreshToken);
            return new JwtResponse(accessToken, refreshToken);
        }else{

            User user = googleOauth.getUser();
            googleOauth.updateRefreshToken(dto.refreshToken());
            //if (user.isDeleted()) {
                //throw new IllegalArgumentException("Deleted User");
            //}
            log.debug("User is successfully selected : {}", user.getId());

            // jwt token 발급
            String userId = user.getId();
            String userRole = user.getRole().name();
            String userType = user.getType().name();
            log.debug("userId : {}. userRole : {}, userType : {}", userId, userRole, userType);

            String accessToken = jwtUtil.generateAccessToken(userId, userRole, userType);
            String refreshToken = jwtUtil.generateRefreshToken(userId, userRole, userType);
            userLoginHistoryService.recordSuccessfulLogin(user);
            saveRefreshTokenWithTtl(userId, refreshToken);
            return new JwtResponse(accessToken, refreshToken);
        }
    }

    public GoogleUserInfoResponse getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<?> request = new HttpEntity<>(headers);

        ResponseEntity<GoogleUserInfoResponse> response =
                restTemplate.exchange(
                        "https://www.googleapis.com/oauth2/v2/userinfo",
                        HttpMethod.GET,
                        request,
                        GoogleUserInfoResponse.class
                );
        log.debug("Google User Info Successfully Get : {}", response.getBody());

        return response.getBody();
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
