package com.project.ds_helper.domain.user.service;


import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.util.CookieUtil;
import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.user.dto.request.MobileNaverLoginRequestDto;
import com.project.ds_helper.domain.user.dto.request.OauthWithdrawRequestDto;
import com.project.ds_helper.domain.user.entity.NaverOauth;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.UserType;
import com.project.ds_helper.domain.user.repository.NaverOauthRepository;
import com.project.ds_helper.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class NaverOauthService {

    @Value("${naver.client.id}")
    private String clientId;

    @Value("${naver.client.secret}")
    private String clientSecret;

    @Value("${naver.redirect.uri}")
    private String redirectUri;

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final CookieUtil cookieUtil;
    private final RestTemplate restTemplate;
    private final UserUtil userUtil;
    private final UserRepository userRepository;
    private final NaverOauthRepository naverOauthRepository;


    public NaverOauthService(JwtUtil jwtUtil, StringRedisTemplate stringRedisTemplate, CookieUtil cookieUtil, @Qualifier(value = "customRestTemplate") RestTemplate restTemplate, UserUtil userUtil, UserRepository userRepository, NaverOauthRepository naverOauthRepository) {
        this.jwtUtil = jwtUtil;
        this.stringRedisTemplate = stringRedisTemplate;
        this.cookieUtil = cookieUtil;
        this.restTemplate = restTemplate;
        this.userUtil = userUtil;
        this.userRepository = userRepository;
        this.naverOauthRepository = naverOauthRepository;
    }


    public String naverLoginUrl() {
        log.debug("naver login-url requested");
        String state = UUID.randomUUID().toString();
        return String.format(
                "https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id=%s&redirect_uri=%s&state=%s",
                clientId, URLEncoder.encode(redirectUri, java.nio.charset.StandardCharsets.UTF_8), state
        );
    }

    public JwtResponse naverLogin(String code, String state) {
        log.debug("naverLogin started");
        log.debug("naver auth code exists: {}, state exists: {}", code != null && !code.isBlank(), state != null && !state.isBlank());
        String _accessToken = getNaverOauthToken(code, state, restTemplate);
        log.debug("accessToken : {}", _accessToken);
        log.debug("naver token fetched successfully");

        Map<String, Object> naverUserInfo = fetchUserInfoFromNaver(_accessToken);
        log.debug("naver user info fetched successfully");
        log.debug("naver user info map is null: {}", naverUserInfo == null);

        String socialOauthId = (String) naverUserInfo.get("id");
        String profileImageUrl = (String) naverUserInfo.get("profile_image");
//        String ageRange = (String) naverUserInfo.get("age");
        String gender = (String) naverUserInfo.get("gender");

        String phoneNumber = (String) naverUserInfo.get("mobile");
        String birthyear = (String) naverUserInfo.get("birthyear");
        // kakao(male, female) 과 naver(M, W) 불일치로 변환
        gender = convertNaverGender(gender);
        String email = (String) naverUserInfo.get("email");

        if (email != null) {
            email = email.trim();
            if (email.isEmpty()) {
                email = null;
            }
        }
        String name = (String) naverUserInfo.get("name");
        //String token = (String) naverUserInfo.get("token");
        log.debug("socialOauthId : {}, profileImageUril : {}, gender : {}, phoneNumber : {}, birthyear : {},email : {}, name : {}", socialOauthId, profileImageUrl, gender, phoneNumber, birthyear, email, name);

        // 카카오 회원가입 유무에 따른 처리 (socialOauthId, email)
        Optional<NaverOauth> optionalNaverOauth = naverOauthRepository.findBySocialOauthIdAndOauthEmail(socialOauthId, email);
        log.debug("optionalNaverOauth selected");

        // 기 가입된 유저 토큰 반환 처리
        if(optionalNaverOauth.isPresent()){
            log.debug("Already Joined Naver Oauth User");

            // 토큰 발급을 위한 userId, userRole 획득
            NaverOauth naverOauth = optionalNaverOauth.get();
            //if (naverOauth.getUser().isDeleted()) {
                //throw new IllegalArgumentException("Deleted User");
            //}
            String userId = naverOauth.getUser().getId();
            String userRole = naverOauth.getUser().getRole().name();
            String userType = naverOauth.getUser().getType().name();
            log.debug("userId : {}, userRole : {}", userId, userRole);

            // jwt 토큰 발급 및 쿠키 저장
            String accessToken = jwtUtil.generateAccessToken(userId, userRole, userType);
            String refreshToken = jwtUtil.generateRefreshToken(userId, userRole, userType);
            log.debug("jwt generated for existing naver user");
            saveRefreshTokenWithTtl(userId, refreshToken);

            return new JwtResponse(accessToken, refreshToken);

        }else {
            log.debug("No linked naver user. creating new user");
            // provider와 무관하게 User 테이블 기준 이메일 중복 가입을 차단한다.
            if (email != null && userRepository.existsByEmail(email)) {
                log.debug("naver signup rejected. duplicated email found in user table. email={}", email);
                throw new IllegalArgumentException("Email Already Exist");
            }
            // Email 기반 로컬 회원가입 여부 조회
            // 로컬 회원이 있다면 KakaoOauth에 매핑
            // 추후 로직 추가

            // 로컬 회원가입 미가입 신규 회원가입 처리
            User newUser = User.builder()
                    .email(email)
                    .name(name)
                    .profileImageUrl(profileImageUrl)
                    .gender(gender)
                    .birthyear(birthyear)
                    .phoneNumber(phoneNumber)
                    .type(UserType.PERSONAL)
                    .naverOauthConnected(true)
                    .build();
            log.debug("newUser is built");

            // 신규 유저 저장
            User user = userRepository.save(newUser);
            log.debug("user saved");

            // KakaoOauth 빌드
            NaverOauth naverOauth = NaverOauth.builder()
                    .user(user)
                    .socialOauthId(socialOauthId)
                    .oauthEmail(email)
                    .build();
            log.debug("naverOauth is built");

            // kakaoOauth 저장
            naverOauthRepository.save(naverOauth);
            log.debug("naverOauth is saved");

            // jwt token 발급
            String userId = user.getId();
            String userRole = user.getRole().name();
            String userType = user.getType().name();
            log.debug("userId : {}. userRole : {}, userType : {}", userId, userRole, userType);

            // jwt 토큰 발급 및 쿠키 저장
            String accessToken = jwtUtil.generateAccessToken(userId, userRole, userType);
            String refreshToken = jwtUtil.generateRefreshToken(userId, userRole, userType);
            log.debug("jwt generated for new naver user");
            saveRefreshTokenWithTtl(userId, refreshToken);

            return new JwtResponse(accessToken, refreshToken);
        }
    }

    public JwtResponse naverLogin(MobileNaverLoginRequestDto dto, HttpServletResponse httpServletResponse) {
        log.debug("naverLogin with accessToken started");
        log.debug("naver accessToken exists: {}", dto.accessToken() != null && !dto.accessToken().isBlank());

        Map<String, Object> naverUserInfo = fetchUserInfoFromNaver(dto.accessToken());
        log.debug("naver user info fetched successfully");
        log.debug("naver user info map is null: {}", naverUserInfo == null);

        String socialOauthId = (String) naverUserInfo.get("id");
        String profileImageUrl = (String) naverUserInfo.get("profile_image");
        String gender = (String) naverUserInfo.get("gender");
        String phoneNumber = (String) naverUserInfo.get("mobile");
        String birthyear = (String) naverUserInfo.get("birthyear");
        gender = convertNaverGender(gender);
        String email = (String) naverUserInfo.get("email");
        if (email != null) {
            email = email.trim();
            if (email.isEmpty()) {
                email = null;
            }
        }
        String name = (String) naverUserInfo.get("name");
        String token = (String) naverUserInfo.get("token");
        log.debug("socialOauthId : {}, profileImageUril : {}, gender : {}, phoneNumber : {}, birthyear : {},email : {}, name : {}, token : {}",
                socialOauthId, profileImageUrl, gender, phoneNumber, birthyear, email, name, token);

        Optional<NaverOauth> optionalNaverOauth = naverOauthRepository.findBySocialOauthIdAndOauthEmail(socialOauthId, email);
        log.debug("optionalNaverOauth selected");
        log.debug("optionalNaverOauth present: {}", optionalNaverOauth.isPresent());
        log.debug("optionalNaverOauth present: {}", optionalNaverOauth.isPresent());

        if (optionalNaverOauth.isPresent()) {
            log.debug("Already Joined Naver Oauth User");

            NaverOauth naverOauth = optionalNaverOauth.get();
            //if (naverOauth.getUser().isDeleted()) {
                //throw new IllegalArgumentException("Deleted User");
            //}
            String userId = naverOauth.getUser().getId();
            String userRole = naverOauth.getUser().getRole().name();
            String userType = naverOauth.getUser().getType().name();
            log.debug("userId : {}, userRole : {}", userId, userRole);

            String accessToken = jwtUtil.generateAccessToken(userId, userRole, userType);
            String refreshToken = jwtUtil.generateRefreshToken(userId, userRole, userType);
            log.debug("jwt generated for existing naver user");
            saveRefreshTokenWithTtl(userId, refreshToken);
            return new JwtResponse(accessToken, refreshToken);
        }

        log.debug("No linked naver user. creating new user");
        // provider와 무관하게 User 테이블 기준 이메일 중복 가입을 차단한다.
        if (email != null && userRepository.existsByEmail(email)) {
            log.debug("mobile naver signup rejected. duplicated email found in user table. email={}", email);
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
                .naverOauthConnected(true)
                .build();
        log.debug("newUser is built");

        User user = userRepository.save(newUser);
        log.debug("user saved");

        NaverOauth naverOauth = NaverOauth.builder()
                .user(user)
                .socialOauthId(socialOauthId)
                .oauthEmail(email)
                .build();
        log.debug("naverOauth is built");

        naverOauthRepository.save(naverOauth);
        log.debug("naverOauth is saved");

        String userId = user.getId();
        String userRole = user.getRole().name();
        String userType = user.getType().name();
        log.debug("userId : {}. userRole : {}, userType : {}", userId, userRole, userType);

        String accessToken = jwtUtil.generateAccessToken(userId, userRole, userType);
        String refreshToken = jwtUtil.generateRefreshToken(userId, userRole, userType);
        log.debug("jwt generated for new naver user");
        saveRefreshTokenWithTtl(userId, refreshToken);
        return new JwtResponse(accessToken, refreshToken);
    }

    public String getNaverOauthToken(String code, String state, RestTemplate restTemplate){
        log.debug("naver get token started");
        log.debug("naver get token params. codeExists={}, stateExists={}, redirectUri={}",
                code != null && !code.isBlank(),
                state != null && !state.isBlank(),
                redirectUri);
        String tokenUrl = "https://nid.naver.com/oauth2.0/token";
        String tokenRequest = String.format(
                "%s?grant_type=authorization_code&client_id=%s&client_secret=%s&code=%s&state=%s",
                tokenUrl, clientId, clientSecret, code, state
        );

        ResponseEntity<Map> tokenResponse = restTemplate.getForEntity(tokenRequest, Map.class);
        log.debug("naver token response status: {}", tokenResponse.getStatusCode());
        log.debug("naver token body exists: {}", tokenResponse.getBody() != null);
        return (String) tokenResponse.getBody().get("access_token");
    }

    public Map<String, Object> fetchUserInfoFromNaver(String accessToken){
        log.debug("naver fetchUserInfo started");
        log.debug("naver accessToken exists: {}", accessToken != null && !accessToken.isBlank());
        // Step 3: 사용자 정보 요청
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> profileResponse = restTemplate.exchange(
                "https://openapi.naver.com/v1/nid/me",
                HttpMethod.GET,
                request,
                Map.class
        );
        log.debug("naver user info response status: {}", profileResponse.getStatusCode());
        log.debug("naver user info body exists: {}", profileResponse.getBody() != null);

        Map<String, Object> responseBody = profileResponse.getBody();
        assert responseBody != null;
        return (Map<String, Object>) responseBody.get("response");
    }

    @org.springframework.transaction.annotation.Transactional
    public void withdraw(org.springframework.security.core.Authentication authentication, OauthWithdrawRequestDto dto) {
        String userId = userUtil.extractUserId(authentication);
        User user = userUtil.findUserById(userId);

        NaverOauth naverOauth = naverOauthRepository.findByUser_Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("Naver OAuth User Not Found"));

        if (!user.isNaverOauthConnected()) {
            throw new IllegalArgumentException("Naver OAuth Not Connected");
        }

        revokeToken(dto.accessToken());
        softDeleteUser(user);
        stringRedisTemplate.delete(jwtUtil.toRedisRefreshTokenKey(userId));
    }

    void revokeToken(String accessToken) {
        String requestUrl = UriComponentsBuilder
                .fromHttpUrl("https://nid.naver.com/oauth2.0/token")
                .queryParam("grant_type", "delete")
                .queryParam("client_id", clientId)
                .queryParam("client_secret", clientSecret)
                .queryParam("access_token", accessToken)
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        ResponseEntity<Map> response = restTemplate.getForEntity(requestUrl, Map.class);
        Map body = response.getBody();
        if (body == null || !"success".equals(body.get("result"))) {
            throw new IllegalStateException("Naver Token Revocation Failed");
        }
    }

    private String convertNaverGender(String gender) {
        if (gender == null || gender.isBlank()) {
            return null;
        }
        return gender.equalsIgnoreCase("M") ? "male" : "female";
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

    private void softDeleteUser(User user) {
        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
    }
}

