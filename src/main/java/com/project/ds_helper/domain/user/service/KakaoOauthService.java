package com.project.ds_helper.domain.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.util.CookieUtil;
import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.user.dto.request.MobileKakaoLoginRequestDto;
import com.project.ds_helper.domain.user.dto.request.OauthWithdrawRequestDto;
import com.project.ds_helper.domain.user.dto.response.KakaoTokenResponse;
import com.project.ds_helper.domain.user.dto.response.KakaoUserResponse;
import com.project.ds_helper.domain.user.dto.response.WithdrawUserResponseDto;
import com.project.ds_helper.domain.user.entity.KakaoOauth;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.UserType;
import com.project.ds_helper.domain.user.repository.KakaoOauthRepository;
import com.project.ds_helper.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
public class KakaoOauthService {

    @Value("${kakao.client.id}")
    private String clientId;

    @Value("${kakao.redirect.uri}")
    private String redirectUri;

    @Value("${kakao.client.secret}")
    String clientSecret;

    private final WebClient kakaoOauthWebClient;
    private final WebClient kakaoApiWebClient;
    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final ObjectMapper objectMapper;
    private final KakaoOauthRepository kakaoOauthRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserUtil userUtil;
    private final UserWithdrawalService userWithdrawalService;
    private final UserLoginHistoryService userLoginHistoryService;

    public KakaoOauthService(
            @Qualifier("kakaoOauthWebClient") WebClient kakaoOauthWebClient,
            @Qualifier("kakaoApiWebClient") WebClient kakaoApiWebClient,
            RestTemplate restTemplate,
            JwtUtil jwtUtil,
            CookieUtil cookieUtil,
            @Qualifier("customObjectMapper") ObjectMapper objectMapper,
            KakaoOauthRepository kakaoOauthRepository,
            UserRepository userRepository,
            @Qualifier("CustomStringRedisTemplate") StringRedisTemplate stringRedisTemplate,
            UserUtil userUtil,
            UserWithdrawalService userWithdrawalService,
            UserLoginHistoryService userLoginHistoryService
    ) {
        this.kakaoOauthWebClient = kakaoOauthWebClient;
        this.kakaoApiWebClient = kakaoApiWebClient;
        this.restTemplate = restTemplate;
        this.jwtUtil = jwtUtil;
        this.cookieUtil = cookieUtil;
        this.objectMapper = objectMapper;
        this.kakaoOauthRepository = kakaoOauthRepository;
        this.userRepository = userRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.userUtil = userUtil;
        this.userWithdrawalService = userWithdrawalService;
        this.userLoginHistoryService = userLoginHistoryService;
    }

    public String getKakaoLoginUrl() {
        log.debug("login-url-responded");

        return "https://kauth.kakao.com/oauth/authorize?"
                + "client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code";
    }

    @Transactional
    public JwtResponse kakaoLogin(String code) throws IOException {
        log.debug("kakaoAuthToken : {}", code);
        log.debug("kakaoLogin started");

        KakaoTokenResponse kakaoTokenResponse = fetchToken(code);
        log.debug("kakao token fetched successfully");

        if (kakaoTokenResponse == null) {
            log.debug("kakao token response is null");
            throw new IOException("Kakao token response is null");
        }

        String kakaoAccessToken = kakaoTokenResponse.getAccessToken();
        log.debug("kakao accessToken exists: {}", kakaoAccessToken != null && !kakaoAccessToken.isBlank());

        KakaoUserResponse kakaoUserResponse = fetchUserInfo(kakaoAccessToken);
        log.debug("kakao user responded successfully");
        log.debug("kakaoUserResponse is null: {}", kakaoUserResponse == null);

        if (kakaoUserResponse == null) {
            throw new IOException("Kakao user response is null");
        }

        Long socialOauthId = kakaoUserResponse.getSocialOauthId();
        log.debug("socialOauthId extracted successfully: {}", socialOauthId);

        if (kakaoUserResponse.getKakaoAccount() == null) {
            log.debug("kakaoAccount is null");
            throw new IOException("No Kakao Account");
        }

        String email = kakaoUserResponse.getKakaoAccount().getEmail();
        if (email != null) {
            email = email.trim();
            if (email.isEmpty()) {
                email = null;
            }
        }
        String name = kakaoUserResponse.getKakaoAccount().getName();
        String gender = kakaoUserResponse.getKakaoAccount().getGender();
        String birthyear = kakaoUserResponse.getKakaoAccount().getBirthyear();

        String profileImageUrl = "";
        String phoneNumber = "";
        if (kakaoUserResponse.getKakaoAccount().getProfile() != null) {
            profileImageUrl = kakaoUserResponse.getKakaoAccount().getProfile().getProfileImageUrl();
            phoneNumber = kakaoUserResponse.getKakaoAccount().getPhoneNumber();
        }
        log.debug(
                "socialOauthId : {}, email : {}, name : {}, profileImageUrl : {}, gender : {}, birthyear : {}, phoneNumber : {}",
                socialOauthId, email, name, profileImageUrl, gender, birthyear, phoneNumber
        );

        Optional<KakaoOauth> optionalKakaoOauth =
                kakaoOauthRepository.findBySocialOauthIdAndOauthEmail(socialOauthId, email);
        log.debug("optionalKakaoOauth selected");
        log.debug("optionalKakaoOauth present: {}", optionalKakaoOauth.isPresent());

        if (optionalKakaoOauth.isPresent()) {
            log.debug("Already Joined Kakao Oauth User");

            KakaoOauth kakaoOauth = optionalKakaoOauth.get();
            kakaoOauth.updateRefreshToken(kakaoTokenResponse.getRefreshToken());
            //if (kakaoOauth.getUser().isDeleted()) {
                //throw new IllegalArgumentException("Deleted User");
            //}
            String userId = kakaoOauth.getUser().getId();
            String userRole = kakaoOauth.getUser().getRole().name();
            String userType = kakaoOauth.getUser().getType().name();
            log.debug("existing user resolved. userId : {}, userRole : {}, userType : {}", userId, userRole, userType);

            String accessToken = jwtUtil.generateAccessToken(userId, userRole, userType);
            String refreshToken = jwtUtil.generateRefreshToken(userId, userRole, userType);
            log.debug("jwt generated for existing kakao user");
            userLoginHistoryService.recordSuccessfulLogin(kakaoOauth.getUser());
            saveRefreshTokenWithTtl(userId, refreshToken);

            return new JwtResponse(accessToken, refreshToken);
        } else {
            log.debug("No linked kakao user. creating new user");
            // provider와 무관하게 User 테이블 기준 이메일 중복 가입을 차단한다.
            if (email != null && userRepository.existsByEmail(email)) {
                log.debug("kakao signup rejected. duplicated email found in user table. email={}", email);
                throw new IllegalArgumentException("Email Already Exist");
            }

            User newUser = User.builder()
                    .email(email)
                    .name(name)
                    .profileImageUrl(profileImageUrl)
                    .gender(gender)
                    .birthyear(birthyear)
                    .phoneNumber(phoneNumber)
                    .type(UserType.PERSONAL)
                    .kakaoOauthConnected(true)
                    .build();
            log.debug("newUser is built");

            User user = userRepository.save(newUser);
            log.debug("user saved");
            log.debug("new kakao user saved. userId={}", user.getId());

            KakaoOauth kakaoOauth = KakaoOauth.builder()
                    .user(user)
                    .socialOauthId(socialOauthId)
                    .oauthEmail(email)
                    .refreshToken(kakaoTokenResponse.getRefreshToken())
                    .build();
            log.debug("kakaoOauth is built");

            kakaoOauthRepository.save(kakaoOauth);
            log.debug("kakaoOauth is saved");
            log.debug("kakaoOauth mapping saved for socialOauthId={}", socialOauthId);

            String userId = user.getId();
            String userRole = user.getRole().name();
            String userType = user.getType().name();
            log.debug("new user resolved. userId : {}, userRole : {}, userType : {}", userId, userRole, userType);

            String accessToken = jwtUtil.generateAccessToken(userId, userRole, userType);
            String refreshToken = jwtUtil.generateRefreshToken(userId, userRole, userType);
            log.debug("jwt generated for new kakao user");
            userLoginHistoryService.recordSuccessfulLogin(user);
            saveRefreshTokenWithTtl(userId, refreshToken);

            return new JwtResponse(accessToken, refreshToken);
        }
    }

    public KakaoTokenResponse fetchToken(String code) {
        log.debug("fetchToken started. clientId={}, redirectUri={}, codeExists={}",
                clientId,
                redirectUri,
                code != null && !code.isBlank());
        KakaoTokenResponse kakaoTokenResponse = kakaoOauthWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/oauth/token")
                        .build()
                )
                .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                        .with("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("redirect_uri", redirectUri)
                        .with("code", code))
                .retrieve()
                .bodyToMono(KakaoTokenResponse.class)
                .block(Duration.ofMillis(5000));
        log.debug("KakaoTokenResponse : {}", kakaoTokenResponse);
        return kakaoTokenResponse;
    }

    public KakaoUserResponse fetchUserInfo(String accessToken) throws IOException {
        log.debug("fetchUserInfo started");
        String responseBody = kakaoApiWebClient.get()
                .uri("/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .exchangeToMono(response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> {
                                    log.debug("kakao user response status: {}", response.statusCode());
                                    log.debug("kakao user raw response: {}", body);
                                    return body;
                                })
                )
                .block(Duration.ofMillis(5000));

        KakaoUserResponse kakaoUserResponse = objectMapper.readValue(responseBody, KakaoUserResponse.class);
        log.debug(
                "fetchUserInfo completed. responseNull={}, hasAccount={}, socialOauthId={}",
                kakaoUserResponse == null,
                kakaoUserResponse != null && kakaoUserResponse.getKakaoAccount() != null,
                kakaoUserResponse == null ? null : kakaoUserResponse.getSocialOauthId()
        );
        return kakaoUserResponse;
    }

    @Transactional
    public WithdrawUserResponseDto withdraw(org.springframework.security.core.Authentication authentication, OauthWithdrawRequestDto dto) {
        String userId = userUtil.extractUserId(authentication);
        User user = userUtil.findUserById(userId);

        KakaoOauth kakaoOauth = kakaoOauthRepository.findByUser_Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("Kakao OAuth User Not Found"));

        if (!user.isKakaoOauthConnected()) {
            throw new IllegalArgumentException("Kakao OAuth Not Connected");
        }

        String providerAccessToken = refreshAccessToken(kakaoOauth);
        unlink(providerAccessToken);
        return userWithdrawalService.softDeleteAndDeleteRefreshToken(user);
    }

    String refreshAccessToken(KakaoOauth kakaoOauth) {
        if (kakaoOauth.getRefreshToken() == null || kakaoOauth.getRefreshToken().isBlank()) {
            throw new IllegalStateException("Kakao OAuth Refresh Token Not Found");
        }

        KakaoTokenResponse tokenResponse = kakaoOauthWebClient.post()
                .uri(uriBuilder -> uriBuilder.path("/oauth/token").build())
                .body(BodyInserters.fromFormData("grant_type", "refresh_token")
                        .with("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("refresh_token", kakaoOauth.getRefreshToken()))
                .retrieve()
                .bodyToMono(KakaoTokenResponse.class)
                .block(Duration.ofMillis(5000));

        if (tokenResponse == null || tokenResponse.getAccessToken() == null || tokenResponse.getAccessToken().isBlank()) {
            throw new IllegalStateException("Kakao Access Token Refresh Failed");
        }

        kakaoOauth.updateRefreshToken(tokenResponse.getRefreshToken());
        return tokenResponse.getAccessToken();
    }

    void unlink(String accessToken) {
        kakaoApiWebClient.post()
                .uri("/v1/user/unlink")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofMillis(5000));
    }

    public void generateJwtTokenAndPutInCookie(
            HttpServletResponse httpServletResponse,
            String userId,
            String userRole,
            String userType
    ) {
        String accessToken = jwtUtil.generateAccessToken(userId, userRole, userType);
        String refreshToken = jwtUtil.generateRefreshToken(userId, userRole, userType);
        log.debug("accessToken : {}, refreshToken : {}", accessToken, refreshToken);

        saveRefreshTokenWithTtl(userId, refreshToken);

        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.addAccessTokenCookie(accessToken).toString());
        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.addRefreshTokenCookie(refreshToken).toString());
        log.debug("jwt token is put in cookie");
    }

    @Transactional
    public JwtResponse kakaoLogin(MobileKakaoLoginRequestDto dto, HttpServletResponse httpServletResponse) throws IOException {
        log.debug("kakaoLogin with accessToken started");
        log.debug("kakao accessToken exists: {}", dto.accessToken() != null && !dto.accessToken().isBlank());

        KakaoUserResponse kakaoUserResponse = fetchUserInfo(dto.accessToken());
        log.debug("kakao user responded successfully");

        Long socialOauthId = kakaoUserResponse.getSocialOauthId();

        if (kakaoUserResponse.getKakaoAccount() == null) {
            throw new IOException("No Kakao Account");
        }

        String email = kakaoUserResponse.getKakaoAccount().getEmail();
        if (email != null) {
            email = email.trim();
            if (email.isEmpty()) {
                email = null;
            }
        }
        String name = kakaoUserResponse.getKakaoAccount().getName();
        String gender = kakaoUserResponse.getKakaoAccount().getGender();
        String birthyear = kakaoUserResponse.getKakaoAccount().getBirthyear();

        String profileImageUrl = "";
        String phoneNumber = "";
        if (kakaoUserResponse.getKakaoAccount().getProfile() != null) {
            profileImageUrl = kakaoUserResponse.getKakaoAccount().getProfile().getProfileImageUrl();
            phoneNumber = kakaoUserResponse.getKakaoAccount().getPhoneNumber();
        }
        log.debug("socialOauthId : {}, email : {}, name : {}, profileImageUrl : {}, gender : {},birthyear : {}, phoneNumber : {}",
                socialOauthId, email, name, profileImageUrl, gender, birthyear, phoneNumber);

        Optional<KakaoOauth> optionalKakaoOauth = kakaoOauthRepository.findBySocialOauthIdAndOauthEmail(socialOauthId, email);
        log.debug("optionalKakaoOauth selected");

        if (optionalKakaoOauth.isPresent()) {
            log.debug("Already Joined Kakao Oauth User");

            KakaoOauth kakaoOauth = optionalKakaoOauth.get();
            kakaoOauth.updateRefreshToken(dto.refreshToken());
            //if (kakaoOauth.getUser().isDeleted()) {
                //throw new IllegalArgumentException("Deleted User");
            //}
            String userId = kakaoOauth.getUser().getId();
            String userRole = kakaoOauth.getUser().getRole().name();
            String userType = kakaoOauth.getUser().getType().name();
            log.debug("userId : {}, userRole : {}", userId, userRole);

            String accessToken = jwtUtil.generateAccessToken(userId, userRole, userType);
            String refreshToken = jwtUtil.generateRefreshToken(userId, userRole, userType);
            saveRefreshTokenWithTtl(userId, refreshToken);
            return new JwtResponse(accessToken, refreshToken);
        }

        // provider와 무관하게 User 테이블 기준 이메일 중복 가입을 차단한다.
        if (email != null && userRepository.existsByEmail(email)) {
            log.debug("mobile kakao signup rejected. duplicated email found in user table. email={}", email);
            throw new IllegalArgumentException("Email Already Exist");
        }

        User newUser = User.builder()
                .email(email)
                .name(name)
                .profileImageUrl(profileImageUrl)
                .gender(gender)
                .birthyear(birthyear)
                .phoneNumber(phoneNumber)
                .type(UserType.PERSONAL)
                .kakaoOauthConnected(true)
                .build();
        log.debug("newUser is built");

        User user = userRepository.save(newUser);
        log.debug("user saved");

        KakaoOauth kakaoOauth = KakaoOauth.builder()
                .user(user)
                .socialOauthId(socialOauthId)
                .oauthEmail(email)
                .refreshToken(dto.refreshToken())
                .build();
        log.debug("kakaoOauth is built");

        kakaoOauthRepository.save(kakaoOauth);
        log.debug("kakaoOauth is saved");

        String userId = user.getId();
        String userRole = user.getRole().name();
        String userType = user.getType().name();
        log.debug("userId : {}. userRole : {}, userType : {}", userId, userRole, userType);

        String accessToken = jwtUtil.generateAccessToken(userId, userRole, userType);
        String refreshToken = jwtUtil.generateRefreshToken(userId, userRole, userType);
        saveRefreshTokenWithTtl(userId, refreshToken);
        return new JwtResponse(accessToken, refreshToken);
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
